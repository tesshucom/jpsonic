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

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URISyntaxException;
import java.nio.file.Path;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.controller.InternalHelpController.FileStatistics;
import org.junit.jupiter.api.Nested;
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
@SuppressWarnings({ "PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyStaticImports" }) // pmd/pmd/issues/1084
class InternalHelpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
    void testOkForAdmins() throws Exception {
        mockMvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void testNotOkForUsers() throws Exception {
        mockMvc.perform(get("/internalhelp").contentType(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
    }

    @Nested
    class DoesLocaleSupportUtf8Test {

        private final InternalHelpController controller = new InternalHelpController(null, null, null, null, null, null,
                null, null);

        @Test
        void testNull() {
            assertFalse(controller.doesLocaleSupportUtf8(null));
        }

        @Test
        void testContainsUtf8() {
            assertTrue(controller.doesLocaleSupportUtf8("utf8"));
            assertTrue(controller.doesLocaleSupportUtf8("utf-8"));
        }

        @Test
        void testNotContainsUtf8() {
            assertFalse(controller.doesLocaleSupportUtf8("MS932"));
        }
    }

    @Nested
    class FileStatisticsTest {
        @Test
        void testSetFromPath() throws URISyntaxException {
            Path path = Path.of(InternalHelpControllerTest.class.getResource("/MEDIAS/Music").toURI());
            FileStatistics fileStatistics = new FileStatistics();
            fileStatistics.setFromPath(path);
            assertEquals("Music", fileStatistics.getName());
            assertNotNull(fileStatistics.getFreeFilesystemSizeBytes());
            assertTrue(fileStatistics.isReadable());
            assertTrue(fileStatistics.isWritable());
            assertTrue(fileStatistics.isExecutable());
            assertNotNull(fileStatistics.getTotalFilesystemSizeBytes());
            assertEquals(path.toString(), fileStatistics.getPath());
        }
    }
}
