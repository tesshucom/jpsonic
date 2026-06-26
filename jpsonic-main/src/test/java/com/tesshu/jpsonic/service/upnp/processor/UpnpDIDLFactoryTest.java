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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit;
import com.tesshu.jpsonic.domain.model.UserSettings;
import com.tesshu.jpsonic.domain.provider.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.PlayerProvider;
import com.tesshu.jpsonic.domain.provider.TranscodingProvider;
import com.tesshu.jpsonic.domain.provider.UserProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpKeyManager;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.infrastructure.core.NeedsHome;
import com.tesshu.jpsonic.infrastructure.core.NeedsTranscode;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.UPnPSKeys;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.Res;

@NeedsHome
@NeedsTranscode
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports", "PMD.SingularField",
        "PMD.AvoidUsingHardCodedIP" })
class UpnpDIDLFactoryTest {

    private PlayerProvider playerProvider;
    private SettingsFacade settingsFacade;
    private MediaFileProvider mediaFileProvider;
    private UpnpDIDLFactory factory;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.basic.baseLanUrl, "http://192.168.1.1")
            .withBoolean(UPnPSKeys.basic.uriWithFileExtensions, false)
            .buildWithDefault();
        init();
    }

    @Ignore
    void init() {
        MediaFile mediaFile = mock(MediaFile.class);
        when(mediaFile.format()).thenReturn(new MediaFile.Format("mp3"));

        playerProvider = mock(PlayerProvider.class);
        Player player = new Player(0, "guest", BitRateLimit.OFF, "127.0.0.0", Instant.now());
        when(playerProvider.getUPnPPlayer()).thenReturn(player);

        UserSettings settings = new UserSettings("guest", BitRateLimit.MAX_128);
        UserProvider userProvider = mock(UserProvider.class);
        when(userProvider.getUserSettings(anyString())).thenReturn(settings);

        TranscodingProvider transcodingProvider = mock(TranscodingProvider.class);
        final TranscodingParametersPlanner parametersPlanner = new TranscodingParametersPlanner(
                settingsFacade, userProvider, transcodingProvider);

        mediaFileProvider = mock(MediaFileProvider.class);
        final int id = 0;
        final int folderId = 0;
        final String pathString = "path";
        final String format = "mp3";
        String type = "MUSIC";
        Integer bitRate = 256;
        Integer durationSeconds = 512;
        long fileSize = 128;
        String artist = "artist";
        String album = "album";
        String title = "title";
        MediaFile song = new MediaFile(id, folderId, pathString, format, type, bitRate,
                durationSeconds, fileSize, artist, album, title);
        when(mediaFileProvider.requireMediaFile(anyInt())).thenReturn(song);

        UpnpKeyManager upnpKeyManager = mock(UpnpKeyManager.class);
        when(upnpKeyManager.getKey()).thenReturn("dummyKey");
        UpnpPayloadCodec upnpPayloadCodec = new UpnpPayloadCodec(upnpKeyManager);

        factory = new UpnpDIDLFactory(settingsFacade, upnpPayloadCodec,
                mock(MediaFileService.class), mediaFileProvider, playerProvider, parametersPlanner);
    }

    @Test
    void testToRes() {
        com.tesshu.jpsonic.persistence.api.entity.MediaFile dummy = new com.tesshu.jpsonic.persistence.api.entity.MediaFile();
        dummy.setId(0);

        Res res = factory.toRes(dummy);
        assertEquals("http://192.168.1.1/ext/upnp/stream/ec24379a1b.mp3", res.getValue());
    }
}
