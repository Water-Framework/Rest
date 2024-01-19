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

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApi;
import it.water.core.interceptors.WaterAbstractInterceptor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author Aristide Cittadino
 * This class implements the concept of Rest Controller Proxy.
 * Each Rest Controller registered will be instantiated with specific interface suppoting
 * different rest service frameworks like Spring MVC, Apache CXF or Rest Easy.
 */
public class RestControllerProxy extends WaterAbstractInterceptor<RestApi> implements InvocationHandler {
    private static Logger log = LoggerFactory.getLogger(RestControllerProxy.class);
    private Object concreteRestController;
    @Getter
    private ComponentRegistry componentsRegistry;

    public RestControllerProxy(Object concreteRestController, ComponentRegistry componentRegistry) {
        this.concreteRestController = concreteRestController;
        this.componentsRegistry = componentRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("Invoking method {} with args {}", method, args);
        Class<?>[] argsClasses = (args == null) ? null : new Class[args.length];
        for (int i = 0; args != null && i < args.length; i++) {
            argsClasses[i] = method.getParameterTypes()[i];
        }
        //Since invocation happens on the generic interface, the system must match the interface method with the implementation
        //So we search for the same method inside the annotated @FrameworkRestController class and invokes it
        Method concreteRestControllerMethod = concreteRestController.getClass().getMethod(method.getName(), argsClasses);
        if (concreteRestControllerMethod != null) {
            executeInterceptorBeforeMethod((RestApi) concreteRestController, method, args);
            Object invoke = concreteRestControllerMethod.invoke(concreteRestController, args);
            executeInterceptorAfterMethod((RestApi) concreteRestController, method, args, invoke);
            return invoke;
        }
        return null;
    }

    public static Object createRestProxy(ComponentRegistry componentsRegistry,Class<?> concreteRestApiInterface, Object concreteRestControllerInstance) {
        log.debug("Creating Rest Proxy for {} with instance {}", concreteRestApiInterface, concreteRestControllerInstance);
        RestControllerProxy restControllerProxy = new RestControllerProxy(concreteRestControllerInstance,componentsRegistry);
        if (concreteRestApiInterface != null) {
            return Proxy.newProxyInstance(RestControllerProxy.class.getClassLoader(), new Class[]{concreteRestApiInterface}, restControllerProxy);
        }
        return null;
    }
}
