/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.service.rest.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import it.water.core.api.security.Authenticable;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.security.model.principal.RolePrincipal;
import it.water.service.rest.api.options.JwtSecurityOptions;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.service.rest.api.security.jwt.TokenRevocationStore;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

/**
 * @Author Aristide Cittadino
 * Implementation of JwtTokenService with Nimbus: mvn:com.nimbusds/nimbus-jose-jwt
 */
@FrameworkComponent
public class NimbusJwtTokenService implements JwtTokenService {
    private static Logger log = LoggerFactory.getLogger(NimbusJwtTokenService.class);
    @Inject
    @Setter
    private EncryptionUtil encryptionUtil;

    @Inject
    @Setter
    private JwtSecurityOptions jwtSecurityOptions;

    @Inject
    @Setter
    private TokenRevocationStore tokenRevocationStore;

    //simple thread-safe cache of the JWKS public key, keyed by the JWKS URL it was loaded from.
    //avoids hitting the JWKS endpoint on every validation. Refreshed automatically if the URL changes.
    //url and key are published together as a single immutable holder, so a reader can never see a key
    //paired with the wrong URL.
    private final java.util.concurrent.atomic.AtomicReference<JwksCacheEntry> jwksCache = new java.util.concurrent.atomic.AtomicReference<>();

    private static final class JwksCacheEntry {
        private final String url;
        private final RSAPublicKey key;

        JwksCacheEntry(String url, RSAPublicKey key) {
            this.url = url;
            this.key = key;
        }
    }

    /**
     * Generates jwt token from an authenticable.
     * NOTE: if the jwt is encrypted, it encodes more information like roles and admin profilo (if the user is admin).
     * This information accelerates the validation process since roles and admin profile is encoded inside the jwt
     * and there's no need to retrieve this info from the database.
     *
     * @param authenticable
     * @return
     */
    @Override
    public String generateJwtToken(Authenticable authenticable) {
        SignedJWT signedJWT = generateSignedJWT(authenticable);
        return signedJWT.serialize();
    }

