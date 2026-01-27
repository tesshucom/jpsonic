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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.infrastructure.NeedsHome;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@ActiveProfiles("test")
@NeedsHome
class UserChartControllerTest {

    @Autowired
    private FontLoader fontLoader;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new UserChartController(mock(SecurityService.class), fontLoader))
            .build();
    }

    @WithMockUser(username = "admin")
    @Test
    void testGet() throws Exception {
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/" + ViewName.USER_CHART.value()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertNotNull(result);
    }
}
