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

package com.tesshu.jpsonic.persistence.api.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class MusicIndexTest {

    @Test
    void testEquals() {
        MusicIndex m1 = new MusicIndex("0");
        MusicIndex m2 = null;
        assertNotEquals(m1, m2);
        assertNotEquals(m1, new Object());
        m2 = new MusicIndex("1");
        assertNotEquals(m1, m2);
        m2 = new MusicIndex("0");
        assertEquals(m1, m2);
        assertEquals(m1, m2);
    }
}
