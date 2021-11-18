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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.GeneralSettingsCommand;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneralSettingsControllerTest {

    private static final String VIEW_NAME = "generalSettings";

    private SettingsService settingsService;
    private GeneralSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        controller = new GeneralSettingsController(settingsService, mock(SecurityService.class),
                mock(ShareService.class), mock(OutlineHelpSelector.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(1)
    void testGet() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.GENERAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        GeneralSettingsCommand command = (GeneralSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(2)
    void testPost() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.GENERAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        GeneralSettingsCommand command = (GeneralSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        command.setThemeIndex("1");

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.GENERAL_SETTINGS.value())
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(
                        MockMvcResultMatchers.flash().attribute(Attributes.Redirect.RELOAD_FLAG.value(), Boolean.FALSE))
                .andExpect(
                        MockMvcResultMatchers.flash().attribute(Attributes.Redirect.TOAST_FLAG.value(), Boolean.TRUE))
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.GENERAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(3)
    void testReload() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.GENERAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        GeneralSettingsCommand command = (GeneralSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        // When the theme is changed
        command.setThemeIndex("1");

        controller.post(command, Mockito.mock(RedirectAttributes.class));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(3)
    void testPostWithIndexOptions() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.GENERAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        GeneralSettingsCommand command = (GeneralSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        assertEquals(IndexScheme.NATIVE_JAPANESE, command.getIndexScheme());
        assertTrue(command.isIgnoreFullWidth());
        assertTrue(command.isDeleteDiacritic());
        command.setDeleteDiacritic(false);
        command.setIgnoreFullWidth(false);
        ArgumentCaptor<Boolean> deleteDiacritic = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> ignoreFullWidth = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setDeleteDiacritic(deleteDiacritic.capture());
        Mockito.doNothing().when(settingsService).setIgnoreFullWidth(ignoreFullWidth.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertTrue(deleteDiacritic.getValue());
        assertTrue(ignoreFullWidth.getValue());

        command.setIndexScheme(IndexScheme.ROMANIZED_JAPANESE);
        command.setDeleteDiacritic(true);
        command.setIgnoreFullWidth(false);
        deleteDiacritic = ArgumentCaptor.forClass(Boolean.class);
        ignoreFullWidth = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setDeleteDiacritic(deleteDiacritic.capture());
        Mockito.doNothing().when(settingsService).setIgnoreFullWidth(ignoreFullWidth.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertTrue(deleteDiacritic.getValue());
        assertTrue(ignoreFullWidth.getValue());

        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        command.setDeleteDiacritic(true);
        command.setIgnoreFullWidth(true);
        deleteDiacritic = ArgumentCaptor.forClass(Boolean.class);
        ignoreFullWidth = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setDeleteDiacritic(deleteDiacritic.capture());
        Mockito.doNothing().when(settingsService).setIgnoreFullWidth(ignoreFullWidth.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertTrue(deleteDiacritic.getValue());
        assertTrue(ignoreFullWidth.getValue());
    }
}
