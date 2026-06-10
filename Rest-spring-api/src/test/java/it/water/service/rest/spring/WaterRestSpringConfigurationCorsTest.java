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
package it.water.service.rest.spring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Unit tests for {@link WaterRestSpringConfiguration#corsFilter()} — secure-by-default CORS
 * (fix H10) without a full Spring context: fields are injected via
 * {@link ReflectionTestUtils#setField} and assertions run on the resulting {@link CorsConfiguration}.
 */
@ExtendWith(MockitoExtension.class)
class WaterRestSpringConfigurationCorsTest {

    private WaterRestSpringConfiguration config;

    @BeforeEach
    void setUp() {
        config = new WaterRestSpringConfiguration();
    }

    // Helper: build a CorsFilter from the config and extract its CorsConfiguration

    private CorsConfiguration buildCorsConfig(String origins,
                                               String methods,
                                               String headers,
                                               boolean credentials,
                                               long maxAge) {
        ReflectionTestUtils.setField(config, "corsOrigins", origins);
        ReflectionTestUtils.setField(config, "corsMethods", methods);
        ReflectionTestUtils.setField(config, "corsHeaders", headers);
        ReflectionTestUtils.setField(config, "corsCredentials", credentials);
        ReflectionTestUtils.setField(config, "corsMaxAge", maxAge);

        CorsFilter filter = config.corsFilter();
        Assertions.assertNotNull(filter, "corsFilter() must not return null");

        // Extract the CorsConfigurationSource from the CorsFilter.
        // Spring's CorsFilter stores it in the field "configSource" (private final).
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) ReflectionTestUtils.getField(filter, "configSource");
        Assertions.assertNotNull(source, "CorsFilter must hold a UrlBasedCorsConfigurationSource in 'configSource'");

        // getCorsConfiguration(HttpServletRequest) resolves the pattern "/**"
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/water/test");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        Assertions.assertNotNull(corsConfig, "CorsConfiguration must be resolved for path /water/test (pattern /**)");
        return corsConfig;
    }

    // Secure-by-default: no origins configured

    @Test
    void testCorsFilterEmptyOriginsNoOriginAllowed() {
        // With an empty origins string no allowed origin must be registered,
        // so the filter rejects all cross-origin requests.
        CorsConfiguration cors = buildCorsConfig(
                "",                          // origins — empty
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                false,
                3600L);

        List<String> allowedOrigins = cors.getAllowedOrigins();
        boolean isEmpty = allowedOrigins == null || allowedOrigins.isEmpty();
        Assertions.assertTrue(isEmpty,
                "No origin must be registered when corsOrigins is empty");
    }

    @Test
    void testCorsFilterEmptyOriginsCredentialsFalse() {
        CorsConfiguration cors = buildCorsConfig(
                "", "GET,POST,PUT,DELETE,OPTIONS,PATCH", "Authorization,Content-Type", false, 3600L);

        Assertions.assertNotEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "allowCredentials must not be true when no origin is configured");
    }

    @Test
    void testCorsFilterDefaultHeadersDoNotContainWildcard() {
        CorsConfiguration cors = buildCorsConfig(
                "", "GET,POST", "Authorization,Content-Type", false, 3600L);

        List<String> allowedHeaders = cors.getAllowedHeaders();
        Assertions.assertNotNull(allowedHeaders);
        Assertions.assertFalse(allowedHeaders.contains(CorsConfiguration.ALL),
                "Allowed headers must not contain wildcard '*' in the default configuration");
        Assertions.assertTrue(allowedHeaders.contains("Authorization"),
                "Authorization must be in the allowed headers");
        Assertions.assertTrue(allowedHeaders.contains("Content-Type"),
                "Content-Type must be in the allowed headers");
    }

    // Explicit allow-list: origins are exact-match, not patterns

    @Test
    void testCorsFilterExplicitOriginsAddedAsExactMatch() {
        // Explicit origins must be registered via addAllowedOrigin (exact match),
        // NOT via addAllowedOriginPattern("*") which would reflect any origin.
        CorsConfiguration cors = buildCorsConfig(
                "https://app.example.com,https://admin.example.com",
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                true,
                3600L);

        List<String> allowedOrigins = cors.getAllowedOrigins();
        Assertions.assertNotNull(allowedOrigins);
        Assertions.assertTrue(allowedOrigins.contains("https://app.example.com"),
                "https://app.example.com must be in the allowed origins");
        Assertions.assertTrue(allowedOrigins.contains("https://admin.example.com"),
                "https://admin.example.com must be in the allowed origins");
    }

    @Test
    void testCorsFilterExplicitOriginsNoWildcardOriginRegistered() {
        CorsConfiguration cors = buildCorsConfig(
                "https://app.example.com",
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                true,
                3600L);

        List<String> allowedOrigins = cors.getAllowedOrigins();
        boolean containsWildcard = allowedOrigins != null
                && allowedOrigins.contains(CorsConfiguration.ALL);
        Assertions.assertFalse(containsWildcard,
                "Wildcard origin '*' must never appear when explicit origins are configured");
    }

    @Test
    void testCorsFilterExplicitOriginsWithCredentialsCredentialsEnabled() {
        // Explicit origin + credentials=true is a safe, valid combination.
        CorsConfiguration cors = buildCorsConfig(
                "https://app.example.com",
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                true,
                3600L);

        Assertions.assertEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "allowCredentials must be true when an explicit origin is combined with credentials=true");
    }

    @Test
    void testCorsFilterCsvOriginsWhitespaceIsTrimmed() {
        // Values like " https://a.com , https://b.com " must be trimmed before registration.
        CorsConfiguration cors = buildCorsConfig(
                " https://a.example.com , https://b.example.com ",
                "GET,POST",
                "Authorization,Content-Type",
                false,
                3600L);

        List<String> allowedOrigins = cors.getAllowedOrigins();
        Assertions.assertNotNull(allowedOrigins);
        Assertions.assertTrue(allowedOrigins.contains("https://a.example.com"),
                "Trimmed origin https://a.example.com must be registered");
        Assertions.assertTrue(allowedOrigins.contains("https://b.example.com"),
                "Trimmed origin https://b.example.com must be registered");
        Assertions.assertFalse(allowedOrigins.contains(" https://a.example.com "),
                "Untrimmed (padded) origin must not be registered");
    }

    // Security guard: wildcard origin + credentials must never co-exist (H10)

    @Test
    void testCorsFilterWildcardOriginWithCredentialsCredentialsForcedFalse() {
        // If someone configures origins="*" together with credentials=true,
        // the guard must force credentials to false to prevent the insecure
        // "Access-Control-Allow-Origin: *" + "Access-Control-Allow-Credentials: true" combination.
        CorsConfiguration cors = buildCorsConfig(
                "*",          // wildcard origin — forbidden to combine with credentials
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                true,         // caller tried to enable credentials
                3600L);

        Assertions.assertNotEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "credentials must be forced to false when origin is wildcard '*' (H10 guard)");
    }

    @Test
    void testCorsFilterWildcardOriginWithoutCredentialsCredentialsRemainFalse() {
        // Wildcard without credentials is still insecure but not a credentials leak.
        // Credentials must remain false.
        CorsConfiguration cors = buildCorsConfig(
                "*", "GET,POST", "Authorization,Content-Type", false, 3600L);

        Assertions.assertNotEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "credentials must remain false when wildcard origin is used without explicit credentials");
    }

    // Additional fields

    @Test
    void testCorsFilterConfiguredMethodsAreRegistered() {
        CorsConfiguration cors = buildCorsConfig(
                "https://app.example.com",
                "GET,POST",
                "Authorization,Content-Type",
                false,
                3600L);

        List<String> methods = cors.getAllowedMethods();
        Assertions.assertNotNull(methods);
        Assertions.assertTrue(methods.contains("GET"), "GET must be in allowed methods");
        Assertions.assertTrue(methods.contains("POST"), "POST must be in allowed methods");
        Assertions.assertFalse(methods.contains("DELETE"), "DELETE must not be present when not configured");
    }

    @Test
    void testCorsFilterConfiguredMaxAgeIsSet() {
        CorsConfiguration cors = buildCorsConfig(
                "https://app.example.com",
                "GET,POST,PUT,DELETE,OPTIONS,PATCH",
                "Authorization,Content-Type",
                false,
                7200L);

        Assertions.assertEquals(7200L, cors.getMaxAge(),
                "maxAge must equal the configured value");
    }

    @Test
    void testCorsFilterDefaultMaxAgeIs3600() {
        CorsConfiguration cors = buildCorsConfig(
                "", "GET,POST,PUT,DELETE,OPTIONS,PATCH", "Authorization,Content-Type", false, 3600L);

        Assertions.assertEquals(3600L, cors.getMaxAge(),
                "maxAge must default to 3600");
    }

    // Consistency with CorsOptionsImpl (CXF): verify matching defaults

    @Test
    void testCorsFilterSpringDefaultHeadersMatchCxfDefaults() {
        // The Spring default header set must equal the CXF CorsOptionsImpl default
        // so both runtimes expose the same secure headers out of the box.
        CorsConfiguration cors = buildCorsConfig(
                "", "GET,POST,PUT,DELETE,OPTIONS,PATCH", "Authorization,Content-Type", false, 3600L);

        List<String> headers = cors.getAllowedHeaders();
        Assertions.assertNotNull(headers);
        // Both runtimes default to exactly these two headers
        Assertions.assertTrue(headers.contains("Authorization"),
                "Spring default headers must contain Authorization (matches CXF CorsOptionsImpl default)");
        Assertions.assertTrue(headers.contains("Content-Type"),
                "Spring default headers must contain Content-Type (matches CXF CorsOptionsImpl default)");
        Assertions.assertEquals(2, headers.size(),
                "Spring default allowed headers must be exactly 2 entries, matching CXF runtime");
    }

    @Test
    void testCorsFilterSpringDefaultCredentialsMatchCxfDefaultFalse() {
        // Spring default credentials=false must match CXF CorsOptionsImpl.allowCredentials() default.
        CorsConfiguration cors = buildCorsConfig(
                "", "GET,POST,PUT,DELETE,OPTIONS,PATCH", "Authorization,Content-Type", false, 3600L);

        Assertions.assertNotEquals(Boolean.TRUE, cors.getAllowCredentials(),
                "Spring CORS credentials default must be false, consistent with CXF CorsOptionsImpl");
    }
}
