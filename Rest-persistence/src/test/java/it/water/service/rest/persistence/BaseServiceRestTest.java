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
package it.water.service.rest.persistence;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.query.order.DefaultQueryOrder;
import it.water.service.rest.persistence.test.TestEntityServiceRest;
import it.water.service.rest.persistence.test.entity.TestEntity;
import it.water.service.rest.persistence.test.service.TestEntityServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class BaseServiceRestTest {
    @Mock
    private TestEntityServiceImpl testEntityService;
    private TestEntity simpleTestEntity;

    @BeforeAll
    private void beforeAll() {
        simpleTestEntity = new TestEntity();
        simpleTestEntity.setEntityField("field");
        simpleTestEntity.setEntityVersion(1);
        simpleTestEntity.setId(1);
    }

    @Test
    void testSaveRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.save(Mockito.any(TestEntity.class))).thenReturn(simpleTestEntity);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertNotNull(testEntityServiceRest.save(simpleTestEntity));
    }

    @Test
    void testUpdateRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.update(Mockito.any(TestEntity.class))).thenReturn(simpleTestEntity);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertNotNull(testEntityServiceRest.update(simpleTestEntity));
    }

    @Test
    void testRemoveRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertDoesNotThrow(() -> testEntityServiceRest.remove(1l));
    }

    @Test
    void testFindRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.find(Mockito.any(Long.class))).thenReturn(simpleTestEntity);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertNotNull(testEntityServiceRest.find(1L));
    }

    @Test
    void testFindAllRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        PaginableResult<TestEntity> paginableResult = new PaginatedResult<>(1, 1, 1, 1, Arrays.asList(simpleTestEntity));
        Mockito.when(testEntityService.findAll(Mockito.any(), Mockito.any(Integer.class), Mockito.any(Integer.class), Mockito.any())).thenReturn(paginableResult);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertNotNull(testEntityServiceRest.findAll(1, 1, null, null));
    }

    @Test
    void testExceptionOnSaveRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.save(Mockito.any(TestEntity.class))).thenThrow(DuplicateEntityException.class);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertThrows(DuplicateEntityException.class, () -> testEntityServiceRest.save(simpleTestEntity));
    }

    @Test
    void testRuntimeExceptionOnSaveRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.save(Mockito.any(TestEntity.class))).thenThrow(WaterRuntimeException.class);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertThrows(WaterRuntimeException.class, () -> testEntityServiceRest.save(simpleTestEntity));
    }

    @Test
    void testUExceptionOnUpdateRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.update(Mockito.any(TestEntity.class))).thenThrow(ValidationException.class);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertThrows(ValidationException.class, () -> testEntityServiceRest.update(simpleTestEntity));
    }

    @Test
    void testExceptionOnRemoveRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.doThrow(UnauthorizedException.class).when(testEntityService).remove(1);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertThrows(UnauthorizedException.class,() -> testEntityServiceRest.remove(1l));
    }

    @Test
    void testExceptionOnFindRestMethod() {
        Mockito.when(testEntityService.getEntityType()).thenCallRealMethod();
        Mockito.when(testEntityService.find(Mockito.any(Long.class))).thenThrow(EntityNotFound.class);
        TestEntityServiceRest testEntityServiceRest = new TestEntityServiceRest(testEntityService);
        Assertions.assertThrows(EntityNotFound.class, () -> testEntityServiceRest.find(1L));
    }

}
