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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.domain.Version;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.VersionService;
import com.tesshu.jpsonic.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(NeedsHome.class) // For static access to log files
class HelpControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException, URISyntaxException, IOException {
        SecurityService securityService = mock(SecurityService.class);
        VersionService versionService = mock(VersionService.class);
        Mockito.when(versionService.getLocalVersion()).thenReturn(new Version("v110.0.0"));
        mockMvc = MockMvcBuilders
            .standaloneSetup(new HelpController(versionService, securityService))
            .build();

        Path testLog = Path.of(TestCaseUtils.jpsonicHomePathForTest(), "jpsonic.log");
        if (!Files.exists(testLog)) {
            FileUtil.createDirectories(Path.of(TestCaseUtils.jpsonicHomePathForTest()));
            testLog = Files.createFile(testLog);
            Path dummySource = Path.of(HelpControllerTest.class.getResource("/banner.txt").toURI());
            Files.copy(dummySource, testLog, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/" + ViewName.HELP.value()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("help", modelAndView.getViewName());
    }
}
