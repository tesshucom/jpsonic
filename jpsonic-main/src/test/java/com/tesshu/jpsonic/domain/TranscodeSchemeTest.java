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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link TranscodeScheme}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class TranscodeSchemeTest {

    /**
     * Tests {@link TranscodeScheme#strictest}.
     */
    @Test
    void testStrictest() {
        assertSame(TranscodeScheme.OFF, TranscodeScheme.OFF.strictest(null));
        assertSame(TranscodeScheme.OFF, TranscodeScheme.OFF.strictest(TranscodeScheme.OFF));
        assertSame(TranscodeScheme.MAX_128, TranscodeScheme.OFF.strictest(TranscodeScheme.MAX_128));
        assertSame(TranscodeScheme.MAX_128, TranscodeScheme.MAX_128.strictest(null));
        assertSame(TranscodeScheme.MAX_128, TranscodeScheme.MAX_128.strictest(TranscodeScheme.OFF));
        assertSame(TranscodeScheme.MAX_128, TranscodeScheme.MAX_128.strictest(TranscodeScheme.MAX_256));
        assertSame(TranscodeScheme.MAX_128, TranscodeScheme.MAX_320.strictest(TranscodeScheme.MAX_128));
        assertSame(TranscodeScheme.MAX_320, TranscodeScheme.MAX_1411.strictest(TranscodeScheme.MAX_320));
    }

    @Test
    void testOf() {
        assertEquals(TranscodeScheme.OFF, TranscodeScheme.of("OFF"));
        assertEquals(TranscodeScheme.OFF, TranscodeScheme.of(""));
        assertEquals(TranscodeScheme.OFF, TranscodeScheme.of(null));
        assertEquals(TranscodeScheme.MAX_128, TranscodeScheme.of("MAX_128"));
        assertEquals(TranscodeScheme.MAX_256, TranscodeScheme.of("MAX_256"));
        assertEquals(TranscodeScheme.MAX_320, TranscodeScheme.of("MAX_320"));
        assertEquals(TranscodeScheme.MAX_1411, TranscodeScheme.of("MAX_1411"));
    }

    @Test
    void testFromMaxBitRate() {
        assertEquals(TranscodeScheme.OFF, TranscodeScheme.fromMaxBitRate(-1));
        assertEquals(TranscodeScheme.OFF, TranscodeScheme.fromMaxBitRate(0));
        assertEquals(TranscodeScheme.MAX_128, TranscodeScheme.fromMaxBitRate(1));
        assertEquals(TranscodeScheme.MAX_128, TranscodeScheme.fromMaxBitRate(127));
        assertEquals(TranscodeScheme.MAX_128, TranscodeScheme.fromMaxBitRate(128));
        assertEquals(TranscodeScheme.MAX_256, TranscodeScheme.fromMaxBitRate(129));
        assertEquals(TranscodeScheme.MAX_256, TranscodeScheme.fromMaxBitRate(256));
        assertEquals(TranscodeScheme.MAX_320, TranscodeScheme.fromMaxBitRate(257));
        assertEquals(TranscodeScheme.MAX_320, TranscodeScheme.fromMaxBitRate(320));
        assertEquals(TranscodeScheme.MAX_1411, TranscodeScheme.fromMaxBitRate(321));
        assertEquals(TranscodeScheme.MAX_1411, TranscodeScheme.fromMaxBitRate(1411));
        assertNull(TranscodeScheme.fromMaxBitRate(1412));
    }
}
