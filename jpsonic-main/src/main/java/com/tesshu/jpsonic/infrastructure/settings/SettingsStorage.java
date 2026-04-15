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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles reading, staging, and saving configuration values. Manages
 * persistence to the property file using FileBasedConfigurationBuilder and
 * Configuration.
 *
 * <p>
 * Main functions:
 * <ul>
 * <li>Retrieve typed values based on SettingKey</li>
 * <li>Stage values (remove when default or null)</li>
 * <li>Commit all staged settings and save</li>
 * <li>Clean up deprecated properties</li>
 * </ul>
 * </p>
 */
@Component
final class SettingsStorage {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsStorage.class);

    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
    private final Configuration config;
    private final AtomicBoolean running = new AtomicBoolean(true);

    SettingsStorage(FileBasedConfigurationBuilder<FileBasedConfiguration> builder,
            Configuration config) {
        super();
        this.builder = builder;
        this.config = config;
    }

    int getInt(@NonNull SettingKey<Integer> key) {
        return config.getInteger(key.name(), key.defaultValue());
    }

    long getLong(@NonNull SettingKey<Long> key) {
        return config.getLong(key.name(), key.defaultValue());
    }

    boolean getBoolean(@NonNull SettingKey<Boolean> key) {
        return config.getBoolean(key.name(), key.defaultValue());
    }

    @Nullable
    String getString(@NonNull SettingKey<String> key) {
        return config.getString(key.name(), key.defaultValue());
    }

    boolean hasValue(SettingKey<?> key) {
        return config.containsKey(key.name());
    }

    <V> void staging(@NonNull SettingKey<V> key, @Nullable V value) {
        if (Objects.equals(key.defaultValue(), value)) {
            config.clearProperty(key.name());
            return;
        }

        if (value == null) {
            config.clearProperty(key.name());
            return;
        }

        config.setProperty(key.name(), value);
    }

    void commitAll() {
        staging(SystemSKeys.savedAt, Instant.now().toEpochMilli());
        save();
    }

    <V> void commit(@NonNull SettingKey<V> key, @Nullable V value) {
        staging(key, value);
        commitAll();
    }

    void save() {
        try {
            builder.save();
        } catch (ConfigurationException e) {
            LOG.error("Unable to write to property file.", e);
        }
    }

    void cleanup() {
        if (!isRunning()) {
            return;
        }
        List<String> graveyardProperties;
        try (InputStream in = SettingsStorage.class
            .getResourceAsStream("GraveyardProperties.txt")) {
            graveyardProperties = StringUtil.readLines(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        graveyardProperties.forEach(keyName -> {
            if (config.containsKey(keyName)) {
                config.clearProperty(keyName);
            }
        });
        save();
        running.set(false);
    }

    boolean isRunning() {
        return running.get();
    }
}
