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
package it.water.service.rest.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.api.service.EntityExtensionService;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.service.rest.api.WaterJacksonMapper;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

/**
 * End-to-end coverage for {@link WaterJsonSerializer}, {@link WaterJsonDeserializer},
 * {@link WaterJsonSerializerModifier} and {@link WaterJsonDeserializerModifier}, exercised
 * through the real {@link it.water.service.rest.WaterDefaultJacksonMapper} the framework
 * wires up (registers {@link WaterJacksonModule}, which installs the bean modifiers on every
 * POJO the ObjectMapper (de)serializes).
 */
@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WaterJacksonRoundTripTest implements Service {

    private static final class DummyView {
        // marker class only, used to force serializers.getActiveView() != null
    }

    /**
     * Minimal {@link ComponentConfiguration} implementation, built without depending on
     * Core-registry (not exposed on this module's test compile classpath) so that a component
     * can be registered directly against {@link ComponentRegistry#registerComponent}.
     */
    private static final class SimpleComponentConfiguration implements ComponentConfiguration {
        private final Properties props = new Properties();

        void withProp(String name, String value) {
            props.put(name, value);
        }

        @Override
        public int getPriority() {
            return 1;
        }

        @Override
        public boolean isPrimary() {
            return false;
        }

        @Override
        public Properties getConfiguration() {
            return props;
        }

        @Override
        public Dictionary<String, Object> getConfigurationAsDictionary() {
            Hashtable<String, Object> table = new Hashtable<>();
            props.forEach((key, value) -> table.put(String.valueOf(key), value));
            return table;
        }

        @Override
        public void addProperty(String name, Object value) {
            props.put(name, value);
        }

        @Override
        public void removeProperty(String name) {
            props.remove(name);
        }

        @Override
        public boolean hasProperty(String name) {
            return props.containsKey(name);
        }
    }

    @Inject
    @Setter
    private WaterJacksonMapper waterJacksonMapper;

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Test
    @Order(1)
    void serializeAndDeserializeSimpleEntityWithoutActiveView() throws Exception {
        ObjectMapper mapper = waterJacksonMapper.getJacksonMapper();
        Assertions.assertNotNull(mapper, "WaterDefaultJacksonMapper must expose an initialized ObjectMapper");

        SimpleTestEntity entity = new SimpleTestEntity();
        entity.setId(10L);
        entity.setName("water");

        String json = mapper.writeValueAsString(entity);
        Assertions.assertTrue(json.contains("water"), "Serialized JSON must contain the entity's field value");

        SimpleTestEntity restored = mapper.readValue(json, SimpleTestEntity.class);
        Assertions.assertEquals(10L, restored.getId());
        Assertions.assertEquals("water", restored.getName());
    }

    @Test
    @Order(2)
    void serializeAndDeserializeSimpleEntityWithActiveView() throws Exception {
        ObjectMapper mapper = waterJacksonMapper.getJacksonMapper();

        SimpleTestEntity entity = new SimpleTestEntity();
        entity.setId(20L);
        entity.setName("viewed");

        // Forces serializers.getActiveView() != null inside WaterJsonSerializer
        String json = mapper.writerWithView(DummyView.class).writeValueAsString(entity);
        Assertions.assertTrue(json.contains("viewed"));

        // Forces ctxt.getActiveView() != null inside WaterJsonDeserializer
        SimpleTestEntity restored = mapper.readerWithView(DummyView.class)
                .forType(SimpleTestEntity.class)
                .readValue(json);
        Assertions.assertEquals(20L, restored.getId());
        Assertions.assertEquals("viewed", restored.getName());
    }

    @Test
    @Order(3)
    void deserializeExpandableEntityCapturesExtraFieldsWithoutExtensionService() throws Exception {
        ObjectMapper mapper = waterJacksonMapper.getJacksonMapper();
        String json = "{\"id\":1,\"name\":\"exp\",\"customField\":\"customValue\"}";

        ExpandableTestEntity restored = mapper.readValue(json, ExpandableTestEntity.class);

        Assertions.assertEquals(1L, restored.getId());
        Assertions.assertNotNull(restored.getExtraFields());
        Assertions.assertEquals("customValue", restored.getExtraFields().get("customField"),
                "Unknown JSON properties must be captured via @JsonAnySetter into extraFields");
        // No EntityExtensionService registered for this type -> extension must remain null
        Assertions.assertNull(restored.getExtension());

        // Serializing back exercises the ExpandableEntity branch of WaterJsonSerializer
        // (extension == null, so the extra-fields-to-map conversion is skipped).
        String out = mapper.writeValueAsString(restored);
        Assertions.assertNotNull(out);
    }

    @Test
    @Order(4)
    void deserializeExpandableEntityConvertsExtraFieldsWhenExtensionServiceRegistered() throws Exception {
        EntityExtensionService extensionService = new EntityExtensionService() {
            @Override
            public Class<? extends BaseEntity> relatedType() {
                return ExpandableTestEntity.class;
            }

            @Override
            public Class<? extends BaseEntity> type() {
                return TestEntityExtension.class;
            }
        };
        SimpleComponentConfiguration configuration = new SimpleComponentConfiguration();
        configuration.withProp(EntityExtensionService.RELATED_ENTITY_PROPERTY, ExpandableTestEntity.class.getName());
        // Registered against the shared (JVM-wide) TestRuntimeInitializer registry, so it is
        // unregistered again in the finally block to avoid leaking state into other test classes.
        ComponentRegistration<EntityExtensionService, ?> registration =
                componentRegistry.registerComponent(EntityExtensionService.class, extensionService, configuration);
        try {
            ObjectMapper mapper = waterJacksonMapper.getJacksonMapper();
            String json = "{\"id\":2,\"name\":\"exp2\",\"extraNumber\":42}";

            ExpandableTestEntity restored = mapper.readValue(json, ExpandableTestEntity.class);

            Assertions.assertNotNull(restored.getExtension(),
                    "A registered EntityExtensionService matching the entity type must produce a non-null extension");
            Assertions.assertTrue(restored.getExtension() instanceof TestEntityExtension);
            Assertions.assertEquals(42, ((TestEntityExtension) restored.getExtension()).getExtraNumber());

            // Serializing back exercises the branch where extension != null (converted to a map)
            String out = mapper.writeValueAsString(restored);
            Assertions.assertNotNull(out);
        } finally {
            componentRegistry.unregisterComponent(registration);
        }
    }
}
