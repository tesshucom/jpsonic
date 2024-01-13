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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.service.upnp.processor.composite.IndexOrSong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.jupnp.support.model.DIDLContent;
import org.springframework.beans.factory.annotation.Autowired;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.InstantiationToGetClass" })
class IndexProcTest {

    @Nested
    @Order(1)
    class UnitTest {

        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private MediaFileService mediaFileService;
        private MusicIndexService musicIndexService;
        private IndexProc proc;

        @BeforeEach
        public void setup() {
            util = mock(UpnpProcessorUtil.class);
            factory = mock(UpnpDIDLFactory.class);
            mediaFileService = mock(MediaFileService.class);
            musicIndexService = mock(MusicIndexService.class);
            proc = new IndexProc(util, factory, mediaFileService, musicIndexService);
        }

        @Test
        void testGetProcId() {
            assertEquals("index", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            MusicIndex musicIndex = new MusicIndex("A");
            SortedMap<MusicIndex, Integer> map = new TreeMap<>((a, b) -> 0);
            map.put(musicIndex, 99);

            when(musicIndexService.getMusicFolderContentCounts(anyList()))
                    .thenReturn(new MusicFolderContent.Counts(map, 0));
            assertNull(proc.createContainer(new IndexOrSong(musicIndex)));
            verify(factory, times(1)).toMusicIndex(any(MusicIndex.class), any(ProcId.class), anyInt());
        }

        @Test
        void testAddItem() {
            DIDLContent parent = new DIDLContent();
            assertEquals(0, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());

            MusicIndex musicIndex = new MusicIndex("A");
            proc.addDirectChild(parent, new IndexOrSong(musicIndex));
            assertEquals(1, parent.getCount());
            assertEquals(1, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());

            proc.addDirectChild(parent, new IndexOrSong(new MediaFile()));
            assertEquals(2, parent.getCount());
            assertEquals(1, parent.getContainers().size());
            assertEquals(1, parent.getItems().size());
        }

        @Test
        void testGetDirectChildren() {
            MusicIndex musicIndex = new MusicIndex("A");
            SortedMap<MusicIndex, Integer> map = new TreeMap<>((a, b) -> 0);
            map.put(musicIndex, 99);
            when(musicIndexService.getMusicFolderContentCounts(anyList(), any(new MediaType[0].getClass())))
                    .thenReturn(new MusicFolderContent.Counts(map, 0));

            assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
            verify(musicIndexService, times(1)).getMusicFolderContentCounts(anyList(),
                    any(new MediaType[0].getClass()));
            verify(mediaFileService, times(1)).getDirectChildFiles(anyList(), anyLong(), anyLong(),
                    any(new MediaType[0].getClass()));
        }

        @Test
        void testGetDirectChildrenCount() {
            MusicIndex musicIndex = new MusicIndex("A");
            SortedMap<MusicIndex, Integer> map = new TreeMap<>((a, b) -> 0);
            map.put(musicIndex, 99);
            when(musicIndexService.getMusicFolderContentCounts(anyList(), any(new MediaType[0].getClass())))
                    .thenReturn(new MusicFolderContent.Counts(map, 1));
            assertEquals(2, proc.getDirectChildrenCount());
        }

        @Test
        void testGetDirectChild() {
            MusicIndex musicIndex = new MusicIndex("A");
            SortedMap<MusicIndex, Integer> map = new TreeMap<>((a, b) -> 0);
            map.put(musicIndex, 99);
            when(musicIndexService.getMusicFolderContentCounts(anyList(), any(new MediaType[0].getClass())))
                    .thenReturn(new MusicFolderContent.Counts(map, 1));

            IndexOrSong indexOrSong = proc.getDirectChild("A");
            assertEquals(musicIndex, indexOrSong.getMusicIndex());

            assertNull(proc.getDirectChild("81"));

            MediaFile song = new MediaFile();
            when(mediaFileService.getMediaFile(anyString())).thenReturn(song);
            indexOrSong = proc.getDirectChild("82");
            assertEquals(song, indexOrSong.getSong());
        }

        @Test
        void testGetChildren() {
            MediaFile song = new MediaFile();
            assertEquals(Collections.emptyList(), proc.getChildren(new IndexOrSong(song), 0, 100));
            verify(mediaFileService, never()).getChildrenOf(anyList(), any(MusicIndex.class), anyLong(), anyLong(),
                    any(MediaType.class));

            MusicIndex musicIndex = new MusicIndex("A");
            assertEquals(Collections.emptyList(), proc.getChildren(new IndexOrSong(musicIndex), 0, 100));
            verify(mediaFileService, times(1)).getChildrenOf(anyList(), any(MusicIndex.class), anyLong(), anyLong(),
                    any(new MediaType[0].getClass()));
        }

        @Test
        void testGetChildSizeOf() {
            MusicIndex musicIndex = new MusicIndex("A");
            assertEquals(0, proc.getChildSizeOf(new IndexOrSong(musicIndex)));

            SortedMap<MusicIndex, Integer> map = new TreeMap<>((a, b) -> 0);
            map.put(musicIndex, 99);
            when(musicIndexService.getMusicFolderContentCounts(anyList(), any(new MediaType[0].getClass())))
                    .thenReturn(new MusicFolderContent.Counts(map, 0));
            assertEquals(99, proc.getChildSizeOf(new IndexOrSong(musicIndex)));

            assertEquals(0, proc.getChildSizeOf(new IndexOrSong(new MediaFile())));
        }

        @Test
        void testAddChild() {

            DIDLContent parent = new DIDLContent();
            assertEquals(0, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());

            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.DIRECTORY);
            proc.addChild(parent, mediaFile);
            assertEquals(1, parent.getCount());
            assertEquals(1, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());
            verify(factory, times(1)).toArtist(any(MediaFile.class), anyInt());

            parent = new DIDLContent();
            mediaFile.setMediaType(MediaType.ALBUM);
            clearInvocations(factory);
            proc.addChild(parent, mediaFile);
            assertEquals(1, parent.getCount());
            assertEquals(1, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());
            verify(factory, times(1)).toAlbum(any(MediaFile.class), anyInt());

            parent = new DIDLContent();
            mediaFile.setMediaType(MediaType.MUSIC);
            clearInvocations(factory);
            proc.addChild(parent, mediaFile);
            assertEquals(1, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(1, parent.getItems().size());
            verify(factory, times(1)).toMusicTrack(any(MediaFile.class));

            parent = new DIDLContent();
            mediaFile.setMediaType(MediaType.VIDEO);
            clearInvocations(factory);
            proc.addChild(parent, mediaFile);
            assertEquals(0, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());
            verify(factory, never()).toMusicTrack(any(MediaFile.class));

            parent = new DIDLContent();
            mediaFile.setMediaType(MediaType.AUDIOBOOK);
            clearInvocations(factory);
            proc.addChild(parent, mediaFile);
            assertEquals(0, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());
            verify(factory, never()).toMusicTrack(any(MediaFile.class));

            parent = new DIDLContent();
            mediaFile.setMediaType(MediaType.PODCAST);
            clearInvocations(factory);
            proc.addChild(parent, mediaFile);
            assertEquals(0, parent.getCount());
            assertEquals(0, parent.getContainers().size());
            assertEquals(0, parent.getItems().size());
            verify(factory, never()).toMusicTrack(any(MediaFile.class));
        }
    }

