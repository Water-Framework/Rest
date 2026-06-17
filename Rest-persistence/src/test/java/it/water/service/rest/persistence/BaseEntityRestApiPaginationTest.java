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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the pagination-cap logic in {@link BaseEntityRestApi#findAll}.
 *
 * Fix #22 coverage targets for the REST layer:
 *  - delta > MAX_PAGE_SIZE (200)     → clamped to 200 before forwarding to service
 *  - delta == MAX_PAGE_SIZE          → forwarded unchanged
 *  - delta one above MAX_PAGE_SIZE   → clamped to 200
 *  - delta null                      → set to HYPERIOT_DEFAULT_PAGINATION_DELTA (20)
 *  - delta == 0                      → set to HYPERIOT_DEFAULT_PAGINATION_DELTA
 *  - delta negative                  → set to HYPERIOT_DEFAULT_PAGINATION_DELTA
 *  - page null                       → set to 1
 *  - page == 0                       → set to 1
 *  - small valid delta               → forwarded unchanged (not grown to cap)
 *  - findAll() no-args               → delegates to findAll(null, null, null, null)
 *
 * The service layer call is verified via Mockito argument capture, so no
 * actual database or container is needed.
 */
@ExtendWith(MockitoExtension.class)
class BaseEntityRestApiPaginationTest {

    // ------------------------------------------------------------------
    // Minimal concrete subclass of the abstract BaseEntityRestApi<T>
    // ------------------------------------------------------------------

    /** Minimal entity stub — only needs to implement BaseEntity. */
    private static class StubEntity implements BaseEntity {
        private long id;
        private Integer entityVersion = 1;

        @Override public long getId() { return id; }
        @Override public Integer getEntityVersion() { return entityVersion; }
        @Override public void setEntityVersion(Integer version) { this.entityVersion = version; }
        @Override public java.util.Date getEntityCreateDate() { return null; }
        @Override public java.util.Date getEntityModifyDate() { return null; }
        @Override public boolean isExpandableEntity() { return false; }
    }

    private BaseEntityRestApi<StubEntity> restApi;

    @Mock
    private BaseEntityApi<StubEntity> entityApi;

    @Mock
    private PaginableResult<StubEntity> mockResult;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // BaseEntityApi.findAll signature: findAll(Query filter, int delta, int page, QueryOrder order)
        // We stub the broadest matcher so every test call gets a non-null result.
        Mockito.lenient().when(entityApi.findAll(
                        Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
               .thenReturn(mockResult);
        // findAll logs getEntityService().getEntityType().getSimpleName(); stub it to avoid NPE in the log line.
        Mockito.lenient().when(entityApi.getEntityType()).thenAnswer(inv -> StubEntity.class);

        restApi = new BaseEntityRestApi<>() {
            @Override
            protected BaseEntityApi<StubEntity> getEntityService() {
                return entityApi;
            }
        };
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Calls findAll and captures the effective delta argument forwarded to the service.
     * Signature of getEntityService().findAll: (Query filter, int delta, int page, QueryOrder order)
     */
    private int capturedDelta(Integer requestedDelta, Integer requestedPage) {
        Mockito.clearInvocations(entityApi);
        restApi.findAll(requestedDelta, requestedPage, null, null);
        ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
        // filter=null, order=null because we always pass null from the test
        Mockito.verify(entityApi).findAll(
                Mockito.<Query>isNull(), deltaCaptor.capture(), Mockito.anyInt(), Mockito.<QueryOrder>isNull());
        return deltaCaptor.getValue();
    }

    /** Captures the effective page forwarded to the service layer. */
    private int capturedPage(Integer requestedDelta, Integer requestedPage) {
        Mockito.clearInvocations(entityApi);
        restApi.findAll(requestedDelta, requestedPage, null, null);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(entityApi).findAll(
                Mockito.<Query>isNull(), Mockito.anyInt(), pageCaptor.capture(), Mockito.<QueryOrder>isNull());
        return pageCaptor.getValue();
    }

    // ------------------------------------------------------------------
    // Constant verification
    // ------------------------------------------------------------------

    @Test
    void maxPageSizeConstant_is200() {
        Assertions.assertEquals(200, BaseEntityRestApi.MAX_PAGE_SIZE);
    }

    @Test
    void defaultPaginationDeltaConstant_is20() {
        Assertions.assertEquals(20, BaseEntityRestApi.HYPERIOT_DEFAULT_PAGINATION_DELTA);
    }

    // ------------------------------------------------------------------
    // Delta clamping — above cap
    // ------------------------------------------------------------------

    @Test
    void findAll_deltaAboveMaxPageSize_clampedTo200() {
        int effective = capturedDelta(100_000, 1);
        Assertions.assertEquals(BaseEntityRestApi.MAX_PAGE_SIZE, effective,
                "delta=100000 must be clamped to MAX_PAGE_SIZE");
    }

    @Test
    void findAll_deltaOneAboveMaxPageSize_clampedTo200() {
        int effective = capturedDelta(BaseEntityRestApi.MAX_PAGE_SIZE + 1, 1);
        Assertions.assertEquals(BaseEntityRestApi.MAX_PAGE_SIZE, effective,
                "delta one above MAX_PAGE_SIZE must be clamped");
    }

    // ------------------------------------------------------------------
    // Delta at the boundary (not clamped)
    // ------------------------------------------------------------------

    @Test
    void findAll_deltaExactlyMaxPageSize_notClamped() {
        int effective = capturedDelta(BaseEntityRestApi.MAX_PAGE_SIZE, 1);
        Assertions.assertEquals(BaseEntityRestApi.MAX_PAGE_SIZE, effective,
                "delta == MAX_PAGE_SIZE must not be reduced");
    }

    @Test
    void findAll_smallValidDelta_notClamped() {
        int effective = capturedDelta(10, 1);
        Assertions.assertEquals(10, effective,
                "A small valid delta must be forwarded unchanged");
    }

    // ------------------------------------------------------------------
    // Delta null / zero / negative → HYPERIOT_DEFAULT_PAGINATION_DELTA
    // ------------------------------------------------------------------

    @Test
    void findAll_deltaNullPage1_usesDefaultDelta() {
        int effective = capturedDelta(null, 1);
        Assertions.assertEquals(BaseEntityRestApi.HYPERIOT_DEFAULT_PAGINATION_DELTA, effective,
                "null delta must fall back to HYPERIOT_DEFAULT_PAGINATION_DELTA");
    }

    @Test
    void findAll_deltaZeroPage1_usesDefaultDelta() {
        int effective = capturedDelta(0, 1);
        Assertions.assertEquals(BaseEntityRestApi.HYPERIOT_DEFAULT_PAGINATION_DELTA, effective,
                "delta=0 must fall back to HYPERIOT_DEFAULT_PAGINATION_DELTA");
    }

    @Test
    void findAll_deltaNegativePage1_usesDefaultDelta() {
        int effective = capturedDelta(-5, 1);
        Assertions.assertEquals(BaseEntityRestApi.HYPERIOT_DEFAULT_PAGINATION_DELTA, effective,
                "negative delta must fall back to HYPERIOT_DEFAULT_PAGINATION_DELTA");
    }

    // ------------------------------------------------------------------
    // Page null / zero → 1
    // ------------------------------------------------------------------

    @Test
    void findAll_pageNullValidDelta_usesPage1() {
        int effectivePage = capturedPage(10, null);
        Assertions.assertEquals(1, effectivePage,
                "null page must default to 1");
    }

    @Test
    void findAll_pageZeroValidDelta_usesPage1() {
        int effectivePage = capturedPage(10, 0);
        Assertions.assertEquals(1, effectivePage,
                "page=0 must default to 1");
    }

    @Test
    void findAll_pageNegativeValidDelta_usesPage1() {
        int effectivePage = capturedPage(10, -3);
        Assertions.assertEquals(1, effectivePage,
                "negative page must default to 1");
    }

    // ------------------------------------------------------------------
    // findAll() no-args delegates to findAll(null, null, null, null)
    // ------------------------------------------------------------------

    @Test
    void findAll_noArgs_delegatesToFindAllWithNullParams_usesDefaultDeltaAndPage1() {
        Mockito.clearInvocations(entityApi);
        restApi.findAll();
        ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(entityApi).findAll(
                Mockito.<Query>isNull(), deltaCaptor.capture(), pageCaptor.capture(), Mockito.<QueryOrder>isNull());
        Assertions.assertEquals(BaseEntityRestApi.HYPERIOT_DEFAULT_PAGINATION_DELTA, deltaCaptor.getValue(),
                "findAll() no-args must use default delta");
        Assertions.assertEquals(1, pageCaptor.getValue(),
                "findAll() no-args must use page=1");
    }

    // ------------------------------------------------------------------
    // Combined: huge delta with null page
    // ------------------------------------------------------------------

    @Test
    void findAll_hugeDeltaAndNullPage_clampsDeltaAndSetsPage1() {
        Mockito.clearInvocations(entityApi);
        restApi.findAll(999_999, null, null, null);
        ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(entityApi).findAll(
                Mockito.<Query>isNull(), deltaCaptor.capture(), pageCaptor.capture(), Mockito.<QueryOrder>isNull());
        Assertions.assertEquals(BaseEntityRestApi.MAX_PAGE_SIZE, deltaCaptor.getValue());
        Assertions.assertEquals(1, pageCaptor.getValue());
    }
}
