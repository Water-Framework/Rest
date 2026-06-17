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
package it.water.service.rest.spring.security;

import it.water.core.api.registry.ComponentRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link SpringJwtAuthenticationFilter#postHandle} method (fix #26).
 *
 * <p>The filter is instantiated directly with a mock {@link ComponentRegistry} because
 * {@code postHandle} does not use the registry at all — it only operates on
 * {@link HttpServletRequest} and {@link HttpServletResponse}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>X-Content-Type-Options and Cache-Control are always set on every response.</li>
 *   <li>Strict-Transport-Security is set when {@code request.isSecure()} is true.</li>
 *   <li>Strict-Transport-Security is NOT set when the request is plain HTTP.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SpringJwtAuthenticationFilterSecurityHeadersTest {

    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String VALUE_NOSNIFF = "nosniff";
    private static final String VALUE_NO_STORE = "no-store";
    private static final String VALUE_HSTS = "max-age=31536000";

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    // -------------------------------------------------------------------
    // Non-secure (plain HTTP) — HSTS must NOT be set
    // -------------------------------------------------------------------

    @Test
    void postHandle_nonSecureRequest_setsBaseHeadersButNotHsts() throws Exception {
        when(request.isSecure()).thenReturn(false);

        SpringJwtAuthenticationFilter filter = new SpringJwtAuthenticationFilter(componentRegistry);
        filter.postHandle(request, response, new Object(), null);

        verify(response).setHeader(HEADER_X_CONTENT_TYPE_OPTIONS, VALUE_NOSNIFF);
        verify(response).setHeader(HEADER_CACHE_CONTROL, VALUE_NO_STORE);
        verify(response, never()).setHeader(eq(HEADER_STRICT_TRANSPORT_SECURITY), anyString());
    }

    // -------------------------------------------------------------------
    // Secure (HTTPS) — HSTS must be set
    // -------------------------------------------------------------------

    @Test
    void postHandle_secureRequest_setsBaseHeadersAndHsts() throws Exception {
        when(request.isSecure()).thenReturn(true);

        SpringJwtAuthenticationFilter filter = new SpringJwtAuthenticationFilter(componentRegistry);
        filter.postHandle(request, response, new Object(), null);

        verify(response).setHeader(HEADER_X_CONTENT_TYPE_OPTIONS, VALUE_NOSNIFF);
        verify(response).setHeader(HEADER_CACHE_CONTROL, VALUE_NO_STORE);
        verify(response).setHeader(HEADER_STRICT_TRANSPORT_SECURITY, VALUE_HSTS);
    }

    // -------------------------------------------------------------------
    // postHandle never throws regardless of ModelAndView (null or non-null)
    // -------------------------------------------------------------------

    @Test
    void postHandle_nullModelAndView_doesNotThrow() {
        when(request.isSecure()).thenReturn(false);

        SpringJwtAuthenticationFilter filter = new SpringJwtAuthenticationFilter(componentRegistry);
        Assertions.assertDoesNotThrow(
                () -> filter.postHandle(request, response, new Object(), null),
                "#26: postHandle must not throw when ModelAndView is null");
    }
}
