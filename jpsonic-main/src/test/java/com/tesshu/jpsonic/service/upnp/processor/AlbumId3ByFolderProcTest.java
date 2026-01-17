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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.List;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.system.CoverArtScheme;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFAlbum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("PMD.TooManyStaticImports")
class AlbumId3ByFolderProcTest {

    private MediaFileService mediaFileService;
    private AlbumDao albumDao;
    private AlbumId3ByFolderProc proc;

    @BeforeEach
    public void setup() {
        mediaFileService = mock(MediaFileService.class);
        albumDao = mock(AlbumDao.class);
        UpnpProcessorUtil util = mock(UpnpProcessorUtil.class);
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
        FolderOrAlbumLogic folderOrAlbumLogic = new FolderOrAlbumLogic(util, factory, albumDao);
        proc = new AlbumId3ByFolderProc(mediaFileService, albumDao, factory, folderOrAlbumLogic);
    }

    @Test
    void testGetProcId() {
        assertEquals("alid3bf", proc.getProcId().getValue());
    }

    @Test
    void testGetChildrenWithAlbum() {
        int id = 99;
        Album album = new Album();
        album.setId(id);
        album.setName("album");
        album.setArtist("artist");
        Mockito.when(albumDao.getAlbum(id)).thenReturn(album);
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        assertEquals(0, proc.getChildren(folderOrAlbum, 0, 2).size());
        Mockito
            .verify(mediaFileService, Mockito.times(1))
            .getSongsForAlbum(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void testGetChildrenWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        Mockito
            .when(albumDao
                .getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
            .thenReturn(List.of(new Album()));
        FolderOrFAlbum folderOrArtist = new FolderOrFAlbum(folder);
        assertEquals(1, proc.getChildren(folderOrArtist, 0, 2).size());
        Mockito
            .verify(albumDao, Mockito.times(1))
            .getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
    }

    @Test
    void testAddChild() {
        Album album = new Album();
        album.setId(0);
        album.setName("album");
        album.setArtist("artist");
        AlbumOrSong albumOrSong = new AlbumOrSong(album);

        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        assertEquals(0, content.getItems().size());
        proc.addChild(content, albumOrSong);
        assertEquals(1, content.getContainers().size());
        assertEquals(0, content.getItems().size());

        content = new DIDLContent();
        MediaFile song = new MediaFile();
        albumOrSong = new AlbumOrSong(song);
        assertEquals(0, content.getItems().size());
        assertEquals(0, content.getItems().size());
        proc.addChild(content, albumOrSong);
        assertEquals(0, content.getContainers().size());
        assertEquals(1, content.getItems().size());
    }
}
