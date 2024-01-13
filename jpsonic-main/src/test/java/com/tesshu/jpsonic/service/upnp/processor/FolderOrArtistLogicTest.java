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
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrArtist;
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
    private MusicFolderDao musicFolderDao;
    private FolderOrArtistLogic logic;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        artistDao = mock(ArtistDao.class);
        musicFolderDao = mock(MusicFolderDao.class);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(mock(SettingsService.class), mock(JWTSecurityService.class),
                mock(MediaFileService.class), mock(PlayerService.class), mock(TranscodingService.class));
        logic = new FolderOrArtistLogic(util, factory, musicFolderDao, artistDao);
    }

    @Test
    void testCreateContainerWithFolder() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        FolderOrArtist folderOrArtist = new FolderOrArtist(folder);
        Mockito.when(artistDao.getArtistsCount(Mockito.anyList())).thenReturn(100);
        Container container = logic.createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(StorageFolder.class, container);
        assertEquals("randomSongByFolderArtist/99", container.getId());
        assertEquals("randomSongByFolderArtist", container.getParentID());
        assertEquals("Music", container.getTitle());
        assertEquals(100, container.getChildCount());
    }

    @Test
    void testCreateContainerWithArtist() {
        Artist artist = new Artist();
        FolderOrArtist folderOrArtist = new FolderOrArtist(artist);
        artist.setId(999);
        artist.setName("Artist");
        artist.setAlbumCount(20);
        Container container = logic.createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(MusicArtist.class, container);
        assertEquals("randomSongByFolderArtist/artist:999", container.getId());
        assertEquals("randomSongByFolderArtist", container.getParentID());
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
        List<FolderOrArtist> folderOrArtists = logic.getDirectChildren(0, 4);
        Mockito.verify(artistDao, Mockito.never()).getAlphabetialArtists(anyInt(), anyInt(), anyList());
        folderOrArtists.forEach(folder -> assertFalse(folder.isArtist()));

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
        Mockito.verify(artistDao, Mockito.never()).getArtistsCount(Mockito.anyList());

        folders = List.of(folder1);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildrenCount());
        Mockito.verify(artistDao, Mockito.times(1)).getArtistsCount(Mockito.anyList());
    }

    @Test
    void testGetDirectChildWithArtist() {
        int id = 99;
        Artist artist = new Artist();
        artist.setId(id);
        artist.setName("artist");
        Mockito.when(artistDao.getArtist(id)).thenReturn(artist);
        FolderOrArtist folderOrArtist = new FolderOrArtist(artist);
        String compositeId = folderOrArtist.createCompositeId();
        FolderOrArtist.toId(folderOrArtist.createCompositeId());
        logic.getDirectChild(compositeId);
        assertEquals(artist, logic.getDirectChild(compositeId).getArtist());
    }

    @Test
    void testGetDirectChildWithFolder() {
        MusicFolder folder1 = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        MusicFolder folder2 = new MusicFolder(1, "/folder2", "folder2", true, now(), 2, false);
        MusicFolder folder3 = new MusicFolder(2, "/folder3", "folder3", true, now(), 3, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(musicFolderDao.getAllMusicFolders()).thenReturn(folders);
        assertEquals(folder3, logic.getDirectChild("2").getFolder());
    }

    @Test
    void testGetChildSizeOfWithArtist() {
        Artist artist = new Artist();
        artist.setId(0);
        artist.setName("artist");
        artist.setAlbumCount(3);
        FolderOrArtist folderOrArtist = new FolderOrArtist(artist);
        assertEquals(3, logic.getChildSizeOf(folderOrArtist));
    }

    @Test
    void testGetChildSizeOfWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrArtist folderOrArtist = new FolderOrArtist(folder);
        assertEquals(0, logic.getChildSizeOf(folderOrArtist));
        Mockito.verify(artistDao, Mockito.times(1)).getArtistsCount(anyList());
    }
}
