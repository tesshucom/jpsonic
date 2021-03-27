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

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit test of {@link StatusService}.
 *
 * @author Sindre Mehus
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class StatusServiceTest {

    @Autowired
    private StatusService statusService;
    private Player player1;

    @BeforeAll
    public static void beforeAll() throws IOException {
        System.setProperty("jpsonic.home", TestCaseUtils.jpsonicHomePathForTest());
        TestCaseUtils.cleanJpsonicHomeForTest();
    }

    @BeforeEach
    public void setUp() throws Exception {
        player1 = new Player();
        player1.setId(1);
    }

    @Test
    public void testSimpleAddRemove() {
        TransferStatus status = statusService.createStreamStatus(player1);
        assertTrue(status.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(status), statusService.getAllStreamStatuses(), "Wrong list of statuses.");
        assertEquals(Arrays.asList(status), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        statusService.removeStreamStatus(status);
        assertFalse(status.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(status), statusService.getAllStreamStatuses(), "Wrong list of statuses.");
        assertEquals(Arrays.asList(status), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");
    }

    @Test
    public void testMultipleStreamsSamePlayer() {
        TransferStatus statusA = statusService.createStreamStatus(player1);
        TransferStatus statusB = statusService.createStreamStatus(player1);

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getAllStreamStatuses()); // In the bad case? Right?
        assertNotEquals(Arrays.asList(statusA, statusB), statusService.getAllStreamStatuses(),
                "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusA), statusService.getAllStreamStatuses());
        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses());

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getStreamStatusesForPlayer(player1)); // In the bad case? Right?
        assertEquals(Arrays.asList(statusA), statusService.getStreamStatusesForPlayer(player1));
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1));

        // Stop stream A.
        statusService.removeStreamStatus(statusA);
        assertFalse(statusA.isActive(), "Wrong status.");

        // assertTrue("Wrong status.", statusB.isActive()); // In the bad case? Right?
        assertFalse(statusB.isActive());

        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses(), "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        // Stop stream B.
        statusService.removeStreamStatus(statusB);
        assertFalse(statusB.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses(), "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");

        // Start stream C.
        TransferStatus statusC = statusService.createStreamStatus(player1);
        assertTrue(statusC.isActive(), "Wrong status.");
        assertEquals(Arrays.asList(statusC), statusService.getAllStreamStatuses(), "Wrong list of statuses.");
        assertEquals(Arrays.asList(statusC), statusService.getStreamStatusesForPlayer(player1),
                "Wrong list of statuses.");
    }
}
