package it.water.service.rest;

import it.water.core.permission.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

class RestServiceTest {
    @Test
    void testGenericExceptionMapper() {
        GenericExceptionMapperProvider exceptionMapperProvider = new GenericExceptionMapperProvider();
        Response r = exceptionMapperProvider.toResponse(new UnauthorizedException());
        Assertions.assertEquals(401, r.getStatus());
        GenericExceptionMapperProvider.ErrorMessage errorMessage = new GenericExceptionMapperProvider.ErrorMessage(403, "message");
        Assertions.assertEquals(403, errorMessage.getStatus());
        Assertions.assertEquals("message", errorMessage.getMessage());
    }
}
