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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RememberMeKeyManagerTest {

    private ArgumentCaptor<String> keyCaptor;
    private SettingsFacade settingsFacade;
    private RememberMeKeyManager keyManager;

    @BeforeEach
    void setup() {
        keyCaptor = ArgumentCaptor.forClass(String.class);
        settingsFacade = SettingsFacadeBuilder
            .create()
            .captureString(SKeys.deprecatedSecrets.rememberMeKey, keyCaptor)
            .build();
        keyManager = new RememberMeKeyManager(settingsFacade);
    }

    @Test
    void testEnvKeyIsReturned() {
        System.setProperty("jpsonic.rememberMeKey", "fromEnv");
        assertEquals("fromEnv", keyManager.getKey());
        System.clearProperty("jpsonic.rememberMeKey");
        assertEquals(0, keyCaptor.getAllValues().size());
    }

    @Test
    void testRandomKeyIsReturnedWhenNoEnvAndNoSettings() {
        System.clearProperty("jpsonic.rememberMeKey");
        String envKey = EnvironmentProvider.getInstance().getRememberMeKey();
        String propKey = settingsFacade.get(SKeys.deprecatedSecrets.rememberMeKey);
        String randomKey = keyManager.getKey();
        assertNotNull(randomKey);
        assertNotEquals(randomKey, envKey);
        assertNotEquals(randomKey, propKey);
        assertEquals(0, keyCaptor.getAllValues().size());
    }
}
