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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.infrastructure.filesystem.FileSystemSKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class SettingsRuntimeTest {

    @Mock
    SettingsStorage storage;

    SettingsRuntime runtime;

    @BeforeEach
    void testsetUp() {
        runtime = new SettingsRuntime(storage);
    }

    // ------------------------------------------------------------
    // getCachedList / ensureListCache
    // ------------------------------------------------------------

    @Test
    void testGetDelegatesCorrectly() {
        when(storage.getBoolean(SKeys.musicFolder.scan.ignoreFileTimestamps)).thenReturn(true);
        assertInstanceOf(Boolean.class, runtime.get(SKeys.musicFolder.scan.ignoreFileTimestamps));

        when(storage.getString(SKeys.advanced.smtp.port)).thenReturn("1234");
        assertInstanceOf(String.class, runtime.get(SKeys.advanced.smtp.port));

        when(storage.getInt(SKeys.advanced.scanLog.scanLogRetention)).thenReturn(123);
        assertInstanceOf(Integer.class, runtime.get(SKeys.advanced.scanLog.scanLogRetention));

        when(storage.getLong(SKeys.advanced.bandwidth.downloadBitrateLimit)).thenReturn(123L);
        assertInstanceOf(Long.class, runtime.get(SKeys.advanced.bandwidth.downloadBitrateLimit));
    }

    @Test
    void testGetCachedListCachesAfterFirstCall() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        when(storage.getString(key)).thenReturn("mp3 flac wav");

        List<String> first = runtime.getCachedList(key);
        assertEquals(List.of("mp3", "flac", "wav"), first);

        List<String> second = runtime.getCachedList(key);
        assertSame(first, second);

        verify(storage, times(1)).getString(key);
    }

    @Test
    void testGetCachedListUsesDefaultWhenBlank() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        when(storage.getString(key)).thenReturn("   ");

        List<String> list = runtime.getCachedList(key);

        assertEquals(Arrays.asList(key.defaultValue().split("\\s+")), list);
    }

    @Test
    void testGetCachedListUsesDefaultValueWhenBlank() {
        SettingKey<String> key = SKeys.general.index.ignoredArticles;

        when(storage.getString(key)).thenReturn("   ");

        List<String> list = runtime.getCachedList(key);

        assertEquals(Arrays.asList("The", "El", "La", "Las", "Le", "Les"), list);
    }

    @Test
    void testGetCachedListThrowsOnUnsupportedKey() {
        SettingKey<String> unsupported = SKeys.advanced.smtp.password;

        assertThrows(IllegalArgumentException.class, () -> runtime.getCachedList(unsupported));
    }

    // ------------------------------------------------------------
    // invalidateIfChanged
    // ------------------------------------------------------------

    @Test
    void testStagingBlankStringStoresNull() {
        // Arrange: a string key with default null
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        // Act: staging with a blank string
        runtime.staging(key, "");

        // Assert: storage receives null (treated as "unset")
        verify(storage).staging(key, null);
    }

    @Test
    void testInvalidateIfChangedClearsCacheWhenValueChanges() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        // First call builds the initial cache
        when(storage.getString(key)).thenReturn("mp3 flac");
        runtime.getCachedList(key);

        // Clear invocation history to isolate the "after change" world
        Mockito.clearInvocations(storage);

        // Simulate changed value
        when(storage.getString(key)).thenReturn("wav aac");
        runtime.staging(key, "wav aac");

        // Next call must fetch from storage again due to invalidation
        runtime.getCachedList(key);

        // Ensure storage was accessed again after the change
        verify(storage, times(1)).getString(key);

    }

    @Test
    void testInvalidateIfChangedDoesNothingWhenValueSame() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        when(storage.getString(key)).thenReturn("mp3 flac");

        // Build initial cache
        List<String> first = runtime.getCachedList(key);

        // Staging with the same value should NOT invalidate the cache
        runtime.staging(key, "mp3 flac");

        // Should return the same cached list instance (no re-parse)
        List<String> second = runtime.getCachedList(key);

        assertSame(first, second, "Cache should not be rebuilt when value is unchanged");
    }

    // ------------------------------------------------------------
    // getCachedPattern
    // ------------------------------------------------------------

    @Test
    void testGetCachedPatternCachesAfterFirstCall() {
        SettingKey<String> key = FileSystemSKeys.excludePatternString;

        when(storage.getString(key)).thenReturn("foo.*");

        Pattern p1 = runtime.getCachedPattern(key);
        Pattern p2 = runtime.getCachedPattern(key);

        assertSame(p1, p2);
        verify(storage, times(1)).getString(key);
    }

    @Test
    void testGetCachedPatternReturnsNullWhenDefaultNull() {
        SettingKey<String> key = FileSystemSKeys.excludePatternString;

        when(storage.getString(key)).thenReturn("");

        Pattern p = runtime.getCachedPattern(key);
        assertNull(p);
    }

    @Test
    void testGetCachedPatternThrowsOnUnsupportedKey() {
        SettingKey<String> unsupported = SKeys.general.extension.musicFileTypes;

        assertThrows(IllegalArgumentException.class, () -> runtime.getCachedPattern(unsupported));
    }

    // ------------------------------------------------------------
    // getDecodedString
    // ------------------------------------------------------------

    @Test
    void testGetDecodedStringDecodesHex() {
        SettingKey<String> key = SKeys.advanced.smtp.password;

        when(storage.getString(key)).thenReturn("68656c6c6f"); // "hello"

        String decoded = runtime.getDecodedString(key);
        assertEquals("hello", decoded);
    }

    @Test
    void testGetDecodedStringThrowsOnUnsupportedKey() {
        SettingKey<String> unsupported = SKeys.general.extension.musicFileTypes;

        assertThrows(IllegalArgumentException.class, () -> runtime.getDecodedString(unsupported));
    }

    // ------------------------------------------------------------
    // get()
    // ------------------------------------------------------------

    @Test
    void testGetDelegatesToStorageByType() {
        SettingKey<Integer> intKey = SKeys.advanced.bandwidth.bufferSize;
        when(storage.getInt(intKey)).thenReturn(1234);

        Integer v = runtime.get(intKey);
        assertEquals(1234, v);

        verify(storage).getInt(intKey);
    }

    // ------------------------------------------------------------
    // commit
    // ------------------------------------------------------------

    @Test
    void testCommitCallsStagingAndCommitAll() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;

        runtime.commit(key, "mp3");

        verify(storage).staging(key, "mp3");
        verify(storage).commitAll();
    }
}
