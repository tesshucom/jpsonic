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

package com.tesshu.jpsonic.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class FontSchemeFilterTest {

    private static final String ADMIN_NAME = "admin";

    @Autowired
    private ServletContext servletContext;

    private FontSchemeFilter fontSchemeFilter;

    @BeforeEach
    public void setup() {
        assertNotNull(servletContext);
        fontSchemeFilter = new FontSchemeFilter();
        MockFilterConfig mockFilterConfig = new MockFilterConfig(servletContext);
        fontSchemeFilter.init(mockFilterConfig);
    }

    @Test
    void testDoFilterWithoutUser() throws ExecutionException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        try {
            fontSchemeFilter.doFilter(req, res, chain);
            assertEquals("", req.getAttribute("viewhint.fontFace").toString());
            assertNotNull(req.getAttribute("viewhint.fontFamily"));
            assertEquals(14, req.getAttribute("viewhint.fontSize"));
        } catch (IOException | ServletException e) {
            throw new ExecutionException(e);
        }
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testDoFilter() throws ExecutionException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        try {
            fontSchemeFilter.doFilter(req, res, chain);
            assertNotNull(req.getAttribute("viewhint.fontFace"));
            assertNotNull(req.getAttribute("viewhint.fontFamily"));
            assertEquals(14, req.getAttribute("viewhint.fontSize"));
        } catch (IOException | ServletException e) {
            throw new ExecutionException(e);
        }
    }
}
