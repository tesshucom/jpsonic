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

package com.tesshu.jpsonic.infrastructure.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.core.NeedsHome;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.BuilderParameters;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NeedsHome
class SettingsStorageCommitTest {

    private Path propertyFile;
    private Configuration config;
    private SettingsStorage storage;

    @BeforeEach
    void setUp() {
        propertyFile = EnvironmentProvider.getInstance().getPropertyFilePath();

        if (!Files.exists(propertyFile)) {
            try {
                Files.createFile(propertyFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        layout.setGlobalSeparator("=");

        BuilderParameters params = new Parameters()
            .properties()
            .setFile(propertyFile.toFile())
            .setSynchronizer(new ReadWriteSynchronizer())
            .setLayout(layout);

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class)
            .configure(params);
        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new UncheckedException(e);
        }
        storage = new SettingsStorage(builder, config);
    }

    @Test
    void commitClearsWhenValueEqualsDefault() throws Exception {
        SettingKey<Integer> key = SKeys.SKey.of("k1", SettingKey.ValueType.INTEGER, 10);
        config.setProperty("k1", 99);

        String before = Files.readString(propertyFile);
        storage.commit(key, 10);
        String after = Files.readString(propertyFile);

        assertFalse(config.containsKey("k1"));
        assertNotEquals(before, after);
    }

    @Test
    void commitClearsWhenValueIsNull() throws Exception {
        SettingKey<String> key = SKeys.SKey.of("k2", SettingKey.ValueType.STRING, "x");
        config.setProperty("k2", "value");

        String before = Files.readString(propertyFile);
        storage.commit(key, null);
        String after = Files.readString(propertyFile);

        assertFalse(config.containsKey("k2"));
        assertNotEquals(before, after);
    }

    @Test
    void commitSetsWhenValueIsNonDefault() throws Exception {
        SettingKey<Boolean> key = SKeys.SKey.of("k3", SettingKey.ValueType.BOOLEAN, false);

        final String before = Files.readString(propertyFile);
        storage.commit(key, true);
        final String after = Files.readString(propertyFile);

        assertTrue(config.containsKey("k3"));
        assertEquals(true, config.getProperty("k3"));
        assertNotEquals(before, after);
    }

    @Test
    void commitClearsExistingKeyWhenValueBecomesNull() throws Exception {
        SettingKey<String> key = SKeys.SKey.of("k4", SettingKey.ValueType.STRING, "x");

        // create existing key in file
        storage.commit(key, "value");
        String afterCreate = Files.readString(propertyFile);
        assertTrue(afterCreate.contains("k4"));

        // clear existing key
        storage.commit(key, null);
        String afterDelete = Files.readString(propertyFile);

        assertFalse(config.containsKey("k4"));
        assertNotEquals(afterCreate, afterDelete);
        assertFalse(afterDelete.contains("k4"));
    }
}
