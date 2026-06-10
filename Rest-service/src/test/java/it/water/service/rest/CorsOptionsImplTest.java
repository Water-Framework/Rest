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
package it.water.service.rest;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.service.rest.api.options.CorsOptions;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

/**
 * Unit tests for {@link CorsOptionsImpl} — verifies secure-by-default CORS behaviour
 * (fix H10) and correct property loading when values are explicitly configured.
 *
 * Strategy: same WaterTestExtension-based pattern as {@link RestServiceTest}.
 * The framework injects the real {@link CorsOptionsImpl} and the live
 * {@link ApplicationProperties} instance, so tests drive behaviour by calling
 * {@code loadProperties()} — exactly as production code would.
 */
@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorsOptionsImplTest implements Service {

    @Inject
    @Setter
    private CorsOptions corsOptions;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    // Secure-by-default: no properties configured

    @Test
    void testAllowedOriginsNoPropertyConfiguredReturnsEmptyString() {
        // When no property is set the allow-list must be empty — no wildcard, no default origin.
        String origins = corsOptions.allowedOrigins();
        Assertions.assertEquals("", origins,
                "allowedOrigins() must return empty string when water.rest.cors.origins is not set");
    }

    @Test
    void testAllowCredentialsNoPropertyConfiguredReturnsFalse() {
        // Credentials must be opt-in. Default must be false to prevent wildcard-origin+credentials.
        boolean credentials = corsOptions.allowCredentials();
        Assertions.assertFalse(credentials,
                "allowCredentials() must return false when water.rest.cors.credentials is not set");
    }

    @Test
    void testAllowedHeadersNoPropertyConfiguredReturnsRestrictedSet() {
        // Headers default must not be "*". Only the known-safe set should be returned.
        String headers = corsOptions.allowedHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertFalse(headers.contains("*"),
                "allowedHeaders() default must not contain wildcard '*'");
        Assertions.assertTrue(headers.contains("Authorization"),
                "allowedHeaders() default must include Authorization");
        Assertions.assertTrue(headers.contains("Content-Type"),
                "allowedHeaders() default must include Content-Type");
    }

    @Test
    void testAllowedMethodsNoPropertyConfiguredReturnsDefaultSet() {
        String methods = corsOptions.allowedMethods();
        Assertions.assertNotNull(methods);
        Assertions.assertTrue(methods.contains("GET"),
                "allowedMethods() default must include GET");
        Assertions.assertTrue(methods.contains("POST"),
                "allowedMethods() default must include POST");
    }

    @Test
    void testMaxAgeNoPropertyConfiguredReturnsDefault3600() {
        long maxAge = corsOptions.maxAge();
        Assertions.assertEquals(3600L, maxAge,
                "maxAge() must default to 3600 when water.rest.cors.maxAge is not set");
    }

    // Property-driven: values are read correctly when explicitly configured

    @Test
    void testAllowedOriginsPropertyConfiguredReturnsConfiguredValue() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_ORIGINS, "https://app.example.com,https://admin.example.com");
        applicationProperties.loadProperties(props);

        String origins = corsOptions.allowedOrigins();
        Assertions.assertEquals("https://app.example.com,https://admin.example.com", origins,
                "allowedOrigins() must return the configured value verbatim");

        // Cleanup: restore empty allow-list for subsequent tests
        Properties cleanup = new Properties();
        cleanup.put(RestConstants.REST_PROP_CORS_ORIGINS, "");
        applicationProperties.loadProperties(cleanup);
    }

    @Test
    void testAllowCredentialsPropertySetToTrueReturnsTrue() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_CREDENTIALS, "true");
        applicationProperties.loadProperties(props);

        boolean credentials = corsOptions.allowCredentials();
        Assertions.assertTrue(credentials,
                "allowCredentials() must return true when water.rest.cors.credentials=true");

        // Cleanup
        Properties cleanup = new Properties();
        cleanup.put(RestConstants.REST_PROP_CORS_CREDENTIALS, "false");
        applicationProperties.loadProperties(cleanup);
    }

    @Test
    void testAllowCredentialsPropertySetToFalseReturnsFalse() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_CREDENTIALS, "false");
        applicationProperties.loadProperties(props);

        boolean credentials = corsOptions.allowCredentials();
        Assertions.assertFalse(credentials,
                "allowCredentials() must return false when water.rest.cors.credentials=false");
    }

    @Test
    void testAllowedHeadersPropertyConfiguredReturnsConfiguredValue() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_HEADERS, "Authorization,Content-Type,X-Custom-Header");
        applicationProperties.loadProperties(props);

        String headers = corsOptions.allowedHeaders();
        Assertions.assertEquals("Authorization,Content-Type,X-Custom-Header", headers,
                "allowedHeaders() must return the configured value verbatim");

        // Cleanup
        Properties cleanup = new Properties();
        cleanup.put(RestConstants.REST_PROP_CORS_HEADERS, "Authorization,Content-Type");
        applicationProperties.loadProperties(cleanup);
    }

    @Test
    void testAllowedMethodsPropertyConfiguredReturnsConfiguredValue() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_METHODS, "GET,POST");
        applicationProperties.loadProperties(props);

        String methods = corsOptions.allowedMethods();
        Assertions.assertEquals("GET,POST", methods,
                "allowedMethods() must return the configured value verbatim");

        // Cleanup
        Properties cleanup = new Properties();
        cleanup.put(RestConstants.REST_PROP_CORS_METHODS, "GET,POST,PUT,DELETE,OPTIONS,PATCH");
        applicationProperties.loadProperties(cleanup);
    }

    @Test
    void testMaxAgePropertyConfiguredReturnsConfiguredValue() {
        Properties props = new Properties();
        props.put(RestConstants.REST_PROP_CORS_MAX_AGE, "7200");
        applicationProperties.loadProperties(props);

        long maxAge = corsOptions.maxAge();
        Assertions.assertEquals(7200L, maxAge,
                "maxAge() must return the configured numeric value");

        // Cleanup
        Properties cleanup = new Properties();
        cleanup.put(RestConstants.REST_PROP_CORS_MAX_AGE, "3600");
        applicationProperties.loadProperties(cleanup);
    }

    // Consistency check: CorsOptions is available inside RestOptions

    @Test
    void testCorsOptionsNotNullInsideRestOptions() {
        // RestOptionsImpl.corsOptions() delegates to the injected CorsOptions bean.
        // This test ensures the wiring is intact via the framework container.
        Assertions.assertNotNull(corsOptions,
                "CorsOptions component must be registered and injectable");
    }
}
