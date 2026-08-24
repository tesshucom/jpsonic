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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.tesshu.jpsonic.domain.model.MediaFile;
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
class RecentAlbumByFolderProcTest {

    private MediaFileProvider mediaFileProvider;
    private MusicFolderProvider musicFolderProvider;
    private RecentAlbumByFolderProc processor;

    @BeforeEach
    void setup() {
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        mediaFileProvider = mock(MediaFileProvider.class);
        musicFolderProvider = mock(MusicFolderProvider.class);
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        processor = new RecentAlbumByFolderProc(musicFolderProvider, mediaFileProvider,
                settingsFacade, factory);
    }

    @Test
    void testGetProcId() {
        assertEquals("rbf", processor.getProcId().getValue());
    }

    @Nested
    class GetChildrenTest {

        private MediaFile folder;

        @BeforeEach
        void setup() {
            folder = new MediaFile(1, "pathString", 0, "format", "DIRECTORY", 256, 60, 9999,
                    "artist", "album", "title", "albumArtist", 0, "genre", 2026, "thumbUri",
                    "composer", "reading", "#", "comment");
            when(musicFolderProvider.getGuestFolders())
                .thenReturn(List
                    .of(new MusicFolder(0, folder.pathString(), "name1", false, null, 0, false)));
        }

        @Test
        void testAlbumChildren() {
            MediaFile album = new MediaFile(1, "pathString", 0, "format", "ALBUM", 256, 60, 9999,
                    "artist", "album", "title", "albumArtist", 0, "genre", 2026, "thumbUri",
                    "composer", "reading", "#", "comment");
            assertTrue(processor.getChildren(album, 0, 0).isEmpty());

            verify(mediaFileProvider, times(1))
                .findChildren(any(MediaFile.class), any(RuntimeOrderPolicy.ChildOrder.class),
                        anyLong(), anyLong(), any(MediaFile.Type[].class));
            verify(mediaFileProvider, never())
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }

        @Test
        void testFolderChildrenWithCountZero() {
            when(mediaFileProvider.countAlbums(ArgumentMatchers.<MusicFolder>anyList()))
                .thenReturn(100);
            assertTrue(processor.getChildren(folder, 0, Integer.MAX_VALUE).isEmpty());

            verify(mediaFileProvider, never())
                .findChildren(any(MediaFile.class), any(RuntimeOrderPolicy.ChildOrder.class),
                        anyLong(), anyLong(), any(MediaFile.Type[].class));
            verify(mediaFileProvider, times(1))
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }

        @Test
        void testFolderChildrenWithValidValue() {
            when(mediaFileProvider.countAlbums(ArgumentMatchers.<MusicFolder>anyList()))
                .thenReturn(1);
            assertTrue(processor.getChildren(folder, 0, 1).isEmpty());

            verify(mediaFileProvider, never())
                .findChildren(any(MediaFile.class), any(RuntimeOrderPolicy.ChildOrder.class),
                        anyLong(), anyLong(), any(MediaFile.Type[].class));
            verify(mediaFileProvider, times(1))
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }
    }
}
