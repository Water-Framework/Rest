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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.service.rest.AbstractRestApiManager;
import it.water.service.rest.GenericExceptionMapperProvider;
import it.water.service.rest.api.options.RestOptions;
import it.water.service.rest.manager.cxf.security.filters.jwt.CxfJwtAuthenticationFilter;
import lombok.Setter;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    //used for jackson serialization
    private ObjectMapper jacksonObjectMapper;
    private Server server;
    private boolean stopped = true;
    private Iterable<Class<?>> frameworkRestApis;
    private CxfJwtAuthenticationFilter jwtAuthenticationFilter;

    public CxfRestApiManager() {
        this.jacksonObjectMapper = new ObjectMapper();
    }

    @Override
    public void setAnnotatedRestApis(Iterable<Class<?>> frameworkRestApis) {
        this.frameworkRestApis = frameworkRestApis;
    }

    @Override
    public synchronized void startRestApiServer() {
        if (!stopped)
            this.stopRestApiServer();
        String restRootContext = this.restOptions.restRootContext();
        log.info("Registering base REST resources under : {}", restRootContext);
        // configuring CXF Server with interceptors,features and providers
        List<Feature> features = new ArrayList<>();
        features.add(addSwaggerFeature());
        //configuring providers
        List<Object> providers = new ArrayList<>();
        providers.add(getJacksonJsonProvider());
        providers.add(getGenericExceptionMapper());
        //all filters registered as @FrameworkComponent will be added
        List<ContainerRequestFilter> filters = getContainerRequestFilters();
        providers.addAll(filters);

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress("/");
        factory.setFeatures(features);
        factory.setProviders(providers);
        Map<Class<?>, ResourceProvider> resourceClassesAndProviders = new HashMap<>();
        this.getRegisteredApis().forEach(restApi -> {
            try {
                //finds the concrete rest controller which has @FrameworkRestController
                Class<?> serviceClass = this.getRestImplementation(restApi);
                //Finds the concrete rest api which uses a specific rest implementation
                Class<?> concreteRestApiInterface = findConcreteRestApi(this.frameworkRestApis, restApi);
                //create a Per Request Resource Provider which instantiates a proxy of the correct interface per each request
                resourceClassesAndProviders.put(concreteRestApiInterface, new PerRequestProxyProvider(componentRegistry,concreteRestApiInterface, serviceClass));
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
            this.server = factory.create();
            this.stopped = false;
            this.componentRegistry.registerComponent(Server.class, this.server, null);
        } else {
            log.warn("No Resource has been found for {}", Server.class.getName());
        }
    }

    @Override
    public synchronized void stopRestApiServer() {
        try {
            server.stop();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            server.destroy();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        stopped = true;
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
        return new JacksonJsonProvider(this.jacksonObjectMapper);
    }

    private List<ContainerRequestFilter> getContainerRequestFilters() {
        List<ContainerRequestFilter> filters = new ArrayList<>();
        try {
            filters.addAll(this.componentRegistry.findComponents(ContainerRequestFilter.class, null));
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        //creating if not exists and adding jwt Authentication filter
        if (this.jwtAuthenticationFilter == null)
            this.jwtAuthenticationFilter = new CxfJwtAuthenticationFilter(this.componentRegistry);
        filters.add(this.jwtAuthenticationFilter);
        return filters;
    }

}
