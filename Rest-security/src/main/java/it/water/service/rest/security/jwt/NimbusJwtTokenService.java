/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

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
import it.water.core.api.entity.Authenticable;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.security.model.principal.RolePrincipal;
import it.water.service.rest.api.options.JwtSecurityOptions;
import it.water.service.rest.api.security.jwt.JwtTokenService;
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
        try {
            return verifySignature(jwtStr) && validateIssuers(validIssuers, SignedJWT.parse(jwtStr));
        } catch (ParseException e) {
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
            if(signedJWT != null) {
                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                verified = signedJWT.verify(verifier) && validateExpiration(signedJWT);
                return verified;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
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
                if (rolesNames != null && !rolesNames.isEmpty())
                    rolesNames.forEach(role -> principals.add(new RolePrincipal(role)));
                if (user != null && !user.isBlank()) {
                    principals.add(new it.water.core.security.model.principal.UserPrincipal(user, isAdmin, loggedEntityId, claimsSet.getSubject()));
                }
                return principals;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptySet();
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
        long jwtTokenDuration = jwtSecurityOptions.jwtTokenDurationMillis();
        long expirationTime = Instant.now().toEpochMilli() + jwtTokenDuration;
        builder.expirationTime(Date.from(Instant.ofEpochMilli(expirationTime)));
        //default token encryption is enabled, so this information is not visibile to the end user
        //if you want to remove jwt token encryption please consider this aspect
        builder.claim(JWTConstants.JWT_CLAIM_ROLES, authenticable.getRoles())
                .claim(JWTConstants.JWT_CLAIM_IS_ADMIN, authenticable.isAdmin())
                .claim(JWTConstants.JWT_CLAIM_LOGGED_ENTITY_ID, authenticable.getId());
        builder.issuer(authenticable.getClass().getName());
        return builder.build();
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
            if (jwt instanceof SignedJWT) {
                return (SignedJWT) jwt;
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
        try {
            URL jwksURL = new URL(jwtSecurityOptions.jwsURL());
            JWKSet jwkSet = JWKSet.load(jwksURL);
            // Find the JWK that matches the 'kid' from the JWS token's header
            JWK matchingKey = jwkSet.getKeyByKeyId(keyId);
            if (matchingKey != null) {
                return matchingKey.toRSAKey().toRSAPublicKey();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
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
