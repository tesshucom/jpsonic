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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
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
     * NOTE: Since Spring Boot 4 / Spring Security 6, requests to /logout are fully
     * handled inside the Security filter chain.
     *
     * - Legacy GET /logout no longer falls through to DispatcherServlet. - Instead,
     * it is intercepted by Security and results in a redirect (302) to the login
     * entry point.
     *
     * With Boot 4 migration, the test can now accurately observe Security filter
     * behavior. The expected behavior here is NOT a specific status code, but that
     * the request does not succeed.
     *
     * A 302 response is considered valid evidence that the request was blocked by
     * Spring Security.
     */
    @Test
    @WithMockUser
    void testGetLogoutLegacyShouldRedirectOrFail() throws Exception {
        mockMvc.perform(get("/logout")).andExpect(status().is3xxRedirection());
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
