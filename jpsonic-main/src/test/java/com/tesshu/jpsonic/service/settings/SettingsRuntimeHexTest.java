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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tesshu.jpsonic.infrastructure.core.NeedsHome;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NeedsHome
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
public class SettingsRuntimeHexTest {

    private SettingsStorage storage;
    private SettingsRuntime runtime;
    private Configuration config;

    @BeforeEach
    void setUp() {
        SettingsConfiguration cfg = new SettingsConfiguration();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = cfg.settingsBuilder();
        config = cfg.settingsConfig(builder);
        storage = new SettingsStorage(builder, config);
        runtime = new SettingsRuntime(storage);
    }

    @Nested
    class EncodeTest {

        @Test
        void testStagingEncodedStringNormalValue() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            runtime.stagingEncodedString(key, "abc123");
            storage.commitAll();
            String stored = storage.getString(key);
            assertNotNull(stored);
            assertFalse(stored.isBlank());
            assertNotEquals("abc123", stored);
        }

        @Test
        void testStagingEncodedStringNullValue() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            runtime.stagingEncodedString(key, "abc");
            storage.commitAll();
            assertNotNull(storage.getString(key));
            runtime.stagingEncodedString(key, null);
            storage.commitAll();
            assertFalse(config.containsKey(key.name()));
            assertEquals(key.defaultValue(), storage.getString(key));
        }

        @Test
        void testStagingEncodedStringBlankValue() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            runtime.stagingEncodedString(key, "");
            storage.commitAll();
            String stored = storage.getString(key);
            assertNull(stored);
        }

        @Test
        void testStagingEncodedStringRejectsUnsupportedKey() {
            SettingKey<String> key = SKeys.general.index.ignoredArticles;
            assertThrows(IllegalArgumentException.class,
                    () -> runtime.stagingEncodedString(key, "x"));
        }
    }

    @Nested
    class DecodeTest {

        @Test
        void testGetDecodedStringNormalValue() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            runtime.stagingEncodedString(key, "abc123");
            storage.commitAll();
            String decoded = runtime.getDecodedString(key);
            assertEquals("abc123", decoded);
        }

        @Test
        void testGetDecodedStringNullValue() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            runtime.stagingEncodedString(key, null);
            storage.commitAll();
            assertNull(runtime.getDecodedString(key));
        }

        @Test
        void testGetDecodedStringBrokenHexReturnsNull() {
            SettingKey<String> key = SKeys.advanced.smtp.password;
            config.setProperty(key.name(), "ZZZZZZ");
            storage.commitAll();
            assertNull(runtime.getDecodedString(key));
        }

        @Test
        void testGetDecodedStringRejectsUnsupportedKey() {
            SettingKey<String> key = SKeys.general.index.ignoredArticles;
            assertThrows(IllegalArgumentException.class, () -> runtime.getDecodedString(key));
        }
    }
}
