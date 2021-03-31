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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.airsonic.player.Integration;
import org.airsonic.player.TestCaseUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // pmd/pmd/issues/1084
public class InternalHelpControllerTest {

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    public static void beforeAll() throws IOException {
        System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
        TestCaseUtils.cleanJpsonicHomeForTest();
    }

    @Integration
    @Test
    @WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
    public void testOkForAdmins() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Integration
    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    public void testNotOkForUsers() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
    }
}
