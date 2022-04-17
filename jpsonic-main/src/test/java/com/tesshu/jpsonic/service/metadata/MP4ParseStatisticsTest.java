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

package com.tesshu.jpsonic.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MP4ParseStatisticsTest {

    private MP4ParseStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new MP4ParseStatistics();
    }

    @Test
    void testAddCmdLeadTime() {
        assertEquals(0, statistics.leadTimeCmd.size());
        statistics.addCmdLeadTime(0);
        statistics.addCmdLeadTime(1);
        statistics.addCmdLeadTime(2);
        assertEquals(3, statistics.leadTimeCmd.size());
    }

    @Test
    void testAddTikaLeadTime() {
        assertEquals(0, statistics.leadTimeTika.size());
        statistics.addTikaLeadTime(0, 0);
        statistics.addTikaLeadTime(0, 1);
        statistics.addTikaLeadTime(0, 2);
        statistics.addTikaLeadTime(0, 3);
        assertEquals(4, statistics.leadTimeTika.size());
    }

    @Test
    void testGetCmdLeadTimeEstimate() {
        assertEquals(0, statistics.leadTimeCmd.size());
        assertEquals(MP4ParseStatistics.CMD_LEAD_TIME_DEFAULT, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(3_000);
        assertEquals(1, statistics.leadTimeCmd.size());
        assertEquals(MP4ParseStatistics.CMD_LEAD_TIME_DEFAULT, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(3_000); // median = 3_000
        assertEquals(2, statistics.leadTimeCmd.size());
        assertEquals(3_000, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(4_000); // median = 3_000
        assertEquals(3, statistics.leadTimeCmd.size());
        assertEquals(3_000, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(4_000); // median = 3_500
        assertEquals(4, statistics.leadTimeCmd.size());
        assertEquals(3_000, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(4_000); // median = 4_000
        assertEquals(5, statistics.leadTimeCmd.size());
        assertEquals(3_600, statistics.getCmdLeadTimeEstimate());

        statistics.addCmdLeadTime(4_000); // median = 4_000
        assertEquals(6, statistics.leadTimeCmd.size());
        assertEquals(3_666, statistics.getCmdLeadTimeEstimate());

        /* The time required for the calculation is probably 1ms or less than 1ms */
        statistics = new MP4ParseStatistics();
        Random random = new Random();
        for (int i = 0; i < 60; i++) {
            statistics.addCmdLeadTime(random.nextInt(10) * 100 + 2_000);
        }
        assertEquals(60, statistics.leadTimeCmd.size());

        /* The history is rotated. Only the latest 60 is referenced */
        for (int i = 0; i < 1000; i++) {
            statistics.addCmdLeadTime(random.nextInt(10) * 100 + 2_000);
        }
        statistics.getCmdLeadTimeEstimate();
        assertEquals(60, statistics.leadTimeCmd.size());
    }

    @Test
    void testGetTikaBpmsEstimate() {

        assertEquals(0, statistics.leadTimeTika.size());
        assertEquals(MP4ParseStatistics.TIKA_BPMS_DEFAULT, statistics.getTikaBpmsEstimate());

        statistics.addTikaLeadTime(30_000_000, 2_000);
        statistics.addTikaLeadTime(35_000_000, 3_000);
        statistics.addTikaLeadTime(40_000_000, 4_000);
        statistics.addTikaLeadTime(35_000_000, 3_000);
        statistics.addTikaLeadTime(30_000_000, 2_000);

        assertEquals(5, statistics.leadTimeTika.size());
        // average
        assertEquals(12_666, statistics.getTikaBpmsEstimate());

        statistics = new MP4ParseStatistics();
        for (int i = 0; i < 60; i++) {
            statistics.addTikaLeadTime(30_000_000 + i * 1_000, 2_000);
        }
        assertEquals(60, statistics.leadTimeTika.size());
        // average + siguma
        assertEquals(15_023, statistics.getTikaBpmsEstimate());

        /* The history is rotated. Only the latest 60 is referenced */
        for (int i = 0; i < 1000; i++) {
            statistics.addTikaLeadTime(10_000, 500);
        }
        statistics.getTikaBpmsEstimate();
        assertEquals(60, statistics.leadTimeTika.size());
    }

    @Test
    void testGetThreshold() {

        // default (60Mb). WIP: There is room for consideration.
        assertEquals(60_000_000, statistics.getThreshold());

        statistics.addCmdLeadTime(2_200);
        statistics.addCmdLeadTime(2_200);
        assertEquals(66_000_000, statistics.getThreshold());

        statistics.addTikaLeadTime(750_015_140, 1_800);
        statistics.addTikaLeadTime(750_015_140, 1_800);
        assertEquals(916_685_000, statistics.getThreshold());
    }
}
