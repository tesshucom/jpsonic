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

package com.tesshu.jpsonic.service.upnp.processor;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFAlbum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class RecentAlbumId3ByFolderProcTest {

    private MediaFileService mediaFileService;
    private AlbumDao albumDao;
    private RecentAlbumId3ByFolderProc processor;

    @BeforeEach
    public void setup() {
        mediaFileService = mock(MediaFileService.class);
        albumDao = mock(AlbumDao.class);
        UpnpDIDLFactory factory = mock(UpnpDIDLFactory.class);
        FolderOrAlbumLogic folderOrAlbumLogic = mock(FolderOrAlbumLogic.class);
        processor = new RecentAlbumId3ByFolderProc(mediaFileService, albumDao, factory,
                folderOrAlbumLogic);

    }

    @Test
    void testGetProcId() {
        assertEquals("rid3bf", processor.getProcId().getValue());
    }

    @Nested
    class GetChildrenTest {

        private final MusicFolder folder = new MusicFolder("pathString", "name1", true, null,
                false);

        @Test
        void testAlbumChildren() {
            Album album = new Album();
            album.setArtist("artist");
            album.setName("album");
            FolderAlbum folderAlbum = new FolderAlbum(folder, album);
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folderAlbum);
            assertTrue(processor.getChildren(folderOrAlbum, 0, 0).isEmpty());
            verify(mediaFileService, times(1))
                .getSongsForAlbum(anyLong(), anyLong(), anyString(), anyString());
            verify(albumDao, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testFolderChildrenWithCountZero() {
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
            assertTrue(processor.getChildren(folderOrAlbum, 0, 0).isEmpty());
            verify(mediaFileService, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
            verify(albumDao, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testFolderChildrenWithValidValue() {
            when(albumDao.getAlbumCount(ArgumentMatchers.<MusicFolder>anyList())).thenReturn(1);
            FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
            assertTrue(processor.getChildren(folderOrAlbum, 0, 1).isEmpty());
            verify(mediaFileService, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
            verify(albumDao, times(1))
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }
    }
}
