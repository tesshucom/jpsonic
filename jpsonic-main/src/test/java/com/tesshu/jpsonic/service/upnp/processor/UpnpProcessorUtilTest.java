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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

class UpnpProcessorUtilTest {

    private MusicFolderService musicFolderService;
    private SettingsService settingsService;
    private TranscodingDao transcodingDao;
    private UpnpProcessorUtil util;

    @BeforeEach
    public void setup() {
        musicFolderService = mock(MusicFolderService.class);
        transcodingDao = mock(TranscodingDao.class);
        settingsService = mock(SettingsService.class);
        PlayerService playerService = mock(PlayerService.class);
        util = new UpnpProcessorUtil(settingsService, musicFolderService, mock(SecurityService.class),
                mock(JpsonicComparators.class), new JWTSecurityService(settingsService), playerService,
                new TranscodingService(settingsService, null, transcodingDao, playerService, null));
    }

    @Test
    void testGetAllMusicFolders() {
        Mockito.when(settingsService.isDlnaGuestPublish()).thenReturn(true);
        assertNotNull(util.getGuestMusicFolders());
    }

    @Test
    void testCreateURIStringWithToken() {

        MediaFile song = new MediaFile();
        song.setFormat("flac");
        assertFalse(settingsService.isUriWithFileExtensions());
        Mockito.when(transcodingDao.getTranscodingsForPlayer(Mockito.anyInt())).thenReturn(Collections.emptyList());

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://192.168.1.1/ext/stream")
                .queryParam("id", 0).queryParam("player", 0);
        assertFalse(util.createURIStringWithToken(builder, song).endsWith(".flac"));

        Mockito.when(settingsService.isUriWithFileExtensions()).thenReturn(true);
        assertTrue(util.createURIStringWithToken(builder, song).endsWith(".flac"));
        song.setFormat(null);

        assertFalse(util.createURIStringWithToken(builder, song).endsWith(".flac"));
        song.setFormat("flac");
        Transcoding transcoding = new Transcoding(0, "mp3", "flac", "mp3", null, null, null, true);
        Mockito.when(transcodingDao.getTranscodingsForPlayer(Mockito.anyInt())).thenReturn(Arrays.asList(transcoding));
        assertTrue(util.createURIStringWithToken(builder, song).endsWith(".mp3"));
    }

    @Test
    void testIsGenreCountAvailable() {
        Mockito.when(settingsService.isDlnaGenreCountVisible()).thenReturn(false);
        assertFalse(util.isGenreCountAvailable());

        Mockito.when(settingsService.isDlnaGenreCountVisible()).thenReturn(true);
        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(0, null, null, true, null));
        Mockito.when(musicFolderService.getAllMusicFolders()).thenReturn(musicFolders);
        Mockito.when(musicFolderService.getMusicFoldersForUser(User.USERNAME_GUEST)).thenReturn(musicFolders);
        assertTrue(util.isGenreCountAvailable());

        Mockito.when(musicFolderService.getAllMusicFolders()).thenReturn(Collections.emptyList());
        Mockito.when(musicFolderService.getMusicFoldersForUser(User.USERNAME_GUEST)).thenReturn(musicFolders);
        assertFalse(util.isGenreCountAvailable());

        Mockito.when(musicFolderService.getAllMusicFolders()).thenReturn(musicFolders);
        Mockito.when(musicFolderService.getMusicFoldersForUser(User.USERNAME_GUEST))
                .thenReturn(Collections.emptyList());
        assertFalse(util.isGenreCountAvailable());
    }
}
