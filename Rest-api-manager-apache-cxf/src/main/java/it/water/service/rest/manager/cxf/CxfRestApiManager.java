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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestFilter;

import it.water.core.api.service.rest.RestApiManager;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.service.rest.RestApiRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.service.rest.AbstractRestApiManager;
import it.water.service.rest.GenericExceptionMapperProvider;
import it.water.service.rest.api.WaterJacksonMapper;
import it.water.service.rest.api.options.RestOptions;
import it.water.service.rest.manager.cxf.security.filters.jwt.CxfJwtAuthenticationFilter;
import lombok.Setter;

/**
 * @Author Aristide Cittadino
 * <p>
 * Rest Api Manager for Apache CXF.
 * If Apache CXF is the runtime chosen for rest APIs, it will register every rest jax rs service found inside the application classpath.
 */
@FrameworkComponent
public class CxfRestApiManager extends AbstractRestApiManager implements RestApiManager {
    private Logger log = LoggerFactory.getLogger(CxfRestApiManager.class.getName());
    @Inject
    @Setter
    private RestOptions restOptions;
    @Inject
    @Setter
    private ComponentRegistry componentRegistry;
    @Inject
    @Setter
    private RestApiRegistry restApiRegistry;
    @Inject
    @Setter
    private WaterJacksonMapper waterJacksonMapper;

    private Server server;
    private CxfJwtAuthenticationFilter jwtAuthenticationFilter;

    @OnActivate
    public synchronized void onActivate(RestApiRegistry restApiRegistry, RestOptions restOptions, WaterJacksonMapper waterJacksonMapper, ComponentRegistry componentRegistry) {
        //on activation injection is supported by onActivate parameters
        this.restApiRegistry = restApiRegistry;
        this.restOptions = restOptions;
        this.waterJacksonMapper = waterJacksonMapper;
        this.componentRegistry = componentRegistry;
    }

    @Override
    public synchronized void startRestApiServer() {
        if (this.restApiRegistry == null)
            return;
        this.stopRestApiServer();
        String restRootContext = (this.restOptions != null) ? this.restOptions.restRootContext() : "/water";
        log.info("Registering base REST resources under : {}", restRootContext);
        // configuring CXF Server with interceptors,features and providers
        List<Feature> features = new ArrayList<>();
        features.add(addSwaggerFeature());
        //configuring providers
        List<Object> providers = new ArrayList<>();
        JacksonJsonProvider jacksonJsonProvider = getJacksonJsonProvider();
        if (jacksonJsonProvider != null)
            providers.add(jacksonJsonProvider);
        providers.add(getGenericExceptionMapper());
        //all filters registered as @FrameworkComponent will be added
        List<ContainerRequestFilter> filters = getContainerRequestFilters();
        providers.addAll(filters);
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("/");
        factory.setFeatures(features);
        factory.setProviders(providers);
        Map<Class<?>, ResourceProvider> resourceClassesAndProviders = new HashMap<>();
        Map<Class<? extends RestApi>, Class<?>> registeredApis = this.restApiRegistry.getRegisteredRestApis();
        registeredApis.keySet().forEach(restApi -> {
            try {
                //finds the concrete rest controller which has @FrameworkRestController
                Class<?> serviceClass = registeredApis.get(restApi);
                //Finds the concrete rest api which uses a specific rest implementation
                Class<?> concreteRestApiInterface = restApi;
                //create a Per Request Resource Provider which instantiates a proxy of the correct interface per each request
                resourceClassesAndProviders.put(concreteRestApiInterface, new PerRequestProxyProvider(componentRegistry, concreteRestApiInterface, serviceClass));
                log.debug("Registered REST api: {} with implementation {}", serviceClass, concreteRestApiInterface);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });

        if (!resourceClassesAndProviders.isEmpty()) {
            List<Class<?>> resourceClasses = new ArrayList<>(resourceClassesAndProviders.keySet());
            //add all rest api as rest resources
            factory.setResourceClasses(resourceClasses);
            resourceClassesAndProviders.keySet().forEach(resourceClass ->
                    //add all rest services as providers for the resource api
                    factory.setResourceProvider(resourceClass, resourceClassesAndProviders.get(resourceClass)));
            log.debug("Starting CXF Rest API Server....");
            this.server = factory.create();
            this.componentRegistry.registerComponent(Server.class, this.server, null);
            log.debug("CXF Rest API Server Started!");
        } else {
            log.warn("No Resource has been found for {}", Server.class.getName());
        }
    }

    @Override
    @OnDeactivate
    public synchronized void stopRestApiServer() {
        //if not instance we search for other server registered
        if (this.server == null) {
            try {
                server = this.componentRegistry.findComponent(Server.class, null);
            } catch (Exception e) {
                log.debug("No running cxf server ...");
            }
        }

        try {
            if (server != null)
                server.stop();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            if (server != null)
                server.destroy();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            this.componentRegistry.unregisterComponent(Server.class, this.server);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        server = null;
    }

    /**
     * @return
     */
    public Server getCxfServer() {
        return server;
    }

    private Swagger2Feature addSwaggerFeature() {
        log.info("Registering Swagger Feature");
        String contextRoot = this.restOptions.restRootContext();
        Swagger2Feature swagger = new Swagger2Feature();
        swagger.setTitle(contextRoot + ": Application Rest Services ");
        swagger.setDescription("List of all " + contextRoot + " rest services");
        swagger.setUsePathBasedConfig(true); // Necessary for OSGi
        swagger.setPrettyPrint(true);
        swagger.setBasePath(contextRoot);
        swagger.setSupportSwaggerUi(true);
        return swagger;
    }

    private GenericExceptionMapperProvider getGenericExceptionMapper() {
        return new GenericExceptionMapperProvider();
    }

    private JacksonJsonProvider getJacksonJsonProvider() {
        if (this.waterJacksonMapper != null)
            return new JacksonJsonProvider(this.waterJacksonMapper.getJacksonMapper());
        return null;
    }

    private List<ContainerRequestFilter> getContainerRequestFilters() {
        List<ContainerRequestFilter> filters = new ArrayList<>();
        try {
            filters.addAll(this.componentRegistry.findComponents(ContainerRequestFilter.class, null));
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        //creating if not exists and adding jwt Authentication filter
        if (this.restOptions != null && this.restOptions.securityOptions() != null && this.restOptions.securityOptions().validateJwt() && this.jwtAuthenticationFilter == null)
            this.jwtAuthenticationFilter = new CxfJwtAuthenticationFilter(this.componentRegistry);
        if (this.jwtAuthenticationFilter != null)
            filters.add(this.jwtAuthenticationFilter);
        return filters;
    }

}
