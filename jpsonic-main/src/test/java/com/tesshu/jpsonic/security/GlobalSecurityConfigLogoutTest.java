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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class GlobalSecurityConfigLogoutTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verify that /logout is handled by Spring Security filter chain and not by
     * static resource handler.
     */
    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void logoutShouldBeHandledBySecurityFilter() throws Exception {
        mockMvc
            .perform(get("/logout"))
            // should redirect to /login?logout instead of 404
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    /**
     * Verify that /logout is accessible without authentication (permitAll) if
     * configured as such.
     */
    @Test
    void logoutShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc
            .perform(get("/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    /**
     * Optional: Ensure that /logout does not result in NoResourceFoundException.
     */
    @Test
    void logoutShouldNotReturn404() throws Exception {
        mockMvc.perform(get("/logout")).andExpect(result -> {
            int status = result.getResponse().getStatus();
            if (status == HttpStatus.NOT_FOUND.value()) {
                throw new AssertionError(
                        "Expected /logout to be handled by security filter, but got 404");
            }
        });
    }

    /**
     * No static resource logout (#2792)
     */
    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void logoutShouldFailWhenContextPathIsUsed() throws Exception {
        mockMvc
            .perform(get("/jpsonic/logout")) // With context root
            .andExpect(status().isNotFound());
    }
}
