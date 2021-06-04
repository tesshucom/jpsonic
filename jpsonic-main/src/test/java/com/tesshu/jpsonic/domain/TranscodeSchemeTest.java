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

import static com.tesshu.jpsonic.domain.TranscodeScheme.MAX_32;
import static com.tesshu.jpsonic.domain.TranscodeScheme.MAX_64;
import static com.tesshu.jpsonic.domain.TranscodeScheme.OFF;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link TranscodeScheme}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class TranscodeSchemeTest {

    /**
     * Tests {@link TranscodeScheme#strictest}.
     */
    @Test
    public void testStrictest() {
        assertSame(OFF, OFF.strictest(null), "Error in strictest().");
        assertSame(OFF, OFF.strictest(OFF), "Error in strictest().");
        assertSame(MAX_32, OFF.strictest(MAX_32), "Error in strictest().");
        assertSame(MAX_32, MAX_32.strictest(null), "Error in strictest().");
        assertSame(MAX_32, MAX_32.strictest(OFF), "Error in strictest().");
        assertSame(MAX_32, MAX_32.strictest(MAX_64), "Error in strictest().");
        assertSame(MAX_32, MAX_64.strictest(MAX_32), "Error in strictest().");
    }
}
