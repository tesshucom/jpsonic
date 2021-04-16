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

package org.airsonic.player.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * Unit test of {@link PlayQueue}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = { "org.airsonic.player", "com.tesshu.jpsonic" })
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class PlayQueueTest {

    @Autowired
    private JpsonicComparators jpsonicComparators;

    @Test
    public void testEmpty() {
        PlayQueue playQueue = new PlayQueue();
        assertEquals(0, playQueue.size());
        assertTrue(playQueue.isEmpty());
        assertEquals(0, playQueue.getFiles().size());
        assertNull(playQueue.getCurrentFile());
    }

    @Test
    public void testStatus() {
        PlayQueue playQueue = new PlayQueue();
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());

        playQueue.setStatus(PlayQueue.Status.STOPPED);
        assertEquals(PlayQueue.Status.STOPPED, playQueue.getStatus());

        playQueue.addFiles(true, new TestMediaFile());
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());

        playQueue.clear();
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());
    }

    @Test
    public void testMoveUp() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveUp(0);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveUp(9999);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.moveUp(1);
        assertPlaylistEquals(playQueue, 0, "B", "A", "C", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.moveUp(3);
        assertPlaylistEquals(playQueue, 2, "A", "B", "D", "C");
    }

    @Test
    public void testMoveDown() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveDown(0);
        assertPlaylistEquals(playQueue, 1, "B", "A", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveDown(9999);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.moveDown(1);
        assertPlaylistEquals(playQueue, 2, "A", "C", "B", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.moveDown(3);
        assertPlaylistEquals(playQueue, 3, "A", "B", "C", "D");
    }

    @Test
    public void testRemove() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, 0, "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, 0, "B", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.removeFileAt(3);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.removeFileAt(1);
        assertPlaylistEquals(playQueue, 1, "A", "C", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.removeFileAt(3);
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");

        playQueue = createPlaylist(0, "A");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, -1);
    }

    @Test
    public void testNext() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C");
        assertFalse(playQueue.isRepeatEnabled());
        playQueue.next();
        assertPlaylistEquals(playQueue, 1, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, -1, "A", "B", "C");

        playQueue = createPlaylist(0, "A", "B", "C");
        playQueue.setRepeatEnabled(true);
        assertTrue(playQueue.isRepeatEnabled());
        playQueue.next();
        assertPlaylistEquals(playQueue, 1, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
    }

    @Test
    public void testPlayAfterEndReached() {
        PlayQueue playQueue = createPlaylist(2, "A", "B", "C");
        playQueue.setStatus(PlayQueue.Status.PLAYING);
        playQueue.next();
        assertNull(playQueue.getCurrentFile());
        assertEquals(PlayQueue.Status.STOPPED, playQueue.getStatus());

        playQueue.setStatus(PlayQueue.Status.PLAYING);
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());
        assertEquals(0, playQueue.getIndex());
        assertEquals("A", playQueue.getCurrentFile().getName());
    }

    @Test
    public void testPlayLast() {
        PlayQueue playQueue = createPlaylist(1, "A", "B", "C");

        playQueue.addFiles(true, new TestMediaFile("D"));
        assertPlaylistEquals(playQueue, 1, "A", "B", "C", "D");

        playQueue.addFiles(false, new TestMediaFile("E"));
        assertPlaylistEquals(playQueue, 0, "E");
    }

    @Test
    public void testAddFilesAt() {
        PlayQueue playQueue = createPlaylist(0);

        playQueue.addFilesAt(Arrays.asList(new TestMediaFile("A"), new TestMediaFile("B"), new TestMediaFile("C")), 0);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.addFilesAt(Arrays.asList(new TestMediaFile("D"), new TestMediaFile("E")), 1);
        assertPlaylistEquals(playQueue, 0, "A", "D", "E", "B", "C");

        playQueue.addFilesAt(Arrays.asList(new TestMediaFile("F")), 0);
        assertPlaylistEquals(playQueue, 0, "F", "A", "D", "E", "B", "C");

    }

    @Test
    public void testUndo() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C");
        playQueue.setIndex(2);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.removeFileAt(2);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.clear();
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.addFiles(true, new TestMediaFile());
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.moveDown(1);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.moveUp(1);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
    }

    @Test
    public void testOrder() {
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, new TestMediaFile(2, "Artist A", "Album B"));
        playQueue.addFiles(true, new TestMediaFile(1, "Artist C", "Album C"));
        playQueue.addFiles(true, new TestMediaFile(3, "Artist B", "Album A"));
        playQueue.addFiles(true, new TestMediaFile(null, "Artist D", "Album D"));
        playQueue.setIndex(2);
        assertEquals(Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber(), "Error in sort.");

        // Order by track.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(JpsonicComparators.OrderBy.TRACK));
        assertNull(playQueue.getFile(0).getTrackNumber(), "Error in sort().");
        assertEquals(Integer.valueOf(1), playQueue.getFile(1).getTrackNumber(), "Error in sort.");
        assertEquals(Integer.valueOf(2), playQueue.getFile(2).getTrackNumber(), "Error in sort.");
        assertEquals(Integer.valueOf(3), playQueue.getFile(3).getTrackNumber(), "Error in sort.");
        assertEquals(Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber());

        // Order by artist.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ARTIST));
        assertEquals("Artist A", playQueue.getFile(0).getArtist(), "Error in sort.");
        assertEquals("Artist B", playQueue.getFile(1).getArtist(), "Error in sort.");
        assertEquals("Artist C", playQueue.getFile(2).getArtist(), "Error in sort.");
        assertEquals("Artist D", playQueue.getFile(3).getArtist(), "Error in sort.");
        assertEquals(Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber(), "Error in sort.");

        // Order by album.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ALBUM));
        assertEquals("Album A", playQueue.getFile(0).getAlbumName(), "Error in sort.");
        assertEquals("Album B", playQueue.getFile(1).getAlbumName(), "Error in sort.");
        assertEquals("Album C", playQueue.getFile(2).getAlbumName(), "Error in sort.");
        assertEquals("Album D", playQueue.getFile(3).getAlbumName(), "Error in sort.");
        assertEquals(Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber(), "Error in sort.");
    }

    private void assertPlaylistEquals(PlayQueue playQueue, int index, String... songs) {
        assertEquals(songs.length, playQueue.size());
        for (int i = 0; i < songs.length; i++) {
            assertEquals(songs[i], playQueue.getFiles().get(i).getName());
        }

        if (index == -1) {
            assertNull(playQueue.getCurrentFile());
        } else {
            assertEquals(songs[index], playQueue.getCurrentFile().getName());
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TestMediaFile) Not reusable.
    private PlayQueue createPlaylist(int index, String... songs) {
        PlayQueue playQueue = new PlayQueue();
        for (String song : songs) {
            playQueue.addFiles(true, new TestMediaFile(song));
        }
        playQueue.setIndex(index);
        return playQueue;
    }

    private static class TestMediaFile extends MediaFile {

        private String name;
        private Integer track;
        private String album;
        private String artist;

        TestMediaFile() {
            super();
        }

        TestMediaFile(String name) {
            super();
            this.name = name;
        }

        TestMediaFile(Integer track, String artist, String album) {
            super();
            this.track = track;
            this.album = album;
            this.artist = artist;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public Integer getTrackNumber() {
            return track;
        }

        @Override
        public String getArtist() {
            return artist;
        }

        @Override
        public String getArtistReading() {
            return artist;
        }

        @Override
        public String getAlbumName() {
            return album;
        }

        @Override
        public String getAlbumReading() {
            return album;
        }

        @Override
        public File getFile() {
            return new File(name);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, track, album, artist);
        }
    }
}
