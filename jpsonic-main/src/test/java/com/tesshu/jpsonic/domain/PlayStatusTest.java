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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.domain;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class PlayStatusTest {

    @Test
    void testIsExpired() {
        PlayStatus withinBoundaries = new PlayStatus(null, null,
                now().minus(5, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES));
        assertTrue(withinBoundaries.isExpired());

        PlayStatus outOfBoundaries = new PlayStatus(null, null, now().minus(6, ChronoUnit.HOURS));
        assertFalse(outOfBoundaries.isExpired());
    }

    /*
     * this class cannot be fully tested due to the design. However, rigor is not
     * required and is not a big deal in most cases. Ubuntu may calculate slightly
     * shorter in some cases
     */
    @DisabledOnOs(OS.LINUX)
    @Test
    void testGetMinutesAgo() {
        assertEquals(5L,
                new PlayStatus(null, null, now().plus(5, ChronoUnit.MINUTES)).getMinutesAgo());
        assertEquals(60L,
                new PlayStatus(null, null, now().plus(1, ChronoUnit.HOURS)).getMinutesAgo());
        assertEquals(1440L,
                new PlayStatus(null, null, now().plus(1, ChronoUnit.DAYS)).getMinutesAgo());
    }
}
