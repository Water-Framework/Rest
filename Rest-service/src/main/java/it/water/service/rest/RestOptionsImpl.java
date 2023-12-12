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
package it.water.service.rest;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.service.rest.api.options.JwtSecurityOptions;
import it.water.service.rest.api.options.RestOptions;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author Aristide Cittadino
 * Basic Properties for rest module. Loaded by default from the main application prop file.
 */
@FrameworkComponent
public class RestOptionsImpl implements RestOptions {

    @Inject
    @Setter
    @Getter
    private JwtSecurityOptions jwtSecurityOptions;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Override
    public String frontendUrl() {
        if (applicationProperties.getProperty(RestConstants.REST_PROP_FRONTEND_URL) != null)
            return String.valueOf(applicationProperties.getProperty(RestConstants.REST_PROP_FRONTEND_URL));
        return "";
    }

    @Override
    public String servicesUrl() {
        if (applicationProperties.getProperty(RestConstants.REST_PROP_SERVICES_URL) != null)
            return String.valueOf(applicationProperties.getProperty(RestConstants.REST_PROP_SERVICES_URL));
        return "";
    }

    @Override
    public String restRootContext() {
        if (applicationProperties.getProperty(RestConstants.REST_PROP_ROOT_CONTEXT) != null)
            return String.valueOf(applicationProperties.getProperty(RestConstants.REST_PROP_ROOT_CONTEXT));
        return "";
    }

    @Override
    public String uploadFolderPath() {
        if (applicationProperties.getProperty(RestConstants.REST_PROP_UPLOAD_PATH) != null)
            return String.valueOf(applicationProperties.getProperty(RestConstants.REST_PROP_UPLOAD_PATH));
        return "";
    }

    @Override
    public long uploadMaxFileSize() {
        if (applicationProperties.getProperty(RestConstants.REST_PROP_UPLOAD_MAX_FILE_SIZE) != null)
            return Integer.parseInt(String.valueOf(applicationProperties.getProperty(RestConstants.REST_PROP_UPLOAD_MAX_FILE_SIZE)));
        return 1024;
    }

    @Override
    public JwtSecurityOptions securityOptions() {
        return getJwtSecurityOptions();
    }
}
