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

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
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
class StatusControllerTest {

    private static final String ADMIN_NAME = "admin";

    @Mock
    private StatusService statusService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private SecurityService securityService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        Mockito.when(securityService.getCurrentUser(Mockito.any())).thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ""));
        mockMvc = MockMvcBuilders.standaloneSetup(new StatusController(statusService, settingsService, securityService))
                .build();
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/status.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("status", modelAndView.getViewName());
    }
}
