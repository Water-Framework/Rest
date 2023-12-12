/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.service.rest.persistence.test.service;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.service.BaseEntityApi;

import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.service.rest.persistence.test.entity.TestEntity;


/**
 * This class will be mocked inside tests
 */
public class TestEntityServiceImpl implements BaseEntityApi<TestEntity> {
    @Override
    public TestEntity save(TestEntity entity) {
        return null;
    }

    @Override
    public TestEntity update(TestEntity entity) {
        return entity;
    }

    /**
     * Used to simulate different
     *
     * @param id parameter that indicates a entity id
     */
    @Override
    public void remove(long id) {
        if (id > 1)
            throw new UnauthorizedException();
    }

    @Override
    public TestEntity find(long id) {
        return null;
    }

    @Override
    public TestEntity find(Query filter) {
        return null;
    }

    @Override
    public PaginableResult<TestEntity> findAll(Query filter, int delta, int page, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query filter) {
        return 0;
    }

    @Override
    public Class<TestEntity> getEntityType() {
        return TestEntity.class;
    }
}
