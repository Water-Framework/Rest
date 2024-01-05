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
package it.water.service.rest.spring.controller;

import it.water.service.rest.RootRestApiImpl;
import it.water.service.rest.spring.api.SpringRootApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController extends RootRestApiImpl implements SpringRootApi {
    private Logger log = LoggerFactory.getLogger(RootController.class);
    @Override
    public String sayHi() {
        log.debug("Invoking Root Rest Controller Spring Version");
        return super.sayHi();
    }
}
