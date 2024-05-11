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

import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.service.rest.api.WaterJacksonMapper;
import lombok.Getter;

/**
 * @Author Aristide Cittadino.
 * If a developer wants to customize how jackson serializes/deserializes objects
 * he has just to create a component implementing WaterJacksonMapper creating the ObjectMapper with
 * the desired configuration.
 */
@FrameworkComponent(services = WaterJacksonMapper.class, priority = 1)
public class WaterDefaultJacksonMapper implements WaterJacksonMapper {
    @Getter
    private ObjectMapper mapper;

    public WaterDefaultJacksonMapper() {
        this.mapper = new ObjectMapper();
    }

    /**
     * @return a default object mapper with default configurations
     */
    @Override
    public ObjectMapper getJacksonMapper() {
        return mapper;
    }

}
