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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JMediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

class MediaFileUpnpProcessorTest {

    @Nested
    class UnitTest {

        private UpnpProcessorUtil upnpProcessorUtil;
        private JMediaFileService mediaFileService;
        private MediaFileUpnpProcessor mediaFileUpnpProcessor;

        @BeforeEach
        public void setup() {
            upnpProcessorUtil = mock(UpnpProcessorUtil.class);
            mediaFileService = mock(JMediaFileService.class);
            mediaFileUpnpProcessor = new MediaFileUpnpProcessor(mock(UpnpProcessDispatcher.class), upnpProcessorUtil,
                    mediaFileService, mock(PlayerService.class));
        }

        @Test
        void testGetItems() throws URISyntaxException {

            // If there is a single music folder, the first level is not the music folder, but the
            // child (artist) of the music folder.
            Path musicFolderPath1 = Path.of(MediaFileUpnpProcessorTest.class.getResource("/MEDIAS/Music").toURI());
            MediaFile folder1 = new MediaFile();
            folder1.setMediaType(MediaType.DIRECTORY);
            Mockito.when(mediaFileService.getMediaFile(musicFolderPath1)).thenReturn(folder1);

            Mockito.when(upnpProcessorUtil.getGuestMusicFolders()).thenReturn(
                    Arrays.asList(new MusicFolder(0, musicFolderPath1.toString(), "Music", true, new Date())));
            List<MediaFile> result = mediaFileUpnpProcessor.getItems(0, Integer.MAX_VALUE);
            Mockito.verify(mediaFileService, Mockito.times(1)).getMediaFile(Mockito.any(Path.class));
            assertEquals(0, result.size());
            Mockito.clearInvocations(mediaFileService);

            // If there are multiple Music folders, the first level is the Music folder.
            Path musicFolderPath2 = Path.of(MediaFileUpnpProcessorTest.class.getResource("/MEDIAS/Music2").toURI());
            MediaFile folder2 = new MediaFile();
            folder2.setMediaType(MediaType.DIRECTORY);
            Mockito.when(mediaFileService.getMediaFile(musicFolderPath2)).thenReturn(folder2);

            Mockito.when(upnpProcessorUtil.getGuestMusicFolders()).thenReturn(
                    Arrays.asList(new MusicFolder(0, musicFolderPath1.toString(), "Music1", true, new Date()),
                            new MusicFolder(1, musicFolderPath2.toString(), "Music2", true, new Date())));
            result = mediaFileUpnpProcessor.getItems(0, Integer.MAX_VALUE);
            Mockito.verify(mediaFileService, Mockito.times(2)).getMediaFile(Mockito.any(Path.class));
            assertEquals(2, result.size());
        }
    }

    @SpringBootTest
    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private MediaFileUpnpProcessor mediaFileUpnpProcessor;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() throws URISyntaxException {

            musicFolders = Arrays.asList(new MusicFolder(1,
                    Path.of(MediaFileUpnpProcessorTest.class.getResource("/MEDIAS/Sort/Pagination/Artists").toURI())
                            .toString(),
                    "Artists", true, new Date()));

            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetItemCount() {
            // 31 + 22(topnodes)
            assertEquals(53, mediaFileUpnpProcessor.getItemCount());
        }

        @Test
        void testGetItems() {

            List<MediaFile> items = mediaFileUpnpProcessor.getItems(0, 10);
            assertEquals(10, items.size());

            items = mediaFileUpnpProcessor.getItems(10, 10);
            assertEquals(10, items.size());

            items = mediaFileUpnpProcessor.getItems(20, 100);
            assertEquals(33, items.size());

            items = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> !a.getName().startsWith("single"))
                    .collect(Collectors.toList());
            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(items.stream().map(MediaFile::getName).collect(Collectors.toList())));
        }

        @Test
        void testGetChildSizeOf() {
            List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());
            assertEquals(31, mediaFileUpnpProcessor.getChildSizeOf(artists.get(0)));
        }

        @Test
        void testgetChildren() {

            List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            List<MediaFile> children = mediaFileUpnpProcessor.getChildren(artists.get(0), 0, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i), children.get(i).getName());
            }

            children = mediaFileUpnpProcessor.getChildren(artists.get(0), 10, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 10), children.get(i).getName());
            }

            children = mediaFileUpnpProcessor.getChildren(artists.get(0), 20, 100);
            assertEquals(11, children.size());
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 20), children.get(i).getName());
            }

        }

        @Test
        void testAlbum() {

            settingsService.setSortAlbumsByYear(false);

            List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            MediaFile artist = artists.get(0);

            List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());
            assertTrue(UpnpProcessorTestUtils
                    .validateJPSonicNaturalList(albums.stream().map(a -> a.getName()).collect(Collectors.toList())));

        }

        @Test
        void testAlbumByYear() {

            // The result change depending on the setting
            settingsService.setSortAlbumsByYear(true);
            List<String> reversedByYear = new ArrayList<>(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST);
            Collections.reverse(reversedByYear);

            List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream()
                    .filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            MediaFile artist = artists.get(0);

            List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(31, albums.size());
            assertEquals(reversedByYear, albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

        }

        @Test
        void testSongs() {

            settingsService.setSortAlbumsByYear(false);

            List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream()
                    .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
            assertEquals(1, artists.size());

            MediaFile artist = artists.get(0);
            assertEquals("20", artist.getName());

            List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(1, albums.size());

            MediaFile album = albums.get(0);
            assertEquals("ALBUM", album.getName()); // the case where album name is different between file and id3

            List<MediaFile> songs = mediaFileUpnpProcessor.getChildren(album, 0, Integer.MAX_VALUE);
            assertEquals(1, songs.size());

            MediaFile song = songs.get(0);
            assertEquals("empty", song.getName());

        }
    }

}
