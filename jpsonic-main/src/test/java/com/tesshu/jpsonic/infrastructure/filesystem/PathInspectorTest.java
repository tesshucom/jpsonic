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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.filesystem;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PathInspectorTest {

    private final PathInspector pathInspector = new PathInspector();

    @Test
    void testIsWithinHierarchy() {

        /*
         * The parameters of this function are not completely free strings. Any
         * file/folder strings will be filtered to
         * RootPathEntryGuard#validateFolderPath. (Sub-paths with string patterns that
         * cannot be registered in MusicFolder will not be generated during scanning.)
         */
        RootPathEntryGuard.validateFolderPath("\\").ifPresent(folder -> Assertions.fail());
        RootPathEntryGuard.validateFolderPath("/").ifPresent(folder -> Assertions.fail());
        RootPathEntryGuard.validateFolderPath("\\music").ifPresent(folder -> Assertions.fail());
        RootPathEntryGuard.validateFolderPath("\\music\\").ifPresent(folder -> Assertions.fail());

        assertTrue(pathInspector.isWithinHierarchy("/music/foo.mp3", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("", "/tmp"));
        assertFalse(pathInspector.isWithinHierarchy("foo.mp3", "/tmp"));
        assertFalse(pathInspector.isWithinHierarchy("/music/foo.mp3", "/tmp"));
        assertFalse(pathInspector.isWithinHierarchy("/music/foo.mp3", "/tmp/music"));

        // Test that references to the parent directory (..) is not allowed.
        assertTrue(pathInspector.isWithinHierarchy("/music/foo..mp3", "/music"));
        assertTrue(pathInspector.isWithinHierarchy("/music/foo..", "/music"));
        assertTrue(pathInspector.isWithinHierarchy("/music/foo.../", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/foo/..", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("../music/foo", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/../foo", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/../bar/../foo", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music\\foo\\..", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("..\\music/foo", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music\\../foo", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/..\\bar/../foo", "/music"));

        // Number of characters and number of layers
        assertFalse(pathInspector.isWithinHierarchy("/musik/foo.mp3", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/sub/sub.mp3", "/music/sub/sub/sub"));
        assertFalse(pathInspector.isWithinHierarchy("/".concat("A".repeat(1001)), "/music"));
    }

    /*
     * #852. https://wiki.sei.cmu.edu/confluence/display/java/STR02-J.+Specify+an+
     * appropriate+locale+when+ comparing+locale-dependent+data
     */
    @Test
    void testIsWithinHierarchySTR02J() {
        assertTrue(pathInspector.isWithinHierarchy("/music/foo.mp3", "/Music"));
        assertTrue(pathInspector
            .isWithinHierarchy("/\u0130\u0049/foo.mp3", // İI
                    "/\u0069\u0131")); // iı
    }

    /*
     * In the previous implementation, the pattern that misjudgment by startwith
     * occurs.
     */
    @EnabledOnOs(OS.LINUX)
    @Test
    void testEmbeddedPathString4Linux() {
        assertTrue(pathInspector.isWithinHierarchy("/music/foo.mp3", "/music"));
        assertFalse(pathInspector.isWithinHierarchy("/music/foo.mp3", "/music2"));
        assertFalse(pathInspector.isWithinHierarchy("/music2/foo.mp3", "/music"));
        assertTrue(pathInspector.isWithinHierarchy("/music2/foo.mp3", "/music2"));
    }

    /*
     * In the previous implementation, the pattern that misjudgment by startwith
     * occurs.
     */
    @EnabledOnOs(OS.WINDOWS)
    @Test
    void testEmbeddedPathString4Win() {
        assertTrue(pathInspector.isWithinHierarchy("C:\\music\\foo.mp3", "C:\\music"));
        assertFalse(pathInspector.isWithinHierarchy("C:\\music\\foo.mp3", "C:\\music2"));
        assertFalse(pathInspector.isWithinHierarchy("C:\\music2\\foo.mp3", "C:\\music"));
        assertTrue(pathInspector.isWithinHierarchy("C:\\music2\\foo.mp3", "C:\\music2"));
    }

    @Test
    void testToIdentityName() {

        assertNull(PathInspector.toIdentityName(null));

        assertEquals("", PathInspector.toIdentityName(Path.of("/")));

        assertEquals(File.separator + "child.mp3",
                PathInspector.toIdentityName(Path.of("/child.mp3")));

        assertEquals("MusicFolder" + File.separator + "artist",
                PathInspector.toIdentityName(Path.of("/MusicFolder/artist")));

        assertEquals("artist" + File.separator + "child.mp3",
                PathInspector.toIdentityName(Path.of("/MusicFolder/artist/child.mp3")));
    }
}
