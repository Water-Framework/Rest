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
package it.water.service.rest.persistence.test.entity;

import it.water.core.api.permission.ProtectedEntity;

import java.util.Date;

/**
 * Just a test entity, persistence is not needed
 */
public class TestEntity implements ProtectedEntity {
    private long id;
    private Date entityCreateDate;
    private Date entityModifyDate;
    private int entityVersion;
    private String entityField;

    public String getEntityField() {
        return entityField;
    }

    public void setEntityField(String entityField) {
        this.entityField = entityField;
    }

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
    public int getEntityVersion() {
        return entityVersion;
    }

    @Override
    public String getSystemApiClassName() {
        return null;
    }

    public void setEntityVersion(int entityVersion) {
        this.entityVersion = entityVersion;
    }

}
