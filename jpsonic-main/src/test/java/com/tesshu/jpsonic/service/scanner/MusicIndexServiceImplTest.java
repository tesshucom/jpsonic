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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

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
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.service.MediaFileService;
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
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports", "PMD.InstantiationToGetClass" })
class MusicIndexServiceImplTest {

    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private MusicIndexServiceImpl musicIndexService;
    private ArtistDao artistDao;

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
        musicIndexService = new MusicIndexServiceImpl(settingsService, mediaFileService, artistDao, readingUtils);
    }

    @Test
    void testGetMusicFolderContent() {
        Mockito.when(mediaFileService.getMediaFile(any(Path.class))).thenReturn(new MediaFile());
        MediaFile artist1 = new MediaFile();
        artist1.setTitle("The Flipper's Guitar");
        artist1.setPathString("path1");
        artist1.setMusicIndex("F");
        MediaFile artist2 = new MediaFile();
        artist2.setTitle("abcde");
        artist2.setPathString("path2");
        artist2.setMusicIndex("A");
        List<MediaFile> artists = Arrays.asList(artist1, artist2);
        Mockito.when(mediaFileService.getIndexedDirs(anyList())).thenReturn(artists);

        MediaFile song = new MediaFile();
        song.setTitle("It's file directly under the music folder");
        song.setPathString("path3");
        List<MediaFile> songs = Arrays.asList(song);
        Mockito.when(
                mediaFileService.getDirectChildFiles(anyList(), anyLong(), anyLong(), any(new MediaType[0].getClass())))
                .thenReturn(songs);

        MusicFolder folder = new MusicFolder(0, "path", "name", true, now(), 0, false);
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
    void testGetIndexedId3Artists() {
        Artist artist1 = new Artist(0, "The Flipper's Guitar", null, 0, now(), false, 0, null, null, -1, "");
        artist1.setMusicIndex("F");
        Artist artist2 = new Artist(0, "abcde", null, 0, now(), false, 0, null, null, -1, null);
        artist2.setMusicIndex("A");
        List<Artist> artists = Arrays.asList(artist1, artist2);

        List<MusicFolder> folders = Collections.emptyList();
        Mockito.when(artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders)).thenReturn(artists);

        SortedMap<MusicIndex, List<Artist>> indexedArtists = musicIndexService.getIndexedId3Artists(folders);
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
    void testGetShortcuts() throws URISyntaxException {
        Mockito.when(settingsService.getShortcutsAsArray())
                .thenReturn(StringUtil.split("Shortcuts \"New Incoming\" Podcast Metadata"));
        MusicFolder folder = new MusicFolder(
                Path.of(MusicIndexServiceImplTest.class.getResource("/MEDIAS/Music").toURI()).toString(), "Music", true,
                now(), false);
        assertEquals(0, musicIndexService.getShortcuts(Arrays.asList(folder)).size());

        MediaFile artist = new MediaFile();
        artist.setPathString("path");
        Mockito.when(mediaFileService.getMediaFile(any(Path.class))).thenReturn(artist);
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
        class GetIndexTestWithArtistIndexable {

            @Test
            void testLatin() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A B C");
                MusicIndexParser musicIndexParser = musicIndexService.getParser();

                Artist artist = new Artist();
                artist.setName("Abcde");
                artist.setReading("Abcde");
                assertEquals("A", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("The Beatles");
                artist.setReading("The Beatles");
                assertEquals("B", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("あいうえお");
                artist.setReading("あいうえお");
                assertEquals("#", musicIndexParser.getIndex(artist).getIndex());
            }

            @Test
            void testLatinJapanese() {
                Mockito.when(settingsService.getIndexString())
                        .thenReturn("A B C あ(ア) い(ア) う(ア) え(ア) お(ア) か(カ) き(キ) く(ク) け(ケ) こ(コ) は(ハヒフヘホ)");
                MusicIndexParser musicIndexParser = musicIndexService.getParser();

                Artist artist = new Artist();
                artist.setName("Abcde");
                artist.setReading("Abcde");
                assertEquals("A", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("あいうえお");
                artist.setReading("あいうえお");
                assertEquals("あ", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("きくけこ");
                artist.setReading("きくけこ");
                assertEquals("き", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("ぐげご");
                artist.setReading("ぐげご");
                assertEquals("く", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("ビートルズ");
                artist.setReading("ビートルズ");
                assertEquals("は", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("The Beatles");
                artist.setReading("ビートルズ");
                assertEquals("B", musicIndexParser.getIndex(artist).getIndex());
            }

            /*
             * #852. https://wiki.sei.cmu.edu/confluence/display/java/STR02-J.+Specify+an+appropriate+locale+when+
             * comparing+locale-dependent+data
             */
            @Test
            void testGetIndexSTR02J() {
                Mockito.when(settingsService.getIndexString()).thenReturn("A i ı");
                MusicIndexParser musicIndexParser = musicIndexService.getParser();

                Artist artist = new Artist();
                artist.setName("abcde");
                artist.setReading("abcde");
                assertEquals("A", musicIndexParser.getIndex(artist).getIndex());

                artist.setName("\u0130"); // İ
                artist.setReading("\u0130"); // İ
                assertEquals("\u0069", musicIndexParser.getIndex(artist).getIndex()); // i

                artist.setName("\u0069"); // i
                artist.setReading("\u0069"); // i
                assertEquals("\u0069", musicIndexParser.getIndex(artist).getIndex()); // i

                artist.setName("\u0049"); // I
                artist.setReading("\u0049"); // I
                assertEquals("\u0069", musicIndexParser.getIndex(artist).getIndex()); // i

                artist.setName("\u0131"); // ı
                artist.setReading("\u0131"); // ı
                assertEquals("\u0069", musicIndexParser.getIndex(artist).getIndex()); // i
            }
        }
    }
}
