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

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.ResolvedAudioTranscodingParameters;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrAlbumLogic;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class AlbumId3ByFolderProcTest {

    private AlbumProvider albumProvider;
    private AlbumId3ByFolderProc proc;
    private MediaFileProvider mediaFileProvider;

    @BeforeEach
    void setup() {
        TranscodingParametersPlanner parametersPlanner = mock(TranscodingParametersPlanner.class);
        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.format()).thenReturn(new MediaFile.Format("mp3"));
        ResolvedAudioTranscodingParameters param = new ResolvedAudioTranscodingParameters(false,
                mediaFile, null, null);
        assertNotNull(param.outputFormat());
        when(parametersPlanner
            .resolveAudioTranscodingParameters(nullable(Player.class), nullable(MediaFile.class),
                    nullable(Integer.class), nullable(String.class)))
            .thenReturn(param);

        SettingsFacade settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040")
            .build();
        mediaFileProvider = mock(MediaFileProvider.class);
        PlayerProvider playerProvider = mock(PlayerProvider.class);
        UpnpPayloadCodec upnpPayloadCodec = mock(UpnpPayloadCodec.class);

        UPnPDIDLFactory factory = new UPnPDIDLFactory(settingsFacade, upnpPayloadCodec,
                mediaFileProvider, playerProvider, parametersPlanner);
        albumProvider = mock(AlbumProvider.class);
        FolderOrAlbumLogic folderOrAlbumLogic = new FolderOrAlbumLogic(
                mock(MusicFolderProvider.class), albumProvider, factory);
        proc = new AlbumId3ByFolderProc(mediaFileProvider, albumProvider, factory,
                folderOrAlbumLogic);
    }

    @Test
    void testGetProcId() {
        assertEquals("alid3bf", proc.getProcId().getValue());
    }

    @Test
    void testGetChildrenWithAlbum() {
        int id = 99;
        Album album = new Album(id, "album", "artist", 1, null);
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        assertEquals(0, proc.getChildren(folderOrAlbum, 0, 2).size());
        Mockito
            .verify(mediaFileProvider, Mockito.times(1))
            .findChildren(anyList(), any(Album.class), anyLong(), anyLong(),
                    any(MediaFile.Type[].class));
    }

    @Test
    void testGetChildrenWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        when(albumProvider
            .findAlbums(anyList(), any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(),
                    anyLong()))
            .thenReturn(List.of(new Album(0, "album", "artist", 1, null)));
        FolderOrFAlbum folderOrArtist = new FolderOrFAlbum(folder);
        assertEquals(1, proc.getChildren(folderOrArtist, 0, 2).size());
        Mockito
            .verify(albumProvider, Mockito.times(1))
            .findAlbums(anyList(), any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(),
                    anyLong());
    }

    @Test
    void testAddChild() {
        Album album = new Album(0, "album", "artist", 1, null);
        AlbumOrSong albumOrSong = new AlbumOrSong(album);

        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        assertEquals(0, content.getItems().size());
        proc.addChild(content, albumOrSong);
        assertEquals(1, content.getContainers().size());
        assertEquals(0, content.getItems().size());

        content = new DIDLContent();
        MediaFile song = new MediaFile(1, "pathString", null, "format", "MUSIC", 256, 60, 9999,
                "artist", "album", "title", "albumArtist", 0, "genre", 2026, "thumbUri", "composer",
                "reading", "#", "comment");
        albumOrSong = new AlbumOrSong(song);
        assertEquals(0, content.getItems().size());
        assertEquals(0, content.getItems().size());
        proc.addChild(content, albumOrSong);
        assertEquals(0, content.getContainers().size());
        assertEquals(1, content.getItems().size());
    }
}
