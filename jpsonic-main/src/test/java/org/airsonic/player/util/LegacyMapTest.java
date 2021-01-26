
package org.airsonic.player.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

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

        map = LegacyMap.of("String1", Integer.valueOf(1), "String2", Integer.valueOf(2));
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
