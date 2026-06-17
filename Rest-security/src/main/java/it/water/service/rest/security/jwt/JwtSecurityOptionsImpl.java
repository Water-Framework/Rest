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
package it.water.service.rest.security.jwt;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.service.rest.api.options.JwtSecurityOptions;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Aristide Cittadino
 * Basic Properties for rest security module. Loaded by default from the main application prop file.
 */
@FrameworkComponent
public class JwtSecurityOptionsImpl implements JwtSecurityOptions {
    private static Logger log = LoggerFactory.getLogger(JwtSecurityOptionsImpl.class);
    private static final String DEBUG_MSG = "getting property Jwt Security Option: {} with Value: {}";
    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    /**
     * Default false
     *
     * @return
     */
    @Override
    public boolean validateJwtWithJwsUrl() {
        boolean value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATE_BY_JWS, false);
        log.debug(DEBUG_MSG, "validateJwtWithJWSUrl", value);
        return value;
    }

    @Override
    public String jwtKeyId() {
        String value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATE_BY_JWS_KEY_ID, "");
        log.debug(DEBUG_MSG, "jwtKeyId", value);
        return value;
    }

    /**
     * Default true
     *
     * @return
     */
    @Override
    public boolean encryptJWTToken() {
        boolean value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_ENCRYPT_JWT_TOKEN, false);
        log.debug(DEBUG_MSG, "encryptJWTToken", value);
        return value;
    }

    /**
     * Default empty
     *
     * @return
     */
    @Override
    public String jwsURL() {
        String value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_JWS_URL, "");
        log.debug(DEBUG_MSG, "jwsURL", value);
        return value;
    }

    /**
     * Default one hour
     *
     * @return
     */
    @Override
    public long jwtTokenDurationMillis() {
        long value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_JWT_DURATION_MILLIS, 3600000l);
        log.debug(DEBUG_MSG, "jwtTokenDurationMillis", value);
        return value;
    }

    @Override
    public boolean validateJwt() {
        boolean validationEnabled = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_VALIDATION_ENABLED, true);
        log.debug(DEBUG_MSG, "jwt Validation Enabled", validationEnabled);
        return validationEnabled;
    }

    /**
     * Default empty: when not configured the audience defaults to the token issuer.
     *
     * @return
     */
    @Override
    public String jwtAudience() {
        String value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_AUDIENCE, "");
        log.debug(DEBUG_MSG, "jwtAudience", value);
        return value;
    }

    /**
     * Default 60 seconds.
     *
     * @return
     */
    @Override
    public long jwtClockSkewSeconds() {
        long value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_CLOCK_SKEW_SECONDS, JWTConstants.JWT_DEFAULT_CLOCK_SKEW_SECONDS);
        log.debug(DEBUG_MSG, "jwtClockSkewSeconds", value);
        return value;
    }

    /**
     * Default false.
     *
     * @return
     */
    @Override
    public boolean testMode() {
        boolean value = applicationProperties.getPropertyOrDefault(JWTConstants.JWT_PROP_TEST_MODE, false);
        log.debug(DEBUG_MSG, "testMode", value);
        return value;
    }
}
