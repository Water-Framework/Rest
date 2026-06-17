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
package it.water.service.rest.manager.cxf.security.filters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CxfSecurityHeadersFilter} (fix #26).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>X-Content-Type-Options and Cache-Control are always added to every response.</li>
 *   <li>Strict-Transport-Security is added only when the request scheme is https.</li>
 *   <li>Strict-Transport-Security is NOT added when the request scheme is http.</li>
 *   <li>Null/missing UriInfo is handled gracefully (no HSTS, no NPE).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CxfSecurityHeadersFilterTest {

    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String VALUE_NOSNIFF = "nosniff";
    private static final String VALUE_NO_STORE = "no-store";
    private static final String VALUE_HSTS = "max-age=31536000";

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @Mock
    private UriInfo uriInfo;

    private final CxfSecurityHeadersFilter filter = new CxfSecurityHeadersFilter();

    // -------------------------------------------------------------------
    // HTTP scheme — HSTS must NOT be added
    // -------------------------------------------------------------------

    @Test
    void filter_httpScheme_addsBaseHeadersButNotHsts() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://example.com/water/test"));

        filter.filter(requestContext, responseContext);

        Assertions.assertEquals(VALUE_NOSNIFF, headers.getFirst(HEADER_X_CONTENT_TYPE_OPTIONS),
                "#26: X-Content-Type-Options must always be 'nosniff'");
        Assertions.assertEquals(VALUE_NO_STORE, headers.getFirst(HEADER_CACHE_CONTROL),
                "#26: Cache-Control must always be 'no-store'");
        Assertions.assertNull(headers.getFirst(HEADER_STRICT_TRANSPORT_SECURITY),
                "#26: Strict-Transport-Security must NOT be added for plain http requests");
    }

    // -------------------------------------------------------------------
    // HTTPS scheme — HSTS must be added
    // -------------------------------------------------------------------

    @Test
    void filter_httpsScheme_addsBaseHeadersAndHsts() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("https://secure.example.com/water/test"));

        filter.filter(requestContext, responseContext);

        Assertions.assertEquals(VALUE_NOSNIFF, headers.getFirst(HEADER_X_CONTENT_TYPE_OPTIONS),
                "#26: X-Content-Type-Options must always be 'nosniff'");
        Assertions.assertEquals(VALUE_NO_STORE, headers.getFirst(HEADER_CACHE_CONTROL),
                "#26: Cache-Control must always be 'no-store'");
        Assertions.assertEquals(VALUE_HSTS, headers.getFirst(HEADER_STRICT_TRANSPORT_SECURITY),
                "#26: Strict-Transport-Security must be '" + VALUE_HSTS + "' when scheme is https");
    }

    // -------------------------------------------------------------------
    // HTTPS scheme — case-insensitive check (HTTPS uppercase)
    // -------------------------------------------------------------------

    @Test
    void filter_httpsSchemeUppercase_addsHsts() throws IOException {
        // The isSecure check uses equalsIgnoreCase — verify uppercase HTTPS is treated as secure
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        // java.net.URI is final (not mockable) and preserves the scheme as given, so an uppercase
        // "HTTPS" scheme exercises the equalsIgnoreCase branch with a real URI.
        when(uriInfo.getRequestUri()).thenReturn(URI.create("HTTPS://localhost:8080/water/test"));

        filter.filter(requestContext, responseContext);

        Assertions.assertEquals(VALUE_HSTS, headers.getFirst(HEADER_STRICT_TRANSPORT_SECURITY),
                "#26: equalsIgnoreCase must treat 'HTTPS' as secure and emit HSTS");
    }

    // -------------------------------------------------------------------
    // Null UriInfo — must not throw, HSTS must be absent
    // -------------------------------------------------------------------

    @Test
    void filter_nullUriInfo_addsBaseHeadersWithoutHsts() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> filter.filter(requestContext, responseContext),
                "#26: null UriInfo must not cause NullPointerException");

        Assertions.assertEquals(VALUE_NOSNIFF, headers.getFirst(HEADER_X_CONTENT_TYPE_OPTIONS),
                "#26: X-Content-Type-Options must still be added when UriInfo is null");
        Assertions.assertEquals(VALUE_NO_STORE, headers.getFirst(HEADER_CACHE_CONTROL),
                "#26: Cache-Control must still be added when UriInfo is null");
        Assertions.assertNull(headers.getFirst(HEADER_STRICT_TRANSPORT_SECURITY),
                "#26: HSTS must be absent when UriInfo is null (cannot determine scheme)");
    }

    // -------------------------------------------------------------------
    // Null RequestUri — must not throw, HSTS must be absent
    // -------------------------------------------------------------------

    @Test
    void filter_nullRequestUri_addsBaseHeadersWithoutHsts() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> filter.filter(requestContext, responseContext),
                "#26: null requestUri must not cause NullPointerException");

        Assertions.assertNull(headers.getFirst(HEADER_STRICT_TRANSPORT_SECURITY),
                "#26: HSTS must be absent when requestUri is null");
    }

    // -------------------------------------------------------------------
    // putSingle semantics — calling filter twice overwrites headers (not duplicates)
    // -------------------------------------------------------------------

    @Test
    void filter_calledTwice_headersContainExactlyOneValueEach() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("https://secure.example.com/water/test"));

        filter.filter(requestContext, responseContext);
        filter.filter(requestContext, responseContext);

        Assertions.assertEquals(1, headers.get(HEADER_X_CONTENT_TYPE_OPTIONS).size(),
                "#26: putSingle must not accumulate duplicate X-Content-Type-Options values");
        Assertions.assertEquals(1, headers.get(HEADER_CACHE_CONTROL).size(),
                "#26: putSingle must not accumulate duplicate Cache-Control values");
        Assertions.assertEquals(1, headers.get(HEADER_STRICT_TRANSPORT_SECURITY).size(),
                "#26: putSingle must not accumulate duplicate Strict-Transport-Security values");
    }
}
