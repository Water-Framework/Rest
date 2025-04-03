package it.water.service.rest.manager.cxf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.EntityExtension;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

public class TestPojoExtension implements EntityExtension {

    @Getter
    @Setter
    private String extensionField;

    public TestPojoExtension() {
    }

    @Override
    @JsonIgnore
    public void setupExtensionFields(long l, BaseEntity baseEntity) {

    }

    @Override
    @JsonIgnore
    public long getRelatedEntityId() {
        return 0;
    }

    @Override
    public void setRelatedEntityId(long l) {

    }

    @Override
    @JsonIgnore
    public long getId() {
        return 0;
    }

    @Override
    @JsonIgnore
    public Date getEntityCreateDate() {
        return null;
    }

    @Override
    @JsonIgnore
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    @JsonIgnore
    public Integer getEntityVersion() {
        return 0;
    }

    @Override
    public void setEntityVersion(Integer integer) {

    }

    @Override
    @JsonIgnore
    public boolean isExpandableEntity() {
        return EntityExtension.super.isExpandableEntity();
    }

    @Override
    @JsonIgnore
    public String getResourceName() {
        return EntityExtension.super.getResourceName();
    }
}
