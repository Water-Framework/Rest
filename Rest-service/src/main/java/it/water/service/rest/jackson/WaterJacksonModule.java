package it.water.service.rest.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import it.water.core.api.registry.ComponentRegistry;

public class WaterJacksonModule extends SimpleModule {

    private transient ComponentRegistry componentRegistry;

    public WaterJacksonModule(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        //Adding custom serializer and deserializer
        context.addBeanSerializerModifier(new WaterJsonSerializerModifier());
        context.addBeanDeserializerModifier(new WaterJsonDeserializerModifier(componentRegistry));
    }
}
