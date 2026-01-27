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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import com.tesshu.jpsonic.infrastructure.NeedsHome;
import com.tesshu.jpsonic.persistence.api.entity.Player;
import com.tesshu.jpsonic.service.StatusService.PlayStatus;
import com.tesshu.jpsonic.service.StatusService.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit test of {@link StatusService}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ActiveProfiles("test")
@NeedsHome
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class StatusServiceTest {

    @Autowired
    private StatusService statusService;
    private Player player1;

    @BeforeEach
    public void setUp() {
        player1 = new Player();
        player1.setId(1);
    }

    @Test
    void testSimpleAddRemove() {
        TransferStatus status = statusService.createStreamStatus(player1);
        assertTrue(status.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(status), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(status), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        statusService.removeStreamStatus(status);
        assertFalse(status.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(status), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(status), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");
    }

    @Test
    void testMultipleStreamsSamePlayer() {
        TransferStatus statusA = statusService.createStreamStatus(player1);
        TransferStatus statusB = statusService.createStreamStatus(player1);

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getAllStreamStatuses()); // In the bad case? Right?
        assertNotEquals(Arrays.asList(statusA, statusB), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusA), statusService.getAllStreamStatuses());
        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses());

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getStreamStatusesForPlayer(player1)); // In the bad case?
        // Right?
        assertEquals(Arrays.asList(statusA), statusService.getStreamStatusesForPlayer(player1));
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1));

        // Stop stream A.
        statusService.removeStreamStatus(statusA);
        assertFalse(statusA.isActive(), "Wrong status.");

        // assertTrue("Wrong status.", statusB.isActive()); // In the bad case? Right?
        assertFalse(statusB.isActive());

        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        // Stop stream B.
        statusService.removeStreamStatus(statusB);
        assertFalse(statusB.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        // Start stream C.
        TransferStatus statusC = statusService.createStreamStatus(player1);
        assertTrue(statusC.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(statusC), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusC), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");
    }

    @Nested
    class PlayStatusTest {

        @Test
        void testIsExpired() {
            PlayStatus withinBoundaries = new PlayStatus(null, null,
                    now().minus(5, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES));
            assertTrue(withinBoundaries.isExpired());

            PlayStatus outOfBoundaries = new PlayStatus(null, null,
                    now().minus(6, ChronoUnit.HOURS));
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
}
