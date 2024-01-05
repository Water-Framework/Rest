
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

package it.water.service.rest.persistence;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.service.BaseEntityApi;
import it.water.core.api.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @param <T> parameter that indicates a generic class
 * @Author Aristide Cittadino.
 * Model class for BaseEntityRestApi. This
 * class implements all methods to test every response, and any
 * exceptions, produced in the CRUD operations invoked by the Rest
 * services.
 */
public abstract class BaseEntityRestApi<T extends BaseEntity> implements Service {
    private Logger log = LoggerFactory.getLogger(BaseEntityRestApi.class.getName());

    public static final int HYPERIOT_DEFAULT_PAGINATION_DELTA = 20;

    /**
     * Response and any exceptions for save operation
     *
     * @param entity parameter that indicates a generic entity
     * @return response for save operation
     */
    public T save(T entity) {
        log.debug("Invoking Save entity from rest service for {} {}", this.getEntityService().getEntityType().getSimpleName(), entity);
        entity = this.getEntityService().save(entity);
        return entity;
    }

    /**
     * Response and any exceptions for update operation
     *
     * @param entity parameter that indicates a generic entity
     * @return response for update operation
     */
    public T update(T entity) {
        log.debug("Invoking Update entity from rest service for {} {}", this.getEntityService().getEntityType().getSimpleName(), entity);
        return this.getEntityService().update(entity);
    }

    /**
     * Response and any exceptions for remove operation
     *
     * @param id parameter that indicates a entity id
     * @return response for remove operation
     */
    public void remove(long id) {
        log.debug("Invoking Remove entity from rest service for {} with id: {}", this.getEntityService().getEntityType().getSimpleName(), id);
        this.getEntityService().remove(id);
    }

    /**
     * Response and any exceptions for find operation
     *
     * @param id parameter that indicates a entity id
     * @return response for find operation
     */
    public T find(long id) {
        log.debug("Invoking Find entity from rest service for {} with id: {}", this.getEntityService().getEntityType().getSimpleName(), id);
        return this.getEntityService().find(id);
    }

    /**
     * Response and any exceptions for find all
     *
     * @return response for find all
     */
    public PaginableResult<T> findAll(Integer delta, Integer page, Query filter, QueryOrder order) {
        log.debug("Invoking Find All entity from rest service for {}", this.getEntityService().getEntityType().getSimpleName());
        if (delta == null || delta <= 0) delta = HYPERIOT_DEFAULT_PAGINATION_DELTA;
        if (page == null || page <= 0) page = 1;
        return this.getEntityService().findAll(filter, delta, page, order);
    }

    /**
     * Return current EntityService
     */
    protected abstract BaseEntityApi<T> getEntityService();

}
