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

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.domain.MusicIndex.SortableArtistWithArtist;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexServiceUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.MusicIndexServiceImpl.MusicIndexParser;
import com.tesshu.jpsonic.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link MusicIndex}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class MusicIndexServiceImplTest {

    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private MusicIndexServiceImpl musicIndexService;
    private ArtistDao artistDao;
    private JpsonicComparators comparators;

    @BeforeEach
    public void setup() throws ExecutionException {
        mediaFileService = mock(MediaFileService.class);
        artistDao = mock(ArtistDao.class);
        settingsService = mock(SettingsService.class);
        String ignoredArticles = "The El La Las Le Les";
        Mockito.when(settingsService.getIgnoredArticles()).thenReturn(ignoredArticles);
        Mockito.when(settingsService.getIgnoredArticlesAsArray())
                .thenReturn(Arrays.asList(ignoredArticles.split("\\s+")));
        String indexString = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ)";
        Mockito.when(settingsService.getIndexString()).thenReturn(indexString);
        Mockito.when(settingsService.getLocale()).thenReturn(new Locale("ja", "jp", ""));
        JapaneseReadingUtils readingUtils = new JapaneseReadingUtils(settingsService);
        comparators = new JpsonicComparators(settingsService, readingUtils);
        MusicIndexServiceUtils utils = new MusicIndexServiceUtils(settingsService, mediaFileService, readingUtils,
                comparators);
        musicIndexService = new MusicIndexServiceImpl(settingsService, mediaFileService, artistDao, utils);
    }

    @Test
    void testGetIndexedArtistsListOfMusicFolderBoolean() {
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(new MediaFile());
        MediaFile child1 = new MediaFile();
        child1.setTitle("The Flipper's Guitar");
        child1.setPathString("path1");
        MediaFile child2 = new MediaFile();
        child2.setTitle("abcde");
        child2.setPathString("path2");
        List<MediaFile> children = Arrays.asList(child1, child2);
        Mockito.when(mediaFileService.getChildrenOf(Mockito.any(MediaFile.class), Mockito.anyBoolean(),
                Mockito.anyBoolean())).thenReturn(children);
        MusicFolder folder = new MusicFolder(0, "path", "name", true, now(), 0);

        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> indexedArtists = musicIndexService
                .getMusicFolderContent(Arrays.asList(folder)).getIndexedArtists();
        assertEquals(2, indexedArtists.size());
        Iterator<MusicIndex> iterator = indexedArtists.keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", indexedArtists.get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", indexedArtists.get(musicIndex).get(0).getName());
    }

    @Test
    void testGetIndexedArtistsListOfArtist() {
        Artist artist1 = new Artist(0, "The Flipper's Guitar", null, 0, now(), false, 0, null, null, -1);
        Artist artist2 = new Artist(0, "abcde", null, 0, now(), false, 0, null, null, -1);
        List<Artist> artists = Arrays.asList(artist1, artist2);

        List<MusicFolder> folders = Collections.emptyList();
        Mockito.when(artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders)).thenReturn(artists);

        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> indexedArtists = musicIndexService
                .getIndexedId3Artists(folders);
        assertEquals(2, indexedArtists.size());
        Iterator<MusicIndex> iterator = indexedArtists.keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", indexedArtists.get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", indexedArtists.get(musicIndex).get(0).getName());
    }

    @Test
    void testGetMusicFolderContent() {
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(new MediaFile());
        MediaFile child1 = new MediaFile();
        child1.setTitle("The Flipper's Guitar");
        child1.setPathString("path1");
        MediaFile child2 = new MediaFile();
        child2.setTitle("abcde");
        child2.setPathString("path2");
        List<MediaFile> children = Arrays.asList(child1, child2);
        MediaFile song = new MediaFile();
        song.setTitle("It's file directly under the music folder");
        song.setPathString("path3");
        List<MediaFile> songs = Arrays.asList(song);
        Mockito.when(mediaFileService.getChildrenOf(Mockito.any(MediaFile.class), Mockito.anyBoolean(),
                Mockito.anyBoolean())).thenReturn(children).thenReturn(songs).thenThrow(new RuntimeException("Fail"));
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(new MediaFile());
        MusicFolder folder = new MusicFolder(0, "path", "name", true, now(), 0);

        MusicFolderContent content = musicIndexService.getMusicFolderContent(Arrays.asList(folder));
        assertEquals(2, content.getIndexedArtists().size());
        Iterator<MusicIndex> iterator = content.getIndexedArtists().keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", content.getIndexedArtists().get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", content.getIndexedArtists().get(musicIndex).get(0).getName());

        assertEquals(1, content.getSingleSongs().size());
        assertEquals("It's file directly under the music folder", content.getSingleSongs().get(0).getTitle());
    }

    @Test
    void testGetSingleSongs() throws URISyntaxException {
        Path path = Path.of(MusicIndexServiceImplTest.class.getResource("/MEDIAS").toURI());
        MusicFolder musicFolder = new MusicFolder(path.toString(), "musicFolder", false, null);
        assertEquals(path, musicFolder.toPath());

        final List<MusicFolder> musicFolders = Arrays.asList(musicFolder);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(0);
        mediaFile.setPathString(path.toString());

        Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(null);
        musicIndexService.getSingleSongs(musicFolders);
        Mockito.verify(mediaFileService, Mockito.never()).getChildrenOf(mediaFile, true, false);

        Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(mediaFile);
        musicIndexService.getSingleSongs(musicFolders);
        Mockito.verify(mediaFileService, Mockito.times(1)).getChildrenOf(mediaFile, true, false);
    }

    @Test
    void testGetShortcuts() throws URISyntaxException {
        Mockito.when(settingsService.getShortcutsAsArray())
                .thenReturn(StringUtil.split("Shortcuts \"New Incoming\" Podcast Metadata"));
        MusicFolder folder = new MusicFolder(
                Path.of(MusicIndexServiceImplTest.class.getResource("/MEDIAS/Music").toURI()).toString(), "Music", true,
                now());
        assertEquals(0, musicIndexService.getShortcuts(Arrays.asList(folder)).size());

        MediaFile artist = new MediaFile();
        artist.setPathString("path");
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(artist);
        assertEquals(0, musicIndexService.getShortcuts(Arrays.asList(folder)).size());

        artist.setPathString(
                Path.of(MusicIndexServiceImplTest.class.getResource("/MEDIAS/Music/_DIR_ Ravel").toURI()).toString());
        assertEquals(0, musicIndexService.getShortcuts(Arrays.asList(folder)).size());

        List<MediaFile> children = Arrays.asList(new MediaFile());
        Mockito.when(mediaFileService.getChildrenOf(artist, true, true)).thenReturn(children);
        assertEquals(1, musicIndexService.getShortcuts(Arrays.asList(folder)).size());
    }

    @Nested
    class MusicIndexParserTest {

        @Nested
        class CreateIndexesFromExpressionTest {

            @Test
            void testCreateIndexesFromSingleTokenExpression() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A");
                List<MusicIndex> indexes = musicIndexService.getParser().getIndexes();
                assertEquals(1, indexes.size());
                MusicIndex index = indexes.get(0);
                assertEquals("A", index.getIndex());
                assertEquals(1, index.getPrefixes().size());
                assertEquals("A", index.getPrefixes().get(0));

                Mockito.when(settingsService.getIndexString()).thenReturn("The");
                musicIndexService.clear();
                indexes = musicIndexService.getParser().getIndexes();
                assertEquals(1, indexes.size());
                index = indexes.get(0);
                assertEquals("The", index.getIndex());
                assertEquals(1, indexes.size());
                assertEquals(1, index.getPrefixes().size());
                assertEquals("The", index.getPrefixes().get(0));

                Mockito.when(settingsService.getIndexString()).thenReturn("X-Z(XYZ)");
                musicIndexService.clear();
                indexes = musicIndexService.getParser().getIndexes();
                assertEquals(1, indexes.size());
                index = indexes.get(0);
                assertEquals("X-Z", index.getIndex());
                assertEquals(3, index.getPrefixes().size());
                assertEquals("X", index.getPrefixes().get(0));
                assertEquals("Y", index.getPrefixes().get(1));
                assertEquals("Z", index.getPrefixes().get(2));
            }

            @Test
            void testCreateIndexesFromMultipleTokensExpression() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A B  The X-Z(XYZ)");
                List<MusicIndex> indexes = musicIndexService.getParser().getIndexes();
                assertEquals(4, indexes.size());

                assertEquals("A", indexes.get(0).getIndex());
                assertEquals(1, indexes.get(0).getPrefixes().size());
                assertEquals("A", indexes.get(0).getPrefixes().get(0));

                assertEquals("B", indexes.get(1).getIndex());
                assertEquals(1, indexes.get(1).getPrefixes().size());
                assertEquals("B", indexes.get(1).getPrefixes().get(0));

                assertEquals("The", indexes.get(2).getIndex());
                assertEquals(1, indexes.get(2).getPrefixes().size());
                assertEquals("The", indexes.get(2).getPrefixes().get(0));

                assertEquals("X-Z", indexes.get(3).getIndex());
                assertEquals(3, indexes.get(3).getPrefixes().size());
                assertEquals("X", indexes.get(3).getPrefixes().get(0));
                assertEquals("Y", indexes.get(3).getPrefixes().get(1));
                assertEquals("Z", indexes.get(3).getPrefixes().get(2));
            }
        }

        @Nested
        class GetIndexTest {

            @SuppressWarnings("deprecation")
            @Test
            void testUsual() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A B C");
                MusicIndexParser musicIndexParser = musicIndexService.getParser();

                SortableArtistWithArtist saIndexed = new SortableArtistWithArtist("Abcde", "Abcde", null,
                        comparators.sortableArtistOrder());
                assertEquals("A", musicIndexParser.getIndex(saIndexed).getIndex());

                SortableArtistWithArtist saOthers = new SortableArtistWithArtist("あいうえお", "あいうえお", null,
                        comparators.sortableArtistOrder());
                assertEquals("#", musicIndexParser.getIndex(saOthers).getIndex());
            }

            /*
             * #852. https://wiki.sei.cmu.edu/confluence/display/java/STR02-J.+Specify+an+appropriate+locale+when+
             * comparing+locale-dependent+data
             */
            @SuppressWarnings("deprecation")
            @Test
            void testGetIndexSTR02J() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A i ı");
                MusicIndexParser musicIndexParser = musicIndexService.getParser();

                SortableArtistWithArtist sa1 = new SortableArtistWithArtist("abcde", "abcde", null,
                        comparators.sortableArtistOrder());
                assertEquals("A", musicIndexParser.getIndex(sa1).getIndex());

                SortableArtistWithArtist sa2 = new SortableArtistWithArtist("\u0130", "\u0130", // İ İ
                        null, comparators.sortableArtistOrder());
                assertEquals("\u0069", musicIndexParser.getIndex(sa2).getIndex()); // i

                SortableArtistWithArtist sa3 = new SortableArtistWithArtist("\u0069", "\u0069", // i i
                        null, comparators.sortableArtistOrder());
                assertEquals("\u0069", musicIndexParser.getIndex(sa3).getIndex()); // i

                SortableArtistWithArtist sa4 = new SortableArtistWithArtist("\u0049", "\u0049", // I I
                        null, comparators.sortableArtistOrder());
                assertEquals("\u0069", musicIndexParser.getIndex(sa4).getIndex()); // i

                SortableArtistWithArtist sa5 = new SortableArtistWithArtist("\u0131", "\u0131", // ı ı
                        null, comparators.sortableArtistOrder());
                assertEquals("\u0069", musicIndexParser.getIndex(sa5).getIndex()); // i
            }
        }
    }
}
