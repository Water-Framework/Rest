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
package it.water.service.rest.persistence.test;

import it.water.core.api.service.BaseEntityApi;

import it.water.service.rest.persistence.BaseEntityRestApi;
import it.water.service.rest.persistence.test.entity.TestEntity;
import it.water.service.rest.persistence.test.service.TestEntityServiceImpl;

public class TestEntityServiceRest extends BaseEntityRestApi<TestEntity> {

    private TestEntityServiceImpl testEntityService;

    public TestEntityServiceRest(TestEntityServiceImpl testEntityService) {
        this.testEntityService = testEntityService;
    }

    @Override
    protected BaseEntityApi<TestEntity> getEntityService() {
        return testEntityService;
    }
}
