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
package it.water.service.rest.spring.controller;

import it.water.service.rest.StatusRestApiImpl;
import it.water.service.rest.spring.api.SpringStatusApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class StatusController extends StatusRestApiImpl implements SpringStatusApi {
    private Logger log = LoggerFactory.getLogger(StatusController.class);
    @Override
    public String checkModuleWorking() {
        log.debug("Invoking Status Rest Controller Spring Version");
        return super.checkModuleWorking();
    }
}
