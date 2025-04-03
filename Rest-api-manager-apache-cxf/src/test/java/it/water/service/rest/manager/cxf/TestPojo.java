package it.water.service.rest.manager.cxf;

import com.fasterxml.jackson.annotation.*;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestPojo implements ExpandableEntity {
    @Getter
    @Setter
    private String fieldA;
    @Getter
    @Setter
    private String fieldB;

    private Map<String, Object> extraFields = new HashMap<>();

    private EntityExtension entityExtension = null;

    @Override
    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    @Override
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setExtraFields(Map<String, Object> map) {
        this.extraFields = map;
    }

    @JsonAnySetter
    public void setExtraFields(String key, Object value) {
        extraFields.put(key, value);
    }

    @JsonIgnore
    @Override
    public EntityExtension getExtension() {
        return entityExtension;
    }

    @Override
    public void setExtension(EntityExtension entityExtension) {
        this.entityExtension = entityExtension;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Date getEntityCreateDate() {
        return null;
    }

    @Override
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    public Integer getEntityVersion() {
        return 0;
    }

    @Override
    public void setEntityVersion(Integer integer) {
    }

    @Override
    @JsonIgnore
    public boolean isExpandableEntity() {
        return ExpandableEntity.super.isExpandableEntity();
    }

    @Override
    @JsonIgnore
    public String getResourceName() {
        return ExpandableEntity.super.getResourceName();
    }
}
