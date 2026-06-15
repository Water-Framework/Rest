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

/**
 * @Author Aristide Cittadino
 * Denylist of revoked JWT identifiers (jti). A token is added on logout and stays denied until its
 * natural expiry, after which it can be safely forgotten (an expired token is already rejected by
 * signature/expiration validation).
 */
public interface TokenRevocationStore {

    /**
     * Marks a token jti as revoked until its natural expiry.
     *
     * @param jti                   the JWT id (claim "jti"); ignored if null/blank
     * @param expiresAtEpochMillis  the token expiration in epoch millis; after this instant the
     *                              entry may be lazily evicted
     */
    void revoke(String jti, long expiresAtEpochMillis);

    /**
     * @param jti the JWT id to check
     * @return true if the jti has been revoked and has not yet passed its natural expiry; false for null jti
     */
    boolean isRevoked(String jti);
}
