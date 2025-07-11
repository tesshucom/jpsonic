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

package com.tesshu.jpsonic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Dimension;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Sindre Mehus
 */
class HLSControllerTest {

    private final HLSController controller = new HLSController(null, null, null, null);

    @Test
    void testParseBitRateSuccess() {
        Pair<Integer, Dimension> pair = controller.parseBitRate("1000");
        assertEquals(1000, pair.getLeft());
        assertNull(pair.getRight());
        pair = controller.parseBitRate("1000@400x300");
        assertEquals(1000, pair.getLeft());
        assertEquals(400, pair.getRight().width);
        assertEquals(300, pair.getRight().height);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testParseBitRateParseError1() {
        Assertions
            .assertThrows(IllegalArgumentException.class, () -> controller.parseBitRate("asdfl"));
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testParseBitRateParseError2() {
        Assertions
            .assertThrows(IllegalArgumentException.class,
                    () -> controller.parseBitRate("1000@300"));
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testParseBitRateParseError3() {
        Assertions
            .assertThrows(IllegalArgumentException.class,
                    () -> controller.parseBitRate("1000@300x400ZZ"));
    }
}
