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
package it.water.service.rest;

import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RestApiRegistryImpl}. Only {@link Class} references to marker
 * interfaces/implementations are needed — the registry never instantiates them.
 */
@ExtendWith(MockitoExtension.class)
class RestApiRegistryImplTest {

    private interface SampleRestApi extends RestApi {
        // marker interface used only to exercise class-keyed registration
    }

    private interface OtherRestApi extends RestApi {
        // second marker interface used for negative lookups
    }

    private static class SampleRestApiImpl implements SampleRestApi {
        // empty concrete implementation, never instantiated by the registry
    }

    private static class OtherRestApiImplStub implements SampleRestApi {
        // used only to prove that the first registration wins (computeIfAbsent semantics)
    }

    @Mock
    private RestApiManager restApiManager;

    private RestApiRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new RestApiRegistryImpl();
        registry.setRestApiManager(restApiManager);
    }

    @Test
    void addRestApiServiceRegistersConcreteClass() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        Assertions.assertEquals(SampleRestApiImpl.class, registry.getRestApiImplementation(SampleRestApi.class));
    }

    @Test
    void addRestApiServiceDoesNotOverwriteExistingRegistration() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        registry.addRestApiService(SampleRestApi.class, OtherRestApiImplStub.class);
        Assertions.assertEquals(SampleRestApiImpl.class, registry.getRestApiImplementation(SampleRestApi.class),
                "computeIfAbsent must not overwrite an existing registration for the same interface");
    }

    @Test
    void getRestApiImplementationUnknownInterfaceReturnsNull() {
        Assertions.assertNull(registry.getRestApiImplementation(OtherRestApi.class));
    }

    @Test
    void removeRestApiServiceRemovesExistingRegistration() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        registry.removeRestApiService(SampleRestApi.class);
        Assertions.assertNull(registry.getRestApiImplementation(SampleRestApi.class));
    }

    @Test
    void removeRestApiServiceNotRegisteredDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> registry.removeRestApiService(OtherRestApi.class));
    }

    @Test
    void getRegisteredRestApisReturnsUnmodifiableMap() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        Map<Class<? extends RestApi>, Class<?>> registered = registry.getRegisteredRestApis();
        Assertions.assertEquals(1, registered.size());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> registered.put(OtherRestApi.class, OtherRestApiImplStub.class));
    }

    @Test
    void sendRestartApiManagerRestartRequestAfterRegistrationStartsServer() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        registry.sendRestartApiManagerRestartRequest();
        verify(restApiManager, times(1)).startRestApiServer();
    }

    @Test
    void sendRestartApiManagerRestartRequestWithoutChangeDoesNotRestartTwice() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        registry.sendRestartApiManagerRestartRequest();
        registry.sendRestartApiManagerRestartRequest();
        verify(restApiManager, times(1)).startRestApiServer();
    }

    @Test
    void removeRestApiServiceChangesUuidTriggeringRestart() {
        registry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        registry.sendRestartApiManagerRestartRequest();
        registry.removeRestApiService(SampleRestApi.class);
        registry.sendRestartApiManagerRestartRequest();
        verify(restApiManager, times(2)).startRestApiServer();
    }

    @Test
    void sendRestartApiManagerRestartRequestWithNullManagerDoesNotThrow() {
        RestApiRegistryImpl freshRegistry = new RestApiRegistryImpl();
        freshRegistry.addRestApiService(SampleRestApi.class, SampleRestApiImpl.class);
        Assertions.assertDoesNotThrow(freshRegistry::sendRestartApiManagerRestartRequest,
                "sendRestartApiManagerRestartRequest must tolerate a never-injected (null) RestApiManager");
        verify(restApiManager, never()).startRestApiServer();
    }
}
