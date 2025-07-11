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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class PodcastProcTest {

    private PodcastService podcastService;
    private PodcastProc proc;

    @BeforeEach
    public void setup() {
        SettingsService settingsService = mock(SettingsService.class);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService,
                mock(JWTSecurityService.class), mock(MediaFileService.class),
                mock(PlayerService.class), mock(TranscodingService.class));
        podcastService = mock(PodcastService.class);
        proc = new PodcastProc(factory, podcastService);
    }

    @Test
    void testGetProcId() {
        assertEquals("podcast", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        PodcastChannel podcastChannel = new PodcastChannel(10, "url", "title", null, null, null,
                null);
        when(podcastService.getEpisodes(anyInt())).thenReturn(Collections.emptyList());
        Container container = proc.createContainer(podcastChannel);
        assertInstanceOf(MusicAlbum.class, container);
        assertEquals("podcast/10", container.getId());
        assertEquals("podcast", container.getParentID());
        assertEquals("title", container.getTitle());
        assertEquals(0, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
        verify(podcastService, times(1)).getAllChannels();
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(0, proc.getDirectChildrenCount());
        verify(podcastService, times(1)).getAllChannels();
    }

    @Test
    void testGetDirectChild() {
        assertNull(proc.getDirectChild("0"));
        verify(podcastService, times(1)).getChannel(anyInt());
    }

    @Test
    void testGetChildren() {
        PodcastChannel channel = new PodcastChannel(0, null, null, null, null, null, null);
        assertEquals(Collections.emptyList(), proc.getChildren(channel, 0, 0));
        verify(podcastService, times(1)).getEpisodes(anyInt());
    }

    @Test
    void testGetChildSizeOf() {
        PodcastChannel channel = new PodcastChannel(0, null, null, null, null, null, null);
        assertEquals(0, proc.getChildSizeOf(channel));
        verify(podcastService, times(1)).getEpisodes(anyInt());
    }

    @Test
    void testAddChild() {
        DIDLContent content = new DIDLContent();
        PodcastEpisode episode = new PodcastEpisode(null, null, "url", null, null, null, null, null,
                null, null, null, null);
        proc.addChild(content, episode);
        verify(podcastService, never()).getChannel(anyInt());
        assertEquals(0, content.getCount());
        assertEquals(0, content.getContainers().size());
        assertEquals(0, content.getItems().size());

        episode = new PodcastEpisode(0, null, "url", null, null, null, null, null, null, null, null,
                null);
        proc.addChild(content, episode);
        verify(podcastService, never()).getChannel(anyInt());
        assertEquals(0, content.getCount());
        assertEquals(0, content.getContainers().size());
        assertEquals(0, content.getItems().size());

        episode = new PodcastEpisode(0, 0, "url", null, null, null, null, null, null, null, null,
                null);
        proc.addChild(content, episode);
        verify(podcastService, times(1)).getChannel(anyInt());
        assertEquals(0, content.getCount());
        assertEquals(0, content.getContainers().size());
        assertEquals(0, content.getItems().size());

        PodcastChannel channel = new PodcastChannel("url");
        when(podcastService.getChannel(anyInt())).thenReturn(channel);
        clearInvocations(podcastService);
        content = new DIDLContent();
        proc.addChild(content, episode);
        verify(podcastService, times(1)).getChannel(anyInt());
        assertEquals(1, content.getCount());
        assertEquals(0, content.getContainers().size());
        assertEquals(1, content.getItems().size());
    }
}
