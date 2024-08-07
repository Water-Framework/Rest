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
package it.water.service.rest.jaxrs.api;

import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.service.rest.api.RootApi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * @Author Aristide Cittadino
 * Re-defining Rest API with specific technology annotations JAX RS.
 * Swagger Annotation can be set on the generic Api StatusApi
 */
@Path("/")
@FrameworkRestApi
public interface DefaultRootApi extends RootApi {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Override
    String sayHi();
}
