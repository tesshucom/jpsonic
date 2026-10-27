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

package com.tesshu.jpsonic.feature.filesystem;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;

import com.tesshu.jpsonic.infrastructure.filesystem.PathInspector;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LibraryAccessPolicyTest {

    private SettingsFacade settingsFacade;
    private LibraryAccessPolicy service;
    private PathInspector pathInspector;
    private MusicFolderService musicFolderService;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder.create().build();
        pathInspector = new PathInspector();
        musicFolderService = mock(MusicFolderService.class);
    }

    @Ignore
    void init() {
        service = new LibraryAccessPolicy(settingsFacade, pathInspector, musicFolderService);
    }

    @Test
    void testIsWriteAllowed() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(SKeys.podcast.folder, "")
            .build();
        init();
        assertThrows(IllegalArgumentException.class, () -> service.isWriteAllowed(null));
        assertThrows(IllegalArgumentException.class, () -> service.isWriteAllowed(Path.of("/")));
        assertFalse(service.isWriteAllowed(Path.of("")));
        Mockito
            .when(musicFolderService.getAllMusicFolders(false, true))
            .thenReturn(Arrays.asList(new MusicFolder("/test", "test", true, null, false)));
        assertTrue(service.isWriteAllowed(Path.of("/test/cover.jpg")));
    }
}
