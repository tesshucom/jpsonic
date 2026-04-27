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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileNameSanitizerTest {

    @Test
    void testSanitize() {
        assertEquals("foo", FileNameSanitizer.sanitize("foo"));
        assertEquals("foo.mp3", FileNameSanitizer.sanitize("foo.mp3"));
        assertEquals("foo.mp3", FileNameSanitizer.sanitize("foo.mp3..."));
        assertEquals("foo-bar", FileNameSanitizer.sanitize("foo/bar"));
        assertEquals("foo-bar", FileNameSanitizer.sanitize("foo\\bar"));
        assertEquals("foo-bar", FileNameSanitizer.sanitize("foo:bar"));
    }
}
