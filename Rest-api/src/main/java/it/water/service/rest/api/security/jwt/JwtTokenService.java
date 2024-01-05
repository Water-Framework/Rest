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
package it.water.service.rest.api.security.jwt;

import it.water.core.api.entity.Authenticable;
import it.water.core.api.service.Service;

import java.security.Principal;
import java.util.List;
import java.util.Set;

/**
 * @Author Aristide Cittadino
 * Service for generating and validating JWT Tokens in both ways:
 * - Signed only
 * - Singed and encrypted
 */
public interface JwtTokenService extends Service {

    /**
     * Generates signed jwt token. If water.rest.security.jwt.encrypt is true, it encrypts the token.
     *
     * @param authenticable
     * @return
     */
    String generateJwtToken(Authenticable authenticable);

    /**
     * Validates jwt token in terms of signature,expiration and issuer
     *
     * @param jwt
     * @return true if it is valida jwt token
     */
    boolean validateToken(List<String> validIssuers, String jwt);


    /**
     * @param jwtToken
     * @return principal list containing Role principals and User Principal
     */
    Set<Principal> getPrincipals(String jwtToken);

    /**
     *
     * @return JWK For remote token validation
     */
    String getJWK();
}