    /**
     * Validates the JWT Token from the inner key or from the JWS URL depending on property:
     * it.water.service.rest.security.jwt.validate.by.jws
     *
     * @param jwtStr
     * @return
     */
    @Override
    public boolean validateToken(List<String> validIssuers, String jwtStr) {
        if (jwtStr == null || jwtStr.isBlank())
            return false;
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwtStr);
            return verifySignature(jwtStr) && validateIssuers(validIssuers, signedJWT) && validateAudience(validIssuers, signedJWT) && !isRevoked(signedJWT);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Checks the revocation denylist by jti. A token without a jti (e.g. legacy) is treated as
     * not-revoked so validation does not NPE on it.
     *
     * @param signedJWT parsed token
     * @return true if the token's jti is present in the revocation store
     */
    private boolean isRevoked(SignedJWT signedJWT) {
        if (tokenRevocationStore == null)
            return false;
        try {
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            return jti != null && tokenRevocationStore.isRevoked(jti);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean verifySignature(String jwtStr) {
        try {
            RSAPublicKey publicKey = null;
            boolean verified = false;
            if (jwtSecurityOptions.validateJwtWithJwsUrl()) {
                String keyId = jwtSecurityOptions.jwtKeyId();
                publicKey = retrievePublicKeyFromJWSUrl(keyId);
                if (publicKey == null)
                    throw new WaterRuntimeException("No Public Key found matching {} Key ID" + keyId);
            } else {
                publicKey = (RSAPublicKey) encryptionUtil.getServerKeyPair().getPublic();
            }
            SignedJWT signedJWT = getSignedJWTToken(jwtStr);
            if (signedJWT != null) {
                //algorithm pinning: only RS256 is accepted on the verify path. This rejects "alg"
                //downgrade/confusion attacks (e.g. forged "none" or HS256 headers) before any
                //cryptographic verification is attempted.
                if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                    log.warn("Rejecting JWT: unexpected signing algorithm {}", signedJWT.getHeader().getAlgorithm());
                    return false;
                }
                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                verified = signedJWT.verify(verifier) && validateExpiration(signedJWT) && validateNotBefore(signedJWT);
                return verified;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Validates the not-before (nbf) claim: a token whose nbf is in the future (beyond the
     * configured clock-skew leeway) is rejected. A token without nbf (e.g. legacy) is accepted
     * so validation never NPEs and stays backward compatible.
     *
     * @param jwtToken parsed token
     * @return true if the token is not used before its nbf time
     */
    private boolean validateNotBefore(SignedJWT jwtToken) {
        try {
            Date notBefore = jwtToken.getJWTClaimsSet().getNotBeforeTime();
            if (notBefore == null)
                return true; //legacy token without nbf: do not reject on nbf alone
            long skewMillis = jwtSecurityOptions.jwtClockSkewSeconds() * 1000L;
            long nowWithSkew = Instant.now().toEpochMilli() + skewMillis;
            return notBefore.getTime() <= nowWithSkew;
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates the audience (aud) claim. Every token issued by this service now carries an aud
     * (configured audience, or the issuer as default). On validation:
     * <ul>
     *     <li>if a token carries no aud (legacy), it is accepted (do not reject on aud alone);</li>
     *     <li>if an explicit audience is configured, the token's aud must contain it;</li>
     *     <li>otherwise (default audience = issuer) the token's aud must intersect the valid issuers,
     *     which is symmetric with how the audience is generated.</li>
     * </ul>
     *
     * @param validIssuers list of valid issuers (used as the default expected audience set)
     * @param jwtToken     parsed token
     * @return true if the audience is acceptable
     */
    private boolean validateAudience(List<String> validIssuers, SignedJWT jwtToken) {
        try {
            List<String> audience = jwtToken.getJWTClaimsSet().getAudience();
            if (audience == null || audience.isEmpty()) {
                //legacy token without aud: accept so pre-existing tokens keep working
                return true;
            }
            String configured = jwtSecurityOptions.jwtAudience();
            if (configured != null && !configured.isBlank())
                return audience.contains(configured);
            //default audience equals the issuer: accept when aud intersects the valid issuers
            return validIssuers != null && !Collections.disjoint(audience, validIssuers);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @param validIssuers list of valid possible issuers
     * @param jwtToken
     * @return
     */
    private boolean validateIssuers(List<String> validIssuers, SignedJWT jwtToken) {
        try {
            return validIssuers.contains(jwtToken.getJWTClaimsSet().getIssuer());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @param jwtToken
     * @return true if token is still valid
     */
    private boolean validateExpiration(SignedJWT jwtToken) {
        try {
            Date expirationTime = jwtToken.getJWTClaimsSet().getExpirationTime();
            return !expirationTime.before(new Date(Instant.now().toEpochMilli()));
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates different types of Principals (Principal and RolePrincipal) so the system can act looking at the assigned principals to the logged user
     *
     * @param jwtTokenStr
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<Principal> getPrincipals(String jwtTokenStr) {
        if (!verifySignature(jwtTokenStr))
            return Collections.emptySet();

        try {
            Set<Principal> principals = new HashSet<>();
            SignedJWT jwtToken = getSignedJWTToken(jwtTokenStr);
            if (jwtToken != null) {
                JWTClaimsSet claimsSet = jwtToken.getJWTClaimsSet();
                List<String> rolesNames = (List<String>) claimsSet.getClaim(JWTConstants.JWT_CLAIM_ROLES);
                String user = claimsSet.getSubject();
                boolean isAdmin = claimsSet.getBooleanClaim(JWTConstants.JWT_CLAIM_IS_ADMIN);
                long loggedEntityId = claimsSet.getLongClaim(JWTConstants.JWT_CLAIM_LOGGED_ENTITY_ID);
                Long companyId = claimsSet.getLongClaim(JWTConstants.JWT_CLAIM_COMPANY_ID);
                String impersonatedBy = claimsSet.getStringClaim(JWTConstants.JWT_CLAIM_IMPERSONATED_BY);
                if (rolesNames != null && !rolesNames.isEmpty())
                    rolesNames.forEach(role -> principals.add(new RolePrincipal(role)));
                if (user != null && !user.isBlank()) {
                    principals.add(new it.water.core.security.model.principal.UserPrincipal(user, isAdmin, loggedEntityId, claimsSet.getSubject(), companyId, impersonatedBy));
                }
                return principals;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptySet();
    }

    /**
     * Revokes a raw token by extracting its jti and expiration and storing them in the revocation
     * denylist until natural expiry. Idempotent and safe: unparseable tokens or tokens with no jti
     * are silently ignored so logout never fails.
     *
     * @param token raw (signed) JWT string
     */
    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank() || tokenRevocationStore == null)
            return;
        try {
            SignedJWT signedJWT = getSignedJWTToken(token);
            if (signedJWT == null)
                return;
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            String jti = claimsSet.getJWTID();
            if (jti == null || jti.isBlank())
                return;
            Date expiration = claimsSet.getExpirationTime();
            long expMillis = expiration != null ? expiration.getTime() : Long.MAX_VALUE;
            tokenRevocationStore.revoke(jti, expMillis);
        } catch (Exception e) {
            //logout must be idempotent/safe: never surface a parse error to the caller
            log.warn("Could not revoke token: {}", e.getMessage());
        }
    }

    @Override
    public String getJWK() {
        JWK jwk = new RSAKey.Builder((RSAPublicKey) encryptionUtil.getServerKeyPair().getPublic())
                .algorithm(JWSAlgorithm.RS256)
                .keyID(jwtSecurityOptions.jwtKeyId())
                .build();
        return jwk.toPublicJWK().toJSONString();
    }

    /**
     * Generates signed (eventually encrypted) JWT
     *
     * @param authenticable
     * @return
     */
    private SignedJWT generateSignedJWT(Authenticable authenticable) {
        RSAPrivateKey privateKey = (RSAPrivateKey) encryptionUtil.getServerKeyPair().getPrivate();
        JWTClaimsSet jwtClaimsSet = generateClaims(authenticable);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(getKeyId())
                .build();
        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        JWSSigner signer = new RSASSASigner(privateKey);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new WaterRuntimeException(e.getMessage());
        }
        return signedJWT;
    }

    /**
     * Generates claims from the authenticable
     *
     * @param authenticable
     * @return
     */
    private JWTClaimsSet generateClaims(Authenticable authenticable) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(authenticable.getScreenName());
        long nowMillis = Instant.now().toEpochMilli();
        long jwtTokenDuration = jwtSecurityOptions.jwtTokenDurationMillis();
        long expirationTime = nowMillis + jwtTokenDuration;
        Date issueDate = new Date(nowMillis);
        builder.expirationTime(Date.from(Instant.ofEpochMilli(expirationTime)));
        //unique token id (jti) so individual tokens can be revoked on logout, plus issued-at (iat)
        builder.jwtID(UUID.randomUUID().toString());
        builder.issueTime(issueDate);
        //not-before (nbf) equals issue time: the token is not valid before it was minted
        builder.notBeforeTime(issueDate);
        //default token encryption is enabled, so this information is not visibile to the end user
        //if you want to remove jwt token encryption please consider this aspect
        Set<String> roleNames = new HashSet<>();
        authenticable.getRoles().forEach(role -> roleNames.add(role.getName()));
        builder.claim(JWTConstants.JWT_CLAIM_ROLES, roleNames)
                .claim(JWTConstants.JWT_CLAIM_IS_ADMIN, authenticable.isAdmin())
                .claim(JWTConstants.JWT_CLAIM_LOGGED_ENTITY_ID, authenticable.getLoggedEntityId());
        if (authenticable.getActiveCompanyId() != null)
            builder.claim(JWTConstants.JWT_CLAIM_COMPANY_ID, authenticable.getActiveCompanyId());
        //impersonation marker: emitted ONLY for impersonation tokens so genuine-login tokens stay byte-identical
        if (authenticable.getImpersonatedBy() != null)
            builder.claim(JWTConstants.JWT_CLAIM_IMPERSONATED_BY, authenticable.getImpersonatedBy());
        builder.issuer(authenticable.getIssuer());
        //audience (aud): use the configured audience when present, otherwise default to the issuer so
        //the claim is never empty and generation/validation stay symmetric.
        builder.audience(resolveExpectedAudience(authenticable.getIssuer()));
        return builder.build();
    }

    /**
     * Resolves the audience to embed in (and to expect from) a token. Uses the configured
     * audience (water.rest.security.jwt.audience) when set; otherwise defaults to the issuer.
     *
     * @param issuer issuer of the token (fallback audience)
     * @return the expected audience value, never null/blank as long as issuer is non-blank
     */
    private String resolveExpectedAudience(String issuer) {
        String configured = jwtSecurityOptions.jwtAudience();
        if (configured != null && !configured.isBlank())
            return configured;
        return issuer;
    }


    /**
     * Decrypt encrypted jwt token
     *
     * @param jwtStr
     * @return
     */
    private SignedJWT getSignedJWTToken(String jwtStr) {
        try {
            JWT jwt = JWTParser.parse(jwtStr);
            if (jwt instanceof SignedJWT signedJWT) {
                return signedJWT;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieve jwt token from the configured jws url from property: it.water.service.rest.security.jwt.jws.url
     * <p>
     * //@param signedJWT
     *
     * @return
     */
    private RSAPublicKey retrievePublicKeyFromJWSUrl(String keyId) {
        String jwsUrl = jwtSecurityOptions.jwsURL();
        //enforce https for the JWKS endpoint so the public key cannot be swapped by a network MITM.
        //plaintext http is only tolerated in test mode (spring/localhost test runners).
        enforceHttpsJwksUrl(jwsUrl);
        //serve from cache when the URL is unchanged: avoids hitting the JWKS endpoint on every validation
        JwksCacheEntry cached = jwksCache.get();
        if (cached != null && jwsUrl != null && jwsUrl.equals(cached.url)) {
            return cached.key;
        }
        try {
            URL jwksURL = new URL(jwsUrl);
            JWKSet jwkSet = JWKSet.load(jwksURL);
            // Find the JWK that matches the 'kid' from the JWS token's header
            JWK matchingKey = jwkSet.getKeyByKeyId(keyId);
            if (matchingKey != null) {
                RSAPublicKey publicKey = matchingKey.toRSAKey().toRSAPublicKey();
                //publish url and key together as one immutable holder
                jwksCache.set(new JwksCacheEntry(jwsUrl, publicKey));
                return publicKey;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Rejects a plaintext-http JWKS URL with a clear exception, unless the framework is running in
     * test mode (water.testMode=true), where plaintext http localhost endpoints are tolerated.
     * Only the network plaintext scheme {@code http://} is refused (MITM-swappable key, see #16);
     * non-network local schemes ({@code file:}, {@code classpath:}) and {@code https://} are allowed.
     *
     * @param jwsUrl configured JWKS URL
     */
    private void enforceHttpsJwksUrl(String jwsUrl) {
        if (jwsUrl == null || jwsUrl.isBlank())
            return;
        boolean isPlaintextHttp = jwsUrl.toLowerCase(Locale.ROOT).startsWith("http://");
        if (isPlaintextHttp && !jwtSecurityOptions.testMode()) {
            throw new WaterRuntimeException("JWKS URL must not use plaintext http; refusing to fetch the JWT public key over a MITM-swappable channel: " + jwsUrl);
        }
    }

    /**
     * Generates certificate header key id starting from private key
     *
     * @return
     */
    private String getKeyId() {
        return Hex.toHexString(jwtSecurityOptions.jwtKeyId().getBytes(StandardCharsets.UTF_8));
    }
}
