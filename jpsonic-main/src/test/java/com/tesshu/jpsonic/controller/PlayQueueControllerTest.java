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

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class PlayQueueControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new PlayQueueController(mock(SettingsService.class),
                    mock(SecurityService.class), mock(PlayerService.class)))
            .build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testHandleRequestInternal() throws Exception {
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/" + ViewName.PLAY_QUEUE.value()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("playQueue", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
    }
}
