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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Covers the reflection-hierarchy utility methods of {@link GenericJWTAuthFilter}
 * (getAnnotationFromHierarchy / getAllAnnotationsFromHierarchy), which are the largest
 * uncovered portion of this class, plus additional edge cases of getTokenFromRequest
 * not already exercised by {@link RestSecurityTest}.
 *
 * Note: the fixture classes are in the same package as GenericJWTAuthFilter, so the
 * protected utility methods are directly reachable without subclassing.
 */
class GenericJWTAuthFilterTest {

    private final GenericJWTAuthFilter filter = new GenericJWTAuthFilter();

    // -------------------------------------------------------------------
    // Fixtures for annotation-hierarchy resolution
    // -------------------------------------------------------------------

    private static class DirectAnnotated {
        @LoggedIn
        public void directMethod() {
            // fixture only: annotation declared directly on this method
        }
    }

    private static class ParentAnnotated {
        @LoggedIn
        public void inheritedMethod() {
            // fixture only: annotation declared on the superclass method
        }
    }

    private static class ChildOverride extends ParentAnnotated {
        @Override
        public void inheritedMethod() {
            // fixture only: override does NOT redeclare the annotation
        }
    }

    private interface AnnotatedInterface {
        @LoggedIn
        void interfaceMethod();
    }

    private static class InterfaceImpl implements AnnotatedInterface {
        @Override
        public void interfaceMethod() {
            // fixture only: annotation lives solely on the interface method
        }
    }

    private static class NoAnnotationAtAll {
        public void plainMethod() {
            // fixture only: no annotation anywhere in the hierarchy
        }
    }

    // -------------------------------------------------------------------
    // getAnnotationFromHierarchy
    // -------------------------------------------------------------------

    @Test
    void getAnnotationFromHierarchyDirectAnnotationReturnsAnnotation() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("directMethod");
        Annotation found = filter.getAnnotationFromHierarchy(LoggedIn.class, method);
        Assertions.assertNotNull(found, "Annotation declared directly on the method must be found");
        Assertions.assertTrue(found instanceof LoggedIn);
    }

    @Test
    void getAnnotationFromHierarchySuperclassAnnotationReturnsAnnotation() throws NoSuchMethodException {
        Method method = ChildOverride.class.getMethod("inheritedMethod");
        Annotation found = filter.getAnnotationFromHierarchy(LoggedIn.class, method);
        Assertions.assertNotNull(found, "Annotation declared on the overridden superclass method must be found");
    }

    @Test
    void getAnnotationFromHierarchyInterfaceAnnotationReturnsAnnotation() throws NoSuchMethodException {
        Method method = InterfaceImpl.class.getMethod("interfaceMethod");
        Annotation found = filter.getAnnotationFromHierarchy(LoggedIn.class, method);
        Assertions.assertNotNull(found, "Annotation declared on the implemented interface method must be found");
    }

    @Test
    void getAnnotationFromHierarchyNoAnnotationReturnsNull() throws NoSuchMethodException {
        Method method = NoAnnotationAtAll.class.getMethod("plainMethod");
        Annotation found = filter.getAnnotationFromHierarchy(LoggedIn.class, method);
        Assertions.assertNull(found, "No annotation anywhere in the hierarchy must yield null");
    }

    // -------------------------------------------------------------------
    // getAllAnnotationsFromHierarchy
    // -------------------------------------------------------------------

    @Test
    void getAllAnnotationsFromHierarchyCollectsFromSuperclass() throws NoSuchMethodException {
        Method method = ChildOverride.class.getMethod("inheritedMethod");
        List<Annotation> annotations = filter.getAllAnnotationsFromHierarchy(method);
        Assertions.assertFalse(annotations.isEmpty(), "Must collect the annotation declared on the superclass method");
    }

    @Test
    void getAllAnnotationsFromHierarchyCollectsFromInterface() throws NoSuchMethodException {
        Method method = InterfaceImpl.class.getMethod("interfaceMethod");
        List<Annotation> annotations = filter.getAllAnnotationsFromHierarchy(method);
        Assertions.assertFalse(annotations.isEmpty(), "Must collect the annotation declared on the implemented interface method");
    }

    @Test
    void getAllAnnotationsFromHierarchyDirectAnnotationIncluded() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("directMethod");
        List<Annotation> annotations = filter.getAllAnnotationsFromHierarchy(method);
        Assertions.assertEquals(1, annotations.size(), "Only the directly-declared annotation must be collected");
    }

    @Test
    void getAllAnnotationsFromHierarchyNoAnnotationsReturnsEmptyList() throws NoSuchMethodException {
        Method method = NoAnnotationAtAll.class.getMethod("plainMethod");
        List<Annotation> annotations = filter.getAllAnnotationsFromHierarchy(method);
        Assertions.assertTrue(annotations.isEmpty(), "No annotations anywhere must yield an empty list");
    }

    // -------------------------------------------------------------------
    // getTokenFromRequest edge cases (additional to RestSecurityTest)
    // -------------------------------------------------------------------

    @Test
    void getTokenFromRequestWrongSchemeThrows() {
        Assertions.assertThrows(UnauthorizedException.class,
                () -> filter.getTokenFromRequest("Basic abcdef", null));
    }

    @Test
    void getTokenFromRequestSinglePartHeaderThrows() {
        Assertions.assertThrows(UnauthorizedException.class,
                () -> filter.getTokenFromRequest("justoneword", null));
    }

    @Test
    void getTokenFromRequestTooManyPartsThrows() {
        Assertions.assertThrows(UnauthorizedException.class,
                () -> filter.getTokenFromRequest("Bearer token extra", null));
    }

    @Test
    void getTokenFromRequestNullAuthorizationThrows() {
        Assertions.assertThrows(UnauthorizedException.class,
                () -> filter.getTokenFromRequest(null, null));
    }

    @Test
    void getTokenFromRequestValidBearerReturnsToken() {
        String token = filter.getTokenFromRequest("Bearer abc123", null);
        Assertions.assertEquals("abc123", token);
    }
}
