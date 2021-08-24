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

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.RatingService;
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

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
class SetRatingControllerTest {

    @Mock
    private RatingService ratingService;
    @Mock
    private SecurityService securityService;
    @Mock
    private MediaFileService mediaFileService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        Mockito.when(mediaFileService.getMediaFile(Mockito.anyInt())).thenReturn(new MediaFile());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SetRatingController(ratingService, securityService, mediaFileService)).build();
    }

    @Test
    @WithMockUser(username = "admin")
    void testGet() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/setRating.view").param(Attributes.Request.ID.value(),
                        Integer.toString(0)))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.MAIN.value() + "?id=0"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }
}
