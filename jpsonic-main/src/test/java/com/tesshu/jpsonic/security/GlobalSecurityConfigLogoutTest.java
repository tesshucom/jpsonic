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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(NeedsHome.class)
@SuppressWarnings({ "PMD.UnitTestShouldIncludeAssert", "PMD.TooManyStaticImports",
        "PMD.SignatureDeclareThrowsException" })
class GlobalSecurityConfigLogoutTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * POST /logout should be handled by Spring Security filter chain and redirect
     * to /login?logout
     */
    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void testPostLogoutShouldRedirectToLogin() throws Exception {
        mockMvc
            .perform(post("/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    /**
     * POST /logout without authentication should still redirect to /login?logout
     * (permitAll)
     */
    @Test
    void testPostLogoutWithoutAuthenticationShouldRedirectToLogin() throws Exception {
        mockMvc
            .perform(post("/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    /**
     * Legacy GET /logout is deprecated and expected to fail or be blocked. This
     * test ensures that GET requests do not silently succeed.
     */
    @Test
    @WithMockUser
    void testGetLogoutLegacyShouldReturnMethodNotAllowedOrFail() throws Exception {
        mockMvc.perform(get("/logout")).andExpect(result -> {
            int status = result.getResponse().getStatus();
            assertThat(status, in(Arrays.asList(404, 405)));
        });
    }

    /**
     * No static resource logout (#2792) GET /{contextPath}/logout should fail with
     * 404
     * 
     * @throws Exception
     */
    @WithMockUser(username = "user", roles = { "USER" })
    void testLogoutShouldFailWhenContextPathIsUsed() throws Exception {
        mockMvc.perform(post("/jpsonic/logout").with(csrf())).andExpect(status().isNotFound());
    }
}
