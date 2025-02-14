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

import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.api.service.rest.RestApiRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@FrameworkComponent
public class RestApiRegistryImpl implements RestApiRegistry {
    private Logger log = LoggerFactory.getLogger(RestApiRegistryImpl.class);
    private Map<Class<? extends RestApi>, Class<? extends RestApi>> registeredRestApis = new HashMap<>();
    private UUID currentUuid = UUID.randomUUID();
    private UUID lastUuid = null;
    @Inject
    @Setter
    private RestApiManager restApiManager;

    @Override
    public void addRestApiService(Class<? extends RestApi> restApiInterface, Class<? extends RestApi> concreteClass) {
        log.debug("Registering rest api interface {}", restApiInterface.getName());
        currentUuid = UUID.randomUUID();
        this.registeredRestApis.computeIfAbsent(restApiInterface, key -> concreteClass);
    }

    @Override
    public void removeRestApiService(Class<? extends RestApi> restApi) {
        log.debug("Removing rest api registration interface {}", restApi.getName());
        if (this.registeredRestApis.containsKey(restApi)) {
            currentUuid = UUID.randomUUID();
            this.registeredRestApis.remove(restApi);
        }
    }

    @Override
    public Class<?> getRestApiImplementation(Class<? extends RestApi> restApi) {
        return registeredRestApis.get(restApi);
    }

    @Override
    public void sendRestartApiManagerRestartRequest() {
        if (!currentUuid.equals(lastUuid) && restApiManager != null) {
            lastUuid = currentUuid;
            restApiManager.startRestApiServer();
        }
    }

    @Override
    public Map<Class<? extends RestApi>, Class<?>> getRegisteredRestApis() {
        return Collections.unmodifiableMap(registeredRestApis);
    }

}
