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

import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.STRING;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Provides access to settings values and generates their runtime
 * representations. Reads values from SettingsStorage and performs parsing or
 * caching when needed.
 */
@Component
final class SettingsRuntime {

    private final SettingsStorage storage;

    /*
     * Holds the cached parsing result as an immutable snapshot.
     *
     * <p>This cache is managed through an {@code AtomicReference} based on the
     * following design assumptions:</p>
     *
     * <ul> <li>The source of truth is always a string property, and changes are
     * detected at the string level</li> <li>Whenever the underlying string changes,
     * the entire value is re-parsed and fully reconstructed</li> <li>The parsed
     * result (e.g., a List or Pattern) is treated as an immutable snapshot and
     * never mutated</li> <li>Readers only require a consistent snapshot at
     * retrieval time; strict “latest value” semantics are unnecessary</li>
     * <li>Concurrent initialization or reconstruction is acceptable, and “last
     * write wins” is semantically correct</li> <li>Reads are overwhelmingly more
     * frequent than writes</li> <li>Re-parsing is infrequent and completes
     * quickly</li> </ul>
     *
     * <p>Given these conditions, replacing the snapshot atomically via {@code
     * AtomicReference} provides sufficient consistency while avoiding the
     * complexity and overhead of explicit locking.</p>
     */
    private final AtomicReference<Pattern> excludePattern = new AtomicReference<>();
    private final AtomicReference<List<String>> ignoredArticles = new AtomicReference<>();
    private final AtomicReference<List<String>> musicFileTypes = new AtomicReference<>();
    private final AtomicReference<List<String>> videoFileTypes = new AtomicReference<>();
    private final AtomicReference<List<String>> coverArtFileTypes = new AtomicReference<>();
    private final AtomicReference<List<String>> excludedCoverArts = new AtomicReference<>();
    private final AtomicReference<List<String>> shortcuts = new AtomicReference<>();

    SettingsRuntime(SettingsStorage storage) {
        this.storage = storage;
    }

    @SuppressWarnings("PMD.UnnecessaryBoxing")
    @Nullable
    <T> T get(@NonNull SettingKey<T> key) {
        return (T) switch (key.valueType()) {
        case INTEGER -> Integer.valueOf(storage.getInt((SettingKey<Integer>) key));
        case LONG -> Long.valueOf(storage.getLong((SettingKey<Long>) key));
        case BOOLEAN -> Boolean.valueOf(storage.getBoolean((SettingKey<Boolean>) key));
        case STRING -> storage.getString((SettingKey<String>) key);
        };
    }

    @NonNull
    List<String> getCachedList(@NonNull SettingKey<String> key) {
        if (SKeys.general.index.ignoredArticles.equals(key)) {
            return ensureListCache(SKeys.general.index.ignoredArticles, ignoredArticles);
        }
        if (SKeys.general.extension.musicFileTypes.equals(key)) {
            return ensureListCache(SKeys.general.extension.musicFileTypes, musicFileTypes);
        }
        if (SKeys.general.extension.videoFileTypes.equals(key)) {
            return ensureListCache(SKeys.general.extension.videoFileTypes, videoFileTypes);
        }
        if (SKeys.general.extension.coverArtFileTypes.equals(key)) {
            return ensureListCache(SKeys.general.extension.coverArtFileTypes, coverArtFileTypes);
        }
        if (SKeys.general.extension.excludedCoverArt.equals(key)) {
            return ensureListCache(SKeys.general.extension.excludedCoverArt, excludedCoverArts);
        }
        if (SKeys.general.extension.shortcuts.equals(key)) {
            return ensureListCache(SKeys.general.extension.shortcuts, shortcuts);
        }

        throw new IllegalArgumentException("Unsupported list key: " + key);
    }

