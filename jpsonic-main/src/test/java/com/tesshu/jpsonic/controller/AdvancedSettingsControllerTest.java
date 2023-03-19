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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.AdvancedSettingsCommand;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SuppressWarnings("PMD.TooManyStaticImports")
class AdvancedSettingsControllerTest {

    private static final String VIEW_NAME = "advancedSettings";

    private SettingsService settingsService;
    private AdvancedSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        controller = new AdvancedSettingsController(settingsService, mock(SecurityService.class),
                mock(ShareService.class), mock(OutlineHelpSelector.class), mock(ScannerStateService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPost() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.ADVANCED_SETTINGS.value())
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Test
    void testSetScanLog() {

        Mockito.when(settingsService.getDefaultScanLogRetention()).thenReturn(-1);

        ArgumentCaptor<Integer> scanLogRetention = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Boolean> useScanEvents = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> measureMemory = ArgumentCaptor.forClass(Boolean.class);

        Mockito.doNothing().when(settingsService).setScanLogRetention(scanLogRetention.capture());
        Mockito.doNothing().when(settingsService).setUseScanEvents(useScanEvents.capture());
        Mockito.doNothing().when(settingsService).setMeasureMemory(measureMemory.capture());

        AdvancedSettingsCommand command = new AdvancedSettingsCommand();
        command.setUseScanLog(false);
        command.setScanLogRetention(30);
        command.setUseScanEvents(true);
        command.setMeasureMemory(true);

        controller.setScanLog(command);

        assertEquals(-1, scanLogRetention.getValue());
        assertFalse(useScanEvents.getValue());
        assertFalse(measureMemory.getValue());

        command.setUseScanLog(true);
        controller.setScanLog(command);

        assertEquals(30, scanLogRetention.getValue());
        assertTrue(useScanEvents.getValue());
        assertTrue(measureMemory.getValue());
    }

    /*
     * Full scan required if schema changes (Compare before reflecting settings)
     */
    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testChangeIndexScheme() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        assertFalse(command.isForceInternalValueInsteadOfTags());
        command.setForceInternalValueInsteadOfTags(true);

        assertEquals(IndexScheme.NATIVE_JAPANESE, command.getIndexScheme());
        assertFalse(settingsService.isIgnoreFileTimestamps());

        ArgumentCaptor<Boolean> forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());

        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
        command.setIndexScheme(IndexScheme.NATIVE_JAPANESE);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertFalse(forceInternalValueInsteadOfTags.getValue());

        Mockito.clearInvocations(settingsService);
        forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());
        command.setIndexScheme(IndexScheme.ROMANIZED_JAPANESE);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertTrue(forceInternalValueInsteadOfTags.getValue());

        Mockito.clearInvocations(settingsService);
        forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1)).setForceInternalValueInsteadOfTags(Mockito.anyBoolean());

        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));

        Mockito.clearInvocations(settingsService);
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        assertEquals(IndexScheme.NATIVE_JAPANESE.name(), settingsService.getIndexSchemeName());
        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));

        Mockito.clearInvocations(settingsService);
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        command.setIndexScheme(IndexScheme.NATIVE_JAPANESE);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
    }
}
