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
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.Res;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

class UpnpDIDLFactoryTest {

    private SettingsService settingsService;
    private TranscodingDao transcodingDao;
    private PlayerService playerService;
    private UpnpDIDLFactory factory;

    @BeforeEach
    public void setup() {
        transcodingDao = mock(TranscodingDao.class);
        settingsService = mock(SettingsService.class);
        playerService = mock(PlayerService.class);
        factory = new UpnpDIDLFactory(settingsService, new JWTSecurityService(settingsService),
                mock(MediaFileService.class), playerService,
                new TranscodingService(settingsService, null, transcodingDao, playerService, null));
    }

    @Test
    void testCreateURIStringWithToken() {

        MediaFile song = new MediaFile();
        song.setFormat("flac");
        assertFalse(settingsService.isUriWithFileExtensions());
        Mockito
            .when(transcodingDao.getTranscodingsForPlayer(Mockito.anyInt()))
            .thenReturn(Collections.emptyList());

        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl("http://192.168.1.1/ext/stream")
            .queryParam("id", 0)
            .queryParam("player", 0);
        assertFalse(factory.createURIStringWithToken(builder, song).endsWith(".flac"));

        Mockito.when(settingsService.isUriWithFileExtensions()).thenReturn(true);
        assertTrue(factory.createURIStringWithToken(builder, song).endsWith(".flac"));
        song.setFormat(null);

        assertFalse(factory.createURIStringWithToken(builder, song).endsWith(".flac"));
        song.setFormat("flac");
        Transcoding transcoding = new Transcoding(0, "mp3", "flac", "mp3", null, null, null, true);
        Mockito
            .when(transcodingDao.getTranscodingsForPlayer(Mockito.anyInt()))
            .thenReturn(Arrays.asList(transcoding));
        assertTrue(factory.createURIStringWithToken(builder, song).endsWith(".mp3"));
    }

    @Test
    void testToRes() {
        Player player = new Player();
        Mockito
            .when(playerService.getGuestPlayer(Mockito.nullable(HttpServletRequest.class)))
            .thenReturn(player);
        Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn("http://192.168.1.1");
        MediaFile song = new MediaFile();
        song.setFileSize(123L);
        Res res = factory.toRes(song);
        assertNull(res.getSize());
    }
}
