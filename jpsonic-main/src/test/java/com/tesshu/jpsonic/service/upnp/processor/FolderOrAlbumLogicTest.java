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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.List;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.system.CoverArtScheme;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFAlbum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;
import org.jupnp.support.model.container.StorageFolder;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class FolderOrAlbumLogicTest {

    private UpnpProcessorUtil util;
    private AlbumDao albumDao;
    private FolderOrAlbumLogic logic;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        albumDao = mock(AlbumDao.class);
        SettingsService settingsService = mock(SettingsService.class);
        Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn("https://192.168.1.1:4040");
        JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
        UriComponentsBuilder dummyCoverArtbuilder = UriComponentsBuilder
            .fromUriString(
                    settingsService.getDlnaBaseLANURL() + "/ext/" + ViewName.COVER_ART.value())
            .queryParam("id", "99")
            .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize());
        Mockito
            .when(jwtSecurityService.addJWTToken(Mockito.any(UriComponentsBuilder.class)))
            .thenReturn(dummyCoverArtbuilder);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService, jwtSecurityService,
                mock(MediaFileService.class), mock(PlayerService.class),
                mock(TranscodingService.class));
        logic = new FolderOrAlbumLogic(util, factory, albumDao);

    }

    @Test
    void testCreateContainerWithFolder() {
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
        Mockito.when(albumDao.getAlbumCount(anyList())).thenReturn(100);
        Container container = logic.createContainer(ProcId.ALBUM_ID3_BY_FOLDER, folderOrAlbum);
        assertInstanceOf(StorageFolder.class, container);
        assertEquals("alid3bf/99", container.getId());
        assertEquals("alid3bf", container.getParentID());
        assertEquals("Music", container.getTitle());
        assertEquals(100, container.getChildCount());
    }

    @Test
    void testCreateContainerWithAlbum() {
        Album album = new Album();
        album.setId(999);
        album.setName("Album");
        album.setSongCount(20);
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        Container container = logic.createContainer(ProcId.ALBUM_ID3_BY_FOLDER, folderOrAlbum);
        assertInstanceOf(MusicAlbum.class, container);
        assertEquals("alid3bf/fal:99;999", container.getId());
        assertEquals("alid3bf", container.getParentID());
        assertEquals("Album", container.getTitle());
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
        List<FolderOrFAlbum> folderOrAlbums = logic.getDirectChildren(0, 4);
        Mockito
            .verify(albumDao, Mockito.never())
            .getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
        folderOrAlbums.forEach(folder -> assertFalse(folder.isFolderAlbum()));

        folders = List.of(folder1);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildren(0, 4).size());
        Mockito
            .verify(albumDao, Mockito.times(1))
            .getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder1 = new MusicFolder("/folder1", "folder1", true, now(), false);
        MusicFolder folder2 = new MusicFolder("/folder2", "folder2", true, now(), false);
        MusicFolder folder3 = new MusicFolder("/folder3", "folder3", true, now(), false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildrenCount());
        Mockito.verify(albumDao, Mockito.never()).getAlbumCount(anyList());

        folders = List.of(folder1);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildrenCount());
        Mockito.verify(albumDao, Mockito.times(1)).getAlbumCount(anyList());
    }

    @Test
    void testGetDirectChildWithAlbum() {
        int id = 99;
        Album album = new Album();
        album.setId(id);
        album.setName("album");
        Mockito.when(albumDao.getAlbum(id)).thenReturn(album);
        MusicFolder folder = new MusicFolder(88, "/Nusic", "Music", true, null, null, false);
        Mockito.when(util.getGuestFolders()).thenReturn(List.of(folder));
        FolderAlbum folderAlbum = new FolderAlbum(folder, album);
        String compositeId = folderAlbum.createCompositeId();
        assertEquals(album, logic.getDirectChild(compositeId).getFolderAlbum().album());
    }

    @Test
    void testGetDirectChildWithFolder() {
        MusicFolder folder1 = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        MusicFolder folder2 = new MusicFolder(1, "/folder2", "folder2", true, now(), 2, false);
        MusicFolder folder3 = new MusicFolder(2, "/folder3", "folder3", true, now(), 3, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(util.getGuestFolders()).thenReturn(folders);
        assertEquals(folder3, logic.getDirectChild("2").getFolder());
    }

    @Test
    void testGetChildSizeOfWithAlbum() {
        Album album = new Album();
        album.setId(0);
        album.setName("album");
        album.setSongCount(3);
        MusicFolder folder = new MusicFolder(99, "/Nusic", "Music", true, null, null, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        assertEquals(3, logic.getChildSizeOf(folderOrAlbum));
    }

    @Test
    void testGetChildSizeOfWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
        assertEquals(0, logic.getChildSizeOf(folderOrAlbum));
        Mockito.verify(albumDao, Mockito.times(1)).getAlbumCount(anyList());
    }
}
