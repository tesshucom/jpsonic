/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.feature.auth.rememberme;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tesshu.jpsonic.feature.auth.AuthKeyType;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.core.entity.AuthKey;
import com.tesshu.jpsonic.persistence.core.repository.AuthKeyDao;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

@Component
public class RememberMeKeyManager {

    private static final Logger LOG = LoggerFactory.getLogger(RememberMeKeyManager.class);

    private final SettingsFacade settingsFacade;
    private final AuthKeyDao authKeyDao;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private String key = "NOT READY";

    public RememberMeKeyManager(SettingsFacade settingsFacade, AuthKeyDao authKeyDao) {
        super();
        this.settingsFacade = settingsFacade;
        this.authKeyDao = authKeyDao;
    }

    boolean isRunning() {
        return running.get();
    }

    @NonNull
    String generateKey() {
        byte[] keyBytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(keyBytes);
        return new String(Hex.encode(keyBytes));
    }

    void init() {
        AuthKey authKey = getAuthKey();
        if (authKey != null) {
            KeyRotationType rotationType = KeyRotationType
                .of(settingsFacade.get(RMSKeys.rotationType));

            switch (rotationType) {
            case PERIOD, FIXED -> key = authKey.getValue();
            case RESTART -> {
                performRotation(Instant.now());
                authKey = getAuthKey();
                key = authKey.getValue();
            }
            }

        } else {
            String newKey = generateKey();
            authKeyDao.create(AuthKeyType.REMEMBERME.value(), newKey, Instant.now());
            key = newKey;
        }
        running.set(true);
    }

    @NonNull
    public String getKey() {
        if (!(running.get() && settingsFacade.get(RMSKeys.enable))) {
            throw new IllegalAccessError("""
                    Security Invariant Violation: \
                    Attempted to access RememberMe server key \
                    while the system is in an unready or disabled state. \
                    Filter-level guards must prevent this call.
                    """);
        }
        return key;
    }

    public AuthKey getAuthKey() {
        return authKeyDao.get(AuthKeyType.REMEMBERME.value());
    }

    void performRotation(Instant lastUpdate) {
        AuthKey authKey = getAuthKey();
        assert authKey != null;
        String newKey = generateKey();
        authKey.setValue(newKey);
        authKey.setLastUpdate(lastUpdate);
        authKeyDao.update(authKey);
        this.key = newKey;
    }

    /**
     * Core rotation logic that persists a new key with a specific baseline.
     * 
     * @param lastUpdate The timestamp to be recorded (factual 'now' or normalized
     *                   'midnight').
     */
    void rotate(Instant lastUpdate) {
        if (!(running.get() && settingsFacade.get(RMSKeys.enable))) {
            LOG.warn("""
                    Rotation skipped: \
                    The state changed to inactive just before execution. \
                    This may be due to a concurrent administrative \
                    action or task overlap.
                    """);
            return;
        }
        performRotation(lastUpdate);
    }

    /**
     * Standard rotation triggered by manual action or legacy calls. Records the
     * current timestamp as the factual update time.
     */
    public void rotate() {
        rotate(Instant.now());
    }

    /**
     * Adaptive rotation check used by the scheduler. Normalizes the baseline to
     * midnight if a scheduled period has elapsed.
     */
    void rotateIfNecessary() {
        rotateIfNecessary(Instant.now());
    }

    void rotateIfNecessary(@NonNull Instant now) {
        if (!(running.get() && settingsFacade.get(RMSKeys.enable))) {
            return;
        }

        if (settingsFacade.get(RMSKeys.rotationType) != KeyRotationType.PERIOD.value()) {
            return;
        }

        AuthKey authKey = getAuthKey();
        if (authKey == null) {
            return;
        }

        KeyRotationPeriod period = KeyRotationPeriod.of(settingsFacade.get(RMSKeys.rotationPeriod));
        ZonedDateTime lastUpdate = authKey.getLastUpdate().atZone(ZoneId.systemDefault());
        ZonedDateTime threshold = switch (period) {
        case DAILY -> lastUpdate.plus(1, ChronoUnit.DAYS);
        case WEEKLY -> lastUpdate.plus(1, ChronoUnit.WEEKS);
        case MONTHLY -> lastUpdate.plus(1, ChronoUnit.MONTHS);
        };

        if (now.plus(1, ChronoUnit.HOURS).isAfter(threshold.toInstant())) {
            LOG.info("Automatic key rotation triggered by schedule ({}).", period);

            Instant midnight = LocalDate
                .now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
            rotate(midnight);
        }
    }

    void stop() {
        running.set(false);
    }
}
