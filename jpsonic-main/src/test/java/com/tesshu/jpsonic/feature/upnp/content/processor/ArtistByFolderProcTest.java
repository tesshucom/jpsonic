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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy.AlbumSortOrder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.ArtistOrAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrArtistLogic;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class ArtistByFolderProcTest {

    private ArtistProvider artistProvider;
    private AlbumProvider albumProvider;
    private FolderOrArtistLogic deligate;

    private ArtistByFolderProc proc;

    @BeforeEach
    void setup() {
        artistProvider = mock(ArtistProvider.class);
        albumProvider = mock(AlbumProvider.class);

        SettingsFacade settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040")
            .build();
        MediaFileProvider mediaFileProvider = mock(MediaFileProvider.class);
        PlayerProvider playerProvider = mock(PlayerProvider.class);
        UpnpPayloadCodec upnpPayloadCodec = mock(UpnpPayloadCodec.class);
        TranscodingParametersPlanner transcodingParametersPlanner = mock(
                TranscodingParametersPlanner.class);
        UPnPDIDLFactory factory = new UPnPDIDLFactory(settingsFacade, upnpPayloadCodec,
                mediaFileProvider, playerProvider, transcodingParametersPlanner);

        proc = new ArtistByFolderProc(artistProvider, albumProvider, deligate, settingsFacade,
                factory);
    }

    @Test
    void testGetProcId() {
        assertEquals("artistByFolder", proc.getProcId().getValue());
    }

    @Test
    void testGetChildrenWithArtist() {
        int id = 99;
        Artist artist = new Artist(id, "artist", "path", 3, 0, "reading", 0, "#");
        when(artistProvider.requireArtist(id)).thenReturn(artist);
        MusicFolder folder = new MusicFolder(0, "path", "name", false, null, 0, false);
        FolderArtist folderArtist = new FolderArtist(folder, artist);
        assertEquals(0, proc.getChildren(new FolderOrFArtist(folderArtist), 0, 2).size());
        verify(albumProvider, Mockito.times(1))
            .findChildren(anyList(), any(Artist.class), any(AlbumSortOrder.class), anyLong(),
                    anyLong());
    }

    @Test
    void testGetChildrenWithFolder() {
        MusicFolder folder = new MusicFolder(0, "path", "name", true, Instant.now(), 0, false);
        when(artistProvider
            .findArtists(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong()))
            .thenReturn(Arrays.asList(new Artist(0, "artist", "path", 3, 0, "reading", 0, "#")));
        assertEquals(1, proc.getChildren(new FolderOrFArtist(folder), 0, 2).size());
        verify(artistProvider, Mockito.times(1))
            .findArtists(ArgumentMatchers.<MusicFolder>anyList(), anyLong(), anyLong());
    }

    @Test
    void testAddChild() {
        Artist artist = new Artist(0, "artist", "path", 3, 0, "reading", 0, "#");
        ArtistOrAlbum artistOrAlbum = new ArtistOrAlbum(artist);

        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        proc.addChild(content, artistOrAlbum);
        assertEquals(1, content.getContainers().size());

        content = new DIDLContent();
        Album album = new Album(999, "album", "artist", 20, null);
        artistOrAlbum = new ArtistOrAlbum(album);
        assertEquals(0, content.getItems().size());
        proc.addChild(content, artistOrAlbum);
        assertEquals(1, content.getContainers().size());
    }
}
