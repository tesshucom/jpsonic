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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MediaTypeDetectorTest {

    @Test
    void testGetMimeType() {
        assertEquals("audio/mpeg", MediaTypeDetector.getMimeType("mp3"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType("koko"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(""));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(null));

        // The only input value is the return value of PathInspector#getExtension.
        assertNotEquals("audio/mpeg", MediaTypeDetector.getMimeType(".MP3"));
        assertNotEquals("audio/mpeg", MediaTypeDetector.getMimeType(".mp3"));
    }

    @Test
    void testGetMimeTypeShouldResolveCorrectly() {
        // Test for MediaTypeDetector to ensure O(1) lookups and G1GC-friendly behavior.
        assertEquals("audio/mpeg", MediaTypeDetector.getMimeType("mp3"));
        assertEquals("audio/flac", MediaTypeDetector.getMimeType("flac"));
        assertEquals("video/mp4", MediaTypeDetector.getMimeType("mp4"));
        assertEquals("video/x-matroska", MediaTypeDetector.getMimeType("mkv"));
        assertEquals("image/jpeg", MediaTypeDetector.getMimeType("jpg"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType("unknown"));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(""));
        assertEquals("application/octet-stream", MediaTypeDetector.getMimeType(null));
    }

    @Test
    void testGetSuffixShouldResolveCanonicalExtension() {
        assertEquals("mp3", MediaTypeDetector.getSuffix("audio/mpeg"));
        assertEquals("aac", MediaTypeDetector.getSuffix("audio/mp4"));
        assertNull(MediaTypeDetector.getSuffix("application/pdf"));
        assertNull(MediaTypeDetector.getSuffix(null));
    }
}
