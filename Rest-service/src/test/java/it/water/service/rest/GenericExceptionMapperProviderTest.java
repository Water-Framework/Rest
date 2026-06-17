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

import it.water.core.model.BaseError;
import it.water.core.model.BasicErrorMessage;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.validation.ValidationError;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.entity.model.exceptions.NoResultException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security fix #20 — GenericExceptionMapperProvider sanitization tests.
 *
 * Verifies that unmapped / internal exceptions are never exposed to the client
 * (no FQCN, no raw getMessage() in the response body) while existing 4xx mappings
 * continue to carry meaningful domain error information.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenericExceptionMapperProviderTest {

    // -----------------------------------------------------------------------
    // Constants mirroring the private constants in the class under test
    // -----------------------------------------------------------------------
    private static final String GENERIC_INTERNAL_ERROR_TYPE    = "InternalServerError";
    private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Internal server error";

    private static final int HTTP_200 = Response.Status.OK.getStatusCode();
    private static final int HTTP_401 = Response.Status.UNAUTHORIZED.getStatusCode();
    private static final int HTTP_404 = Response.Status.NOT_FOUND.getStatusCode();
    private static final int HTTP_409 = Response.Status.CONFLICT.getStatusCode();
    private static final int HTTP_422 = 422;
    private static final int HTTP_500 = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    /** Secret that must NEVER appear in the sanitized 500 response. */
    private static final String SECRET_DETAIL = "SQL near line 42 password=hunter2";

    private GenericExceptionMapperProvider mapper;

    @BeforeEach
    void setUp() {
        mapper = new GenericExceptionMapperProvider();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Collects all message strings from a BaseError's errorMessages list.
     */
    private List<String> extractMessages(BaseError error) {
        return error.getErrorMessages().stream()
                .map(m -> ((BasicErrorMessage) m).getMessage())
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Fix #20 — 500 sanitization: RuntimeException must be scrubbed
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void toResponse_runtimeExceptionWithSecretMessage_returns500WithGenericPayload() {
        // Arrange: a RuntimeException whose message would leak internal detail to the client
        RuntimeException leaked = new RuntimeException("super secret internal detail: " + SECRET_DETAIL);

        // Act
        Response response = mapper.toResponse(leaked);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status code
        Assertions.assertEquals(HTTP_500, response.getStatus(),
                "An unmapped RuntimeException must map to HTTP 500");

        // Assert — sanitized type (must be "InternalServerError", NOT the FQCN)
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "The BaseError type must be the generic constant, not the exception FQCN");
        Assertions.assertFalse(error.getType().contains("RuntimeException"),
                "The BaseError type must NOT contain the class name 'RuntimeException'");
        Assertions.assertFalse(error.getType().contains("java.lang"),
                "The BaseError type must NOT leak the java.lang package prefix");

        // Assert — sanitized messages (must be only the generic constant)
        List<String> messages = extractMessages(error);
        Assertions.assertEquals(1, messages.size(),
                "Exactly one generic error message must be present");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_MESSAGE, messages.get(0),
                "The single error message must equal the generic constant");

        // Assert — secret detail absent from type and messages
        Assertions.assertFalse(error.getType().contains(SECRET_DETAIL),
                "The raw exception message must NOT appear in the BaseError type");
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains(SECRET_DETAIL),
                        "The raw exception message must NOT appear in any error message: " + msg));
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains("super secret internal detail"),
                        "Sensitive detail must NOT leak into the error message body"));
    }

    // -----------------------------------------------------------------------
    // Fix #20 — 500 sanitization: checked Exception (non-RuntimeException) must also be scrubbed
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    void toResponse_genericCheckedException_returns500WithGenericPayload() {
        // Arrange: a plain checked Exception (not a RuntimeException)
        // The mapper's catch (Exception e) branch handles this path.
        Exception checked = new Exception("internal DB connection pool exhausted");

        // Act
        Response response = mapper.toResponse(checked);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status code
        Assertions.assertEquals(HTTP_500, response.getStatus(),
                "An unmapped checked Exception must map to HTTP 500");

        // Assert — generic type
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "Checked exceptions must produce the generic InternalServerError type");
        Assertions.assertFalse(error.getType().contains("java.lang.Exception"),
                "FQCN of checked Exception must not appear in BaseError type");

        // Assert — generic message only
        List<String> messages = extractMessages(error);
        Assertions.assertEquals(1, messages.size(),
                "Exactly one generic message must be present for checked exceptions");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_MESSAGE, messages.get(0),
                "The message must be the generic constant, not the real cause");
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains("DB connection pool"),
                        "Internal exception detail must NOT appear in the sanitized response"));
    }

    // -----------------------------------------------------------------------
    // Fix #20 — 500 sanitization: InvocationTargetException wrapping a RuntimeException
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    void toResponse_invocationTargetExceptionWrappingRuntimeException_returns500Sanitized() {
        // Arrange: proxy frameworks typically wrap target exceptions in InvocationTargetException
        RuntimeException cause = new RuntimeException("proxied internal error: credentials=secret");
        InvocationTargetException ite = new InvocationTargetException(cause);

        // Act
        Response response = mapper.toResponse(ite);
        BaseError error = (BaseError) response.getEntity();

        // Assert — sanitized 500
        Assertions.assertEquals(HTTP_500, response.getStatus(),
                "InvocationTargetException wrapping a RuntimeException must map to HTTP 500");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "InvocationTargetException path must also produce the generic type");
        List<String> messages = extractMessages(error);
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains("credentials=secret"),
                        "Wrapped exception detail must be sanitized out of the response"));
    }

    // -----------------------------------------------------------------------
    // Fix #20 — 500 sanitization: UndeclaredThrowableException
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    void toResponse_undeclaredThrowableWrappingRuntimeException_returns500Sanitized() {
        // Arrange: UndeclaredThrowableException can wrap a RuntimeException from a dynamic proxy
        RuntimeException inner = new RuntimeException("internal state leak: token=abc123");
        UndeclaredThrowableException ute = new UndeclaredThrowableException(inner);

        // Act
        Response response = mapper.toResponse(ute);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status and sanitization
        Assertions.assertEquals(HTTP_500, response.getStatus(),
                "UndeclaredThrowableException must produce HTTP 500");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "UndeclaredThrowableException path must use the generic type constant");
        List<String> messages = extractMessages(error);
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains("token=abc123"),
                        "Internal token detail must not appear in the response"));
    }

    // -----------------------------------------------------------------------
    // Fix #20 — 500 sanitization: java.lang.Error is also sanitized
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    void toResponse_outOfMemoryError_returns500WithGenericPayload() {
        // Arrange: JVM Errors (e.g. OOM) must also be sanitized — handleError() calls
        // sanitizedInternalServerError() and returns Response.serverError()
        Error oom = new OutOfMemoryError("Java heap space");

        // Act
        Response response = mapper.toResponse(oom);
        BaseError error = (BaseError) response.getEntity();

        // Assert — serverError() produces 500
        Assertions.assertEquals(HTTP_500, response.getStatus(),
                "An Error must map to HTTP 500 via handleError()");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "Error types must also be sanitized to the generic constant");
        List<String> messages = extractMessages(error);
        Assertions.assertEquals(1, messages.size(),
                "One generic message expected for Error types");
        Assertions.assertEquals(GENERIC_INTERNAL_ERROR_MESSAGE, messages.get(0),
                "The error message for Errors must be the generic constant");
        messages.forEach(msg ->
                Assertions.assertFalse(msg.contains("heap space"),
                        "JVM error detail must not leak to the client"));
    }

    // -----------------------------------------------------------------------
    // Regression — 401 UnauthorizedException: real type + message preserved
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    void toResponse_unauthorizedException_returns401WithDomainType() {
        // Arrange
        UnauthorizedException ex = new UnauthorizedException("Access denied to resource /water/secured");

        // Act
        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status
        Assertions.assertEquals(HTTP_401, response.getStatus(),
                "UnauthorizedException must map to HTTP 401");

        // Assert — type is FQCN of UnauthorizedException (via BaseError.generateError), NOT the generic one
        Assertions.assertEquals(UnauthorizedException.class.getName(), error.getType(),
                "UnauthorizedException must carry its real FQCN as the BaseError type");
        Assertions.assertNotEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "UnauthorizedException must NOT be mapped to the generic InternalServerError type");

        // Assert — status code is correctly stored in the BaseError as well
        Assertions.assertEquals(HTTP_401, error.getStatusCode(),
                "BaseError.statusCode must be 401 for UnauthorizedException");
    }

    // -----------------------------------------------------------------------
    // Regression — 422 ValidationException: real type preserved
    // -----------------------------------------------------------------------

    @Test
    @Order(7)
    void toResponse_validationException_returns422WithDomainType() {
        // Arrange: ValidationException requires a List<ValidationError>
        ValidationError violation = new ValidationError("must not be blank", "name", "");
        ValidationException ex = new ValidationException(List.of(violation));

        // Act
        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status
        Assertions.assertEquals(HTTP_422, response.getStatus(),
                "ValidationException must map to HTTP 422");

        // Assert — type is FQCN (not the generic sanitized constant)
        Assertions.assertEquals(ValidationException.class.getName(), error.getType(),
                "ValidationException must carry its real FQCN as the BaseError type");
        Assertions.assertNotEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "ValidationException must NOT be mapped to the generic InternalServerError type");

        // Assert — statusCode stored correctly
        Assertions.assertEquals(HTTP_422, error.getStatusCode(),
                "BaseError.statusCode must be 422 for ValidationException");
    }

    // -----------------------------------------------------------------------
    // Regression — 404 EntityNotFound: real type preserved
    // -----------------------------------------------------------------------

    @Test
    @Order(8)
    void toResponse_entityNotFound_returns404WithDomainType() {
        // Arrange: EntityNotFound has no-arg constructor
        EntityNotFound ex = new EntityNotFound();

        // Act
        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status
        Assertions.assertEquals(HTTP_404, response.getStatus(),
                "EntityNotFound must map to HTTP 404");

        // Assert — real type retained
        Assertions.assertEquals(EntityNotFound.class.getName(), error.getType(),
                "EntityNotFound must carry its real FQCN as the BaseError type");
        Assertions.assertNotEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "EntityNotFound must NOT be mapped to the generic InternalServerError type");
    }

    // -----------------------------------------------------------------------
    // Regression — 404 NoResultException: real type preserved
    // -----------------------------------------------------------------------

    @Test
    @Order(9)
    void toResponse_noResultException_returns404WithDomainType() {
        // Arrange: NoResultException has no-arg constructor
        NoResultException ex = new NoResultException();

        // Act
        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status
        Assertions.assertEquals(HTTP_404, response.getStatus(),
                "NoResultException must map to HTTP 404");

        // Assert — real type retained
        Assertions.assertEquals(NoResultException.class.getName(), error.getType(),
                "NoResultException must carry its real FQCN as the BaseError type");
        Assertions.assertNotEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "NoResultException must NOT be mapped to the generic InternalServerError type");
    }

    // -----------------------------------------------------------------------
    // Regression — 409 DuplicateEntityException: real type preserved
    // -----------------------------------------------------------------------

    @Test
    @Order(10)
    void toResponse_duplicateEntityException_returns409WithDomainType() {
        // Arrange: DuplicateEntityException @AllArgsConstructor takes String[] uniqueFields
        DuplicateEntityException ex = new DuplicateEntityException(new String[]{"email"});

        // Act
        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        // Assert — status
        Assertions.assertEquals(HTTP_409, response.getStatus(),
                "DuplicateEntityException must map to HTTP 409 (Conflict)");

        // Assert — real type retained
        Assertions.assertEquals(DuplicateEntityException.class.getName(), error.getType(),
                "DuplicateEntityException must carry its real FQCN as the BaseError type");
        Assertions.assertNotEquals(GENERIC_INTERNAL_ERROR_TYPE, error.getType(),
                "DuplicateEntityException must NOT be mapped to the generic InternalServerError type");
    }

    // -----------------------------------------------------------------------
    // Fix #20 — cross-cutting: sanitized type constant value is correct
    // -----------------------------------------------------------------------

    @Test
    @Order(11)
    void toResponse_runtimeException_genericTypeIsLiteralInternalServerError() {
        // Explicit assertion that the sanitized type is exactly "InternalServerError"
        // and not any other value (e.g. the HTTP status phrase "Internal Server Error").
        RuntimeException ex = new RuntimeException("any internal error");

        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();

        Assertions.assertEquals("InternalServerError", error.getType(),
                "The sanitized type constant must be exactly 'InternalServerError' (no spaces, camel-case)");
    }

    // -----------------------------------------------------------------------
    // Fix #20 — cross-cutting: sanitized message is exactly "Internal server error"
    // -----------------------------------------------------------------------

    @Test
    @Order(12)
    void toResponse_runtimeException_genericMessageIsExactLiteral() {
        // Explicit assertion that the sanitized message is exactly "Internal server error"
        // (lowercase 's', not the HTTP status phrase "Internal Server Error").
        RuntimeException ex = new RuntimeException("any internal error");

        Response response = mapper.toResponse(ex);
        BaseError error = (BaseError) response.getEntity();
        List<String> messages = extractMessages(error);

        Assertions.assertFalse(messages.isEmpty(),
                "There must be at least one message in the sanitized BaseError");
        Assertions.assertEquals("Internal server error", messages.get(0),
                "The sanitized message must be exactly 'Internal server error'");
    }
}
