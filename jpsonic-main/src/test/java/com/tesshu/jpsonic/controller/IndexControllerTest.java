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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.UserSettings;
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
@AutoConfigureMockMvc
@ExtendWith(NeedsHome.class)
class IndexControllerTest {

    private static final String ADMIN_NAME = "admin";
    private static final String VIEW_NAME = "index";

    @Mock
    private SecurityService securityService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        UserSettings settings = new UserSettings(ADMIN_NAME);
        Mockito.when(securityService.getCurrentUsername(Mockito.any())).thenReturn(ADMIN_NAME);
        Mockito.when(securityService.getUserSettings(ADMIN_NAME)).thenReturn(settings);
        mockMvc = MockMvcBuilders.standaloneSetup(new IndexController(securityService)).build();
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/index.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
        assertEquals(3, model.size());
        assertEquals(model.get("brand"), "Jpsonic");
        assertFalse((Boolean) model.get("keyboardShortcutsEnabled"));
        assertFalse((Boolean) model.get("showLeft"));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPostWithView() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.post("/index.view").param("mainView",
                        ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
        assertEquals(4, model.size());
        assertEquals(model.get("brand"), "Jpsonic");
        assertFalse((Boolean) model.get("keyboardShortcutsEnabled"));
        assertFalse((Boolean) model.get("showLeft"));
        assertEquals(model.get("mainView"), "musicFolderSettings.view");
    }
}
