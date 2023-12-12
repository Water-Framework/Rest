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
package it.water.service.rest;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractRestApiManager implements RestApiManager {
    private Map<Class<? extends RestApi>, Class<?>> registeredRestApis = new HashMap<>();
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private ComponentRegistry componentRegistry;

    @Override
    public void addRestApiService(Class<? extends RestApi> restApi, Class<?> restImplementation) {
        this.registeredRestApis.put(restApi, restImplementation);
        this.startRestApiServer();
    }

    @Override
    public void removeRestApiService(Class<? extends RestApi> restApi) {
        this.registeredRestApis.remove(restApi);
        this.startRestApiServer();
    }

    @Override
    public Class<?> getRestImplementation(Class<? extends RestApi> restApi) {
        return registeredRestApis.get(restApi);
    }

    @Override
    public Set<Class<? extends RestApi>> getRegisteredApis() {
        return Collections.unmodifiableSet(registeredRestApis.keySet());
    }

}
