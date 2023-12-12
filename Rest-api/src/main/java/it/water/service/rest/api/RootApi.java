/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

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
 * Root API can be used to show generic information about the framework.
 */
@Api(produces = MediaType.TEXT_PLAIN, tags = "Root API")
public interface RootApi extends RestApi {
    @ApiOperation(value = "/", notes = "Application Root API", httpMethod = "GET", produces = MediaType.TEXT_PLAIN)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    String sayHi();
}
