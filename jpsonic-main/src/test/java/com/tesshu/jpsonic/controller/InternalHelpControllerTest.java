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

package com.tesshu.jpsonic.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tesshu.jpsonic.Integration;
import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // pmd/pmd/issues/1084
class InternalHelpControllerTest {

    @Autowired
    private MockMvc mvc;

    @Integration
    @Test
    @WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
    void testOkForAdmins() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Integration
    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void testNotOkForUsers() throws Exception {
        mvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
    }
}
