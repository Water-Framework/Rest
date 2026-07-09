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

import it.water.core.api.bundle.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtSecurityOptionsImpl} — every getter simply delegates to
 * {@link ApplicationProperties#getPropertyOrDefault}, so we cover both the default-value
 * path (property absent) and the overridden-value path (property configured) for each one.
 */
@ExtendWith(MockitoExtension.class)
class JwtSecurityOptionsImplTest {

    @Mock
    private ApplicationProperties applicationProperties;

    private JwtSecurityOptionsImpl jwtSecurityOptions;

    @BeforeEach
    void setUp() {
        jwtSecurityOptions = new JwtSecurityOptionsImpl();
        jwtSecurityOptions.setApplicationProperties(applicationProperties);
    }

    @Test
    void gettersReturnDefaultsWhenNoPropertyConfigured() {
        // ApplicationProperties#getPropertyOrDefault is a default method; the mock must be
        // told to behave as a simple pass-through of the supplied default value.
        when(applicationProperties.getPropertyOrDefault(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(applicationProperties.getPropertyOrDefault(anyString(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Assertions.assertFalse(jwtSecurityOptions.validateJwtWithJwsUrl(), "validateJwtWithJwsUrl must default to false");
        Assertions.assertEquals("", jwtSecurityOptions.jwtKeyId(), "jwtKeyId must default to empty string");
        Assertions.assertFalse(jwtSecurityOptions.encryptJWTToken(), "encryptJWTToken must default to false");
        Assertions.assertEquals("", jwtSecurityOptions.jwsURL(), "jwsURL must default to empty string");
        Assertions.assertEquals(3600000L, jwtSecurityOptions.jwtTokenDurationMillis(), "jwtTokenDurationMillis must default to one hour");
        Assertions.assertTrue(jwtSecurityOptions.validateJwt(), "validateJwt must default to true");
        Assertions.assertEquals("", jwtSecurityOptions.jwtAudience(), "jwtAudience must default to empty string");
        Assertions.assertEquals(JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS, jwtSecurityOptions.jwtClockSkewSeconds(), "jwtClockSkewSeconds must default to 60s");
        Assertions.assertFalse(jwtSecurityOptions.testMode(), "testMode must default to false");
    }

    @Test
    void gettersReturnOverriddenValuesWhenPropertyConfigured() {
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATE_BY_JWS, false)).thenReturn(true);
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATE_BY_JWS_KEY_ID, "")).thenReturn("myKeyId");
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_ENCRYPT_JWT_TOKEN, false)).thenReturn(true);
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_JWS_URL, "")).thenReturn("https://example.com/jwks.json");
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_JWT_DURATION_MILLIS, 3600000L)).thenReturn(7200000L);
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATION_ENABLED, true)).thenReturn(false);
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_AUDIENCE, "")).thenReturn("my-audience");
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_CLOCK_SKEW_SECONDS, JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS)).thenReturn(120L);
        when(applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_TEST_MODE, false)).thenReturn(true);

        Assertions.assertTrue(jwtSecurityOptions.validateJwtWithJwsUrl());
        Assertions.assertEquals("myKeyId", jwtSecurityOptions.jwtKeyId());
        Assertions.assertTrue(jwtSecurityOptions.encryptJWTToken());
        Assertions.assertEquals("https://example.com/jwks.json", jwtSecurityOptions.jwsURL());
        Assertions.assertEquals(7200000L, jwtSecurityOptions.jwtTokenDurationMillis());
        Assertions.assertFalse(jwtSecurityOptions.validateJwt());
        Assertions.assertEquals("my-audience", jwtSecurityOptions.jwtAudience());
        Assertions.assertEquals(120L, jwtSecurityOptions.jwtClockSkewSeconds());
        Assertions.assertTrue(jwtSecurityOptions.testMode());
    }
}
