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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.service.SettingsService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = "com.tesshu.jpsonic")
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class JapaneseReadingUtilsTest {

    @Mock
    private SettingsService settingsService;

    private JapaneseReadingUtils utils;

    @BeforeEach
    public void setup() throws ExecutionException {
        String articles = "The El La Las Le Les";
        Mockito.when(settingsService.getIgnoredArticles()).thenReturn(articles);
        Mockito.when(settingsService.getIgnoredArticlesAsArray()).thenReturn(articles.split("\\s+"));
        Mockito.when(settingsService.isIndexEnglishPrior()).thenReturn(true);
        String language = "ja";
        String country = "jp";
        String variant = "";
        Mockito.when(settingsService.getLocale()).thenReturn(new Locale(language, country, variant));
        utils = new JapaneseReadingUtils(settingsService);
    }

    @AfterEach
    public void afterAll() throws ExecutionException {
        utils.clear();
    }

    @Test
    void testIsPunctuation() {
        assertFalse(JapaneseReadingUtils.isPunctuation('a'));
        assertTrue(JapaneseReadingUtils.isPunctuation('*'));
        assertTrue(JapaneseReadingUtils.isPunctuation('+'));
        assertTrue(JapaneseReadingUtils.isPunctuation('-'));
        assertTrue(JapaneseReadingUtils.isPunctuation(' '));
        assertTrue(JapaneseReadingUtils.isPunctuation('\n'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\t'));
        assertTrue(JapaneseReadingUtils.isPunctuation(','));
        assertTrue(JapaneseReadingUtils.isPunctuation('.'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\''));
        assertTrue(JapaneseReadingUtils.isPunctuation('`'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\"'));
        assertTrue(JapaneseReadingUtils.isPunctuation('。'));
        assertTrue(JapaneseReadingUtils.isPunctuation('、'));
        assertTrue(JapaneseReadingUtils.isPunctuation('('));
        assertTrue(JapaneseReadingUtils.isPunctuation(')'));
        assertTrue(JapaneseReadingUtils.isPunctuation('['));
        assertTrue(JapaneseReadingUtils.isPunctuation(']'));
        assertTrue(JapaneseReadingUtils.isPunctuation('【'));
        assertTrue(JapaneseReadingUtils.isPunctuation('】'));
    }

    @Test
    void testAnalyzeGenre() {
        String name = "現代邦楽";
        String reading = "ゲンダイホウガク";
        Genre genre = new Genre(name);
        utils.analyze(genre);
        assertEquals(name, genre.getName());
        assertEquals(reading, genre.getReading());
    }

    @Documented
    private @interface AnalyzeMediaFileDecisions {
        @interface Conditions {
            @interface Name {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Reading {
                @interface Null {
                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface NotNull {
                }
            }
        }

        @interface Result {
            @interface Name {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Reading {
                @interface Null {
                }

                @interface NotNull {
                    @interface ReadingExpected {
                    }

                    @interface ReadingAnalyzed {
                    }
                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface NotNull {
                    @interface SortExpected {

                    }
                }
            }
        }
    }

    @Nested
    class AnalyzeMediaFile {

        // name
        private final String nameRaw = "The 「あいｱｲ・愛」";
        // So-called sort tag
        private final String sortRaw = "あいあい・あい";

        private final String readingExpected = "アイアイ・アイ";
        private final String sortExpected = "あいあい・あい";

        /*
         * Character string when parsed. Search system analysis and filter specifications exist separately. Note that
         * the analysis at this point does not over-cleanse so as not to affect the morphological analysis
         * specifications when creating the search index.
         */
        private final String readingAnalyzed = "｢アイアイ・アイ｣";

        private MediaFile toMediaFile(String artist, String artistSort) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setArtist(artist);
            mediaFile.setArtistSort(artistSort);
            return mediaFile;
        }

        @AnalyzeMediaFileDecisions.Conditions.Name.Null
        @AnalyzeMediaFileDecisions.Conditions.Reading.Null
        @AnalyzeMediaFileDecisions.Conditions.Sort.Null
        @AnalyzeMediaFileDecisions.Result.Name.Null
        @AnalyzeMediaFileDecisions.Result.Reading.Null
        @AnalyzeMediaFileDecisions.Result.Sort.Null
        @Test
        /*
         * If there is no tag.
         */
        void a01() {
            MediaFile mediaFile = toMediaFile(null, null);
            utils.analyze(mediaFile);
            assertNull(mediaFile.getArtist());
            assertNull(mediaFile.getArtistReading());
            assertNull(mediaFile.getArtistSort());
        }

        @AnalyzeMediaFileDecisions.Conditions.Name.Null
        @AnalyzeMediaFileDecisions.Conditions.Reading.Null
        @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
        @AnalyzeMediaFileDecisions.Result.Name.NotNull
        @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingExpected
        @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
        @Test
        /*
         * If it is rare but only the sort tag is registered. Even if the name is not registered, the name will not be
         * created from the sort tag. The user should not overlook that the tags are in an irregular state.
         */
        void a02() {
            MediaFile mediaFile = toMediaFile(null, sortRaw);
            utils.analyze(mediaFile);
            assertNull(mediaFile.getArtist());
            assertNotNull(mediaFile.getArtistReading());
            assertEquals(readingExpected, mediaFile.getArtistReading());
            assertNotNull(mediaFile.getArtistSort());
            assertEquals(sortExpected, mediaFile.getArtistSort());
        }

        @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
        @AnalyzeMediaFileDecisions.Conditions.Reading.Null
        @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
        @AnalyzeMediaFileDecisions.Result.Name.NotNull
        @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
        @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
        @Test
        /*
         * For Japanese, it is desirable that both name/sort-tag are entered.
         */
        void a03() {
            MediaFile mediaFile = toMediaFile(nameRaw, sortRaw);
            utils.analyze(mediaFile);
            assertNotNull(mediaFile.getArtist());
            assertNotNull(mediaFile.getArtistReading());
            assertEquals(readingExpected, mediaFile.getArtistReading());
            assertNotNull(mediaFile.getArtistSort());
            assertEquals(sortExpected, mediaFile.getArtistSort());
        }

        @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
        @AnalyzeMediaFileDecisions.Conditions.Reading.Null
        @AnalyzeMediaFileDecisions.Conditions.Sort.Null
        @AnalyzeMediaFileDecisions.Result.Name.NotNull
        @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
        @AnalyzeMediaFileDecisions.Result.Sort.Null
        @Test
        /*
         * For names only.Tentatively analyze readings. (In the case of Japanese, 100% correct answer is impossible, so
         * comparison and correction merging are performed after all data scans.) Sort is null, but may be complemented
         * later.
         */
        void a04() {
            MediaFile mediaFile = toMediaFile(nameRaw, null);
            utils.analyze(mediaFile);
            assertNotNull(mediaFile.getArtist());
            assertNotNull(mediaFile.getArtistReading());
            assertEquals(readingAnalyzed, mediaFile.getArtistReading());
            assertNull(mediaFile.getArtistSort());
        }
    }

    @Test
    void testAnalyzePlaylist() {
        final String name = "2021/07/21 22:40 お気に入り";
        final String reading = "2021/07/21 22:40 オキニイリ";
        Playlist playlist = new Playlist();
        playlist.setName(name);
        utils.analyze(playlist);
        assertEquals(name, playlist.getName());
        assertEquals(reading, playlist.getReading());
    }

    @Documented
    private @interface AnalyzeSortCandidateDecisions {
        @interface Conditions {
            @interface SortCandidate {
                @interface Name {
                    @interface NotNull {
                    }
                }

                @interface Sort {
                    @interface Null {
                    }

                    @interface NotNull {
                    }
                }
            }
        }

        @interface Result {
            @interface SortCandidate {
                @interface Reading {
                    @interface NotNull {
                        @interface ReadingExpected {
                        }

                        @interface ReadingAnalyzed {
                        }
                    }
                }

                @interface Sort {
                    @interface NotNull {
                        @interface ReadingAnalyzed {
                        }

                        @interface SortExpected {
                        }

                    }
                }
            }
        }
    }

    @Nested
    class AnalyzeSortCandidate {

        private final String nameRaw = "The 「あいｱｲ・愛」";
        private final String sortRaw = "あいあい・あい";
        private final String readingExpected = "アイアイ・アイ";
        private final String sortExpected = "あいあい・あい";
        private final String readingAnalyzed = "｢アイアイ・アイ｣";

        @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
        @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.Null
        @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingAnalyzed
        @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.ReadingAnalyzed
        @Test
        void a01() {
            SortCandidate candidate = new SortCandidate(nameRaw, null);
            utils.analyze(candidate);
            assertEquals(nameRaw, candidate.getName());
            assertEquals(readingAnalyzed, candidate.getReading());
            assertEquals(readingAnalyzed, candidate.getSort());
        }

        @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
        @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
        @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
        @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
        @Test
        void a02() {
            SortCandidate candidate = new SortCandidate(nameRaw, sortRaw);
            utils.analyze(candidate);
            assertEquals(nameRaw, candidate.getName());
            assertEquals(readingExpected, candidate.getReading());
            assertEquals(sortExpected, candidate.getSort());
        }
    }

    @Nested
    class IsJapaneseReadable {

        private boolean isJapaneseReadable(@NonNull String s) throws ExecutionException {
            Method method;
            try {
                method = utils.getClass().getDeclaredMethod("isJapaneseReadable", String.class);
                method.setAccessible(true);
                return (Boolean) method.invoke(utils, s);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        void testIsJapaneseReadable() throws ExecutionException {

            assertTrue(isJapaneseReadable("abc あいう"));
            assertTrue(isJapaneseReadable("あいうえお"));
            assertTrue(isJapaneseReadable("アイウエオ"));
            assertTrue(isJapaneseReadable("ァィゥェォ"));
            assertTrue(isJapaneseReadable("ｧｨｩｪｫ"));
            assertTrue(isJapaneseReadable("ｱｲｳｴｵ"));
            assertTrue(isJapaneseReadable("亜伊鵜絵尾"));
            assertTrue(isJapaneseReadable("ＡＢＣＤＥ"));
            assertTrue(isJapaneseReadable("αβγ"));
            assertTrue(isJapaneseReadable("つんく♂"));
            assertTrue(isJapaneseReadable("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
            assertTrue(isJapaneseReadable("犬とネコ"));
            assertTrue(isJapaneseReadable("読み"));
            assertTrue(isJapaneseReadable("(読み)"));
            assertTrue(isJapaneseReadable("　「」（）()［］[]；！!？?＃#１２３"));
            assertTrue(isJapaneseReadable("コンピューター"));
            assertTrue(isJapaneseReadable("あい～うえ"));
            assertTrue(isJapaneseReadable("あいうえ～"));
            assertTrue(isJapaneseReadable("～あいうえ"));
            assertTrue(isJapaneseReadable("あ～い～う～え"));
            assertTrue(isJapaneseReadable("　　　　　"));
            assertTrue(isJapaneseReadable("福山雅治"));
            assertTrue(isJapaneseReadable("サシハラ莉乃"));
            assertTrue(isJapaneseReadable("倖田來未"));
            assertTrue(isJapaneseReadable("奥田民生"));
            assertTrue(isJapaneseReadable("Best ～first things～"));

            assertFalse(isJapaneseReadable(null));
            assertFalse(isJapaneseReadable("ABCDE"));
            assertFalse(isJapaneseReadable("bad communication"));
            assertFalse(isJapaneseReadable("BAD COMMUNICATION"));
            assertFalse(isJapaneseReadable("Cæsar"));
            assertFalse(isJapaneseReadable("The Alfee"));
            assertFalse(isJapaneseReadable("[Disc 3]"));
            assertFalse(isJapaneseReadable("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
            assertFalse(isJapaneseReadable("Dvořák: Symphonies #7-9"));
        }
    }

    @Documented
    private @interface CreateIndexableNameArtistDecisions {
        @interface Conditions {
            @interface isIndexEnglishPrior {
                // Currently always true
                @interface True {
                }
            }

            @interface Artist {
                @interface Name {
                    @interface isStartWithAlpha {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistReading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistSort {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }
            }
        }

        @interface Result {
            @interface IndexableName {
                @interface NameDerived {
                }

                @interface NormalizedReading {
                }

            }
        }
    }

    @Nested
    class CreateIndexableNameArtist {

        private @NonNull String createReadingWithNameAndSort(@NonNull String name, @Nullable String sort)
                throws ExecutionException {
            @Unsigned
            String result;
            try {
                Method createReading = utils.getClass().getDeclaredMethod("createReading", String.class, String.class);
                createReading.setAccessible(true);
                result = createReading.invoke(utils, name, sort).toString();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
            return result;
        }

        private Artist createArtist(String name, String sort) throws ExecutionException {
            Artist artist = new Artist();
            artist.setName(name);
            artist.setReading(createReadingWithNameAndSort(name, sort));
            artist.setSort(sort);
            return artist;
        }

        @CreateIndexableNameArtistDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.True
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c01() throws ExecutionException {
            Artist artist = createArtist("abcde", null);
            assertEquals("abcde", artist.getName());
            assertEquals("abcde", artist.getReading());
            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("abcde", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.False
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c02() throws ExecutionException {
            Artist artist = createArtist("日本語名", null);
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());
            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistSort.Empty.False
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c03() throws ExecutionException {
            Artist artist = createArtist("日本語名", "にほんごめい");
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());

            artist.setReading(null);
            assertNull(artist.getReading());

            assertEquals("にほんごめい", artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistSort.Empty.True
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c04() throws ExecutionException {
            Artist artist = createArtist("日本語名", null);
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());

            artist.setReading(null);
            assertNull(artist.getReading());

            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("日本語名", indexableNameString);
        }
    }

    @Documented
    private @interface CreateIndexableNameMediaFileDecisions {
        @interface Conditions {
            @interface isIndexEnglishPrior {
                // Currently always true
                @interface True {
                }
            }

            @interface MediaFile {
                @interface Name {
                    @interface isStartWithAlpha {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistReading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistSort {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }
            }
        }

        @interface Result {
            @interface IndexableName {
                @interface PathDerived {
                }

                @interface NormalizedReading {
                }

            }
        }
    }

    /**
     * Since it is backward compatible, there are many cases, but in reality it is c02 in the case of Japanese. In most
     * cases, the scan is finished when the index is created. That is, the readings have been resolved and there are no
     * null cases.
     */
    @Nested
    class CreateIndexableNameMediaFile {

        private MediaFile createAnalyzedMediaFile(String name, String sort, String path) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setArtist(name);
            mediaFile.setArtistSort(sort);
            mediaFile.setPath(path);
            utils.analyze(mediaFile);
            return mediaFile;
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.True
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c01() {
            MediaFile file = createAnalyzedMediaFile("abcde", null, "abcde-path");
            assertEquals("abcde", file.getArtist());
            assertEquals("abcde", file.getArtistReading());
            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            // path. (All legacy servers have this specification)
            assertEquals("abcde-path", indexableNameString);
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.False
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c02() {
            MediaFile file = createAnalyzedMediaFile("日本語名", null, "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());
            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            // Normalized reading, not path. Most phonological index languages require this
            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistSort.Empty.False
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c03() {
            MediaFile file = createAnalyzedMediaFile("日本語名", "にほんごめい", "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());

            /*
             * Now, This case rarely occurs when normal processing is performed. (For example, older versions of the
             * data. Or if the index was created while the scan was in progress)
             */
            file.setArtistReading(null);
            assertNull(file.getArtistReading());

            assertEquals("にほんごめい", file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            assertEquals("ニホンゴメイ", indexableNameString); // Normalized reading, not path
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.isIndexEnglishPrior.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistSort.Empty.True
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c04() {
            MediaFile file = createAnalyzedMediaFile("日本語名", null, "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());

            /*
             * Now, This case rarely occurs when normal processing is performed. (For example, older versions of the
             * data. Or if the index was created while the scan was in progress)
             */
            file.setArtistReading(null);
            assertNull(file.getArtistReading());

            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            assertEquals("日本語名-path", indexableNameString); // Normalized reading, not path
        }
    }

    @Nested
    class CreateIndexableName {

        private String createIndexableName(@NonNull String s) throws ExecutionException {
            Method method;
            try {
                method = utils.getClass().getDeclaredMethod("createIndexableName", String.class);
                method.setAccessible(true);
                return (String) method.invoke(utils, s);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreateIndexableName() throws ExecutionException {
            assertEquals("アイウエオ", createIndexableName("アイウエオ"));
            assertEquals("ァィゥェォ", createIndexableName("ァィゥェォ"));
            assertEquals("ァィゥェォ", createIndexableName("ｧｨｩｪｫ"));
            assertEquals("アイウエオ", createIndexableName("ｱｲｳｴｵ"));
            assertEquals("ツンク♂", createIndexableName("つんく♂"));
            assertEquals("DJ FUMI★YEAH!", createIndexableName("DJ FUMI★YEAH!"));
            assertEquals("ABCDE", createIndexableName("ABCDE"));

            // Halfwidth and Katakana
            assertEquals("ABCDE", createIndexableName("ＡＢＣＤＥ")); // Fullwidth-Halfwidth
            assertEquals("BAD COMMUNICATION", createIndexableName("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
            assertEquals("アイウエオ", createIndexableName("あいうえお")); // Hiragana-Katakana

            // Normalization specifications vary by language
            assertEquals("ゴウヒロミ", createIndexableName("ゴウヒロミ")); // NFD
            assertEquals("パミュパミュ", createIndexableName("ぱみゅぱみゅ")); // NFD
            assertEquals("コウダクミ", createIndexableName("コウダクミ")); // NFD
        }
    }

    @Documented
    private @interface CreateReadingDecisions {
        @interface Conditions {
            @interface Name {
                @interface StartWithAlpha {
                    @interface Y {
                    }

                    @interface N {
                    }
                }
            }

            @interface Sort {
                @interface StartWithAlpha {
                    @interface Y {
                    }

                    @interface N {
                    }
                }

                @interface JapaneseReadable {
                    @interface Y {
                    }

                    @interface N {
                    }
                }
            }
        }

        @interface Result {
            @interface NameDirived {
            }

            @interface SortDirived {
            }

        }
    }

    @Nested
    class CreateReading {

        private @NonNull String createReadingWithNameAndSort(@NonNull String name, @Nullable String sort)
                throws ExecutionException {
            @Unsigned
            String result;
            try {
                Method createReading = utils.getClass().getDeclaredMethod("createReading", String.class, String.class);
                createReading.setAccessible(true);
                result = createReading.invoke(utils, name, sort).toString();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
            return result;
        }

        @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
        @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
        @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
        @CreateReadingDecisions.Result.SortDirived
        @Test
        void c01() throws ExecutionException {
            assertEquals("It's ニホンゴノヨミ", createReadingWithNameAndSort("abc", "It's 日本語の読み"));
        }

        @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
        @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
        @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.N
        @CreateReadingDecisions.Result.NameDirived
        @Test
        void c02() throws ExecutionException {
            assertEquals("abc", createReadingWithNameAndSort("abc", "It is an English reading"));
        }

        @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
        @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.N
        @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
        @CreateReadingDecisions.Result.NameDirived
        @Test
        void c03() throws ExecutionException {
            assertEquals("abc", createReadingWithNameAndSort("abc", "日本語の読み"));
        }

        @CreateReadingDecisions.Conditions.Name.StartWithAlpha.N
        @Test
        void c04() throws ExecutionException {
            assertEquals("ニホンゴノヨミ", createReadingWithNameAndSort("日本語名", "日本語の読み"));
            assertEquals("ニホンゴメイ", createReadingWithNameAndSort("日本語名", null));
        }

        private @NonNull String createReading(@NonNull String s) throws ExecutionException {
            @Unsigned
            String result;
            try {
                Method createReading = utils.getClass().getDeclaredMethod("createReading", String.class);
                createReading.setAccessible(true);
                result = createReading.invoke(utils, s).toString();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
            return result;
        }

        @Test
        void testCreateReading() throws ExecutionException {

            /*
             * Kuromoji will read the full-width alphabet in Japanese. ＢＢＣ(It's not bbc but ビービーシー.) When this is done
             * in the field of Japanese music, it is often not very good. This conversion is suppressed in Jpsonic.
             * Full-width alphabets will not been read in Japanese.
             */

            assertEquals("アイウエオ", createReading("The あいうえお"));
            assertEquals("アイウエオ", createReading("あいうえお"));
            assertEquals("アイウエオ", createReading("アイウエオ"));
            assertEquals("ァィゥェォ", createReading("ァィゥェォ"));
            assertEquals("ァィゥェォ", createReading("ｧｨｩｪｫ"));
            assertEquals("アイウエオ", createReading("ｱｲｳｴｵ"));
            assertEquals("アイウエオ", createReading("亜伊鵜絵尾"));
            assertEquals("ABCDE", createReading("ABCDE"));
            assertEquals("ABCDE", createReading("ＡＢＣＤＥ"));
            assertEquals("アルファベータガンマ", createReading("αβγ"));
            assertEquals("ツンク♂", createReading("つんく♂"));
            assertEquals("bad communication", createReading("bad communication"));
            assertEquals("BAD COMMUNICATION", createReading("BAD COMMUNICATION"));
            assertEquals("BAD COMMUNICATION", createReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
            assertEquals("イヌトネコ", createReading("犬とネコ"));
            assertEquals(" ｢｣()()[][];!!??##123", createReading("　「」（）()［］[]；！!？?＃#１２３"));
            assertEquals("Cæsar", createReading("Cæsar"));
            assertEquals("Alfee", createReading("The Alfee"));
            assertEquals("コンピューター", createReading("コンピューター"));
            assertEquals("アイ～ウエ", createReading("あい～うえ"));
            assertEquals("アイウエ～", createReading("あいうえ～"));
            assertEquals("～アイウエ", createReading("～あいうえ"));
            assertEquals("ア～イ～ウ～エ", createReading("あ～い～う～え"));
            assertEquals("     ", createReading("　　　　　"));
            assertEquals("[Disc 3]", createReading("[Disc 3]"));
            assertEquals("Best ～first things～", createReading("Best ～first things～"));
            assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                    createReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
            assertEquals("Dvořák: Symphonies #7-9", createReading("Dvořák: Symphonies #7-9"));
            assertEquals("[Disc 3]", createReading("[Disc 3]"));
            assertEquals("フクヤママサハル", createReading("福山雅治")); // Readable case

            assertEquals("サシハラ莉乃", createReading("サシハラ莉乃")); // Unreadable case
            assertEquals("倖タ來ヒツジ", createReading("倖田來未")); // Unreadable case
            assertEquals("オクダ ミンセイ", createReading("奥田　民生")); // Unreadable case

            assertEquals(" ｢｣()()[][];!!??##123", createReading(" 「」（）()［］[]；！!？?＃#１２３"));
        }
    }

    @Nested
    class Normalize {

        private @NonNull String normalize(@NonNull String s) throws ExecutionException {
            @Unsigned
            String result;
            try {
                Method normalize = JapaneseReadingUtils.class.getDeclaredMethod("normalize", String.class);
                normalize.setAccessible(true);
                result = normalize.invoke(utils, s).toString();
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
            return result;
        }

        @Test
        void testNormalize() throws ExecutionException {
            assertEquals("あいうえお", normalize("あいうえお"));
            assertEquals("アイウエオ", normalize("アイウエオ"));
            assertEquals("ァィゥェォ", normalize("ァィゥェォ"));
            assertEquals("ァィゥェォ", normalize("ｧｨｩｪｫ"));
            assertEquals("アイウエオ", normalize("ｱｲｳｴｵ"));
            assertEquals("亜伊鵜絵尾", normalize("亜伊鵜絵尾"));
            assertEquals("ABCDE", normalize("ABCDE"));
            assertEquals("ABCDE", normalize("ＡＢＣＤＥ"));
            assertEquals("αβγ", normalize("αβγ"));
            assertEquals("つんく♂", normalize("つんく♂"));
            assertEquals("bad communication", normalize("bad communication"));
            assertEquals("BAD COMMUNICATION", normalize("BAD COMMUNICATION"));
            assertEquals("BAD COMMUNICATION", normalize("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
            assertEquals("犬とネコ", normalize("犬とネコ"));
            assertEquals("読み", normalize("読み"));
            assertEquals("(読み)", normalize("(読み)"));
            assertEquals(" ｢｣()()[][];!!??##123", normalize("　「」（）()［］[]；！!？?＃#１２３"));
            assertEquals("Cæsar", normalize("Cæsar"));
            assertEquals("The Alfee", normalize("The Alfee"));
            assertEquals("コンピューター", normalize("コンピューター"));
            assertEquals("あい～うえ", normalize("あい～うえ"));
            assertEquals("あいうえ～", normalize("あいうえ～"));
            assertEquals("～あいうえ", normalize("～あいうえ"));
            assertEquals("あ～い～う～え", normalize("あ～い～う～え"));
            assertEquals("     ", normalize("　　　　　"));
            assertEquals("Best ～first things～", normalize("Best ～first things～"));
            assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                    normalize("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
            assertEquals("Dvořák: Symphonies #7-9", normalize("Dvořák: Symphonies #7-9"));
            assertEquals("福山雅治", normalize("福山雅治"));
            assertEquals("サシハラ莉乃", normalize("サシハラ莉乃"));
            assertEquals("倖田來未", normalize("倖田來未"));
        }

    }

    @Nested
    class RemovePunctuationFromJapaneseReading {

        @Test
        void testRemovePunctuationFromJapaneseReading() {
            assertNull(utils.removePunctuationFromJapaneseReading(null));
            assertEquals("あいうえお", utils.removePunctuationFromJapaneseReading("あいうえお"));
            assertEquals("アイウエオ", utils.removePunctuationFromJapaneseReading("アイウエオ"));
            assertEquals("ｱｲｳｴｵ", utils.removePunctuationFromJapaneseReading("ｱｲｳｴｵ"));
            assertEquals("ｱｲｳｴｵ", utils.removePunctuationFromJapaneseReading("｢ｱｲｳｴｵ｣"));
            assertEquals("ァィゥェォ", utils.removePunctuationFromJapaneseReading("ァィゥェォ"));
            assertEquals("アルファベータガンマ", utils.removePunctuationFromJapaneseReading("アルファベータガンマ"));
            assertEquals("ツンク", utils.removePunctuationFromJapaneseReading("ツンク♂"));
            assertEquals("イヌトネコ", utils.removePunctuationFromJapaneseReading("イヌトネコ"));
            assertEquals(" ｢｣()()[][];!!??##123", utils.removePunctuationFromJapaneseReading(" ｢｣()()[][];!!??##123"));
            assertEquals("コンピューター", utils.removePunctuationFromJapaneseReading("コンピューター"));
            assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイ～ウエ"));
            assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイウエ～"));
            assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("～アイウエ"));
            assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("ア～イ～ウ～エ"));
            assertEquals("     ", utils.removePunctuationFromJapaneseReading("     "));
            assertEquals("[Disc 3]", utils.removePunctuationFromJapaneseReading("[Disc 3]"));
            assertEquals("Best ～first things～", utils.removePunctuationFromJapaneseReading("Best ～first things～"));
            assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                    utils.removePunctuationFromJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
            assertEquals("Dvořák: Symphonies #7-9",
                    utils.removePunctuationFromJapaneseReading("Dvořák: Symphonies #7-9"));
            assertEquals("フクヤママサハル", utils.removePunctuationFromJapaneseReading("フクヤママサハル"));
            assertEquals("サシハラ莉乃", utils.removePunctuationFromJapaneseReading("サシハラ莉乃"));
            assertEquals("倖タ來ヒツジ", utils.removePunctuationFromJapaneseReading("倖タ來ヒツジ"));
            assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディローパー"));
            assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ ローパー"));
            assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
            assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
            assertEquals("ｼﾝﾃﾞｨﾛｰﾊﾟｰ", utils.removePunctuationFromJapaneseReading("ｼﾝﾃﾞｨ･ﾛｰﾊﾟｰ"));
        }
    }
}
