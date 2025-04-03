package it.water.service.rest.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.service.EntityExtensionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class WaterJsonDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private final Logger logger = LoggerFactory.getLogger(WaterJsonDeserializer.class);

    private static ObjectMapper objectMapper;
    private ComponentRegistry componentRegistry;
    private JavaType currentEntityType;

    static {
        objectMapper = new ObjectMapper();
        //todo should be inside a property
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public WaterJsonDeserializer(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Class<?> activeView = ctxt.getActiveView();
        Object result = null;
        if (activeView != null)
            result = objectMapper.readerWithView(activeView).readValue(p, this.currentEntityType);
        else
            result = objectMapper.readValue(p, this.currentEntityType);
        if (result instanceof ExpandableEntity expandableEntity) {
            Map<String, Object> extraFields = expandableEntity.getExtraFields();
            EntityExtensionService entityExtensionService = checkExtensionServiceExists(result);
            if (extraFields != null && !extraFields.isEmpty() && entityExtensionService != null) {
                try {
                    EntityExtension extension = (EntityExtension) objectMapper.convertValue(extraFields, entityExtensionService.type());
                    expandableEntity.setExtension(extension);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return result;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
        this.currentEntityType = deserializationContext.getContextualType();
        return this;
    }

    private EntityExtensionService checkExtensionServiceExists(Object entity) {
        try {
            ComponentFilter filter = componentRegistry.getComponentFilterBuilder().createFilter(EntityExtensionService.RELATED_ENTITY_PROPERTY, entity.getClass().getName());
            return componentRegistry.findComponent(EntityExtensionService.class, filter);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
        return null;
    }
}
