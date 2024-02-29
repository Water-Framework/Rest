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

import it.water.core.api.bundle.Runtime;
import it.water.core.api.registry.ComponentRegistry;
import it.water.implementation.spring.security.SpringSecurityContext;
import it.water.service.rest.api.security.LoggedIn;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.service.rest.security.jwt.GenericJWTAuthFilter;
import it.water.service.rest.security.jwt.JWTConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * @Author Aristide Cittadino
 * This class implements a Spring Filter for JWT Authentication.
 * This class is registered from code, injecting the bean instantiated by the framework.
 * This allows to use annotation injection in order to inject jwt service implementation.
 * <p>
 * In this way jwt management service (JwtTokenService) can be customized by implementing a new bean with higher priority
 */
public class SpringJwtAuthenticationFilter extends GenericJWTAuthFilter implements HandlerInterceptor {

    private static Logger log = LoggerFactory.getLogger(SpringJwtAuthenticationFilter.class);
    private ComponentRegistry componentRegistry;

    public SpringJwtAuthenticationFilter(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.debug("Pre handle: {}", SpringJwtAuthenticationFilter.class.getName());
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        LoggedIn annotation = method.getAnnotation(LoggedIn.class);
        if (annotation != null) {
            String authorization = request.getHeader("Authorization");
            String cookie = null;
            if (request.getCookies() != null) {
                Optional<Cookie> cookies = Arrays.stream(request.getCookies()).filter(currCookie -> currCookie.getName().equalsIgnoreCase(JWTConstants.JWT_COOKIE_NAME)).findAny();
                if (cookies.isPresent())
                    cookie = cookies.get().getName();
            }
            JwtTokenService jwtTokenService = this.componentRegistry.findComponent(JwtTokenService.class, null);
            //raise exception if not valid token
            this.validateToken(jwtTokenService, annotation, authorization, cookie);
            //Fill current thread with security context
            Runtime runtime = this.componentRegistry.findComponent(Runtime.class, null);
            runtime.fillSecurityContext(new SpringSecurityContext(jwtTokenService.getPrincipals(getTokenFromRequest(authorization, cookie))));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //do nothing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //do nothing
    }
}
