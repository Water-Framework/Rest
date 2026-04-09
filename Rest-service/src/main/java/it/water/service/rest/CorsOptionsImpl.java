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

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.service.rest.api.options.CorsOptions;
import lombok.Setter;

/**
 * @Author Aristide Cittadino
 * Default implementation of CorsOptions. Values are loaded from application properties
 * with sensible defaults.
 */
@FrameworkComponent
public class CorsOptionsImpl implements CorsOptions {

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Override
    public String allowedOrigins() {
        Object val = applicationProperties.getProperty(RestConstants.REST_PROP_CORS_ORIGINS);
        return val != null ? String.valueOf(val) : "*";
    }

    @Override
    public String allowedMethods() {
        Object val = applicationProperties.getProperty(RestConstants.REST_PROP_CORS_METHODS);
        return val != null ? String.valueOf(val) : "GET,POST,PUT,DELETE,OPTIONS,PATCH";
    }

    @Override
    public String allowedHeaders() {
        Object val = applicationProperties.getProperty(RestConstants.REST_PROP_CORS_HEADERS);
        return val != null ? String.valueOf(val) : "Authorization,Content-Type";
    }

    @Override
    public boolean allowCredentials() {
        Object val = applicationProperties.getProperty(RestConstants.REST_PROP_CORS_CREDENTIALS);
        return val != null ? Boolean.parseBoolean(String.valueOf(val)) : true;
    }

    @Override
    public long maxAge() {
        Object val = applicationProperties.getProperty(RestConstants.REST_PROP_CORS_MAX_AGE);
        return val != null ? Long.parseLong(String.valueOf(val)) : 3600L;
    }
}