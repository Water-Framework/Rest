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
package it.water.service.rest.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.water.core.api.service.rest.RestApi;

import javax.ws.rs.core.MediaType;

/**
 * @Author Aristide Cittadino
 * Status API in order to check if application is working or not.
 * This interfaces is annotated with @FrameworkRestApi this means that each framework will search for implementation
 * of this rest interface with specific technology CXF, Spring , Rest Easy....
 * The framework will search for an interfaces which extends StatusApi. This interfaces should override same method of Status API
 * Using specific technology annotations ex. Spring MVC or Apache CXF (Jax RS annotations)
 */
@Api(produces = MediaType.TEXT_PLAIN, tags = "Status API")
public interface StatusApi extends RestApi {
    @ApiOperation(value = "/status", notes = "Check application status", httpMethod = "GET", produces = MediaType.TEXT_PLAIN)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    String checkModuleWorking();
}
