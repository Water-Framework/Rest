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
package it.water.service.rest.spring.api;

import it.water.core.api.service.rest.RestApi;
import it.water.service.rest.api.security.LoggedIn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestMapping("/test")
public interface TestApi extends RestApi {
    @LoggedIn(issuers = "it.water.service.rest.sprint.api.FakeUser")
    @Produces(MediaType.TEXT_PLAIN)
    @GetMapping
    @RequestMapping("/authenticatedOperation")
    String authenticatedOperation();

    @GetMapping
    @Produces(MediaType.TEXT_PLAIN)
    @RequestMapping("/anonymousOperation")
    String anonymousOperation();
}
