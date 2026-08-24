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

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrAlbumLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class RecentAlbumId3ByFolderProcTest {

    private MediaFileProvider mediaFileProvider;
    private AlbumProvider albumProvider;
    private RecentAlbumId3ByFolderProc processor;

    @BeforeEach
    void setup() {
        mediaFileProvider = mock(MediaFileProvider.class);
        albumProvider = mock(AlbumProvider.class);
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        FolderOrAlbumLogic folderOrAlbumLogic = mock(FolderOrAlbumLogic.class);
        processor = new RecentAlbumId3ByFolderProc(mediaFileProvider, albumProvider, factory,
                folderOrAlbumLogic);
    }

    @Test
    void testGetProcId() {
        assertEquals("rid3bf", processor.getProcId().getValue());
    }

    @Nested
    class GetChildrenTest {

        private final MusicFolder folder = new MusicFolder(0, "pathString", "name1", false, null, 0,
                false);

        @Test
        void testAlbumChildren() {
            Album album = new Album(0, "album", "artist", 0, null);
            FolderAlbum folderAlbum = new FolderAlbum(folder, album);
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folderAlbum);
            assertTrue(processor.getChildren(folderOrAlbum, 0, Integer.MAX_VALUE).isEmpty());
            verify(mediaFileProvider, times(1))
                .findChildren(ArgumentMatchers.<MusicFolder>anyList(), any(Album.class), anyLong(),
                        anyLong(), any(MediaFile.Type[].class));
            verify(albumProvider, never())
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }

        @Test
        void testFolderChildrenWithCountZero() {
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
            assertTrue(processor.getChildren(folderOrAlbum, 0, Integer.MAX_VALUE).isEmpty());
            verify(mediaFileProvider, never())
                .findChildren(ArgumentMatchers.<MusicFolder>anyList(), any(Album.class), anyLong(),
                        anyLong(), any(MediaFile.Type[].class));
            verify(albumProvider, times(1))
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }

        @Test
        void testFolderChildrenWithValidValue() {
            when(albumProvider.countAlbums(ArgumentMatchers.<MusicFolder>anyList())).thenReturn(1);
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
            assertTrue(processor.getChildren(folderOrAlbum, 0, 1).isEmpty());
            verify(mediaFileProvider, never())
                .findChildren(ArgumentMatchers.<MusicFolder>anyList(), any(Album.class), anyLong(),
                        anyLong(), any(MediaFile.Type[].class));
            verify(albumProvider, times(1))
                .findNewestAlbums(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
        }
    }
}
