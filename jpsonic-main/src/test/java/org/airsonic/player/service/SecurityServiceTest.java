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

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.airsonic.player.TestCaseUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link SecurityService}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class SecurityServiceTest {

    @Autowired
    private SecurityService service;

    @BeforeAll
    public static void beforeAll() throws IOException {
        System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
        TestCaseUtils.cleanJpsonicHomeForTest();
    }

    @Test
    public void testIsFileInFolder() {

        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "/"));

        assertTrue(service.isFileInFolder("/music/foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("\\music\\foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music\\"));

        assertFalse(service.isFileInFolder("", "/tmp"));
        assertFalse(service.isFileInFolder("foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp/music"));

        // Test that references to the parent directory (..) is not allowed.
        assertTrue(service.isFileInFolder("/music/foo..mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo..", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.../", "/music"));
        assertFalse(service.isFileInFolder("/music/foo/..", "/music"));
        assertFalse(service.isFileInFolder("../music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../bar/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\foo\\..", "/music"));
        assertFalse(service.isFileInFolder("..\\music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/..\\bar/../foo", "/music"));
    }
}
