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
package it.water.service.rest.api.security;

import it.water.core.api.permission.SecurityContext;

/**
 * @Author Aristide Cittadino
 * Class which maps the concept of REST security Context extending both Framework Security Context and javax.ws.rs.core.SecurityContext.
 * Ti class will be the abstract entity between all implementation (spring,osgi,quarkus....) which wraps the concept of the rest security context.
 */
public interface RsSecurityContext extends SecurityContext, javax.ws.rs.core.SecurityContext {

}
