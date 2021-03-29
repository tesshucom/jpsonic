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

package org.airsonic.player.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class LegacyMapTest {

    @Test
    public void testOf() {
        Map<String, Integer> map = LegacyMap.of();
        assertEquals(0, map.size());
        map.put("String1", 1);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("String1"));
        map.put("String2", null);
        assertEquals(2, map.size());
        assertNull(map.get("String2"));

        map = LegacyMap.of("String1", 1, "String2", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("String1"));
        assertEquals(Integer.valueOf(2), map.get("String2"));
        map.put("String3", 3);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(3), map.get("String3"));
        map.put("String4", null);
        assertEquals(4, map.size());
        assertNull(map.get("String4"));

    }

}
