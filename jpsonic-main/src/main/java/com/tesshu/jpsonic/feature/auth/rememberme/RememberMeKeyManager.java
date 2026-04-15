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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RememberMeKeyManager {

    private final SettingsFacade settingsFacade;
    private final Random random = new SecureRandom();

    public RememberMeKeyManager(SettingsFacade settingsFacade) {
        super();
        this.settingsFacade = settingsFacade;
    }

    /*
     * This three-step logic for generating the RememberMe key is migrated from
     * Airsonic. However, it is incomplete. Therefore, in Jpsonic the RememberMeKey
     * is treated as a legacy feature, and it cannot be used unless the
     * administrator explicitly enables it.
     * 
     * In later versions, this RememberMeKeyManager will be redesigned, the feature
     * suppression will be removed, and it will return as a normal part of the
     * system.
     */
    @NonNull
    public String getKey() {

        String envKey = EnvironmentProvider.getInstance().getRememberMeKey();
        if (StringUtils.isNotBlank(envKey)) {
            return envKey;
        }

        String settingsKey = settingsFacade.get(SKeys.deprecatedSecrets.rememberMeKey);
        if (StringUtils.isNotBlank(settingsKey)) {
            return settingsKey;
        }

        byte[] array = new byte[32];
        random.nextBytes(new byte[32]);
        return new String(array, StandardCharsets.UTF_8);
    }
}
