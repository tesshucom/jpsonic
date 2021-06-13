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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class StreamControllerTest {

    @Autowired
    private StreamController controller;

    @Test
    void testGetRequestedVideoSize() {

        // Valid spec.
        assertEquals(new Dimension(123, 456), controller.getRequestedVideoSize("123x456"), "Wrong size.");
        assertEquals(new Dimension(456, 123), controller.getRequestedVideoSize("456x123"), "Wrong size.");
        assertEquals(new Dimension(1, 1), controller.getRequestedVideoSize("1x1"), "Wrong size.");

        // Missing spec.
        assertNull(controller.getRequestedVideoSize(null), "Wrong size.");

        // Invalid spec.
        assertNull(controller.getRequestedVideoSize("123"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("123x"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("x123"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("x"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("foo123x456bar"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("foo123x456"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("123x456bar"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("fooxbar"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("-1x1"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("1x-1"), "Wrong size.");

        // Too large.
        assertNull(controller.getRequestedVideoSize("3000x100"), "Wrong size.");
        assertNull(controller.getRequestedVideoSize("100x3000"), "Wrong size.");
    }

    @Test
    void testGetSuitableVideoSize() {

        // 4:3 aspect rate
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 200, 400, 300));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 300, 400, 300));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 400, 480, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 500, 480, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 600, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 700, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 800, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 900, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 1000, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 1100, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 1200, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 1500, 640, 480));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 1800, 960, 720));
        assertTrue(doTestGetSuitableVideoSize(1280, 960, 2000, 960, 720));

        // 16:9 aspect rate
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 200, 400, 226));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 300, 400, 226));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 400, 480, 270));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 500, 480, 270));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 600, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 700, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 800, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 900, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 1000, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 1100, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 1200, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 1500, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 1800, 960, 540));
        assertTrue(doTestGetSuitableVideoSize(1280, 720, 2000, 960, 540));

        // Small original size.
        assertTrue(doTestGetSuitableVideoSize(100, 100, 1000, 100, 100));
        assertTrue(doTestGetSuitableVideoSize(100, 1000, 1000, 100, 1000));
        assertTrue(doTestGetSuitableVideoSize(1000, 100, 100, 1000, 100));

        // Unknown original size.
        assertTrue(doTestGetSuitableVideoSize(720, null, 200, 400, 226));
        assertTrue(doTestGetSuitableVideoSize(null, 540, 300, 400, 226));
        assertTrue(doTestGetSuitableVideoSize(null, null, 400, 480, 270));
        assertTrue(doTestGetSuitableVideoSize(720, null, 500, 480, 270));
        assertTrue(doTestGetSuitableVideoSize(null, 540, 600, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(null, null, 700, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(720, null, 1200, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(null, 540, 1500, 640, 360));
        assertTrue(doTestGetSuitableVideoSize(null, null, 2000, 960, 540));

        // Odd original size.
        assertTrue(doTestGetSuitableVideoSize(203, 101, 1500, 204, 102));
        assertTrue(doTestGetSuitableVideoSize(464, 853, 1500, 464, 854));
    }

    private boolean doTestGetSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate,
            int expectedWidth, int expectedHeight) {
        Dimension dimension = controller.getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        assertEquals(expectedWidth, dimension.width, "Wrong width.");
        assertEquals(expectedHeight, dimension.height, "Wrong height.");
        return true;
    }
}
