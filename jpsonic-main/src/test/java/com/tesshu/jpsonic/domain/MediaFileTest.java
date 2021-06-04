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

package com.tesshu.jpsonic.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link MediaFile}.
 *
 * @author Sindre Mehus
 */
public class MediaFileTest {

    @Test
    public void testGetDurationAsString() {
        assertTrue(doTestGetDurationAsString(0, "0:00"));
        assertTrue(doTestGetDurationAsString(1, "0:01"));
        assertTrue(doTestGetDurationAsString(10, "0:10"));
        assertTrue(doTestGetDurationAsString(33, "0:33"));
        assertTrue(doTestGetDurationAsString(59, "0:59"));
        assertTrue(doTestGetDurationAsString(60, "1:00"));
        assertTrue(doTestGetDurationAsString(61, "1:01"));
        assertTrue(doTestGetDurationAsString(70, "1:10"));
        assertTrue(doTestGetDurationAsString(119, "1:59"));
        assertTrue(doTestGetDurationAsString(120, "2:00"));
        assertTrue(doTestGetDurationAsString(1200, "20:00"));
        assertTrue(doTestGetDurationAsString(1201, "20:01"));
        assertTrue(doTestGetDurationAsString(3599, "59:59"));
        assertTrue(doTestGetDurationAsString(3600, "1:00:00"));
        assertTrue(doTestGetDurationAsString(3601, "1:00:01"));
        assertTrue(doTestGetDurationAsString(3661, "1:01:01"));
        assertTrue(doTestGetDurationAsString(4200, "1:10:00"));
        assertTrue(doTestGetDurationAsString(4201, "1:10:01"));
        assertTrue(doTestGetDurationAsString(4210, "1:10:10"));
        assertTrue(doTestGetDurationAsString(36_000, "10:00:00"));
        assertTrue(doTestGetDurationAsString(360_000, "100:00:00"));
    }

    private boolean doTestGetDurationAsString(int seconds, String expected) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setDurationSeconds(seconds);
        assertEquals(expected, mediaFile.getDurationString(), "Error in getDurationString().");
        return true;
    }
}
