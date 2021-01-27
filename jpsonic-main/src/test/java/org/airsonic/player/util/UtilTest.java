
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
