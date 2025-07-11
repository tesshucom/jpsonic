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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.upnp.processor.composite.IndexOrSong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Originally a Nested class of IndexProcTest. (Avoiding the phenomenon that often occurs in JUnit, where multi-nested classes do not work.)
 */
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class IndexProcTest2 {

    @Nested
    @Order(3)
    class MessyFileStructureTest extends AbstractNeedsScan {

        private static final MusicFolder MUSIC_FOLDER = new MusicFolder(0,
                resolveBaseMediaPath("Browsing/MessyFileStructure/Folder"), "Folder", true, now(),
                1, false);

        @Autowired
        private IndexProc indexProc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return List.of(MUSIC_FOLDER);
        }

        @BeforeEach
        public void setup() {
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetDirectChildren() {
            List<IndexOrSong> indexOrSongs = indexProc.getDirectChildren(0, Integer.MAX_VALUE);
            assertEquals(3, indexOrSongs.size());

            // Including Album**
            assertTrue(indexOrSongs.get(0).isMusicIndex());
            assertEquals("A", indexOrSongs.get(0).getMusicIndex().getIndex());

            // Including Dir**
            assertTrue(indexOrSongs.get(1).isMusicIndex());
            assertEquals("D", indexOrSongs.get(1).getMusicIndex().getIndex());

            // Including Music and excluding Movie
            assertFalse(indexOrSongs.get(2).isMusicIndex());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(3, indexProc.getDirectChildrenCount());
        }

        @Test
        void testGetChildren() {
            List<IndexOrSong> indexOrSongs = indexProc.getDirectChildren(0, Integer.MAX_VALUE);
            assertEquals(3, indexOrSongs.size());

            // Including Album**
            assertTrue(indexOrSongs.get(0).isMusicIndex());
            assertEquals("A", indexOrSongs.get(0).getMusicIndex().getIndex());
            List<MediaFile> mediaFiles = indexProc
                .getChildren(indexOrSongs.get(0), 0, Integer.MAX_VALUE);
            assertEquals(1, mediaFiles.size());
            assertEquals("Album1", mediaFiles.get(0).getName());

            // Including Dir**
            assertTrue(indexOrSongs.get(1).isMusicIndex());
            assertEquals("D", indexOrSongs.get(1).getMusicIndex().getIndex());

            mediaFiles = indexProc.getChildren(indexOrSongs.get(1), 0, Integer.MAX_VALUE);
            assertEquals(2, mediaFiles.size());
            assertEquals("Dir1", mediaFiles.get(0).getName());
            assertEquals("Dir4", mediaFiles.get(1).getName());

            // Including Music and excluding Movie
            assertFalse(indexOrSongs.get(2).isMusicIndex());
            mediaFiles = indexProc.getChildren(indexOrSongs.get(2), 0, Integer.MAX_VALUE);
            assertEquals(0, mediaFiles.size());
            assertEquals("song1", indexOrSongs.get(2).getSong().getName());
        }

        @Test
        void testGetChildSizeOf() {
            List<IndexOrSong> indexOrSongs = indexProc.getDirectChildren(0, Integer.MAX_VALUE);
            assertEquals(3, indexOrSongs.size());

            // Including Album**
            assertTrue(indexOrSongs.get(0).isMusicIndex());
            List<MediaFile> mediaFiles = indexProc
                .getChildren(indexOrSongs.get(0), 0, Integer.MAX_VALUE);
            assertEquals(1, mediaFiles.size());
            assertEquals(1, indexProc.getChildSizeOf(indexOrSongs.get(0)));

            // Including Dir**
            mediaFiles = indexProc.getChildren(indexOrSongs.get(1), 0, Integer.MAX_VALUE);
            assertEquals(2, mediaFiles.size());
            assertEquals(2, indexProc.getChildSizeOf(indexOrSongs.get(1)));

            // Including Music and excluding Movie
            assertFalse(indexOrSongs.get(2).isMusicIndex());
            mediaFiles = indexProc.getChildren(indexOrSongs.get(2), 0, Integer.MAX_VALUE);
            assertEquals(0, mediaFiles.size());
            assertEquals(0, indexProc.getChildSizeOf(indexOrSongs.get(2)));
        }
    }
}
