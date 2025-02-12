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

import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.service.rest.api.security.LoggedIn;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class GenericJWTAuthFilter {
    private static Logger log = LoggerFactory.getLogger(GenericJWTAuthFilter.class);
    private static final String EXPECTED_AUTH_SCHEME = "Bearer";

    /**
     * Filter request and inject security context only if Logged in annotation is found on the service
     *
     * @param jwtTokenService      jwt token service
     * @param annotation           annotation on method
     * @param authorizationHeader  Authorization Header content
     * @param authenticationCookie Authentication Header content
     */
    public void validateToken(JwtTokenService jwtTokenService, LoggedIn annotation, String authorizationHeader, String authenticationCookie) {
        if (annotation != null) {
            log.debug("Found @LoggedIn Annotation, starting to validate JWT token");
            String encodedToken = getTokenFromRequest(authorizationHeader, authenticationCookie);
            List<String> issuers = Arrays.asList(annotation.issuers());
            if (!jwtTokenService.validateToken(issuers, encodedToken))
                throw new UnauthorizedException("Invalid JWT token!");
        }
    }

    public String getTokenFromRequest(String authorization, String cookieValue) {
        if ((authorization == null || authorization.isBlank()) && cookieValue != null && !cookieValue.isBlank())
            authorization = cookieValue;
        String[] parts = authorization == null ? null : authorization.split(" ");
        if (parts == null || !EXPECTED_AUTH_SCHEME.equals(parts[0]) || parts.length != 2) {
            throw new UnauthorizedException(EXPECTED_AUTH_SCHEME + " is required");
        }
        return parts[1];
    }

    //todo probably this utitly methods should be moved
    protected Annotation getAnnotationFromHierarchy(Class<? extends Annotation> annotationClass, Method method) {
        List<Annotation> annotations = getAllAnnotationsFromHierarchy(method);
        Optional<Annotation> optionalAnnotation = annotations.stream().filter(curAnnotation -> curAnnotation.annotationType().equals(annotationClass)).findFirst();
        if (optionalAnnotation.isPresent())
            return optionalAnnotation.get();
        return null;
    }

    /**
     * Utility function for retrieving all annotations within a specific method in the whole class hierarchy
     *
     * @param method
     * @return
     */
    protected List<Annotation> getAllAnnotationsFromHierarchy(Method method) {
        Set<Annotation> allAnnotations = new HashSet<>();

        addAnnotationsFromMethod(method, allAnnotations);

        Class<?> currentClass = method.getDeclaringClass().getSuperclass();
        while (currentClass != null) {
            try {
                Method parentMethod = currentClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                addAnnotationsFromMethod(parentMethod, allAnnotations);
            } catch (NoSuchMethodException e) {
                log.debug("No method found in super class for method {}", method.getName());
            }
            currentClass = currentClass.getSuperclass();
        }

        Set<Class<?>> interfaces = getAllInterfaces(method.getDeclaringClass());
        for (Class<?> interfaceClass : interfaces) {
            try {
                Method interfaceMethod = interfaceClass.getMethod(method.getName(), method.getParameterTypes());
                addAnnotationsFromMethod(interfaceMethod, allAnnotations);
            } catch (NoSuchMethodException e) {
                log.debug("No method found in super class for method {}", method.getName());
            }
        }

        return new ArrayList<>(allAnnotations);
    }

    private void addAnnotationsFromMethod(Method method, Set<Annotation> annotations) {
        annotations.addAll(Arrays.asList(method.getAnnotations()));
    }

    private Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<>();
        while (clazz != null) {
            getAllInterfacesRecursively(clazz, interfaces);
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }

    private void getAllInterfacesRecursively(Class<?> clazz, Set<Class<?>> interfaces) {
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            if (interfaces.add(interfaceClass)) {
                getAllInterfacesRecursively(interfaceClass, interfaces);
            }
        }
    }
}