    @NonNull
    List<String> ensureListCache(@NonNull SettingKey<String> key,
            @NonNull AtomicReference<List<String>> cache) {
        List<String> cached = cache.get();
        if (cached != null) {
            return cached;
        }

        String raw = storage.getString(key);
        if (StringUtils.isBlank(raw)) {
            raw = key.defaultValue();
        }

        if (StringUtils.isBlank(raw)) {
            List<String> empty = Collections.emptyList();
            cache.set(empty);
            return empty;
        }

        List<String> parsed = StringUtil.split(raw);
        cache.set(parsed);
        return parsed;
    }

    void invalidateIfChanged(@NonNull SettingKey<String> key, @Nullable String newValue) {
        if (SKeys.musicFolder.exclusion.excludePatternString.equals(key)
                && !Objects.equals(newValue, get(key))) {
            excludePattern.set(null);
            return;
        }

        if ((SKeys.general.extension.musicFileTypes.equals(key)
                || SKeys.general.extension.videoFileTypes.equals(key)
                || SKeys.general.extension.coverArtFileTypes.equals(key)
                || SKeys.general.extension.excludedCoverArt.equals(key)
                || SKeys.general.extension.shortcuts.equals(key)
                || SKeys.general.index.ignoredArticles.equals(key))
                && !Objects.equals(newValue, get(key))) {

            if (SKeys.general.extension.musicFileTypes.equals(key)) {
                musicFileTypes.set(null);
            } else if (SKeys.general.extension.videoFileTypes.equals(key)) {
                videoFileTypes.set(null);
            } else if (SKeys.general.extension.coverArtFileTypes.equals(key)) {
                coverArtFileTypes.set(null);
            } else if (SKeys.general.extension.excludedCoverArt.equals(key)) {
                excludedCoverArts.set(null);
            } else if (SKeys.general.extension.shortcuts.equals(key)) {
                shortcuts.set(null);
            } else if (SKeys.general.index.ignoredArticles.equals(key)) {
                ignoredArticles.set(null);
            }
        }
    }

    <V> void staging(@NonNull SettingKey<V> key, @Nullable V value) {
        V toStore = key.valueType() == STRING && value != null
                && StringUtils.isBlank((String) value) ? null : value;

        if (key.valueType() == STRING) {
            invalidateIfChanged((SettingKey<String>) key, (String) value);
        }

        storage.staging(key, toStore);
    }

    <V> void commit(@NonNull SettingKey<V> key, @Nullable V value) {
        staging(key, value);
        storage.commitAll();
    }

    @Nullable
    String getDecodedString(@NonNull SettingKey<String> key) {
        if (!SKeys.advanced.smtp.password.equals(key)
                && !SKeys.advanced.ldap.managerPassword.equals(key)) {
            throw new IllegalArgumentException("Unsupported decoded string key: " + key);
        }
        try {
            return StringUtil.utf8HexDecode(storage.getString(key));
        } catch (DecoderException e) {
            storage.staging(key, null);
            return null;
        }
    }

    void stagingEncodedString(@NonNull SettingKey<String> key, @Nullable String raw) {
        if (!SKeys.advanced.smtp.password.equals(key)
                && !SKeys.advanced.ldap.managerPassword.equals(key)) {
            throw new IllegalArgumentException("Unsupported encoded string key: " + key);
        }
        String encoded = raw == null ? null : StringUtil.utf8HexEncode(raw);
        staging(key, encoded);
    }

    @Nullable
    Pattern getCachedPattern(@NonNull SettingKey<String> key) {
        if (!SKeys.musicFolder.exclusion.excludePatternString.equals(key)) {
            throw new IllegalArgumentException("Unsupported pattern key: " + key);
        }

        Pattern cached = excludePattern.get();
        if (cached != null) {
            return cached;
        }

        String raw = storage.getString(SKeys.musicFolder.exclusion.excludePatternString);
        if (StringUtils.isBlank(raw)) {
            raw = SKeys.musicFolder.exclusion.excludePatternString.defaultValue();
            if (raw == null) {
                return null;
            }
        }

        Pattern compiled = Pattern.compile(raw);
        excludePattern.set(compiled);
        return compiled;
    }
}
