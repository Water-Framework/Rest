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
import it.water.core.api.registry.ComponentRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WaterDefaultJacksonMapper}, focusing on the "initialize only once"
 * guard on both the {@code @OnActivate} entry point and the direct {@code init(...)} call.
 */
@ExtendWith(MockitoExtension.class)
class WaterDefaultJacksonMapperTest {

    @Mock
    private ComponentRegistry componentRegistry;

    @Test
    void onActivateInitializesMapperOnlyOnce() {
        WaterDefaultJacksonMapper mapper = new WaterDefaultJacksonMapper();
        Assertions.assertNull(mapper.getJacksonMapper(), "Before activation the mapper must not be initialized");

        mapper.onActivate(componentRegistry);
        ObjectMapper firstMapper = mapper.getJacksonMapper();
        Assertions.assertNotNull(firstMapper, "After activation the ObjectMapper must be created");

        // Second activation must be a no-op due to the `initialized` guard.
        mapper.onActivate(componentRegistry);
        Assertions.assertSame(firstMapper, mapper.getJacksonMapper(),
                "A second activation must not replace the already-initialized ObjectMapper instance");
    }

    @Test
    void initDirectlyBuildsObjectMapperOnlyOnce() {
        WaterDefaultJacksonMapper mapper = new WaterDefaultJacksonMapper();
        mapper.init(componentRegistry);
        ObjectMapper firstMapper = mapper.getJacksonMapper();
        Assertions.assertNotNull(firstMapper, "init(...) must create the ObjectMapper");

        mapper.init(componentRegistry);
        Assertions.assertSame(firstMapper, mapper.getJacksonMapper(),
                "Calling init(...) twice must not rebuild the ObjectMapper");
    }
}
