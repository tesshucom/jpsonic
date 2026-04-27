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

class MediaTypeDetectorTest {

    @Test
    void testGetMimeType() {
        assertEquals("audio/mpeg", MediaTypeDetector.getMimeType("mp3"));
        assertEquals("audio/mpeg", MediaTypeDetector.getMimeType(".mp3"));
        assertEquals("audio/mpeg", MediaTypeDetector.getMimeType(".MP3"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType("koko"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(""));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(null));
    }
}
