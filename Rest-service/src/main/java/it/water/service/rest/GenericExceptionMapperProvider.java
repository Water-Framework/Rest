
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
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.entity.model.exceptions.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;

/**
 * @Author Aristide Cittadino
 */
@Provider
public class GenericExceptionMapperProvider implements ExceptionMapper<Throwable> {
    private static Logger log = LoggerFactory.getLogger(GenericExceptionMapperProvider.class.getName());

    public static class ErrorMessage {
        int status;
        String message;
        String developerMessage;

        public ErrorMessage(int status, String message) {
            this.status = status;
            String[] messages = message.split("\\|");
            this.message = messages[0];
            if (messages.length > 1) {
                this.developerMessage = messages[1];
            }
        }

        public int getStatus() {
            return this.status;
        }

        public String getMessage() {
            return this.message;
        }

        public String getDeveloperMessage() {
            return this.developerMessage;
        }
    }

    public Response toResponse(Throwable ex) {
        log.error(ex.getMessage(), ex);

        if (ex instanceof Error error)
            return handleError(error);
        else if (ex instanceof UndeclaredThrowableException undeclaredThrowableException) {
            ex = undeclaredThrowableException.getUndeclaredThrowable();
        }

        BaseError error = handleException((Exception) ex);
        return Response.status(error.getStatusCode()).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected BaseError handleException(Exception t) {
        BaseError response = null;
        try {
            if (t instanceof InvocationTargetException invocationTargetException) {
                throw (Exception) (invocationTargetException).getTargetException();
            }
            throw t;
        } catch (DuplicateEntityException e) {
            log.error("Save failed: entity is duplicated!");
            response = BaseError.generateError(e, null, Response.Status.CONFLICT.getStatusCode());
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            response = BaseError.generateError(e, null, 422);
        } catch (EntityNotFound | NoResultException e) {
            log.error(e.getMessage(), e);
            response = BaseError.generateError(e, null,
                    Response.Status.NOT_FOUND.getStatusCode());
        } catch (UnauthorizedException e) {
            log.error(e.getMessage(), e);
            response = BaseError.generateError(e, null,
                    Response.Status.UNAUTHORIZED.getStatusCode());
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            BasicErrorMessage errorMessage = new BasicErrorMessage(e.getMessage());
            response = BaseError.generateError(e,
                    Arrays.asList(errorMessage), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response = BaseError.generateError(e, null,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        return response;
    }

    private Response handleError(Error e) {
        log.error(e.getMessage(), e);
        BaseError response = BaseError.generateError(e, null,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        return Response.serverError().entity(response).build();
    }
}
