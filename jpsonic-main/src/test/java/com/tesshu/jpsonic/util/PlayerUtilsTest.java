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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
}
