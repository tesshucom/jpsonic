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

import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

class UpnpProcessorUtilTest {

    private SettingsService settingsService;
    private UpnpProcessorUtil util;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        util = new UpnpProcessorUtil(settingsService, mock(MusicFolderService.class), mock(SecurityService.class),
                mock(JpsonicComparators.class), new JWTSecurityService(settingsService),
                mock(TranscodingService.class));
    }

    @Test
    void testGetAllMusicFolders() {
        Mockito.when(settingsService.isDlnaGuestPublish()).thenReturn(true);
        assertNotNull(util.getGuestMusicFolders());
    }

    @Test
    void testCreateURIStringWithToken() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://192.168.1.1/ext/stream")
                .queryParam("id", 0).queryParam("player", 0);
        MediaFile song = new MediaFile();
        song.setFormat("flac");
        assertFalse(settingsService.isUriWithFileExtensions());
        assertFalse(util.createURIStringWithToken(builder, song).endsWith(".flac"));
        Mockito.when(settingsService.isUriWithFileExtensions()).thenReturn(true);
        assertTrue(util.createURIStringWithToken(builder, song).endsWith(".flac"));

        song.setFormat(null);
        assertFalse(util.createURIStringWithToken(builder, song).endsWith(".flac"));
    }
}
