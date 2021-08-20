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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(NeedsHome.class)
class AvatarControllerTest {

    @Autowired
    private AvatarController controller;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testGetLastModified() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USER_NAME.value(), "admin");
        assertNotEquals(0L, controller.getLastModified(req));
    }

    @Test
    void testNotFoundWithAvatarSchemeNone() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.AVATAR.value())
                        .param(Attributes.Request.USER_NAME.value(), "admin")
                        .param(Attributes.Request.FORCE_CUSTOM.value(), Boolean.toString(false)))
                .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
        assertNotNull(result);
    }
}
