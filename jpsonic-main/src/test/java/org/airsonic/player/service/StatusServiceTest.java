/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit test of {@link StatusService}.
 *
 * @author Sindre Mehus
 */
public class StatusServiceTest extends AbstractAirsonicHomeTest {

    @Autowired
    private StatusService statusService;
    private Player player1;

    @Before
    public void setUp() throws Exception {
        player1 = new Player();
        player1.setId(1);
    }

    @Test
    public void testSimpleAddRemove() {
        TransferStatus status = statusService.createStreamStatus(player1);
        assertTrue("Wrong status.", status.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), statusService.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(status),
                statusService.getStreamStatusesForPlayer(player1));

        statusService.removeStreamStatus(status);
        assertFalse("Wrong status.", status.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(status), statusService.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(status),
                statusService.getStreamStatusesForPlayer(player1));
    }

    @Test
    public void testMultipleStreamsSamePlayer() {
        TransferStatus statusA = statusService.createStreamStatus(player1);
        TransferStatus statusB = statusService.createStreamStatus(player1);

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getAllStreamStatuses()); // In the bad case? Right?
        assertNotEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
                statusService.getAllStreamStatuses());
        assertEquals(Arrays.asList(statusA), statusService.getAllStreamStatuses());
        assertEquals(Arrays.asList(statusB), statusService.getAllStreamStatuses());

        // assertEquals("Wrong list of statuses.", Arrays.asList(statusA, statusB),
        // statusService.getStreamStatusesForPlayer(player1)); // In the bad case? Right?
        assertEquals(Arrays.asList(statusA), statusService.getStreamStatusesForPlayer(player1));
        assertEquals(Arrays.asList(statusB), statusService.getStreamStatusesForPlayer(player1));

        // Stop stream A.
        statusService.removeStreamStatus(statusA);
        assertFalse("Wrong status.", statusA.isActive());

        // assertTrue("Wrong status.", statusB.isActive()); // In the bad case? Right?
        assertFalse(statusB.isActive());

        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), statusService.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB),
                statusService.getStreamStatusesForPlayer(player1));

        // Stop stream B.
        statusService.removeStreamStatus(statusB);
        assertFalse("Wrong status.", statusB.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB), statusService.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusB),
                statusService.getStreamStatusesForPlayer(player1));

        // Start stream C.
        TransferStatus statusC = statusService.createStreamStatus(player1);
        assertTrue("Wrong status.", statusC.isActive());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusC), statusService.getAllStreamStatuses());
        assertEquals("Wrong list of statuses.", Arrays.asList(statusC),
                statusService.getStreamStatusesForPlayer(player1));
    }
}
