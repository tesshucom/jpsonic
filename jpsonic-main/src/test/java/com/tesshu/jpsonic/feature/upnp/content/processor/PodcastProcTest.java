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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.model.PodcastChannel;
import com.tesshu.jpsonic.domain.model.PodcastEpisode;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.domain.provider.resource.PodcastProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.ResolvedAudioTranscodingParameters;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class PodcastProcTest {

    private PodcastProvider podcastProvider;
    private MediaFileProvider mediaFileProvider;
    private PodcastProc proc;

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

        podcastProvider = mock(PodcastProvider.class);
        proc = new PodcastProc(podcastProvider, factory);
    }

    @Test
    void testGetProcId() {
        assertEquals("podcast", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        PodcastChannel podcastChannel = new PodcastChannel(99, "title", "COMPLETED", "thumbUri");
        when(podcastProvider
            .countEpisodes(any(PodcastChannel.class), any(PodcastEpisode.EpisodePhase[].class)))
            .thenReturn(5);

        Container container = proc.createContainer(podcastChannel);
        assertInstanceOf(MusicAlbum.class, container);
        assertEquals("podcast/99", container.getId());
        assertEquals("podcast", container.getParentID());
        assertEquals("title", container.getTitle());
        assertEquals(5, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
        verify(podcastProvider, times(1))
            .findChannels(anyLong(), anyLong(), any(PodcastChannel.ChannelPhase[].class));
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(0, proc.getDirectChildrenCount());
        verify(podcastProvider, times(1)).countChannels(any(PodcastChannel.ChannelPhase[].class));
    }

    @Test
    void testGetDirectChild() {
        assertNull(proc.getDirectChild("0"));
        verify(podcastProvider, times(1)).requireChannel(anyInt());
    }

    @Test
    void testGetChildren() {
        PodcastChannel channel = new PodcastChannel(99, "title", "COMPLETED", "thumbUri");
        assertEquals(Collections.emptyList(), proc.getChildren(channel, 0, 0));
        verify(podcastProvider, times(1))
            .findEpisodes(any(PodcastChannel.class), anyLong(), anyLong(),
                    any(PodcastEpisode.EpisodePhase[].class));
    }

    @Test
    void testGetChildSizeOf() {
        PodcastChannel channel = new PodcastChannel(99, "title", "COMPLETED", "thumbUri");
        assertEquals(0, proc.getChildSizeOf(channel));
        verify(podcastProvider, times(1))
            .countEpisodes(any(PodcastChannel.class), any(PodcastEpisode.EpisodePhase[].class));
    }

    @Test
    void testAddChild() {
        PodcastEpisode episode = new PodcastEpisode(1, "title", 99, "COMPLETED", Instant.now(),
                "path");
        PodcastChannel channel = new PodcastChannel(99, "title", "COMPLETED", "thumbUri");
        MediaFile song = new MediaFile(1, "pathString", 0, "format", "MUSIC", 256, 60, 9999,
                "artist", "album", "title", "albumArtist", 0, "genre", 2026, "thumbUri", "composer",
                "reading", "#", "comment");
        when(podcastProvider.requireChannel(anyInt())).thenReturn(channel);
        when(mediaFileProvider.requireMediaFile(any(Path.class))).thenReturn(song);

        DIDLContent content = new DIDLContent();
        proc.addChild(content, episode);
        verify(podcastProvider, times(1)).requireChannel(anyInt());
        assertEquals(1, content.getCount());
        assertEquals(0, content.getContainers().size());
        assertEquals(1, content.getItems().size());
    }
}
