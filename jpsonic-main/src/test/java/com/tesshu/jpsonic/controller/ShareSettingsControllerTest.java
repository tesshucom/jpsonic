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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.controller.ShareSettingsController.ShareInfo;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.Share;
import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
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

@SuppressWarnings("PMD.TooManyStaticImports")
class ShareSettingsControllerTest {

    private ShareService shareService;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        shareService = mock(ShareService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ShareSettingsController(mock(SettingsService.class),
                    mock(MusicFolderService.class), mock(SecurityService.class), shareService,
                    mock(MediaFileService.class)))
            .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testDoGet() throws Exception {

        Instant now = now();
        Share share = new Share();
        share.setCreated(now);
        Mockito
            .when(shareService.getSharesForUser(Mockito.any(User.class)))
            .thenReturn(Arrays.asList(share));
        Mockito
            .when(shareService.getSharedFiles(Mockito.anyInt(), Mockito.anyList()))
            .thenReturn(Arrays.asList(new MediaFile()));

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders
                .get("/" + ViewName.SHARE_SETTINGS.value())
                .param(Attributes.Request.DELETE.value(), "false"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("shareSettings", modelAndView.getViewName());

        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
        List<ShareInfo> shareInfos = (List<ShareInfo>) model.get("shareInfos");
        assertEquals(1, shareInfos.size());
        assertEquals(ZonedDateTime.ofInstant(now, ZoneId.systemDefault()),
                shareInfos.get(0).getShare().getCreatedWithZone());
        assertNull(shareInfos.get(0).getShare().getExpiresWithZone());
        assertNull(shareInfos.get(0).getShare().getLastVisitedWithZone());
        Mockito.clearInvocations(shareService);

        Instant expires = now.plus(2, ChronoUnit.DAYS);
        share.setExpires(expires);
        Instant lastVisited = now.plus(1, ChronoUnit.DAYS);
        share.setLastVisited(lastVisited);
        Mockito
            .when(shareService.getSharesForUser(Mockito.any(User.class)))
            .thenReturn(Arrays.asList(share));
        result = mockMvc
            .perform(MockMvcRequestBuilders
                .get("/" + ViewName.SHARE_SETTINGS.value())
                .param(Attributes.Request.DELETE.value(), "false"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        model = (Map<String, Object>) result.getModelAndView().getModel().get("model");
        shareInfos = (List<ShareInfo>) model.get("shareInfos");
        assertEquals(ZonedDateTime.ofInstant(now, ZoneId.systemDefault()),
                shareInfos.get(0).getShare().getCreatedWithZone());
        assertEquals(ZonedDateTime.ofInstant(expires, ZoneId.systemDefault()),
                shareInfos.get(0).getShare().getExpiresWithZone());
        assertEquals(ZonedDateTime.ofInstant(lastVisited, ZoneId.systemDefault()),
                shareInfos.get(0).getShare().getLastVisitedWithZone());
        Mockito.clearInvocations(shareService);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testDoPost() throws Exception {
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders
                .post("/" + ViewName.SHARE_SETTINGS.value())
                .param(Attributes.Request.DELETE.value(), "false"))
            .andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.SHARE_SETTINGS.value()))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
            .andReturn();
        assertNotNull(result);
    }
}
