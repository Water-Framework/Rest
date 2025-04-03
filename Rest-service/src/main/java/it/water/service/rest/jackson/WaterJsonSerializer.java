package it.water.service.rest.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class WaterJsonSerializer extends JsonSerializer<Object> {
    private static final Logger logger = LoggerFactory.getLogger(WaterJsonSerializer.class);
    private final JsonSerializer<Object> defaultSerializer;
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    public WaterJsonSerializer(JsonSerializer<Object> defaultSerializer) {
        this.defaultSerializer = defaultSerializer;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        logger.debug("Serializing {} to {}", value, defaultSerializer);
        if (value instanceof ExpandableEntity expandableEntity) {
            EntityExtension extension = expandableEntity.getExtension();
            if (extension != null) {
                Map<String, Object> extraFields = objectMapper.convertValue(extension, new TypeReference<Map<String, Object>>() {
                });
                expandableEntity.setExtraFields(extraFields);
            }
        }
        Class<?> activeView = serializers.getActiveView();
        if (activeView != null)
            objectMapper.writerWithView(activeView).writeValue(gen, value);
        else
            objectMapper.writer().writeValue(gen, value);
    }
}
