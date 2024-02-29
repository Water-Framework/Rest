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
package it.water.service.rest.manager.cxf.security.filters.jwt;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.ErrorMessage;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.model.BaseError;
import it.water.core.model.BasicErrorMessage;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.service.rest.api.security.LoggedIn;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.service.rest.security.jwt.GenericJWTAuthFilter;
import it.water.service.rest.security.jwt.JWTConstants;
import it.water.service.rest.security.jwt.JwtSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Aristide Cittadino
 * This class implements a JAXRS Filter for JWT Authentication.
 * This class is registered from code, injecting the bean instantiated by the framework.
 * This allows to use annotation injection in order to inject jwt service implementation.
 * <p>
 * In this way jwt management service (JwtTokenService) can be customized by implementing a new bean with higher priority
 */
@Priority(Priorities.AUTHENTICATION)
@LoggedIn
@Provider
public class CxfJwtAuthenticationFilter extends GenericJWTAuthFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CxfJwtAuthenticationFilter.class.getName());
    @Context
    protected ResourceInfo info;
    private ComponentRegistry componentRegistry;
    private JwtTokenService jwtTokenService;

    public CxfJwtAuthenticationFilter(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
        this.jwtTokenService = this.componentRegistry.findComponent(JwtTokenService.class, null);
        if (this.jwtTokenService == null)
            throw new WaterRuntimeException("No JWT Token Service found, please install one!");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            //if token is not valid it will raise exception
            log.debug("In JwtAuthenticationFilter on class: {}.{}", info.getResourceClass(), info.getResourceMethod());
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            Cookie c = requestContext.getCookies().get(JWTConstants.JWT_COOKIE_NAME);
            String cookieVal = c != null ? c.getValue() : null;
            if (info.getResourceMethod() != null) {
                LoggedIn annotation = info.getResourceMethod().getAnnotation(LoggedIn.class);
                validateToken(jwtTokenService, annotation, authorizationHeader, cookieVal);
                createSecurityContext(authorizationHeader, cookieVal);
            }

        } catch (Exception e) {
            log.debug("Error in Authentication filter:", e);
            List<ErrorMessage> messages = new ArrayList<>();
            BaseError error = new BaseError();
            error.setStatusCode(401);
            error.setType(NotAuthorizedException.class.getName());
            messages.add(new BasicErrorMessage("JWT Token not valid or expired!"));
            error.setErrorMessages(messages);
            requestContext.abortWith(Response.status(401).entity(error)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    /**
     * Injects Security context inside CXF Context
     *
     * @param authorizationHeader
     * @param cookieValue
     */
    private void createSecurityContext(String authorizationHeader, String cookieValue) {
        String encodedToken = this.getTokenFromRequest(authorizationHeader, cookieValue);
        SecurityContext securityContext = new JwtSecurityContext(jwtTokenService.getPrincipals(encodedToken));
        Runtime runtime = this.componentRegistry.findComponent(Runtime.class, null);
        runtime.fillSecurityContext(securityContext);
    }
}
