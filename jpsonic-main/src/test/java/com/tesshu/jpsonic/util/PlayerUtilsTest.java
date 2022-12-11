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

package com.tesshu.jpsonic.util;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;

import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class PlayerUtilsTest {

    @Nested
    class GetDefaultMusicFolderTest {

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void testDefaultOnWin() {
            assertEquals("c:\\music", PlayerUtils.getDefaultMusicFolder());

            System.setProperty("jpsonic.defaultMusicFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultMusicFolder());
            System.setProperty("jpsonic.defaultMusicFolder", "/foo/../bar");
            assertEquals("c:\\music", PlayerUtils.getDefaultMusicFolder());
            System.clearProperty("jpsonic.defaultMusicFolder");
        }

        @Test
        @EnabledOnOs(OS.LINUX)
        void testDefaultOnLinux() {
            assertEquals("/var/music", PlayerUtils.getDefaultMusicFolder());

            System.setProperty("jpsonic.defaultMusicFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultMusicFolder());
            System.setProperty("jpsonic.defaultMusicFolder", "/foo/../bar");
            assertEquals("/var/music", PlayerUtils.getDefaultMusicFolder());
            System.clearProperty("jpsonic.defaultMusicFolder");
        }
    }

    @Nested
    class GetDefaultPodcastFolderTest {

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void testDefaultOnWin() {
            assertEquals("c:\\music\\Podcast", PlayerUtils.getDefaultPodcastFolder());

            System.setProperty("jpsonic.defaultPodcastFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultPodcastFolder());
            System.setProperty("jpsonic.defaultPodcastFolder", "/foo/../bar");
            assertEquals("c:\\music\\Podcast", PlayerUtils.getDefaultPodcastFolder());
            System.clearProperty("jpsonic.defaultPodcastFolder");
        }

        @Test
        @EnabledOnOs(OS.LINUX)
        void testDefaultOnLinux() {
            assertEquals("/var/music/Podcast", PlayerUtils.getDefaultPodcastFolder());

            System.setProperty("jpsonic.defaultPodcastFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultPodcastFolder());
            System.setProperty("jpsonic.defaultPodcastFolder", "/foo/../bar");
            assertEquals("/var/music/Podcast", PlayerUtils.getDefaultPodcastFolder());
            System.clearProperty("jpsonic.defaultPodcastFolder");
        }
    }

    @Nested
    class GetDefaultPlaylistFolderTest {

        @Test
        @EnabledOnOs(OS.WINDOWS)
        void testDefaultOnWin() {
            assertEquals("c:\\playlists", PlayerUtils.getDefaultPlaylistFolder());

            System.setProperty("jpsonic.defaultPlaylistFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultPlaylistFolder());
            System.setProperty("jpsonic.defaultPlaylistFolder", "/foo/../bar");
            assertEquals("c:\\playlists", PlayerUtils.getDefaultPlaylistFolder());
            System.clearProperty("jpsonic.defaultPlaylistFolder");
        }

        @Test
        @EnabledOnOs(OS.LINUX)
        void testDefaultOnLinux() {
            assertEquals("/var/playlists", PlayerUtils.getDefaultPlaylistFolder());

            System.setProperty("jpsonic.defaultPlaylistFolder", "/foo/bar");
            assertEquals("/foo/bar", PlayerUtils.getDefaultPlaylistFolder());
            System.setProperty("jpsonic.defaultPlaylistFolder", "/foo/../bar");
            assertEquals("/var/playlists", PlayerUtils.getDefaultPlaylistFolder());
            System.clearProperty("jpsonic.defaultPlaylistFolder");
        }
    }

    @Test
    void testObjectToStringMapNull() {
        MediaLibraryStatistics statistics = null;
        Map<String, String> stringStringMap = PlayerUtils.objectToStringMap(statistics);
        assertNull(stringStringMap);
    }

    @Test
    void testObjectToStringMap() {
        Instant date = now();
        MediaLibraryStatistics statistics = new MediaLibraryStatistics(date);
        statistics.setAlbumCount(5);
        statistics.setSongCount(4);
        statistics.setArtistCount(910_823);
        statistics.setTotalDurationInSeconds(30L);
        statistics.setTotalLengthInBytes(2_930_491_082L);
        Map<String, String> stringStringMap = PlayerUtils.objectToStringMap(statistics);
        assertEquals("5", stringStringMap.get("albumCount"));
        assertEquals("4", stringStringMap.get("songCount"));
        assertEquals("910823", stringStringMap.get("artistCount"));
        assertEquals("30", stringStringMap.get("totalDurationInSeconds"));
        assertEquals("2930491082", stringStringMap.get("totalLengthInBytes"));
        assertEquals(Long.toString(date.getEpochSecond()) + "." + String.format("%09d", date.getNano()),
                stringStringMap.get("scanDate"));

        MediaLibraryStatistics restored = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class, stringStringMap);
        assertEquals(statistics.getAlbumCount(), restored.getAlbumCount());
        assertEquals(statistics.getArtistCount(), restored.getArtistCount());
        assertEquals(statistics.getScanDate(), restored.getScanDate());
        assertEquals(statistics.getSongCount(), restored.getSongCount());
        assertEquals(statistics.getTotalDurationInSeconds(), restored.getTotalDurationInSeconds());
        assertEquals(statistics.getTotalLengthInBytes(), restored.getTotalLengthInBytes());
    }

    @Test
    void testStringMapToObject() {
        Instant scanDate = now();
        Map<String, String> stringStringMap = LegacyMap.of("albumCount", "5", "songCount", "4", "artistCount", "910823",
                "totalDurationInSeconds", "30", "totalLengthInBytes", "2930491082", "scanDate", scanDate.toString());
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class,
                stringStringMap);
        assertEquals(Integer.valueOf(5), statistics.getAlbumCount());
        assertEquals(Integer.valueOf(4), statistics.getSongCount());
        assertEquals(Integer.valueOf(910_823), statistics.getArtistCount());
        assertEquals(Long.valueOf(30L), statistics.getTotalDurationInSeconds());
        assertEquals(Long.valueOf(2_930_491_082L), statistics.getTotalLengthInBytes());
        assertEquals(scanDate, statistics.getScanDate());
    }

    @Test
    void testStringMapToObjectWithExtraneousData() {
        Instant scanDate = now();
        Map<String, String> stringStringMap = LegacyMap.of("albumCount", "5", "songCount", "4", "artistCount", "910823",
                "totalDurationInSeconds", "30", "totalLengthInBytes", "2930491082", "scanDate", scanDate.toString(),
                "extraneousData", "nothingHereToLookAt");
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class,
                stringStringMap);
        assertEquals(Integer.valueOf(5), statistics.getAlbumCount());
        assertEquals(Integer.valueOf(4), statistics.getSongCount());
        assertEquals(Integer.valueOf(910_823), statistics.getArtistCount());
        assertEquals(Long.valueOf(30L), statistics.getTotalDurationInSeconds());
        assertEquals(Long.valueOf(2_930_491_082L), statistics.getTotalLengthInBytes());
        assertEquals(scanDate, statistics.getScanDate());
    }

    @Test
    void testStringMapToObjectWithNoData() {
        MediaLibraryStatistics statistics = PlayerUtils.stringMapToObject(MediaLibraryStatistics.class, LegacyMap.of());
        assertNotNull(statistics);
    }

    @Test
    void testStringMapToValidObjectWithNoData() {
        assertThrows(IllegalArgumentException.class,
                () -> PlayerUtils.stringMapToValidObject(MediaLibraryStatistics.class, LegacyMap.of()));
    }
}
