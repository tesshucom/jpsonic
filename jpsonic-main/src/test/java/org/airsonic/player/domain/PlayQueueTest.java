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

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ALBUM;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.TRACK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test of {@link PlayQueue}.
 *
 * @author Sindre Mehus
 */
public class PlayQueueTest extends AbstractAirsonicHomeTest {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private JpsonicComparators jpsonicComparators;

    public JpsonicComparators getJpsonicComparators() {
        return jpsonicComparators;
    }

    public void setJpsonicComparators(JpsonicComparators jpsonicComparators) {
        this.jpsonicComparators = jpsonicComparators;
    }

    @Rule
    public ThreadRule r = new ThreadRule(100);

    @AfterClass
    public static void tearDown() {
        executor.shutdownNow();
    }

    private static final PlayQueue common = new PlayQueue();

    @Test
    public synchronized void testName() {
        String name = Thread.currentThread().toString();
        common.setName(name);
        assertEquals(name, common.getName());
    }

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
        assertEquals("Error in sort.", Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber());

        if (jpsonicComparators == null) {
            System.err.println("null!");
        }

        // Order by track.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(TRACK));
        assertEquals("Error in sort().", null, playQueue.getFile(0).getTrackNumber());
        assertEquals("Error in sort().", Integer.valueOf(1), playQueue.getFile(1).getTrackNumber());
        assertEquals("Error in sort().", Integer.valueOf(2), playQueue.getFile(2).getTrackNumber());
        assertEquals("Error in sort().", Integer.valueOf(3), playQueue.getFile(3).getTrackNumber());
        assertEquals("Error in sort().", Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber());

        // Order by artist.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(ARTIST));
        assertEquals("Error in sort().", "Artist A", playQueue.getFile(0).getArtist());
        assertEquals("Error in sort().", "Artist B", playQueue.getFile(1).getArtist());
        assertEquals("Error in sort().", "Artist C", playQueue.getFile(2).getArtist());
        assertEquals("Error in sort().", "Artist D", playQueue.getFile(3).getArtist());
        assertEquals("Error in sort().", Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber());

        // Order by album.
        playQueue.sort(jpsonicComparators.mediaFileOrderBy(ALBUM));
        assertEquals("Error in sort().", "Album A", playQueue.getFile(0).getAlbumName());
        assertEquals("Error in sort().", "Album B", playQueue.getFile(1).getAlbumName());
        assertEquals("Error in sort().", "Album C", playQueue.getFile(2).getAlbumName());
        assertEquals("Error in sort().", "Album D", playQueue.getFile(3).getAlbumName());
        assertEquals("Error in sort().", Integer.valueOf(3), playQueue.getCurrentFile().getTrackNumber());
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
        }

        TestMediaFile(String name) {
            this.name = name;
        }

        TestMediaFile(Integer track, String artist, String album) {
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

    public class ThreadRule implements MethodRule {

        private final int count;

        public ThreadRule(int count) {
            this.count = count;
        }

        @Override
        public Statement apply(final Statement base, FrameworkMethod method, Object target) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    List<Future<?>> futures = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        futures.add(i, executor.submit(() -> {
                            try {
                                base.evaluate();
                            } catch (Throwable t) {
                                throw new RuntimeException(t);
                            }
                        }));
                    }
                    for (Future<?> f : futures) {
                        f.get();
                    }
                }
            };
        }
    }

}
