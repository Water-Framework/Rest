
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
import it.water.service.rest.api.StatusApi;


/**
 * @Author Aristide Cittadino.
 * This service is a cross-framework implementation of the StatusApi.
 * At runtime the bundle initializer will instantiate a proxy of this class making it implementing framework specific interfaces
 * with the right rest annotations (Spring MVC or JAX RS).
 * Swagger annotation can stay in the cross framework class since are valid for every framework.
 */

@FrameworkRestController(referredRestApi = StatusApi.class)
public class StatusRestApiImpl implements StatusApi {

    /**
     * Simple service for checking module status
     *
     * @return WaterBase Module works!
     */
    @Override
    public String checkModuleWorking() {
        return "Water Rest Module works!";
    }

}

