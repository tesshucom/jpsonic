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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.infrastructure.filesystem.FileSystemSKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("PMD.TooManyStaticImports")
class SettingsFacadeTest {

    private SettingsStorage storage;
    private SettingsRuntime runtime;
    private SettingsFacade facade;

    @BeforeEach
    void setup() {
        storage = mock(SettingsStorage.class);
        runtime = mock(SettingsRuntime.class);
        facade = new SettingsFacade(storage, runtime);
    }

    @Test
    void testGetDelegatesToRuntime() {
        SettingKey<String> key = SKeys.general.welcome.loginMessage;
        when(runtime.get(key)).thenReturn("OK");

        String result = facade.get(key);

        assertEquals("OK", result);
        verify(runtime).get(key);
    }

    @Test
    void testGetCachedListDelegatesCorrectly() {
        SettingKey<String> key = SKeys.general.extension.musicFileTypes;
        List<String> expected = List.of("mp3", "flac");
        when(runtime.getCachedList(key)).thenReturn(expected);

        List<String> result = facade.getCachedList(key);

        assertEquals(expected, result);
        verify(runtime).getCachedList(key);
    }

    @Test
    void testGetCachedListRejectsInvalidKey() {
        assertThrows(AssertionError.class, () -> {
            facade.getCachedList(SKeys.advanced.smtp.password);
        });
    }

    @Test
    void testGetCachedPatternDelegatesCorrectly() {
        SettingKey<String> key = FileSystemSKeys.excludePatternString;
        Pattern p = Pattern.compile(".*");
        when(runtime.getCachedPattern(key)).thenReturn(p);

        Pattern result = facade.getCachedPattern(key);

        assertEquals(p, result);
        verify(runtime).getCachedPattern(key);
    }

    @Test
    void testGetDecodedStringDelegatesToRuntime() {
        SettingKey<String> key = SKeys.advanced.smtp.password;
        when(runtime.getDecodedString(key)).thenReturn("decoded");

        String result = facade.getDecodedString(key);

        assertEquals("decoded", result);
        verify(runtime).getDecodedString(key);
    }

    @Test
    void testStagingEncodedStringDelegatesToRuntime() {
        SettingKey<String> key = SKeys.advanced.smtp.password;

        facade.stagingEncodedString(key, "raw");

        verify(runtime).stagingEncodedString(key, "raw");
    }

    @Test
    void testStagingDelegatesToRuntime() {
        SettingKey<String> key = SKeys.general.welcome.loginMessage;

        facade.staging(key, "abc");

        verify(runtime).staging(key, "abc");
    }

    @Test
    void testReset() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        facade = SettingsFacadeBuilder
            .create()
            .captureString(SKeys.general.welcome.title, captor)
            .captureString(SKeys.advanced.ldap.url, captor)
            .captureString(SKeys.transcoding.hlsCommand, captor)
            .build();

        facade
            .stagingDefault(SKeys.general.welcome.title, SKeys.general.welcome.subtitle,
                    SKeys.general.welcome.message, SKeys.advanced.ldap.url,
                    SKeys.transcoding.hlsCommand);

        assertEquals(3, captor.getAllValues().size());
        assertEquals(SKeys.general.welcome.title.defaultValue(), captor.getAllValues().get(0));
        assertEquals(SKeys.advanced.ldap.url.defaultValue(), captor.getAllValues().get(1));
        assertEquals(SKeys.transcoding.hlsCommand.defaultValue(), captor.getAllValues().get(2));
    }

    @Test
    void testCommitAllDelegatesToStorage() {
        facade.commitAll();
        verify(storage).commitAll();
    }
}
