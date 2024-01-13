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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.MediaFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals", "PMD.InstantiationToGetClass" })
class MediaFileProcTest {

    @Nested
    class UnitTest {

        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private MediaFileService mediaFileService;
        private MediaFileProc proc;

        @BeforeEach
        public void setup() {
            util = mock(UpnpProcessorUtil.class);
            factory = mock(UpnpDIDLFactory.class);
            mediaFileService = mock(MediaFileService.class);
            proc = new MediaFileProc(util, factory, mediaFileService);
        }

        @Test
        void testGetProcId() {
            assertEquals("folder", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            MediaFile entity = new MediaFile();
            entity.setMediaType(MediaType.MUSIC);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> proc.createContainer(entity))
                    .withNoCause();
            entity.setMediaType(MediaType.VIDEO);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> proc.createContainer(entity))
                    .withNoCause();
            entity.setMediaType(MediaType.AUDIOBOOK);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> proc.createContainer(entity))
                    .withNoCause();
            entity.setMediaType(MediaType.PODCAST);
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> proc.createContainer(entity))
                    .withNoCause();

            clearInvocations(factory);
            entity.setMediaType(MediaType.ALBUM);
            proc.createContainer(entity);
            verify(factory, times(1)).toAlbum(any(MediaFile.class), anyInt());

            clearInvocations(factory);
            entity.setMediaType(MediaType.DIRECTORY);
            proc.createContainer(entity);
            verify(factory, times(1)).toMusicFolder(any(MediaFile.class), any(ProcId.class), anyInt());

            clearInvocations(factory);
            entity.setArtist("artist");
            proc.createContainer(entity);
            verify(factory, times(1)).toArtist(any(MediaFile.class), anyInt());
        }

        @Test
        void testAddItem() {
            DIDLContent parent = new DIDLContent();
            assertEquals(0, parent.getItems().size());
            assertEquals(0, parent.getContainers().size());

            MediaFile song = new MediaFile();
            song.setMediaType(MediaType.MUSIC);
            proc.addDirectChild(parent, song);
            assertEquals(1, parent.getItems().size());
            assertEquals(0, parent.getContainers().size());

            MediaFile album = new MediaFile();
            album.setMediaType(MediaType.ALBUM);
            proc.addDirectChild(parent, album);
            assertEquals(1, parent.getItems().size());
            assertEquals(1, parent.getContainers().size());
        }

        @Nested
        class GetGetDirectChildrenTest {

            private final MusicFolder folder1 = new MusicFolder("path1", "name1", true, null, false);
            private final MediaFile mfolder1 = new MediaFile();
            private final MediaFile mfolder2 = new MediaFile();
            private final MediaFile mfolder3 = new MediaFile();

            @BeforeEach
            public void setup() {
                mfolder1.setPathString("/path1");
                mfolder2.setPathString("/path2");
                mfolder3.setPathString("/path3");
                when(mediaFileService.getChildrenOf(any(MediaFile.class), anyLong(), anyLong(), any(ChildOrder.class),
                        any(new MediaType[0].getClass()))).thenReturn(
                                List.of(new MediaFile(), new MediaFile(), new MediaFile(), new MediaFile()));
            }

            @Test
            void testNoFolder() {
                assertEquals(0, proc.getDirectChildren(0, 30).size());
            }

            @Test
            void testSingleFolder() {
                when(mediaFileService.getMediaFileStrict(any(String.class))).thenReturn(mfolder1, mfolder2,
                        mfolder3);
                when(util.getGuestFolders()).thenReturn(List.of(folder1));
                assertEquals(4, proc.getDirectChildren(0, 30).size());
            }

            @Test
            void testMultiFolder() {
                when(mediaFileService.getMediaFile(any(Path.class))).thenReturn(mfolder1, mfolder2,
                        mfolder3);
                MusicFolder folder2 = new MusicFolder("path2", "name2", true, null, false);
                MusicFolder folder3 = new MusicFolder("path3", "name3", true, null, false);
                when(util.getGuestFolders()).thenReturn(List.of(folder1, folder2, folder3));
                assertEquals(0, proc.getDirectChildren(10, 0).size());
                assertEquals(3, proc.getDirectChildren(0, 3).size());
                assertEquals(2, proc.getDirectChildren(1, 3).size());
                assertEquals(1, proc.getDirectChildren(2, 2).size());
            }

        }

        // TODO Not enough sort-rule
        @Nested
        class GetGetDirectChildrenCountTest {

            private final MusicFolder folder1 = new MusicFolder("path1", "name1", true, null, false);

            @BeforeEach
            public void setup() {
                when(mediaFileService.getChildrenOf(any(MediaFile.class), anyLong(), anyLong(),
                        any(ChildOrder.class), any(MediaType.class)))
                                .thenReturn(List.of(new MediaFile(), new MediaFile(),
                                        new MediaFile(), new MediaFile()));
            }

            @Test
            void testNoFolder() {
                assertEquals(0, proc.getDirectChildrenCount());
            }

            @Test
            void testSingleFolder() {
                when(util.getGuestFolders()).thenReturn(List.of(folder1));
                assertEquals(0, proc.getDirectChildrenCount());
                verify(mediaFileService, times(1)).getChildSizeOf(anyList(), any(new MediaType[0].getClass()));
            }

            @Test
            void testMultiFolder() {
                MediaFile mfolder1 = new MediaFile();
                mfolder1.setPathString("/path1");
                MediaFile mfolder2 = new MediaFile();
                mfolder2.setPathString("/path2");
                MediaFile mfolder3 = new MediaFile();
                mfolder3.setPathString("/path3");
                when(mediaFileService.getMediaFile(any(Path.class))).thenReturn(mfolder1, mfolder2, mfolder3);
                MusicFolder folder2 = new MusicFolder("path2", "name2", true, null, false);
                MusicFolder folder3 = new MusicFolder("path3", "name3", true, null, false);
                when(util.getGuestFolders()).thenReturn(List.of(folder1, folder2, folder3));
                assertEquals(0, proc.getDirectChildren(10, 0).size());
                assertEquals(3, proc.getDirectChildren(0, 3).size());
                assertEquals(2, proc.getDirectChildren(1, 3).size());
                assertEquals(1, proc.getDirectChildren(2, 3).size());
                verify(mediaFileService, never()).getChildSizeOf(anyList(), any(MediaType.class));
            }
        }

        @Test
        void testDirectChild() {
            assertNull(proc.getDirectChild("0"));
            verify(mediaFileService, times(1)).getMediaFileStrict(anyInt());
        }

        // TODO We may want to consider file filtering.
        @Test
        void testGetChildren() {
            MediaFile root = new MediaFile();
            assertEquals(0, proc.getChildren(root, 0, 0).size());

            MediaFile artist = new MediaFile();
            artist.setArtist("artist");
            assertEquals(0, proc.getChildren(artist, 0, 0).size());

            MediaFile album = new MediaFile();
            album.setArtist("artist");
            album.setMediaType(MediaType.ALBUM);
            assertEquals(0, proc.getChildren(album, 0, 0).size());
        }

        @Test
        void testGetChildSizeOf() {
            assertEquals(0, proc.getChildSizeOf(null));
            verify(mediaFileService, times(1)).getChildSizeOf(nullable(MediaFile.class),
                    any(new MediaType[0].getClass()));
        }

        @Test
        void testAddChild() {
            DIDLContent parent = new DIDLContent();
            assertEquals(0, parent.getItems().size());
            assertEquals(0, parent.getContainers().size());

            MediaFile song = new MediaFile();
            song.setMediaType(MediaType.MUSIC);
            proc.addChild(parent, song);
            assertEquals(1, parent.getItems().size());
            assertEquals(0, parent.getContainers().size());

            MediaFile album = new MediaFile();
            album.setMediaType(MediaType.ALBUM);
            proc.addChild(parent, album);
            assertEquals(1, parent.getItems().size());
            assertEquals(1, parent.getContainers().size());
        }
    }

    @SpringBootTest
    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private MediaFileProc mediaFileProc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() throws URISyntaxException {
            musicFolders = Arrays.asList(new MusicFolder(1,
                    Path.of(MediaFileProcTest.class.getResource("/MEDIAS/Sort/Pagination/Artists").toURI()).toString(),
                    "Artists", true, now(), 1, false));

            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
            settingsService.save();
            populateDatabaseOnlyOnce();
        }

        @Test
        void testDirectChildren() {

            List<MediaFile> items = mediaFileProc.getDirectChildren(0, 10);
            assertEquals(10, items.size());

            items = mediaFileProc.getDirectChildren(10, 10);
            assertEquals(10, items.size());

            items = mediaFileProc.getDirectChildren(20, 100);
            assertEquals(33, items.size());

            items = mediaFileProc.getDirectChildren(0, 100).stream().filter(a -> !a.getName().startsWith("single"))
                    .collect(Collectors.toList());
            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(items.stream().map(MediaFile::getName).collect(Collectors.toList())));
        }

        @Test
        void testDirectChildrenCount() {
            // 31 + 22(topnodes)
            assertEquals(53, mediaFileProc.getDirectChildrenCount());
        }

        @Test
        void testgetChildren() {

            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            List<MediaFile> children = mediaFileProc.getChildren(artists.get(0), 0, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i), children.get(i).getName());
            }

            children = mediaFileProc.getChildren(artists.get(0), 10, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 10), children.get(i).getName());
            }

            children = mediaFileProc.getChildren(artists.get(0), 20, 100);
            assertEquals(11, children.size());
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 20), children.get(i).getName());
            }
        }

        @Test
        void testGetChildSizeOf() {
            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());
            assertEquals(31, mediaFileProc.getChildSizeOf(artists.get(0)));
        }

        @Test
        void testAlbumByName() {

            settingsService.setSortAlbumsByYear(false);

            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            MediaFile artist = artists.get(0);

            List<MediaFile> albums = mediaFileProc.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());
            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(albums.stream().map(a -> a.getName()).collect(Collectors.toList())));

        }

        @Test
        void testAlbumByYear() {

            // The result change depending on the setting
            settingsService.setSortAlbumsByYear(true);
            settingsService.save();
            List<String> reversedByYear = new ArrayList<>(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST);
            Collections.reverse(reversedByYear);

            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            MediaFile artist = artists.get(0);

            List<MediaFile> albums = mediaFileProc.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());
            assertEquals(reversedByYear, albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

        }

        @Test
        void testSongs() {

            settingsService.setSortAlbumsByYear(false);

            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());

            MediaFile artist = artists.get(0);
            assertEquals("20", artist.getName());

            List<MediaFile> albums = mediaFileProc.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(1, albums.size());

            MediaFile album = albums.get(0);
            assertEquals("ALBUM", album.getName()); // the case where album name is different between file and id3

            List<MediaFile> songs = mediaFileProc.getChildren(album, 0, Integer.MAX_VALUE);
            assertEquals(1, songs.size());

            MediaFile song = songs.get(0);
            assertEquals("empty", song.getName());
        }

        @Test
        void testToBrowseResult() {

            List<MediaFile> artists = mediaFileProc.getDirectChildren(0, 100).stream()
                    .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
            MediaFile artist = artists.get(0);
            List<MediaFile> albums = mediaFileProc.getChildren(artist, 0, Integer.MAX_VALUE);
            MediaFile album = albums.get(0);
            List<MediaFile> songs = mediaFileProc.getChildren(album, 0, Integer.MAX_VALUE);
            MediaFile song = songs.get(0);

            ParamSearchResult<MediaFile> searchResult = new ParamSearchResult<>();
            searchResult.getItems().add(song);

            BrowseResult browseResult = mediaFileProc.toBrowseResult(searchResult);
            assertTrue(browseResult.getResult().startsWith("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                    xmlns:dc="http://purl.org/dc/elements/1.1/" \
                    xmlns:sec="http://www.sec.co.kr/" \
                    xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                    <item \
                    """));
            // ... id="***" parentID="***"
            assertTrue(browseResult.getResult().contains("""
                     restricted="1">\
                    <dc:title>empty</dc:title>\
                    <upnp:class>object.item.audioItem.musicTrack</upnp:class>\
                    <upnp:album>AlBum!</upnp:album>\
                    <upnp:artist>20</upnp:artist>\
                    <upnp:originalTrackNumber/>\
                    <upnp:albumArtURI>\
                    """));
            // ... https://192.168.1.1:4040/ext/coverArt.view?
            // id=al-0&amp;size=300&amp;jwt=****** ...
            assertTrue(browseResult.getResult().contains("""
                    </upnp:albumArtURI>\
                    <dc:description/>\
                    <res protocolInfo="http-get:*:audio/mpeg:*" size="13579">\
                    """));
            // ... https://192.168.1.1:4040/ext/stream?
            // id=***&amp;player=***&amp;jwt=*** ...
            assertTrue(browseResult.getResult().endsWith("""
                    </res>\
                    </item>\
                    </DIDL-Lite>\
                    """));
        }
    }
}
