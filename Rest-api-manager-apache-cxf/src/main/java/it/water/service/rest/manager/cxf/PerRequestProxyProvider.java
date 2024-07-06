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
package it.water.service.rest.manager.cxf;

import it.water.core.api.registry.ComponentRegistry;
import it.water.service.rest.RestControllerProxy;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * @Author Aristide Cittadino
 * Custom Per Request Resource Provider which creates a Proxy which implements a Concrete Rest Api Implementation
 * And register as CXF Rest Resource.
 */
public class PerRequestProxyProvider implements ResourceProvider {
    private Logger log = LoggerFactory.getLogger(PerRequestProxyProvider.class);
    private Class<?> concreteRestApiInterface;
    private Constructor<?> serviceClassDefaultConstructor;
    private Method postConstructMethod;
    private Method preDestroyMethod;
    private ComponentRegistry componentRegistry;

    /**
     * @param concreteRestApiInterface    The Framework Rest Api Interface - which uses a specific rest framework ex. Jax RS
     * @param concreteRestControllerClass The Concrete Rest Controller which is annotated with @FrameworkRestController and references a generic Rest Api
     */
    public PerRequestProxyProvider(ComponentRegistry componentRegistry, Class<?> concreteRestApiInterface, Class<?> concreteRestControllerClass) {
        log.debug("Creating Per Request Proxy Provider for interface {} with implementation: {}", concreteRestApiInterface.getName(), concreteRestControllerClass.getName());
        this.concreteRestApiInterface = concreteRestApiInterface;
        this.componentRegistry = componentRegistry;
        Optional<Constructor<?>> defaultConstructor = Arrays.stream(concreteRestControllerClass.getConstructors()).filter(constructor -> constructor.getParameterCount() == 0).findAny();
        if (defaultConstructor.isEmpty()) {
            throw new UnsupportedOperationException("@FrameworkRestController " + concreteRestControllerClass.getName() + " must have default constructor!");
        }
        //instantiates the concrete controller
        serviceClassDefaultConstructor = defaultConstructor.get();
        postConstructMethod = ResourceUtils.findPostConstructMethod(concreteRestControllerClass);
        preDestroyMethod = ResourceUtils.findPreDestroyMethod(concreteRestControllerClass);
    }

    @Override
    public Object getInstance(Message m) {
        try {
            Object instance = serviceClassDefaultConstructor.newInstance();
            InjectionUtils.invokeLifeCycleMethod(instance, postConstructMethod);
            return RestControllerProxy.createRestProxy(componentRegistry, concreteRestApiInterface, instance);
        } catch (Exception ex) {
            String msg = serviceClassDefaultConstructor.getDeclaringClass().getName() + " cannot be instantiated";
            throw new InternalServerErrorException(serverError(msg));
        }
    }

    private Response serverError(String msg) {
        return Response.serverError().entity(msg).build();
    }

    /**
     * {@inheritDoc}
     */
    public void releaseInstance(Message m, Object o) {
        InjectionUtils.invokeLifeCycleMethod(o, preDestroyMethod);
    }

    @Override
    public Class<?> getResourceClass() {
        return concreteRestApiInterface;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
