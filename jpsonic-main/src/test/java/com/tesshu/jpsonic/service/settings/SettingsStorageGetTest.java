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

package com.tesshu.jpsonic.service.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsStorageGetTest {

    private Configuration config;
    private SettingsStorage storage;

    @BeforeEach
    void setUp() {
        config = new BaseConfiguration();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = mock(
                FileBasedConfigurationBuilder.class); // The save() method is not called.
        storage = new SettingsStorage(builder, config);
    }

    @Test
    void testGetIntReturnsConfiguredValueOrDefault() {
        SettingKey<Integer> key = SKeys.SKey.of("intKey", SettingKey.ValueType.INTEGER, 10);
        config.setProperty("intKey", 42);

        assertEquals(42, storage.getInt(key));
        config.clearProperty("intKey");
        assertEquals(10, storage.getInt(key));
    }

    @Test
    void testGetLongReturnsConfiguredValueOrDefault() {
        SettingKey<Long> key = SKeys.SKey.of("longKey", SettingKey.ValueType.LONG, 100L);
        config.setProperty("longKey", 999L);

        assertEquals(999L, storage.getLong(key));
        config.clearProperty("longKey");
        assertEquals(100L, storage.getLong(key));
    }

    @Test
    void testGetBooleanReturnsConfiguredValueOrDefault() {
        SettingKey<Boolean> key = SKeys.SKey.of("boolKey", SettingKey.ValueType.BOOLEAN, false);
        config.setProperty("boolKey", true);

        assertTrue(storage.getBoolean(key));
        config.clearProperty("boolKey");
        assertFalse(storage.getBoolean(key));
    }

    @Test
    void testGetStringReturnsConfiguredValueOrDefault() {
        SettingKey<String> key = SKeys.SKey.of("strKey", SettingKey.ValueType.STRING, "default");
        config.setProperty("strKey", "value");

        assertEquals("value", storage.getString(key));
        config.clearProperty("strKey");
        assertEquals("default", storage.getString(key));
    }
}
