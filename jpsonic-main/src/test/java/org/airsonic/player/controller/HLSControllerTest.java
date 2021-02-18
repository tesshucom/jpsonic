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

package org.airsonic.player.controller;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;

import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Sindre Mehus
 */
@SpringBootTest
public class HLSControllerTest extends AbstractAirsonicHomeTest {

    @Autowired
    private HLSController controller;

    @Test
    public void testParseBitRateSuccess() {
        Pair<Integer, Dimension> pair = controller.parseBitRate("1000");
        assertEquals(1000, pair.getLeft().intValue());
        assertNull(pair.getRight());
        pair = controller.parseBitRate("1000@400x300");
        assertEquals(1000, pair.getLeft().intValue());
        assertEquals(400, pair.getRight().width);
        assertEquals(300, pair.getRight().height);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBitRateParseError1() {
        controller.parseBitRate("asdfl");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBitRateParseError2() {
        controller.parseBitRate("1000@300");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBitRateParseError3() {
        controller.parseBitRate("1000@300x400ZZ");
    }
}
