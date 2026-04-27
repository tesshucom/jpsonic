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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.feature.auth.rememberme.RMSKeys;
import com.tesshu.jpsonic.feature.i18n.I18nSKeys;
import com.tesshu.jpsonic.feature.theme.ThemeSKeys;
import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.filesystem.FileOperations;
import com.tesshu.jpsonic.persistence.DBSKeys;
import com.tesshu.jpsonic.service.upnp.UPnPSKeys;
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
import org.apache.commons.lang3.exception.UncheckedIllegalAccessException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class SettingsFacadeBuilder {

    private final SettingsStorage storage = Mockito.mock(SettingsStorage.class);

    public static SettingsFacadeBuilder create() {
        return new SettingsFacadeBuilder();
    }

    public SettingsFacadeBuilder withInt(SettingKey<Integer> key, int value) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        lenient().when(storage.getInt(key)).thenReturn(value);
        return this;
    }

    public SettingsFacadeBuilder withLong(SettingKey<Long> key, long value) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        lenient().when(storage.getLong(key)).thenReturn(value);
        return this;
    }

    public SettingsFacadeBuilder withString(SettingKey<String> key, String value) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        lenient().when(storage.getString(key)).thenReturn(value);
        return this;
    }

    public SettingsFacadeBuilder withBoolean(SettingKey<Boolean> key, boolean value) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        lenient().when(storage.getBoolean(key)).thenReturn(value);
        return this;
    }

    public SettingsFacadeBuilder withIntAnswer(SettingKey<Integer> key, Answer<?> answer) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(answer).when(storage).getInt(key);
        return this;
    }

    public SettingsFacadeBuilder withLongAnswer(SettingKey<Long> key, Answer<?> answer) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(answer).when(storage).getLong(key);
        return this;
    }

    public SettingsFacadeBuilder withStringAnswer(SettingKey<String> key, Answer<?> answer) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(answer).when(storage).getString(key);
        return this;
    }

    public SettingsFacadeBuilder withBooleanAnswer(SettingKey<Boolean> key, Answer<?> answer) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(answer).when(storage).getBoolean(key);
        return this;
    }

    public SettingsFacadeBuilder captureInt(SettingKey<Integer> key,
            ArgumentCaptor<Integer> captor) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(invocation -> {
            return null;
        }).when(storage).staging(eq(key), captor.capture());
        return this;
    }

    public SettingsFacadeBuilder captureLong(SettingKey<Long> key, ArgumentCaptor<Long> captor) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(invocation -> {
            return null;
        }).when(storage).staging(eq(key), captor.capture());
        return this;
    }

    public SettingsFacadeBuilder captureBoolean(SettingKey<Boolean> key,
            ArgumentCaptor<Boolean> captor) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(invocation -> {
            return null;
        }).when(storage).staging(eq(key), captor.capture());
        return this;
    }

    public SettingsFacadeBuilder captureString(SettingKey<String> key,
            ArgumentCaptor<String> captor) {
        lenient().when(storage.hasValue(key)).thenReturn(true);
        doAnswer(invocation -> {
            return null;
        }).when(storage).staging(eq(key), captor.capture());
        return this;
    }

    /**
     * Builds a SettingsFacade using an empty mock storage.
     *
     * <p>
     * Use this for isolated unit tests where all required settings are explicitly
     * configured by the test.
     * </p>
     */
    public SettingsFacade build() {
        SettingsRuntime runtime = new SettingsRuntime(storage);
        return new SettingsFacade(storage, runtime);
    }

    static List<SettingKey<?>> collectNonNullDefaults(Class<?> root) {
        List<SettingKey<?>> result = new ArrayList<>();
        collectRecursive(root, result);
        return Collections.unmodifiableList(result);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void collectRecursive(Class<?> clazz, List<SettingKey<?>> out) {
        for (Field f : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
                    && SettingKey.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    SettingKey<?> key = (SettingKey<?>) f.get(null);
                    if (key.defaultValue() != null) {
                        out.add(key);
                    }
                } catch (IllegalAccessException e) {
                    throw new UncheckedIllegalAccessException(e);
                }
            }
        }
        for (Class<?> nested : clazz.getDeclaredClasses()) {
            collectRecursive(nested, out);
        }
    }

    public void registerDefaultsIfNull(List<SettingKey<?>> keys, SettingsStorage storage) {
        keys.stream().filter(key -> !storage.hasValue(key)).forEach(key -> {
            Object def = key.defaultValue();
            switch (key.valueType()) {
            case INTEGER -> withInt((SettingKey<Integer>) key, (Integer) def);
            case LONG -> withLong((SettingKey<Long>) key, (Long) def);
            case BOOLEAN -> withBoolean((SettingKey<Boolean>) key, (Boolean) def);
            case STRING -> withString((SettingKey<String>) key, (String) def);
            }
        });
    }

    /**
     * Builds a SettingsFacade using a mock storage with all default values
     * pre-registered. Equivalent to the application state on first launch.
     *
     * <p>
     * Use this when tests depend on default settings being present.
     * </p>
     */
    public SettingsFacade buildWithDefault() {

        // register default settings
        if (!storage.hasValue(SKeys.deprecatedSecrets.jwtKey)) {
            String jwtKey = new BigInteger(130, new SecureRandom()).toString(32);
            withString(SKeys.deprecatedSecrets.jwtKey, jwtKey);
        }
        registerDefaultsIfNull(collectNonNullDefaults(SKeys.class), storage);
        registerDefaultsIfNull(collectNonNullDefaults(DBSKeys.class), storage);
        registerDefaultsIfNull(collectNonNullDefaults(I18nSKeys.class), storage);
        registerDefaultsIfNull(collectNonNullDefaults(ThemeSKeys.class), storage);
        registerDefaultsIfNull(collectNonNullDefaults(UPnPSKeys.class), storage);
        registerDefaultsIfNull(collectNonNullDefaults(RMSKeys.class), storage);
        SettingsRuntime runtime = new SettingsRuntime(storage);
        return new SettingsFacade(storage, runtime);
    }

    /**
     * Builds a SettingsFacade using the real SettingsStorage backed by
     * PropertiesConfiguration.
     *
     * <p>
     * Use this method for integration tests or migration tests that require actual
     * configuration file behavior.
     * </p>
     *
     * <p>
     * <b>Note:</b> This method requires the test environment to provide the
     * necessary physical resources (e.g., @NeedsHome). It must be used together
     * with @NeedsHome-based test setup.
     * </p>
     */
    public SettingsFacade buildReal() {
        Path propertyFile = EnvironmentProvider.getInstance().getPropertyFilePath();

        if (!Files.exists(propertyFile)) {
            try {
                FileOperations.touch(propertyFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        layout.setHeaderComment("""
                Jpsonic preferences. NOTE: This file is automatically generated.
                Do not modify while application is running
                """);
        layout.setGlobalSeparator("=");

        BuilderParameters params = new Parameters()
            .properties()
            .setFile(propertyFile.toFile())
            .setSynchronizer(new ReadWriteSynchronizer())
            .setLayout(layout);

        FileBasedConfigurationBuilder<FileBasedConfiguration> configurationBuilder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class)
            .configure(params);

        Configuration configuration;
        try {
            configuration = configurationBuilder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new UncheckedException(e);
        }
        SettingsStorage storage = new SettingsStorage(configurationBuilder, configuration);
        SettingsRuntime runtime = new SettingsRuntime(storage);
        return new SettingsFacade(storage, runtime);
    }
}
