package it.water.service.rest.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import it.water.core.api.registry.ComponentRegistry;

public class WaterJsonDeserializerModifier extends BeanDeserializerModifier {
    private ComponentRegistry registry;

    public WaterJsonDeserializerModifier(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(
            DeserializationConfig config,
            BeanDescription beanDesc,
            JsonDeserializer<?> defaultDeserializer) {
        return new WaterJsonDeserializer(registry);
    }
}
