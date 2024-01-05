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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JWTConstants {
    public static final String JWT_CLAIM_ROLES = "roles";
    public static final String JWT_CLAIM_LOGGED_ENTITY_ID = "loggedEntityId";
    public static final String JWT_CLAIM_IS_ADMIN = "isAdmin";
    public static final String JWT_PROP_VALIDATE_BY_JWS = "water.service.rest.security.jwt.validate.by.jws";
    public static final String JWT_PROP_VALIDATE_BY_JWS_KEY_ID = "water.service.rest.security.jwt.validate.by.jws.key.id";
    public static final String JWT_PROP_ENCRYPT_JWT_TOKEN = "water.service.rest.security.jwt.encrypt";
    public static final String JWT_PROP_JWS_URL = "water.service.rest.security.jwt.jws.url";
    public static final String JWT_PROP_JWT_DURATION_MILLIS = "water.rest.security.jwt.duration.millis";
    public static final String JWT_COOKIE_NAME = "HIT-AUTH";
}