    @Nested
    @Order(2)
    class IntegrationTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays.asList(
                new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true, now(), 1, false));

        @Autowired
        private IndexProc indexProc;
        @Autowired
        private MediaFileService mediaFileService;

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
            assertEquals(31, indexProc.getDirectChildrenCount());
        }

        @Test
        void testGetItems() {

            List<IndexOrSong> items = indexProc.getDirectChildren(0, 10);
            assertEquals(10, items.size());
            assertEquals("A", items.get(0).getMusicIndex().getIndex());
            assertEquals("B", items.get(1).getMusicIndex().getIndex());
            assertEquals("C", items.get(2).getMusicIndex().getIndex());
            assertEquals("D", items.get(3).getMusicIndex().getIndex());
            assertEquals("E", items.get(4).getMusicIndex().getIndex());
            assertEquals("あ", items.get(5).getMusicIndex().getIndex());
            assertEquals("さ", items.get(6).getMusicIndex().getIndex());
            assertEquals("は", items.get(7).getMusicIndex().getIndex());
            assertEquals("#", items.get(8).getMusicIndex().getIndex());
            assertEquals("single1", items.get(9).getSong().getName());

            items = indexProc.getDirectChildren(10, 10);
            assertEquals(10, items.size());
            assertEquals("single2", items.get(0).getSong().getName());
            assertEquals("single3", items.get(1).getSong().getName());
            assertEquals("single4", items.get(2).getSong().getName());
            assertEquals("single5", items.get(3).getSong().getName());
            assertEquals("single6", items.get(4).getSong().getName());
            assertEquals("single7", items.get(5).getSong().getName());
            assertEquals("single8", items.get(6).getSong().getName());
            assertEquals("single9", items.get(7).getSong().getName());
            assertEquals("single10", items.get(8).getSong().getName());
            assertEquals("single11", items.get(9).getSong().getName());

            items = indexProc.getDirectChildren(20, 10);
            assertEquals(10, items.size());
            assertEquals("single12", items.get(0).getSong().getName());
            assertEquals("single13", items.get(1).getSong().getName());
            assertEquals("single14", items.get(2).getSong().getName());
            assertEquals("single15", items.get(3).getSong().getName());
            assertEquals("single16", items.get(4).getSong().getName());
            assertEquals("single17", items.get(5).getSong().getName());
            assertEquals("single18", items.get(6).getSong().getName());
            assertEquals("single19", items.get(7).getSong().getName());
            assertEquals("single20", items.get(8).getSong().getName());
            assertEquals("single21", items.get(9).getSong().getName());

            items = indexProc.getDirectChildren(30, 10);
            assertEquals(1, items.size());
            assertEquals("single22", items.get(0).getSong().getName());

            items = indexProc.getDirectChildren(0, 5);
            assertEquals(5, items.size());
            assertEquals("A", items.get(0).getMusicIndex().getIndex());
            assertEquals("B", items.get(1).getMusicIndex().getIndex());
            assertEquals("C", items.get(2).getMusicIndex().getIndex());
            assertEquals("D", items.get(3).getMusicIndex().getIndex());
            assertEquals("E", items.get(4).getMusicIndex().getIndex());

            items = indexProc.getDirectChildren(5, 100);
            assertEquals(26, items.size());
            assertEquals("あ", items.get(0).getMusicIndex().getIndex());
            assertEquals("さ", items.get(1).getMusicIndex().getIndex());
            assertEquals("は", items.get(2).getMusicIndex().getIndex());
            assertEquals("#", items.get(3).getMusicIndex().getIndex());
            assertEquals("single1", items.get(4).getSong().getName());

            items = indexProc.getDirectChildren(0, 9);
            assertEquals(9, items.size());
            assertEquals("A", items.get(0).getMusicIndex().getIndex());
            assertEquals("#", items.get(8).getMusicIndex().getIndex());

            items = indexProc.getDirectChildren(8, 1);
            assertEquals(1, items.size());
            assertEquals("#", items.get(0).getMusicIndex().getIndex());

            items = indexProc.getDirectChildren(9, 1);
            assertEquals(1, items.size());
            assertEquals("single1", items.get(0).getSong().getName());

            items = indexProc.getDirectChildren(30, 1);
            assertEquals(1, items.size());
            assertEquals("single22", items.get(0).getSong().getName());
        }

        @Test
        void testGetChildSizeOf() {
            List<IndexOrSong> items = indexProc.getDirectChildren(0, 100);
            assertEquals(31, items.size());
            assertEquals(1, indexProc.getChildSizeOf(items.get(0)));
            assertEquals(1, indexProc.getChildSizeOf(items.get(1)));
            assertEquals(1, indexProc.getChildSizeOf(items.get(2)));
            assertEquals(1, indexProc.getChildSizeOf(items.get(3)));
            assertEquals(1, indexProc.getChildSizeOf(items.get(4)));
            assertEquals(5, indexProc.getChildSizeOf(items.get(5)));
            assertEquals(1, indexProc.getChildSizeOf(items.get(6)));
            assertEquals(5, indexProc.getChildSizeOf(items.get(7)));
            assertEquals(15, indexProc.getChildSizeOf(items.get(8)));
        }

        @Test
        void testGetChildren() {
            List<String> artistNames = indexProc.getDirectChildren(0, 100).stream().filter(IndexOrSong::isMusicIndex)
                    .flatMap(m -> indexProc.getChildren(m, 0, 100).stream()).map(MediaFile::getName).toList();
            assertEquals(INDEX_LIST, artistNames);

            List<IndexOrSong> items = indexProc.getDirectChildren(0, 100);
            assertEquals(31, items.size());
            assertEquals(5, indexProc.getChildSizeOf(items.get(5)));
            assertEquals(5, indexProc.getChildSizeOf(items.get(7)));
            assertEquals(15, indexProc.getChildSizeOf(items.get(8)));

            List<MediaFile> artist = indexProc.getChildren(items.get(5), 0, 3);
            assertEquals(INDEX_LIST.get(5), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(6), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(7), artist.get(2).getName());

            artist = indexProc.getChildren(items.get(5), 3, 100);
            assertEquals(INDEX_LIST.get(8), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(9), artist.get(1).getName());

            artist = indexProc.getChildren(items.get(7), 0, 3);
            assertEquals(INDEX_LIST.get(11), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(12), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(13), artist.get(2).getName());
            artist = indexProc.getChildren(items.get(7), 3, 100);
            assertEquals(INDEX_LIST.get(14), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(15), artist.get(1).getName());

            artist = indexProc.getChildren(items.get(8), 0, 3);

            assertEquals(3, artist.size());
            assertEquals(INDEX_LIST.get(16), artist.get(0).getName());
            assertEquals(INDEX_LIST.get(17), artist.get(1).getName());
            assertEquals(INDEX_LIST.get(18), artist.get(2).getName());
            artist = indexProc.getChildren(items.get(8), 3, 100);
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

            List<IndexOrSong> indexes = indexProc.getDirectChildren(0, 100);
            assertEquals(31, indexes.size());

            IndexOrSong indexOrSong = indexes.get(8);
            assertEquals("#", indexOrSong.getMusicIndex().getIndex());

            List<MediaFile> artists = indexProc.getChildren(indexOrSong, 0, Integer.MAX_VALUE);
            assertEquals(15, artists.size());
            MediaFile artist = artists.get(0);
            assertEquals("10", artist.getName());

            List<MediaFile> albums = mediaFileService.getChildrenOf(artist, 0, Integer.MAX_VALUE, ChildOrder.BY_ALPHA,
                    MediaType.PODCAST, MediaType.AUDIOBOOK, MediaType.VIDEO);
            assertEquals(31, albums.size());

            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(albums.stream().map(MediaFile::getName).collect(Collectors.toList())));
        }

        @Test
        void testSongs() {

            List<IndexOrSong> indexes = indexProc.getDirectChildren(0, 100).stream().filter(i -> i.isMusicIndex())
                    .filter(a -> "#".equals(a.getMusicIndex().getIndex())).collect(Collectors.toList());
            assertEquals(1, indexes.size());

            MusicIndex index = indexes.get(0).getMusicIndex();
            assertEquals("#", index.getIndex());

            List<MediaFile> artists = indexProc.getChildren(indexes.get(0), 0, Integer.MAX_VALUE).stream()
                    .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());

            MediaFile artist = artists.get(0);
            assertEquals("20", artist.getName());

            List<MediaFile> albums = mediaFileService.getChildrenOf(artist, 0, Integer.MAX_VALUE, ChildOrder.BY_ALPHA,
                    MediaType.PODCAST, MediaType.AUDIOBOOK, MediaType.VIDEO);
            assertEquals(1, albums.size());

            MediaFile album = albums.get(0);
            assertEquals("ALBUM", album.getName()); // the case where album name is different between file and id3

            List<MediaFile> songs = mediaFileService.getChildrenOf(album, 0, Integer.MAX_VALUE, ChildOrder.BY_ALPHA);
            assertEquals(1, songs.size());

            MediaFile song = songs.get(0);
            assertEquals("empty", song.getName());
        }
    }
}
