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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Instant;
import java.util.Arrays;

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.ArtistOrAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderArtist;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFArtist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class ArtistByFolderProcTest {

    private UpnpProcessorUtil util;
    private ArtistDao artistDao;
    private AlbumDao albumDao;
    private FolderOrArtistLogic folderOrArtistProc;
    private ArtistByFolderProc proc;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        artistDao = mock(ArtistDao.class);
        albumDao = mock(AlbumDao.class);
        SettingsService settingsService = mock(SettingsService.class);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService,
                mock(JWTSecurityService.class), mock(MediaFileService.class),
                mock(PlayerService.class), mock(TranscodingService.class));
        folderOrArtistProc = new FolderOrArtistLogic(util, factory, artistDao);
        proc = new ArtistByFolderProc(util, factory, artistDao, albumDao, folderOrArtistProc);
    }

    @Test
    void testGetProcId() {
        assertEquals("artistByFolder", proc.getProcId().getValue());
    }

    @Test
    void testGetChildrenWithArtist() {
        int id = 99;
        Artist artist = new Artist();
        artist.setId(id);
        artist.setName("artist");
        Mockito.when(artistDao.getArtist(id)).thenReturn(artist);
        MusicFolder folder = new MusicFolder(0, "path", "name", true, Instant.now(), 0, false);
        FolderArtist folderArtist = new FolderArtist(folder, artist);
        assertEquals(0, proc.getChildren(new FolderOrFArtist(folderArtist), 0, 2).size());
        Mockito
            .verify(albumDao, Mockito.times(1))
            .getAlbumsForArtist(anyLong(), anyLong(), anyString(), anyBoolean(), anyList());
    }

    @Test
    void testGetChildrenWithFolder() {
        MusicFolder folder = new MusicFolder(0, "path", "name", true, Instant.now(), 0, false);
        Mockito
            .when(artistDao
                .getAlphabetialArtists(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList()))
            .thenReturn(Arrays.asList(new Artist()));
        assertEquals(1, proc.getChildren(new FolderOrFArtist(folder), 0, 2).size());
        Mockito
            .verify(artistDao, Mockito.times(1))
            .getAlphabetialArtists(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
    }

    @Test
    void testAddChild() {
        Artist artist = new Artist();
        artist.setId(0);
        artist.setName("artist");
        artist.setAlbumCount(3);
        ArtistOrAlbum artistOrAlbum = new ArtistOrAlbum(artist);

        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        proc.addChild(content, artistOrAlbum);
        assertEquals(1, content.getContainers().size());

        content = new DIDLContent();
        Album album = new Album();
        artistOrAlbum = new ArtistOrAlbum(album);
        proc = new ArtistByFolderProc(util, mock(UpnpDIDLFactory.class), artistDao, albumDao,
                folderOrArtistProc);
        assertEquals(0, content.getItems().size());
        proc.addChild(content, artistOrAlbum);
        assertEquals(1, content.getContainers().size());
    }
}
