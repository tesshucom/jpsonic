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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Provides the public interface for retrieving and updating settings. Acts as a
 * unified entry point to SettingsRuntime and SettingsStorage.
 */
@Component
public class SettingsFacade {

    private final SettingsStorage storage;
    private final SettingsRuntime runtime;

    public SettingsFacade(SettingsStorage storage, SettingsRuntime runtime) {
        super();
        this.storage = storage;
        this.runtime = runtime;
    }

    public @Nullable <T> T get(@NonNull SettingKey<T> key) {
        return runtime.get(key);
    }

    public @NonNull List<String> getCachedList(@NonNull SettingKey<String> key) {
        assert SKeys.general.index.ignoredArticles.equals(key)
                || SKeys.general.extension.musicFileTypes.equals(key)
                || SKeys.general.extension.videoFileTypes.equals(key)
                || SKeys.general.extension.coverArtFileTypes.equals(key)
                || SKeys.general.extension.excludedCoverArt.equals(key)
                || SKeys.general.extension.shortcuts.equals(key);
        return runtime.getCachedList(key);
    }

    public @Nullable Pattern getCachedPattern(@NonNull SettingKey<String> key) {
        assert SKeys.musicFolder.exclusion.excludePatternString.equals(key);
        return runtime.getCachedPattern(key);
    }

    public @Nullable String getDecodedString(@NonNull SettingKey<String> key) {
        assert SKeys.advanced.smtp.password.equals(key)
                || SKeys.advanced.ldap.managerPassword.equals(key);
        return runtime.getDecodedString(key);
    }

    public void stagingEncodedString(@NonNull SettingKey<String> key, @Nullable String raw) {
        assert SKeys.advanced.smtp.password.equals(key)
                || SKeys.advanced.ldap.managerPassword.equals(key);
        runtime.stagingEncodedString(key, raw);
    }

    public <T> void staging(@NonNull SettingKey<T> key, @Nullable T value) {
        runtime.staging(key, value);
    }

    public <T> void stagingDefault(@NonNull SettingKey<T>... keys) {
        Arrays.stream(keys).forEach(k -> staging(k, k.defaultValue()));
    }

    public <T> void commit(SettingKey<T> key, T value) {
        runtime.commit(key, value);
    }

    public void commitAll() {
        storage.commitAll();
    }
}
