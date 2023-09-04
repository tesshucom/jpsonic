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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TranscodingTest {

    @Test
    void testEquals() {
        Transcoding t1 = new Transcoding(null, null, null, null, null, null, null, false);
        Transcoding t2 = null;
        assertNotEquals(t1, t2);
        t2 = new Transcoding(null, null, null, null, null, null, null, false);
        assertNotEquals(t1, t2);
        t1 = new Transcoding(0, null, null, null, null, null, null, false);
        assertNotEquals(t1, new Object());
        assertNotEquals(t1, t2);
        t2 = new Transcoding(1, null, null, null, null, null, null, false);
        assertNotEquals(t1, t2);
        t2 = new Transcoding(0, null, null, null, null, null, null, false);
        assertEquals(t1, t2);
        assertEquals(t1, t1);
    }
}
