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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
class PodcastChannelControllerTest {

    private static final String ADMIN_NAME = "admin";

    @Mock
    private SecurityService securityService;
    @Mock
    private PodcastService podcastService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        Mockito.when(securityService.getCurrentUser(Mockito.any())).thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ""));
        Mockito.when(podcastService.getChannel(Mockito.nullable(int.class))).thenReturn(new PodcastChannel(""));
        Mockito.when(podcastService.getEpisodes(Mockito.nullable(int.class))).thenReturn(Collections.emptyList());
        mockMvc = MockMvcBuilders.standaloneSetup(new PodcastChannelController(securityService, podcastService))
                .build();
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.PODCAST_CHANNEL.value())
                        .param(Attributes.Request.ID.value(), Integer.toString(0)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("podcastChannel", modelAndView.getViewName());
    }
}
