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

import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.BaseError;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.validation.ValidationError;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.service.rest.api.options.RestOptions;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Collections;

@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestServiceTest implements Service {

    @Inject
    @Setter
    private RestOptions restOptions;

    @Test
    void testGenericExceptionMapper() {
        GenericExceptionMapperProvider exceptionMapperProvider = new GenericExceptionMapperProvider();
        Response r = exceptionMapperProvider.toResponse(new UnauthorizedException());
        Assertions.assertEquals(401, r.getStatus());
        GenericExceptionMapperProvider.ErrorMessage errorMessage = new GenericExceptionMapperProvider.ErrorMessage(403, "message");
        Assertions.assertEquals(403, errorMessage.getStatus());
        Assertions.assertEquals("message", errorMessage.getMessage());
        BaseError error = exceptionMapperProvider.handleException(new DuplicateEntityException(new String[]{"a","b"}));
        Assertions.assertEquals(409,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new ValidationException(Collections.emptyList()));
        Assertions.assertEquals(422,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new EntityNotFound());
        Assertions.assertEquals(404,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new NoResultException());
        Assertions.assertEquals(404,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new UnauthorizedException());
        Assertions.assertEquals(401,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new RuntimeException());
        Assertions.assertEquals(500,error.getStatusCode());
        error = exceptionMapperProvider.handleException(new IllegalArgumentException());
        Assertions.assertEquals(500,error.getStatusCode());
    }

    @Test
    void testRestOptions(){
        Assertions.assertNotNull(restOptions.securityOptions());
        Assertions.assertNotNull(restOptions.restRootContext());
        Assertions.assertNotNull(restOptions.servicesUrl());
        Assertions.assertNotNull(restOptions.frontendUrl());
        Assertions.assertNotNull(restOptions.uploadFolderPath());
        Assertions.assertTrue(restOptions.uploadMaxFileSize() > 0);
    }

}
