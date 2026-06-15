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

import it.water.core.interceptors.annotations.FrameworkComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Aristide Cittadino
 * Default in-process {@link TokenRevocationStore}: keeps revoked jti -> expiry until the token
 * would naturally expire, then lazily forgets it. State is per-JVM — multi-node deployments need a
 * shared implementation (e.g. Redis).
 */
@Slf4j
@FrameworkComponent
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    //hard cap on tracked jti to bound memory against a flood of logout/revocation calls
    private static final int MAX_KEYS = 100000;

    private final ConcurrentHashMap<String, RevokedToken> revoked = new ConcurrentHashMap<>();

    /**
     * Per-jti entry. Synchronization is encapsulated here (instance methods lock on {@code this}),
     * so callers never synchronize on a parameter or a field they don't own (avoids SonarQube S2445).
     */
    private static final class RevokedToken {
        private long expiresAtEpochMillis;

        RevokedToken(long expiresAtEpochMillis) {
            this.expiresAtEpochMillis = expiresAtEpochMillis;
        }

        synchronized long expiresAt() {
            return expiresAtEpochMillis;
        }

        /**
         * An entry is expired (safe to drop) once the clock passes the token's natural expiry: from
         * that moment the token is already rejected by expiration validation, so the denylist no
         * longer needs to track it.
         */
        synchronized boolean isExpired(long now) {
            return expiresAtEpochMillis <= now;
        }
    }

    @Override
    public void revoke(String jti, long expiresAtEpochMillis) {
        if (jti == null || jti.isBlank())
            return;
        long now = now();
        //already-expired tokens are harmless; nothing to track
        if (expiresAtEpochMillis <= now)
            return;
        //bound memory before inserting a potentially-new key
        evictIfNeeded();
        revoked.put(jti, new RevokedToken(expiresAtEpochMillis));
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null)
            return false;
        RevokedToken entry = revoked.get(jti);
        if (entry == null)
            return false;
        long now = now();
        if (entry.isExpired(now)) {
            //token reached natural expiry: drop the entry and treat as not-revoked (expiration check handles it)
            revoked.remove(jti, entry);
            return false;
        }
        return true;
    }

    /**
     * Opportunistic, bounded cleanup invoked on writes:
     * 1) drop entries whose token already expired (no live security value);
     * 2) if still above the cap, evict the soonest-to-expire entries until under the cap.
     */
    private void evictIfNeeded() {
        long now = now();
        // pass 1: remove entries whose token already expired
        revoked.entrySet().removeIf(e -> e.getValue().isExpired(now));
        if (revoked.size() <= MAX_KEYS)
            return;
        // pass 2: cap enforcement — evict the soonest-to-expire first (closest to being dropped anyway)
        revoked.entrySet().stream()
                .sorted((x, y) -> Long.compare(x.getValue().expiresAt(), y.getValue().expiresAt()))
                .limit((long) revoked.size() - MAX_KEYS)
                .map(Map.Entry::getKey)
                .forEach(revoked::remove);
        log.warn("Token revocation store exceeded {} keys; evicted soonest-to-expire entries down to the cap", MAX_KEYS);
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
