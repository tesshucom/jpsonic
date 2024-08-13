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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.List;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderArtist;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFArtist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.jupnp.support.model.container.StorageFolder;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class FolderOrArtistLogicTest {

    private UpnpProcessorUtil util;
    private ArtistDao artistDao;
    private FolderOrArtistLogic logic;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        artistDao = mock(ArtistDao.class);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(mock(SettingsService.class), mock(JWTSecurityService.class),
                mock(MediaFileService.class), mock(PlayerService.class), mock(TranscodingService.class));
        logic = new FolderOrArtistLogic(util, factory, artistDao);
    }

    @Test
    void testCreateContainerWithFolder() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(folder);
        Mockito.when(artistDao.getArtistsCount(anyList())).thenReturn(100);
        Container container = logic.createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(StorageFolder.class, container);
        assertEquals("rsbfar/99", container.getId());
        assertEquals("rsbfar", container.getParentID());
        assertEquals("Music", container.getTitle());
        assertEquals(100, container.getChildCount());
    }

    @Test
    void testCreateContainerWithArtist() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        Artist artist = new Artist();
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(new FolderArtist(folder, artist));
        artist.setId(999);
        artist.setName("Artist");
        artist.setAlbumCount(20);
        Container container = logic.createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(MusicArtist.class, container);
        assertEquals("rsbfar/far:99;999", container.getId());
        assertEquals("rsbfar", container.getParentID());
        assertEquals("Artist", container.getTitle());
        assertEquals(20, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        MusicFolder folder1 = new MusicFolder("/folder1", "folder1", true, now(), false);
        MusicFolder folder2 = new MusicFolder("/folder2", "folder2", true, now(), false);
        MusicFolder folder3 = new MusicFolder("/folder3", "folder3", true, now(), false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildren(0, 4).size());
        assertEquals(2, logic.getDirectChildren(0, 2).size());
        assertEquals(2, logic.getDirectChildren(1, 4).size());
        assertEquals(2, logic.getDirectChildren(1, 2).size());
        List<FolderOrFArtist> folderOrArtists = logic.getDirectChildren(0, 4);
        Mockito.verify(artistDao, Mockito.never()).getAlphabetialArtists(anyInt(), anyInt(), anyList());
        folderOrArtists.forEach(folder -> assertFalse(folder.isFolderArtist()));

        folders = List.of(folder1);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildren(0, 4).size());
        Mockito.verify(artistDao, Mockito.times(1)).getAlphabetialArtists(anyInt(), anyInt(), anyList());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder1 = new MusicFolder("/folder1", "folder1", true, now(), false);
        MusicFolder folder2 = new MusicFolder("/folder2", "folder2", true, now(), false);
        MusicFolder folder3 = new MusicFolder("/folder3", "folder3", true, now(), false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildrenCount());
        Mockito.verify(artistDao, Mockito.never()).getArtistsCount(anyList());

        folders = List.of(folder1);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildrenCount());
        Mockito.verify(artistDao, Mockito.times(1)).getArtistsCount(anyList());
    }

    @Test
    void testGetDirectChildWithArtist() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        Mockito.when(util.getGuestFolders()).thenReturn(List.of(folder));
        int id = 88;
        Artist artist = new Artist();
        artist.setId(id);
        artist.setName("artist");
        Mockito.when(artistDao.getArtist(id)).thenReturn(artist);
        FolderArtist folderArtist = new FolderArtist(folder, artist);
        String compositeId = folderArtist.createCompositeId();
        logic.getDirectChild(compositeId);
        assertEquals(artist, logic.getDirectChild(compositeId).getFolderArtist().artist());
    }

    @Test
    void testGetDirectChildWithFolder() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        Mockito.when(util.getGuestFolders()).thenReturn(List.of(folder));
        assertEquals(folder, logic.getDirectChild("99").getFolder());
    }

    @Test
    void testGetChildSizeOfWithArtist() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        Artist artist = new Artist();
        artist.setId(0);
        artist.setName("artist");
        artist.setAlbumCount(3);
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(new FolderArtist(folder, artist));
        assertEquals(3, logic.getChildSizeOf(folderOrArtist));
    }

    @Test
    void testGetChildSizeOfWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(folder);
        assertEquals(0, logic.getChildSizeOf(folderOrArtist));
        Mockito.verify(artistDao, Mockito.times(1)).getArtistsCount(anyList());
    }
}
