
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

import it.water.core.api.service.rest.FrameworkRestController;
import it.water.service.rest.api.RootApi;


/**
 * @Author Aristide Cittadino.
 * This service is a cross-framework implementation of the RootApi.
 * At runtime the bundle initializer will instantiate a proxy of this class making it implementing framework specific interfaces
 * with the right rest annotations (Spring MVC or JAX RS).
 * Swagger annotation can stay in the cross framework class since are valid for every framework.
 */

@FrameworkRestController(referredRestApi = RootApi.class)
public class RootRestApiImpl implements RootApi {

    /**
     * Root API Service
     * TODO: define which information must be shown for example framework versions or else...
     *
     * @return "Hi"
     */
    @Override
    public String sayHi() {
        return "Hi";
    }
}

