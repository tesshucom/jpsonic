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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class AlbumByFolderProcTest {

    private MusicFolderProvider musicFolderProvider;
    private MediaFileProvider mediaFileProvider;
    private AlbumByFolderProc proc;

    @BeforeEach
    void setup() {
        musicFolderProvider = mock(MusicFolderProvider.class);
        mediaFileProvider = mock(MediaFileProvider.class);
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        proc = new AlbumByFolderProc(musicFolderProvider, mediaFileProvider, settingsFacade,
                factory);
    }

    @Test
    void testGetProcId() {
        assertEquals("albf", proc.getProcId().getValue());
    }

    @Nested
    class GetDirectChildrenTest {

        private final MusicFolder folder1 = new MusicFolder(0, "path1", "name1", false, null, 0,
                false);
        private final MusicFolder folder2 = new MusicFolder(0, "path2", "name2", false, null, 0,
                false);

        @Test
        void testNoFolder() {
            assertTrue(proc.getDirectChildren(0, 30).isEmpty());
        }

        @Test
        void testWithSingleFolder() {
            when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder1));
            assertEquals(0, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
            verify(mediaFileProvider, times(1))
                .findAlbums(ArgumentMatchers.<MusicFolder>anyList(),
                        any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(), anyLong());
        }

        @Test
        void testMultiFolder() {
            when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder1, folder2));
            assertEquals(2, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
            verify(mediaFileProvider, times(2)).requireMediaFile(any(MusicFolder.class));
        }
    }

    @Nested
    class GetDirectChildrenCountTest {

        private final MusicFolder folder1 = new MusicFolder(0, "path1", "name1", false, null, 0,
                false);
        private final MusicFolder folder2 = new MusicFolder(0, "path2", "name2", false, null, 0,
                false);

        @Test
        void testNoFolder() {
            when(musicFolderProvider.getGuestFolders()).thenReturn(Collections.emptyList());
            assertEquals(0, proc.getDirectChildrenCount());
        }

        @Test
        void testWithSingleFolder() {
            when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder1));
            assertEquals(0, proc.getDirectChildrenCount());
            verify(mediaFileProvider, times(1))
                .countAlbums(ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testMultiFolder() {
            when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder1, folder2));
            assertEquals(2, proc.getDirectChildrenCount());
            verify(mediaFileProvider, never())
                .findAlbums(ArgumentMatchers.<MusicFolder>anyList(),
                        any(RuntimeOrderPolicy.AlbumSortOrder.class), anyInt(), anyInt());
        }
    }
}
