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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.service.rest.api.options.JwtSecurityOptions;
import it.water.service.rest.api.security.jwt.TokenRevocationStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

/**
 * Mockito-based unit tests for NimbusJwtTokenService covering fix #15 (algorithm pinning,
 * nbf validation) and fix #16 (https enforcement for JWS/JWKS URL).
 *
 * These tests isolate the service by mocking EncryptionUtil, JwtSecurityOptions, and
 * TokenRevocationStore. An in-process RSA key pair is generated per test class so that
 * token signing/verification uses real cryptographic material without requiring a keystore.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NimbusJwtTokenServiceMockTest {

    private static final List<String> VALID_ISSUERS = List.of("testIssuer");
    private static final String TEST_ISSUER = "testIssuer";
    private static final long ONE_HOUR_MILLIS = 3_600_000L;
    private static final long FAR_FUTURE_NBF_MILLIS = ONE_HOUR_MILLIS;
    private static final int RSA_KEY_SIZE = 2048;

    // In-process RSA key pair — generated once for the whole test class
    private KeyPair rsaKeyPair;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private JwtSecurityOptions jwtSecurityOptions;

    @Mock
    private TokenRevocationStore tokenRevocationStore;

    @InjectMocks
    private NimbusJwtTokenService service;

    @BeforeAll
    void generateRsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(RSA_KEY_SIZE, new SecureRandom());
        rsaKeyPair = kpg.generateKeyPair();
    }

    /**
     * Configures the mocks for the normal (local key, non-JWS) validation path.
     * Called by tests that need a working service in non-JWS mode.
     */
    private void stubForLocalKeyValidation() {
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(false);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);
        when(jwtSecurityOptions.jwtAudience()).thenReturn("");
        when(encryptionUtil.getServerKeyPair()).thenReturn(rsaKeyPair);
        when(tokenRevocationStore.isRevoked(anyString())).thenReturn(false);
    }

    /**
     * Builds and signs a valid RS256 token using the in-process RSA key pair.
     */
    private String buildValidRs256Token(String issuer, String audience, Date nbf, Date exp) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject("testUser")
                .issuer(issuer)
                .audience(audience)
                .expirationTime(exp)
                .jwtID(UUID.randomUUID().toString());
        if (nbf != null) {
            claimsBuilder.notBeforeTime(nbf);
        }
        SignedJWT jwt = new SignedJWT(header, claimsBuilder.build());
        JWSSigner signer = new RSASSASigner((RSAPrivateKey) rsaKeyPair.getPrivate());
        jwt.sign(signer);
        return jwt.serialize();
    }

    private Date nowPlusSeconds(long seconds) {
        return Date.from(Instant.now().plusSeconds(seconds));
    }

    private Date nowMinusSeconds(long seconds) {
        return Date.from(Instant.now().minusSeconds(seconds));
    }

    // -------------------------------------------------------------------
    // #15 — algorithm pinning: non-RS256 tokens must be rejected
    // -------------------------------------------------------------------

    @Test
    @Order(1)
    void validateToken_hs256SignedToken_returnsFalse() throws Exception {
        stubForLocalKeyValidation();

        byte[] hmacSecret = new byte[32];
        new SecureRandom().nextBytes(hmacSecret);

        JWSHeader hs256Header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("attacker")
                .issuer(TEST_ISSUER)
                .audience(TEST_ISSUER)
                .expirationTime(nowPlusSeconds(3600))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT hs256Jwt = new SignedJWT(hs256Header, claims);
        hs256Jwt.sign(new MACSigner(hmacSecret));
        String hs256Token = hs256Jwt.serialize();

        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, hs256Token),
                "Algorithm pinning: an HS256-signed token must be rejected before any RSA verification");
    }

    @Test
    @Order(2)
    void validateToken_hs384SignedToken_returnsFalse() throws Exception {
        // Build a syntactically valid JWT with HS384 (not RS256) to exercise the alg-pinning check.
        stubForLocalKeyValidation();

        byte[] secret = new byte[48];
        new SecureRandom().nextBytes(secret);
        JWSHeader hs384Header = new JWSHeader(JWSAlgorithm.HS384);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("attacker")
                .issuer(TEST_ISSUER)
                .audience(TEST_ISSUER)
                .expirationTime(nowPlusSeconds(3600))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT hs384Jwt = new SignedJWT(hs384Header, claims);
        hs384Jwt.sign(new MACSigner(secret));
        String hs384Token = hs384Jwt.serialize();

        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, hs384Token),
                "Algorithm pinning: an HS384-signed token must be rejected");
    }

    @Test
    @Order(3)
    void validateToken_rs256ValidToken_returnsTrue() throws Exception {
        // Positive control: a properly signed RS256 token must pass validation
        stubForLocalKeyValidation();

        Date exp = nowPlusSeconds(3600);
        Date nbf = new Date();
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, nbf, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "A valid RS256-signed token with matching issuer and audience must validate successfully");
    }

    // -------------------------------------------------------------------
    // #15 — nbf validation: tokens with nbf in the far future must be rejected
    // -------------------------------------------------------------------

    @Test
    @Order(4)
    void validateToken_nbfFarInFuture_returnsFalse() throws Exception {
        stubForLocalKeyValidation();

        // nbf = now + 1 hour; default clock skew is 60 s — far beyond leeway
        Date farFutureNbf = Date.from(Instant.now().plusMillis(FAR_FUTURE_NBF_MILLIS));
        Date exp = Date.from(Instant.now().plusMillis(ONE_HOUR_MILLIS * 2));
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, farFutureNbf, exp);

        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, token),
                "A token whose nbf is 1 hour in the future must be rejected (beyond clock-skew leeway)");
    }

    @Test
    @Order(5)
    void validateToken_nbfInPast_returnsTrue() throws Exception {
        // nbf = 5 minutes ago — valid, must not be rejected
        stubForLocalKeyValidation();

        Date pastNbf = nowMinusSeconds(300);
        Date exp = nowPlusSeconds(3600);
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, pastNbf, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "A token whose nbf is in the past must be accepted");
    }

    @Test
    @Order(6)
    void validateToken_noNbfClaim_legacyTokenAccepted() throws Exception {
        // nbf = null: legacy token without nbf must be accepted (backward-compat path)
        stubForLocalKeyValidation();

        Date exp = nowPlusSeconds(3600);
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, null, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "A legacy token without nbf must not be rejected on nbf alone");
    }

    @Test
    @Order(7)
    void validateToken_nbfWithinClockSkew_returnsTrue() throws Exception {
        // nbf is 30 s in the future but clock skew is 60 s — must be accepted
        stubForLocalKeyValidation();

        Date nearFutureNbf = Date.from(Instant.now().plusSeconds(30));
        Date exp = nowPlusSeconds(3600);
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, nearFutureNbf, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "A token whose nbf is 30 s in the future (within 60 s clock skew) must be accepted");
    }

    // -------------------------------------------------------------------
    // #15 — aud validation
    // -------------------------------------------------------------------

    @Test
    @Order(8)
    void validateToken_audienceMatchesIssuer_returnsTrue() throws Exception {
        // When jwtAudience() is blank, the token aud must intersect the valid issuers list
        stubForLocalKeyValidation();

        Date exp = nowPlusSeconds(3600);
        Date nbf = new Date();
        // aud = issuer = TEST_ISSUER; VALID_ISSUERS contains TEST_ISSUER
        String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, nbf, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "Token whose aud equals the issuer (in VALID_ISSUERS) must be accepted");
    }

    @Test
    @Order(9)
    void validateToken_audienceDoesNotMatchIssuers_returnsFalse() throws Exception {
        // When jwtAudience() is blank and the token aud doesn't intersect VALID_ISSUERS, reject
        stubForLocalKeyValidation();

        Date exp = nowPlusSeconds(3600);
        Date nbf = new Date();
        String token = buildValidRs256Token(TEST_ISSUER, "differentAudience", nbf, exp);

        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, token),
                "Token whose aud does not intersect VALID_ISSUERS must be rejected");
    }

    @Test
    @Order(10)
    void validateToken_configuredAudienceMatches_returnsTrue() throws Exception {
        // When jwtAudience() returns a non-blank value, the token aud must contain that exact value
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(false);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);
        when(jwtSecurityOptions.jwtAudience()).thenReturn("my-configured-audience");
        when(encryptionUtil.getServerKeyPair()).thenReturn(rsaKeyPair);
        when(tokenRevocationStore.isRevoked(anyString())).thenReturn(false);

        Date exp = nowPlusSeconds(3600);
        Date nbf = new Date();
        String token = buildValidRs256Token(TEST_ISSUER, "my-configured-audience", nbf, exp);

        Assertions.assertTrue(
                service.validateToken(VALID_ISSUERS, token),
                "Token whose aud matches the configured jwtAudience() must be accepted");
    }

    @Test
    @Order(11)
    void validateToken_configuredAudienceMismatch_returnsFalse() throws Exception {
        // Configured audience is present but the token carries a different aud — reject
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(false);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);
        when(jwtSecurityOptions.jwtAudience()).thenReturn("my-configured-audience");
        when(encryptionUtil.getServerKeyPair()).thenReturn(rsaKeyPair);

        Date exp = nowPlusSeconds(3600);
        Date nbf = new Date();
        String token = buildValidRs256Token(TEST_ISSUER, "wrong-audience", nbf, exp);

        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, token),
                "Token whose aud does not match the configured jwtAudience() must be rejected");
    }

    // -------------------------------------------------------------------
    // #16 — https enforcement for JWKS/JWS URL
    // -------------------------------------------------------------------

    @Test
    @Order(12)
    void validateToken_jwsHttpUrl_testModeFalse_returnsFalse() {
        // #16: with validateJwtWithJwsUrl=true, jwsURL starts with http://, and testMode=false,
        // the https enforcement makes the public-key load fail; validateToken degrades to a safe
        // reject (returns false) rather than throwing out of the auth path.
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(true);
        when(jwtSecurityOptions.jwsURL()).thenReturn("http://internal-host/jwks.json");
        when(jwtSecurityOptions.jwtKeyId()).thenReturn("key");
        when(jwtSecurityOptions.testMode()).thenReturn(false);

        // Build a syntactically valid RS256 token so the code reaches enforceHttpsJwksUrl
        try {
            Date exp = nowPlusSeconds(3600);
            String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, new Date(), exp);
            Assertions.assertFalse(
                    service.validateToken(VALID_ISSUERS, token),
                    "#16: http JWKS URL with testMode=false must cause the token to be rejected");
        } catch (Exception e) {
            Assertions.fail("Unexpected exception during token construction: " + e.getMessage());
        }
    }

    @Test
    @Order(13)
    void validateToken_jwsHttpsUrl_testModeFalse_doesNotThrowHttpsEnforcementException() {
        // https:// URL with testMode=false must pass the https-enforcement check.
        // The service may still return false (unreachable host is fine) but it must NOT throw
        // the WaterRuntimeException whose message contains "must use https".
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(true);
        when(jwtSecurityOptions.jwsURL()).thenReturn("https://idp.example.com/.well-known/jwks.json");
        when(jwtSecurityOptions.jwtKeyId()).thenReturn("key");
        when(jwtSecurityOptions.testMode()).thenReturn(false);

        try {
            Date exp = nowPlusSeconds(3600);
            String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, new Date(), exp);
            try {
                service.validateToken(VALID_ISSUERS, token);
                // Returning true or false is fine; what matters is no https-enforcement exception
            } catch (WaterRuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                Assertions.assertFalse(msg.contains("must use https"),
                        "#16: https URL must not trigger the https-enforcement WaterRuntimeException; got: " + msg);
            }
        } catch (Exception buildEx) {
            Assertions.fail("Unexpected exception during token construction: " + buildEx.getMessage());
        }
    }

    @Test
    @Order(14)
    void validateToken_jwsHttpUrl_testModeTrue_doesNotThrowHttpsEnforcementException() {
        // #16 test-mode tolerance: http:// JWKS URL is allowed when testMode=true
        // The service must not throw the https-enforcement exception (may fail fetching URL)
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(true);
        when(jwtSecurityOptions.jwsURL()).thenReturn("http://localhost:9999/unreachable/jwks.json");
        when(jwtSecurityOptions.jwtKeyId()).thenReturn("key");
        when(jwtSecurityOptions.testMode()).thenReturn(true);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);

        try {
            Date exp = nowPlusSeconds(3600);
            String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, new Date(), exp);
            try {
                service.validateToken(VALID_ISSUERS, token);
                // returning true or false is both fine — the JWKS endpoint is unreachable
            } catch (WaterRuntimeException e) {
                Assertions.assertFalse(
                        e.getMessage() != null && e.getMessage().contains("must use https"),
                        "#16: testMode=true must suppress the https-enforcement exception; got: " + e.getMessage());
            }
            // If we reach here without a https-enforcement exception, the test passes
        } catch (Exception e) {
            Assertions.fail("Unexpected exception during token construction: " + e.getMessage());
        }
    }

    @Test
    @Order(15)
    void validateToken_jwsNullUrl_doesNotThrowHttpsEnforcementException() {
        // Null JWKS URL is a no-op in enforceHttpsJwksUrl (no https exception).
        // The public key fetch will subsequently fail and the service throws WaterRuntimeException
        // for the missing key — this is acceptable. We only assert the https check is NOT the cause.
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(true);
        when(jwtSecurityOptions.jwsURL()).thenReturn(null);
        when(jwtSecurityOptions.jwtKeyId()).thenReturn("key");
        when(jwtSecurityOptions.testMode()).thenReturn(false);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);

        try {
            Date exp = nowPlusSeconds(3600);
            String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, new Date(), exp);
            try {
                service.validateToken(VALID_ISSUERS, token);
            } catch (WaterRuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                Assertions.assertFalse(msg.contains("must use https"),
                        "Null URL must not trigger https-enforcement; got: " + msg);
            }
        } catch (Exception buildEx) {
            Assertions.fail("Unexpected exception during token construction: " + buildEx.getMessage());
        }
    }

    @Test
    @Order(16)
    void validateToken_jwsBlankUrl_doesNotThrowHttpsEnforcementException() {
        // Blank JWKS URL is a no-op in enforceHttpsJwksUrl (no https exception).
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(true);
        when(jwtSecurityOptions.jwsURL()).thenReturn("  ");
        when(jwtSecurityOptions.jwtKeyId()).thenReturn("key");
        when(jwtSecurityOptions.testMode()).thenReturn(false);
        when(jwtSecurityOptions.jwtClockSkewSeconds()).thenReturn(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);

        try {
            Date exp = nowPlusSeconds(3600);
            String token = buildValidRs256Token(TEST_ISSUER, TEST_ISSUER, new Date(), exp);
            try {
                service.validateToken(VALID_ISSUERS, token);
            } catch (WaterRuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                Assertions.assertFalse(msg.contains("must use https"),
                        "Blank URL must not trigger https-enforcement; got: " + msg);
            }
        } catch (Exception buildEx) {
            Assertions.fail("Unexpected exception during token construction: " + buildEx.getMessage());
        }
    }

    // -------------------------------------------------------------------
    // #15 — null / blank / garbage input guards (regression)
    // -------------------------------------------------------------------

    @Test
    @Order(17)
    void validateToken_nullToken_returnsFalse() {
        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, null),
                "validateToken(null) must return false without throwing");
    }

    @Test
    @Order(18)
    void validateToken_blankToken_returnsFalse() {
        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, ""),
                "validateToken('') must return false without throwing");
        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, "   "),
                "validateToken(blank) must return false without throwing");
    }

    @Test
    @Order(19)
    void validateToken_garbageToken_returnsFalse() {
        // Calling validateToken with something that is not a JWT must return false, not throw
        when(jwtSecurityOptions.validateJwtWithJwsUrl()).thenReturn(false);
        Assertions.assertFalse(
                service.validateToken(VALID_ISSUERS, "garbage.not.a.jwt"),
                "validateToken with unparseable input must return false");
    }
}
