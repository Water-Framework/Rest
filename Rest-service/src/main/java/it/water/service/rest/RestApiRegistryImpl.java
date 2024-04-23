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

@FrameworkComponent
public class RestApiRegistryImpl implements RestApiRegistry {
    private Logger log = LoggerFactory.getLogger(RestApiRegistryImpl.class);
    private Map<Class<? extends RestApi>, Class<? extends RestApi>> registeredRestApis = new HashMap<>();
    @Inject
    @Setter
    private RestApiManager restApiManager;

    @Override
    public void addRestApiService(Class<? extends RestApi> restApiInterface, Class<? extends RestApi> concreteClass) {
        this.registeredRestApis.put(restApiInterface, concreteClass);
        signalRestApiManagerToRestart();
    }

    @Override
    public void removeRestApiService(Class<? extends RestApi> restApi) {
        this.registeredRestApis.remove(restApi);
        signalRestApiManagerToRestart();
    }

    @Override
    public Class<?> getRestApiImplementation(Class<? extends RestApi> restApi) {
        return registeredRestApis.get(restApi);
    }

    @Override
    public Map<Class<? extends RestApi>, Class<?>> getRegisteredRestApis() {
        return Collections.unmodifiableMap(registeredRestApis);
    }

    private void signalRestApiManagerToRestart() {
        try {
            if (restApiManager != null) {
                restApiManager.startRestApiServer();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
