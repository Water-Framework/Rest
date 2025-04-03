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
package it.water.service.rest.manager.cxf;

import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.RestApi;
import it.water.service.rest.api.security.LoggedIn;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/test")
@FrameworkRestApi
public interface RestTestApi extends RestApi {
    @LoggedIn(issuers = "it.water.service.rest.manager.cxf.FakeUser")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/authenticatedOperation")
    TestPojo authenticatedOperation();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/anonymousOperation")
    void anonymousOperation(TestPojo testPojo);
}
