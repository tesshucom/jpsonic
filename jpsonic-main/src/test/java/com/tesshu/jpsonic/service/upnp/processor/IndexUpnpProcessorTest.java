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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.service.upnp.processor.UpnpProcessorTestUtils.INDEX_LIST;
import static com.tesshu.jpsonic.service.upnp.processor.UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.service.JMediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class IndexUpnpProcessorTest {

    @Nested
    class UnitTest {

        private UpnpProcessDispatcher dispatcher;
        private UpnpProcessorUtil util;
        private JMediaFileService mediaFileService;
        private MusicIndexService musicIndexService;
        private Ehcache indexCache;
        private IndexUpnpProcessor processor;

        @BeforeEach
        public void setup() {
            dispatcher = mock(UpnpProcessDispatcher.class);
            util = mock(UpnpProcessorUtil.class);
            mediaFileService = mock(JMediaFileService.class);
            musicIndexService = mock(MusicIndexService.class);
            indexCache = mock(Ehcache.class);
            processor = new IndexUpnpProcessor(dispatcher, util, mediaFileService, musicIndexService, indexCache);
        }

        @Test
        void testRefreshIndex() {
            List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(0, "", "name", true, now(), 0));
            Mockito.when(util.getGuestMusicFolders()).thenReturn(musicFolders);
            Mockito.when(musicIndexService.getMusicFolderContent(musicFolders))
                    .thenReturn(new MusicFolderContent(new TreeMap<>(), Collections.emptyList()));
            processor.refreshIndex();
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(musicFolders);
            Mockito.clearInvocations(musicIndexService);
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true, now(), 1));

        @Autowired
        private IndexUpnpProcessor indexUpnpProcessor;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);

            String simpleIndex = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) " // En
                    + "\u3042(\u30A2\u30A4\u30A6\u30A8\u30AA) " // Jp(a)
                    + "\u304B(\u30AB\u30AD\u30AF\u30B1\u30B3) " // Jp(ka)
                    + "\u3055(\u30B5\u30B7\u30B9\u30BB\u30BD) " // Jp(sa)
                    + "\u305F(\u30BF\u30C1\u30C4\u30C6\u30C8) " // Jp(ta)
                    + "\u306A(\u30CA\u30CB\u30CC\u30CD\u30CE) " // Jp(na)
                    + "\u306F(\u30CF\u30D2\u30D5\u30D8\u30DB) " // Jp(ha)
                    + "\u307E(\u30DE\u30DF\u30E0\u30E1\u30E2) " // Jp(ma)
                    + "\u3084(\u30E4\u30E6\u30E8) " // Jp(ya)
                    + "\u3089(\u30E9\u30EA\u30EB\u30EC\u30ED) " // Jp(ra)
                    + "\u308F(\u30EF\u30F2\u30F3)"; // Jp(wa)

            // Test case is created on the premise of simpleIndex.
            settingsService.setIndexString(simpleIndex);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetItemCount() {
            assertEquals(31, indexUpnpProcessor.getItemCount());
        }

        @Test
        void testGetItems() {

            List<MediaFile> items = indexUpnpProcessor.getItems(0, 10);
            assertEquals(10, items.size());
            assertEquals("A", items.get(0).getName());
            assertEquals("B", items.get(1).getName());
            assertEquals("C", items.get(2).getName());
            assertEquals("D", items.get(3).getName());
            assertEquals("E", items.get(4).getName());
            assertEquals("あ", items.get(5).getName());
            assertEquals("さ", items.get(6).getName());
            assertEquals("は", items.get(7).getName());
            assertEquals("#", items.get(8).getName());
            assertEquals("single1", items.get(9).getName());

            items = indexUpnpProcessor.getItems(10, 10);
            assertEquals(10, items.size());
            assertEquals("single2", items.get(0).getName());
            assertEquals("single3", items.get(1).getName());
            assertEquals("single4", items.get(2).getName());
            assertEquals("single5", items.get(3).getName());
            assertEquals("single6", items.get(4).getName());
            assertEquals("single7", items.get(5).getName());
            assertEquals("single8", items.get(6).getName());
            assertEquals("single9", items.get(7).getName());
            assertEquals("single10", items.get(8).getName());
            assertEquals("single11", items.get(9).getName());

            items = indexUpnpProcessor.getItems(20, 10);
            assertEquals(10, items.size());
            assertEquals("single12", items.get(0).getName());
            assertEquals("single13", items.get(1).getName());
            assertEquals("single14", items.get(2).getName());
            assertEquals("single15", items.get(3).getName());
            assertEquals("single16", items.get(4).getName());
            assertEquals("single17", items.get(5).getName());
            assertEquals("single18", items.get(6).getName());
            assertEquals("single19", items.get(7).getName());
            assertEquals("single20", items.get(8).getName());
            assertEquals("single21", items.get(9).getName());

            items = indexUpnpProcessor.getItems(30, 10);
            assertEquals(1, items.size());
            assertEquals("single22", items.get(0).getName());

            items = indexUpnpProcessor.getItems(0, 5);
            assertEquals(5, items.size());
            assertEquals("A", items.get(0).getName());
            assertEquals("B", items.get(1).getName());
            assertEquals("C", items.get(2).getName());
            assertEquals("D", items.get(3).getName());
            assertEquals("E", items.get(4).getName());

            items = indexUpnpProcessor.getItems(5, 100);
            assertEquals(26, items.size());
            assertEquals("あ", items.get(0).getName());
            assertEquals("さ", items.get(1).getName());
            assertEquals("は", items.get(2).getName());
            assertEquals("#", items.get(3).getName());
            assertEquals("single1", items.get(4).getName());

            items = indexUpnpProcessor.getItems(0, 9);
            assertEquals(9, items.size());
            assertEquals("A", items.get(0).getName());
            assertEquals("#", items.get(8).getName());

            items = indexUpnpProcessor.getItems(8, 1);
            assertEquals(1, items.size());
            assertEquals("#", items.get(0).getName());

            items = indexUpnpProcessor.getItems(9, 1);
            assertEquals(1, items.size());
            assertEquals("single1", items.get(0).getName());

            items = indexUpnpProcessor.getItems(30, 1);
            assertEquals(1, items.size());
            assertEquals("single22", items.get(0).getName());
        }

        @Test
        void testGetChildSizeOf() {

            List<MediaFile> items = indexUpnpProcessor.getItems(0, 100);
            assertEquals(31, items.size());

            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(0)));
            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(1)));
            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(2)));
            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(3)));
            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(4)));
            assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(5)));
            assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(6)));
            assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(7)));
            assertEquals(15, indexUpnpProcessor.getChildSizeOf(items.get(8)));

        }

        @Test
        void testgetChildren() {

            List<String> artistNames = indexUpnpProcessor.getItems(0, 100).stream()
                    .flatMap(m -> indexUpnpProcessor.getChildren(m, 0, 100).stream()).map(MediaFile::getName)
                    .collect(Collectors.toList());
            assertEquals(INDEX_LIST, artistNames);

            List<MediaFile> items = indexUpnpProcessor.getItems(0, 100);
            assertEquals(31, items.size());
            assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(5)));
            assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(7)));
            assertEquals(15, indexUpnpProcessor.getChildSizeOf(items.get(8)));

            List<MediaFile> artist = indexUpnpProcessor.getChildren(items.get(5), 0, 3);
            assertEquals(INDEX_LIST.get(5), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(6), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(7), artist.get(2).getName());
            artist = indexUpnpProcessor.getChildren(items.get(5), 3, 100);
            assertEquals(INDEX_LIST.get(8), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(9), artist.get(1).getName());

            artist = indexUpnpProcessor.getChildren(items.get(7), 0, 3);
            assertEquals(INDEX_LIST.get(11), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(12), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(13), artist.get(2).getName());
            artist = indexUpnpProcessor.getChildren(items.get(7), 3, 100);
            assertEquals(INDEX_LIST.get(14), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(15), artist.get(1).getName());

            artist = indexUpnpProcessor.getChildren(items.get(8), 0, 3);

            assertEquals(3, artist.size());
            assertEquals(INDEX_LIST.get(16), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(17), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(18), artist.get(2).getName());
            artist = indexUpnpProcessor.getChildren(items.get(8), 3, 100);
            assertEquals(12, artist.size());
            assertEquals(INDEX_LIST.get(19), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(20), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(21), artist.get(2).getName());
            assertEquals(INDEX_LIST.get(22), artist.get(3).getName());
            assertEquals(INDEX_LIST.get(23), artist.get(4).getName());
            assertEquals(INDEX_LIST.get(24), artist.get(5).getName());
            assertEquals(INDEX_LIST.get(25), artist.get(6).getName());
            assertEquals(INDEX_LIST.get(26), artist.get(7).getName());
            assertEquals(INDEX_LIST.get(27), artist.get(8).getName());
            assertEquals(INDEX_LIST.get(28), artist.get(9).getName());
            assertEquals(INDEX_LIST.get(29), artist.get(10).getName());
            assertEquals(INDEX_LIST.get(30), artist.get(11).getName());

        }

        @Test
        void testAlbum() {

            settingsService.setSortAlbumsByYear(false);

            List<MediaFile> indexes = indexUpnpProcessor.getItems(0, 100);
            assertEquals(31, indexes.size());

            MediaFile index = indexes.get(8);
            assertEquals("#", index.getName());

            List<MediaFile> artists = indexUpnpProcessor.getChildren(index, 0, Integer.MAX_VALUE);
            assertEquals(15, artists.size());
            MediaFile artist = artists.get(0);
            assertEquals("10", artist.getName());

            List<MediaFile> albums = indexUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());

            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(albums.stream().map(MediaFile::getName).collect(Collectors.toList())));

        }

        @Test
        void testAlbumByYear() {

            // The result change depending on the setting
            settingsService.setSortAlbumsByYear(true);
            List<String> reversedByYear = new ArrayList<>(JPSONIC_NATURAL_LIST);
            Collections.reverse(reversedByYear);

            List<MediaFile> indexes = indexUpnpProcessor.getItems(0, 100);
            assertEquals(31, indexes.size());

            MediaFile index = indexes.get(8);
            assertEquals("#", index.getName());

            List<MediaFile> artists = indexUpnpProcessor.getChildren(index, 0, Integer.MAX_VALUE);
            assertEquals(15, artists.size());
            MediaFile artist = artists.get(0);
            assertEquals("10", artist.getName());

            List<MediaFile> albums = indexUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());

            assertEquals(reversedByYear, albums.stream().map(MediaFile::getName).collect(Collectors.toList()));

        }

        @Test
        void testSongs() {

            List<MediaFile> indexes = indexUpnpProcessor.getItems(0, 100).stream().filter(a -> "#".equals(a.getName()))
                    .collect(Collectors.toList());
            assertEquals(1, indexes.size());

            MediaFile index = indexes.get(0);
            assertEquals("#", index.getName());

            List<MediaFile> artists = indexUpnpProcessor.getChildren(index, 0, Integer.MAX_VALUE).stream()
                    .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());

            MediaFile artist = artists.get(0);
            assertEquals("20", artist.getName());

            List<MediaFile> albums = indexUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(1, albums.size());

            MediaFile album = albums.get(0);
            assertEquals("ALBUM", album.getName()); // the case where album name is different between file and id3

            List<MediaFile> songs = indexUpnpProcessor.getChildren(album, 0, Integer.MAX_VALUE);
            assertEquals(1, songs.size());

            MediaFile song = songs.get(0);
            assertEquals("empty", song.getName());
        }
    }
}
