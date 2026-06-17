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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @Author Aristide Cittadino
 * #26: JAX-RS response filter which enforces baseline HTTP security headers on every REST response.
 * <p>
 * - X-Content-Type-Options: nosniff is always emitted to disable MIME sniffing.
 * - Cache-Control: no-store is always emitted since these are API/JSON responses.
 * - Strict-Transport-Security is emitted only when the request was served over TLS/HTTPS,
 * never on plain http (sending HSTS over http is meaningless/misleading).
 * <p>
 * Registered from code in CxfRestApiManager alongside the other CXF providers.
 */
@Provider
public class CxfSecurityHeadersFilter implements ContainerResponseFilter {

    static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    static final String CACHE_CONTROL = "Cache-Control";
    static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    static final String HSTS_VALUE = "max-age=31536000";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().putSingle(X_CONTENT_TYPE_OPTIONS, "nosniff");
        responseContext.getHeaders().putSingle(CACHE_CONTROL, "no-store");
        if (isSecure(requestContext)) {
            responseContext.getHeaders().putSingle(STRICT_TRANSPORT_SECURITY, HSTS_VALUE);
        }
    }

    private boolean isSecure(ContainerRequestContext requestContext) {
        UriInfo uriInfo = requestContext.getUriInfo();
        if (uriInfo != null && uriInfo.getRequestUri() != null && uriInfo.getRequestUri().getScheme() != null) {
            return "https".equalsIgnoreCase(uriInfo.getRequestUri().getScheme());
        }
        return false;
    }
}
