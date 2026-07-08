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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal {@link ExpandableEntity} implementation used to exercise the extension branch of
 * {@link WaterJsonSerializer} / {@link WaterJsonDeserializer}. Unknown JSON properties are
 * captured into {@code extraFields} via {@code @JsonAnySetter}, matching the pattern the
 * deserializer expects ("mapped thanks to @JsonAnySetter (inside entity)").
 */
public class ExpandableTestEntity implements ExpandableEntity {
    private long id;
    private Date entityCreateDate;
    private Date entityModifyDate;
    private Integer entityVersion;
    private String name;
    private Map<String, Object> extraFields = new HashMap<>();
    private EntityExtension extension;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Date getEntityCreateDate() {
        return entityCreateDate;
    }

    public void setEntityCreateDate(Date entityCreateDate) {
        this.entityCreateDate = entityCreateDate;
    }

    @Override
    public Date getEntityModifyDate() {
        return entityModifyDate;
    }

    public void setEntityModifyDate(Date entityModifyDate) {
        this.entityModifyDate = entityModifyDate;
    }

    @Override
    public Integer getEntityVersion() {
        return entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.entityVersion = entityVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    @Override
    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields;
    }

    @JsonAnySetter
    public void addExtraField(String key, Object value) {
        this.extraFields.put(key, value);
    }

    @Override
    public EntityExtension getExtension() {
        return extension;
    }

    @Override
    public void setExtension(EntityExtension extension) {
        this.extension = extension;
    }
}
