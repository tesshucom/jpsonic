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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class PodcastControllerTest {

    private SettingsService settingsService;
    private PlaylistService playlistService;
    private MockMvc mockMvc;
    private final Instant created = ZonedDateTime.of(2022, 10, 20, 3, 4, 5, 6, ZoneOffset.UTC).toInstant();

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        playlistService = mock(PlaylistService.class);
        Mockito.when(settingsService.isPublishPodcast()).thenReturn(true);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PodcastController(settingsService, mock(SecurityService.class), playlistService))
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = "admin")
    void testGet() throws Exception {

        Mockito.when(settingsService.getLocale()).thenReturn(Locale.JAPAN);

        Playlist playlist = new Playlist();
        playlist.setCreated(created);
        Mockito.when(playlistService.getReadablePlaylistsForUser(Mockito.nullable(String.class)))
                .thenReturn(Arrays.asList(playlist));
        MediaFile song = new MediaFile();
        song.setFileSize(20L);
        Mockito.when(playlistService.getFilesInPlaylist(playlist.getId())).thenReturn(Arrays.asList(song));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PODCAST.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("podcast", modelAndView.getViewName());
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");

        // lang depends on SettingsService
        assertEquals("ja", model.get("lang"));

        List<PodcastController.Podcast> podcasts = (List<PodcastController.Podcast>) model.get("podcasts");
        assertEquals(1, podcasts.size());

        // date format must be English
        assertEquals(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z").withZone(ZoneId.systemDefault())
                .withLocale(Locale.ENGLISH).format(created), podcasts.get(0).getPublishDate());
    }
}
