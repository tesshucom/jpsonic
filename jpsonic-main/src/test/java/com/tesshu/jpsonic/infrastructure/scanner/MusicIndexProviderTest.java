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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.MusicFolderContent;
import com.tesshu.jpsonic.domain.model.MusicIndex;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.infrastructure.language.I18nSKeys;
import com.tesshu.jpsonic.infrastructure.language.JapaneseReadingUtils;
import com.tesshu.jpsonic.infrastructure.language.MetadataReadingProcessor;
import com.tesshu.jpsonic.infrastructure.scanner.MusicIndexProviderImpl.MusicIndexParser;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class MusicIndexProviderTest {

    private SettingsFacade settingsFacade;
    private MusicIndexProviderImpl musicIndexProvider;
    private MediaFileProvider mediaFileProvider;
    private ArtistProvider artistProvider;

    private static final String INDEX_STRING = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ)";
    private static final String IGNORED_ARTICLES = "The El La Las Le Les";

    @BeforeEach
    void setup() {
        mediaFileProvider = mock(MediaFileProvider.class);
        artistProvider = mock(ArtistProvider.class);
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(SKeys.general.index.indexString, INDEX_STRING)
            .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(I18nSKeys.localeVariant, "")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.NATIVE_JAPANESE.name())
            .build();
        init();
    }

    @Ignore
    void init() {
        MetadataReadingProcessor proc = new MetadataReadingProcessor(settingsFacade,
                new JapaneseReadingUtils(settingsFacade));
        musicIndexProvider = new MusicIndexProviderImpl(mediaFileProvider, artistProvider,
                settingsFacade, proc);
    }

    @Test
    void testGetMusicFolderContent() {
        MediaFile artist1 = new MediaFile(1, "path1", 0, "format", "DIRECTORY", 256, 60, 9999,
                "artist", "album", "The Flipper's Guitar", "albumArtist", 0, "genre", 2026,
                "thumbUri", "composer", "reading", "F", "comment");
        MediaFile artist2 = new MediaFile(1, "path2", 0, "format", "DIRECTORY", 256, 60, 9999,
                "artist", "album", "abcde", "albumArtist", 0, "genre", 2026, "thumbUri", "composer",
                "reading", "A", "comment");
        List<MediaFile> artists = Arrays.asList(artist1, artist2);
        Mockito.when(mediaFileProvider.findIndexedDirectories(anyList())).thenReturn(artists);
        MediaFile song = new MediaFile(1, "path3", 0, "format", "MUSIC", 256, 60, 9999, "artist",
                "album", "It's file directly under the music folder", "albumArtist", 0, "genre",
                2026, "thumbUri", "composer", "reading", "F", "comment");

        List<MediaFile> songs = Arrays.asList(song);
        Mockito
            .when(mediaFileProvider
                .findChildren(anyList(), anyLong(), anyLong(),
                        any(new MediaFile.Type[0].getClass())))
            .thenReturn(songs);

        MusicFolder folder = new MusicFolder(0, "path", "name", true, now(), 0, false);
        MusicFolderContent content = musicIndexProvider
            .findMusicFolderContent(Arrays.asList(folder));
        assertEquals(2, content.indexedArtists().size());
        Iterator<MusicIndex> iterator = content.indexedArtists().keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.index());
        assertEquals("path2", content.indexedArtists().get(musicIndex).get(0).name());
        assertEquals("abcde", content.indexedArtists().get(musicIndex).get(0).title());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.index());
        assertEquals("path1", content.indexedArtists().get(musicIndex).get(0).name());
        assertEquals("The Flipper's Guitar",
                content.indexedArtists().get(musicIndex).get(0).title());

        assertEquals(1, content.singleSongs().size());
        assertEquals("It's file directly under the music folder",
                content.singleSongs().get(0).title());
    }

    @Test
    void testindexedId3Artists() {
        Artist artist1 = new Artist(0, "The Flipper's Guitar", "path", 3, 0, "reading", 0, "F");
        Artist artist2 = new Artist(1, "abcde", "path", 3, 0, "reading", 0, "A");
        List<Artist> artists = Arrays.asList(artist1, artist2);
        List<MusicFolder> folders = Collections.emptyList();
        Mockito.when(artistProvider.findArtists(folders, 0, Integer.MAX_VALUE)).thenReturn(artists);

        SortedMap<MusicIndex, List<Artist>> indexedArtists = musicIndexProvider
            .findIndexedId3Artists(folders);
        assertEquals(2, indexedArtists.size());
        Iterator<MusicIndex> iterator = indexedArtists.keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.index());
        assertEquals("abcde", indexedArtists.get(musicIndex).get(0).name());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.index());
        assertEquals("The Flipper's Guitar", indexedArtists.get(musicIndex).get(0).name());
    }

    @Nested
    class MusicIndexParserTest {

        @Nested
        class CreateIndexesFromExpressionTest {

            @Test
            void testCreateIndexesFromSingleTokenExpression() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "A")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();
                List<MusicIndex> indexes = musicIndexProvider.getParser().getIndexes();
                assertEquals(1, indexes.size());
                MusicIndex index = indexes.get(0);
                assertEquals("A", index.index());
                assertEquals(1, index.prefixes().size());
                assertEquals("A", index.prefixes().iterator().next());

                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "The")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();
                musicIndexProvider.invalidate();
                indexes = musicIndexProvider.getParser().getIndexes();
                assertEquals(1, indexes.size());
                index = indexes.get(0);
                assertEquals("The", index.index());
                assertEquals(1, indexes.size());
                assertEquals(1, index.prefixes().size());
                assertEquals("The", index.prefixes().iterator().next());

                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "X-Z(XYZ)")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();
                musicIndexProvider.invalidate();
                indexes = musicIndexProvider.getParser().getIndexes();
                assertEquals(1, indexes.size());
                index = indexes.get(0);
                assertEquals("X-Z", index.index());
                assertEquals(3, index.prefixes().size());
                Iterator<String> prefixes = index.prefixes().iterator();
                assertEquals("X", prefixes.next());
                assertEquals("Y", prefixes.next());
                assertEquals("Z", prefixes.next());
            }

            @Test
            void testCreateIndexesFromMultipleTokensExpression() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "A B  The X-Z(XYZ)")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();

                List<MusicIndex> indexes = musicIndexProvider.getParser().getIndexes();
                assertEquals(4, indexes.size());

                assertEquals("A", indexes.get(0).index());
                assertEquals(1, indexes.get(0).prefixes().size());
                assertEquals("A", indexes.get(0).prefixes().iterator().next());

                assertEquals("B", indexes.get(1).index());
                assertEquals(1, indexes.get(1).prefixes().size());
                assertEquals("B", indexes.get(1).prefixes().iterator().next());

                assertEquals("The", indexes.get(2).index());
                assertEquals(1, indexes.get(2).prefixes().size());
                assertEquals("The", indexes.get(2).prefixes().iterator().next());

                assertEquals("X-Z", indexes.get(3).index());
                assertEquals(3, indexes.get(3).prefixes().size());
                Iterator<String> prefixes = indexes.get(3).prefixes().iterator();
                assertEquals("X", prefixes.next());
                assertEquals("Y", prefixes.next());
                assertEquals("Z", prefixes.next());
            }
        }

        @Nested
        class IndexTestWithArtistIndexable {

            @Test
            void testLatin() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "A B C")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();

                MusicIndexParser musicIndexParser = musicIndexProvider.getParser();

                Artist artist = new Artist(0, "Abcde", "path", 3, 0, "Abcde", 0, null);
                assertEquals("A", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "The Beatles", "path", 3, 0, "The Beatles", 0, null);
                assertEquals("B", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "あいうえお", "path", 3, 0, "あいうえお", 0, null);
                assertEquals("#", musicIndexParser.getIndex(artist).index());
            }

            @Test
            void testLatinJapanese() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString,
                            "A B C あ(ア) い(ア) う(ア) え(ア) お(ア) か(カ) き(キ) く(ク) け(ケ) こ(コ) は(ハヒフヘホ)")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();

                MusicIndexParser musicIndexParser = musicIndexProvider.getParser();

                Artist artist = new Artist(0, "Abcde", "path", 3, 0, "Abcde", 0, null);
                assertEquals("A", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "あいうえお", "path", 3, 0, "あいうえお", 0, null);
                assertEquals("あ", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "きくけこ", "path", 3, 0, "きくけこ", 0, null);
                assertEquals("き", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "ぐげご", "path", 3, 0, "ぐげご", 0, null);
                assertEquals("く", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "ビートルズ", "path", 3, 0, "ビートルズ", 0, null);
                assertEquals("は", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "The Beatles", "path", 3, 0, "ビートルズ", 0, null);
                assertEquals("B", musicIndexParser.getIndex(artist).index());
            }

            /*
             * #852. https://wiki.sei.cmu.edu/confluence/display/java/STR02-J.+Specify+an+
             * appropriate+locale+when+ comparing+locale-dependent+data
             */
            @Test
            void testindexSTR02J() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(SKeys.general.index.indexString, "A i ı")
                    .withString(SKeys.general.index.ignoredArticles, IGNORED_ARTICLES)
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(I18nSKeys.localeVariant, "")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.NATIVE_JAPANESE.name())
                    .build();
                init();

                MusicIndexParser musicIndexParser = musicIndexProvider.getParser();

                Artist artist = new Artist(0, "abcde", "path", 3, 0, "abcde", 0, null);
                assertEquals("A", musicIndexParser.getIndex(artist).index());

                artist = new Artist(0, "\u0130", "path", 3, 0, "\u0130", 0, null); // İ İ
                assertEquals("\u0069", musicIndexParser.getIndex(artist).index()); // i

                artist = new Artist(0, "\u0069", "path", 3, 0, "\u0069", 0, null); // i i
                assertEquals("\u0069", musicIndexParser.getIndex(artist).index()); // i

                artist = new Artist(0, "\u0049", "path", 3, 0, "\u0049", 0, null); // I I
                assertEquals("\u0069", musicIndexParser.getIndex(artist).index()); // i

                artist = new Artist(0, "\u0131", "path", 3, 0, "\u0131", 0, null); // ı ı
                assertEquals("\u0069", musicIndexParser.getIndex(artist).index()); // i
            }
        }
    }
}
