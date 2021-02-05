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

package org.airsonic.player.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.Map;

import org.airsonic.player.domain.MediaLibraryStatistics;
import org.junit.Test;

public class UtilTest {

    @Test
    public void objectToStringMapNull() {
        MediaLibraryStatistics statistics = null;
        Map<String, String> stringStringMap = PlayerUtils.objectToStringMap(statistics);
        assertNull(stringStringMap);
    }

    @Test
    public void objectToStringMap() {
        Date date = new Date(1568350960725L);
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(date);
        statistics.incrementAlbums(5);
        statistics.incrementSongs(4);
        statistics.incrementArtists(910823);
        statistics.incrementTotalDurationInSeconds(30);
        statistics.incrementTotalLengthInBytes(2930491082L);
        Map<String, String> stringStringMap = PlayerUtils.objectToStringMap(statistics);
        assertEquals("5", stringStringMap.get("albumCount"));
        assertEquals("4", stringStringMap.get("songCount"));
        assertEquals("910823", stringStringMap.get("artistCount"));
        assertEquals("30", stringStringMap.get("totalDurationInSeconds"));
        assertEquals("2930491082", stringStringMap.get("totalLengthInBytes"));
        assertEquals("1568350960725", stringStringMap.get("scanDate"));
    }

    @Test
    public void stringMapToObject() {
        Map<String, String> stringStringMap = LegacyMap.of("albumCount", "5", "songCount", "4", "artistCount", "910823",
                "totalDurationInSeconds", "30", "totalLengthInBytes", "2930491082", "scanDate", "1568350960725");
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class,
                stringStringMap);
        assertEquals(Integer.valueOf(5), statistics.getAlbumCount());
        assertEquals(Integer.valueOf(4), statistics.getSongCount());
        assertEquals(Integer.valueOf(910823), statistics.getArtistCount());
        assertEquals(Long.valueOf(30L), statistics.getTotalDurationInSeconds());
        assertEquals(Long.valueOf(2930491082L), statistics.getTotalLengthInBytes());
        assertEquals(new Date(1568350960725L), statistics.getScanDate());
    }

    @Test
    public void stringMapToObjectWithExtraneousData() {
        Map<String, String> stringStringMap = LegacyMap.of("albumCount", "5", "songCount", "4", "artistCount", "910823",
                "totalDurationInSeconds", "30", "totalLengthInBytes", "2930491082", "scanDate", "1568350960725",
                "extraneousData", "nothingHereToLookAt");
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class,
                stringStringMap);
        assertEquals(Integer.valueOf(5), statistics.getAlbumCount());
        assertEquals(Integer.valueOf(4), statistics.getSongCount());
        assertEquals(Integer.valueOf(910823), statistics.getArtistCount());
        assertEquals(Long.valueOf(30L), statistics.getTotalDurationInSeconds());
        assertEquals(Long.valueOf(2930491082L), statistics.getTotalLengthInBytes());
        assertEquals(new Date(1568350960725L), statistics.getScanDate());
    }

    public void stringMapToObjectWithNoData() {
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class, LegacyMap.of());
        assertNotNull(statistics);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringMapToValidObjectWithNoData() {
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToValidObject(MediaLibraryStatistics.class,
                LegacyMap.of());
    }

}
