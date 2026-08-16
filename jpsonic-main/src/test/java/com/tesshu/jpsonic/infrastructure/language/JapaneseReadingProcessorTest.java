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

package com.tesshu.jpsonic.infrastructure.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Documented;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.persistence.api.entity.Playlist;
import com.tesshu.jpsonic.persistence.result.SortCandidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class JapaneseReadingProcessorTest {

    private SettingsFacade settingsFacade;

    private JapaneseReadingProcessor proc;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.NATIVE_JAPANESE.name())
            .build();
        JapaneseReadingUtils utils = new JapaneseReadingUtils(settingsFacade);
        proc = new JapaneseReadingProcessor(settingsFacade, utils);
    }

    @AfterEach
    void afterAll() {
        proc.clear();
    }

    @Order(8)
    @Test
    void testAnalyzeGenre() {

        String genreName = "現代邦楽";

        Genre genre = new Genre(genreName, 0, 0);
        assertEquals(genreName, genre.getName());
        assertNull(genre.getReading());

        proc.analyze(genre);
        proc.clear();
        assertEquals("ゲンダイホウガク", genre.getReading());

        // ROMANIZED_JAPANESE
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.ROMANIZED_JAPANESE.name())
            .build();
        proc = new JapaneseReadingProcessor(settingsFacade,
                new JapaneseReadingUtils(settingsFacade));

        proc.analyze(genre);
        proc.clear();
        assertEquals("Gendaihogaku", genre.getReading());

        genre = new Genre(genreName, 0, 0);
        proc.analyze(genre);
        proc.clear();
        assertEquals("Gendaihogaku", genre.getReading());

        // WITHOUT_JP_LANG_PROCESSING
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName,
                    IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
            .build();
        proc = new JapaneseReadingProcessor(settingsFacade,
                new JapaneseReadingUtils(settingsFacade));

        genre = new Genre(genreName, 0, 0);
        proc.analyze(genre);
        assertEquals(genreName, genre.getReading());
    }

    @Documented
    private @interface AnalyzeMediaFileDecisions {
        @interface Conditions {
            @interface Name {
                @interface Null {
                }

                @interface Japanese {
                }

                @interface Latin {
                }
            }

            @interface Reading {
                @interface Null {
                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface Japanese {
                }

                @interface Latin {
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
                    @interface FromNameJp {
                    }

                    @interface FromSortJp {
                    }

                    @interface FromSortLatin {
                    }

                    @interface FromNameLatin {
                    }

                    @interface SortRaw {

                    }

                    @interface RemoveArticlesOnly {

                    }

                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface NotNull {
                    @interface SortJp {

                    }

                    @interface SortLatin {

                    }
                }
            }
        }
    }

    @Order(9)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    @SuppressWarnings("PMD.FinalFieldCouldBeStatic")
    class AnalyzeMediaFileTest {

        private final String nameJp = "The 「あいｱｲ・愛」";
        private final String sortJp = "あいあい・愛";
        private final String nameLatin = "The AiAi Ai!";
        private final String sortLatin = "sortLatinRaw";
        private final String readingFromSortJp2Jp = "アイアイ・アイ";
        private final String readingFromNameJp2Jp = "｢アイアイ・アイ｣";
        private final String readingFromNameLatin = "AiAi Ai!";
        private final String readingFromSortLatin = "SortLatinRaw";
        private final String readingFromNameJp2Latin = "｢aiai・-ai｣";
        private final String readingFromSortJp2Latin = "Aiai・Ai";
        private final String removeArticlesOnlyJp = "「あいｱｲ・愛」";
        private final String removeArticlesOnlyLatin = "AiAi Ai!";

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
        @Order(1)
        @Test
        void c01() {
            MediaFile mediaFile = toMediaFile(null, null);
            proc.analyze(mediaFile);
            assertNull(mediaFile.getArtist());
            assertNull(mediaFile.getArtistReading());
            assertNull(mediaFile.getArtistSort());
        }

        @Nested
        class NativeJapaneseTest {

            @AnalyzeMediaFileDecisions.Conditions.Name.Null
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortJp
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Order(2)
            @Test
            void j02() {
                MediaFile mediaFile = toMediaFile(null, sortJp);
                proc.analyze(mediaFile);
                assertNull(mediaFile.getArtist());
                assertEquals(readingFromSortJp2Jp, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameJp
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void j03() {
                MediaFile mediaFile = toMediaFile(nameJp, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromNameJp2Jp, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortJp
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void j04() {
                MediaFile mediaFile = toMediaFile(nameJp, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromSortJp2Jp, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortLatin
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void j05() {
                MediaFile mediaFile = toMediaFile(nameJp, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(sortLatin, mediaFile.getArtistReading());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameLatin
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void j06() {
                MediaFile mediaFile = toMediaFile(nameLatin, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingFromNameLatin, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameLatin
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void j07() {
                MediaFile mediaFile = toMediaFile(nameLatin, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromNameLatin, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameLatin
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void j08() {
                MediaFile mediaFile = toMediaFile(nameLatin, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingFromNameLatin, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }
        }

        /*
         * Even if Japanese is used for the Sort tag, the reading will be set to the
         * Romanized string. If nothing is set in the Sort tag, the reading value will
         * be set to the Roman alphabet string created from the name.
         */
        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class RomanizedJapaneseTest {

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameJp
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void r03() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromNameJp2Latin, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortJp
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void r04() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromSortJp2Latin, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortLatin
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void r05() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromSortLatin, mediaFile.getArtistReading());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromNameLatin
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void r06() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromNameLatin, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortJp
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void r07() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromSortJp2Latin, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.FromSortLatin
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void r08() {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(readingFromSortLatin, mediaFile.getArtistReading());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }
        }

        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class WithoutJpTest {

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.RemoveArticlesOnly
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void w03() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(removeArticlesOnlyJp, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.SortRaw
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void w04() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(sortJp, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Japanese
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.SortRaw
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void w05() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameJp, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(sortLatin, mediaFile.getArtistReading());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.RemoveArticlesOnly
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            void w06() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, null);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(removeArticlesOnlyLatin, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Japanese
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.SortRaw
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortJp
            @Test
            void w07() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, sortJp);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(sortJp, mediaFile.getArtistReading());
                assertEquals(sortJp, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Latin
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Latin
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.SortRaw
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortLatin
            @Test
            void w08() {
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                proc = new JapaneseReadingProcessor(settingsFacade,
                        new JapaneseReadingUtils(settingsFacade));

                MediaFile mediaFile = toMediaFile(nameLatin, sortLatin);
                proc.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertEquals(sortLatin, mediaFile.getArtistReading());
                assertEquals(sortLatin, mediaFile.getArtistSort());
            }
        }
    }

    @Order(10)
    @Test
    void testAnalyzePlaylist() {
        final String playlistName = "2021/07/21 22:40 お気に入り";

        Playlist playlist = new Playlist();
        playlist.setName(playlistName);
        assertEquals(playlistName, playlist.getName());
        assertNull(playlist.getReading());

        proc.analyze(playlist);
        proc.clear();
        assertEquals("2021/07/21 22:40 オキニイリ", playlist.getReading());

        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.ROMANIZED_JAPANESE.name())
            .build();
        proc = new JapaneseReadingProcessor(settingsFacade,
                new JapaneseReadingUtils(settingsFacade));

        proc.analyze(playlist);
        proc.clear();
        assertEquals("2021/07/21 22:40 Okiniiri", playlist.getReading());

        playlist = new Playlist();
        playlist.setName(playlistName);
        proc.analyze(playlist);
        proc.clear();
        assertEquals("2021/07/21 22:40 Okiniiri", playlist.getReading());

        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName,
                    IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
            .build();
        proc = new JapaneseReadingProcessor(settingsFacade,
                new JapaneseReadingUtils(settingsFacade));

        proc.analyze(playlist);
        proc.clear();
        assertEquals(playlistName, playlist.getReading());
    }

    @Order(11)
    @Nested
    @SuppressWarnings("PMD.FinalFieldCouldBeStatic")
    class AnalyzeSortCandidate {

        private final String nameRaw = "The 「あいｱｲ・愛」";
        private final String sortRaw = "あいあい・あい";
        private final String readingAnalyzedFromSort = "アイアイ・アイ";
        private final String readingAnalyzedFromName = "｢アイアイ・アイ｣";

        @Test
        void testNullSort() {
            SortCandidate candidate = new SortCandidate(nameRaw, null, -1);
            proc.analyze(candidate);
            assertEquals(nameRaw, candidate.getName());
            assertEquals(readingAnalyzedFromName, candidate.getReading());
            assertEquals(readingAnalyzedFromName, candidate.getSort());
        }

        @Test
        void testNotNullSort() {
            SortCandidate candidate = new SortCandidate(nameRaw, sortRaw, -1);
            proc.analyze(candidate);
            assertEquals(nameRaw, candidate.getName());
            assertEquals(readingAnalyzedFromSort, candidate.getReading());
            assertEquals(sortRaw, candidate.getSort());
        }
    }

    @Documented
    private @interface CreateIndexableNameDecisions {
        @interface Conditions {
            @interface IndexScheme {
                @interface NativeJapanese {
                }

                @interface RomanizedJapanese {
                }

                @interface WithoutJp {
                }
            }
        }
    }

    @Order(12)
    @Nested
    class CreateIndexableName {

        @Test
        void assertDeleteDiacritic() {
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    proc.createIndexableName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
            assertEquals("ACEGIKLMNOPRSUWYZ", proc.createIndexableName("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ"));
            assertEquals("AEINOUWY", proc.createIndexableName("ÀÈÌǸÒÙẀỲ"));
            assertEquals("ACEGHIJNOSUWYZ", proc.createIndexableName("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ"));
            assertEquals("AEHIOTUWXY", proc.createIndexableName("ÄËḦÏÖT̈ÜẄẌŸ"));
            assertEquals("AUWY", proc.createIndexableName("ÅŮW̊Y̊"));
            assertEquals("ACDEGHIKLNORSTUZ", proc.createIndexableName("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ"));
            assertEquals("AEINOUVY", proc.createIndexableName("ÃẼĨÑÕŨṼỸ"));
            assertEquals("CDEGHKLNRST", proc.createIndexableName("ÇḐȨĢḨĶĻŅŖŞŢ"));
            assertEquals("ST", proc.createIndexableName("ȘȚ"));
            assertEquals("AEGIOU", proc.createIndexableName("ĂĔĞĬŎŬ"));
            assertEquals("AEGINOUY", proc.createIndexableName("ĀĒḠĪN̄ŌŪȲ"));
            assertEquals("AEIOU", proc.createIndexableName("ĄĘĮǪŲ"));
            assertEquals("OU", proc.createIndexableName("ŐŰ"));
            assertEquals("ABCDEFGHIĿMNOPRSTWXYZ",
                    proc.createIndexableName("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ"));
            assertEquals("OU", proc.createIndexableName("ƠƯ"));

            // Currently does not support stroke deletion
            assertEquals("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ", proc.createIndexableName("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ"));
        }

        @Test
        void assertRemainDiacritic() {

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.ROMANIZED_JAPANESE.name())
                .withBoolean(SKeys.advanced.index.deleteDiacritic, false)

                .build();

            JapaneseReadingUtils utils = new JapaneseReadingUtils(settingsFacade);
            proc = new JapaneseReadingProcessor(settingsFacade, utils);

            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    proc.createIndexableName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
            assertEquals("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ", proc.createIndexableName("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ"));
            assertEquals("ÀÈÌǸÒÙẀỲ", proc.createIndexableName("ÀÈÌǸÒÙẀỲ"));
            assertEquals("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ", proc.createIndexableName("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ"));
            assertEquals("ÄËḦÏÖT̈ÜẄẌŸ", proc.createIndexableName("ÄËḦÏÖT̈ÜẄẌŸ"));
            assertEquals("ÅŮW̊Y̊", proc.createIndexableName("ÅŮW̊Y̊"));
            assertEquals("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ", proc.createIndexableName("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ"));
            assertEquals("ÃẼĨÑÕŨṼỸ", proc.createIndexableName("ÃẼĨÑÕŨṼỸ"));
            assertEquals("ÇḐȨĢḨĶĻŅŖŞŢ", proc.createIndexableName("ÇḐȨĢḨĶĻŅŖŞŢ"));
            assertEquals("ȘȚ", proc.createIndexableName("ȘȚ"));
            assertEquals("ĂĔĞĬŎŬ", proc.createIndexableName("ĂĔĞĬŎŬ"));
            assertEquals("ĀĒḠĪN̄ŌŪȲ", proc.createIndexableName("ĀĒḠĪN̄ŌŪȲ"));
            assertEquals("ĄĘĮǪŲ", proc.createIndexableName("ĄĘĮǪŲ"));
            assertEquals("ŐŰ", proc.createIndexableName("ŐŰ"));
            assertEquals("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ",
                    proc.createIndexableName("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ"));
            assertEquals("ƠƯ", proc.createIndexableName("ƠƯ"));

            // Currently does not support stroke deletion
            assertEquals("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ", proc.createIndexableName("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ"));
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.NativeJapanese
        @Test
        void c01() throws ExecutionException {

            assertEquals("ABCDE", proc.createIndexableName("ABCDE")); // no change
            assertEquals("アイウエオ", proc.createIndexableName("アイウエオ")); // no change
            assertEquals("ァィゥェォ", proc.createIndexableName("ァィゥェォ")); // no change
            assertEquals("ァィゥェォ", proc.createIndexableName("ｧｨｩｪｫ")); // to ** Fullwidth **
            assertEquals("アイウエオ", proc.createIndexableName("ｱｲｳｴｵ")); // to ** Fullwidth **
            assertEquals("ツンク♂", proc.createIndexableName("つんく♂")); // to Katakana
            assertEquals("アイウエオ", proc.createIndexableName("あいうえお")); // to Katakana
            assertEquals("ゴウヒロミ", proc.createIndexableName("ゴウヒロミ")); // NFD
            assertEquals("パミュパミュ", proc.createIndexableName("ぱみゅぱみゅ")); // NFD
            assertEquals("コウダクミ", proc.createIndexableName("コウダクミ")); // NFD

            // Half-width conversion is forcibly executed in this schema.
            assertEquals("ABCDE", proc.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth

            // Diacritic removed is forcibly executed in this schema.
            assertDeleteDiacritic();
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.RomanizedJapanese
        @Test
        void c02() throws ExecutionException {

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.ROMANIZED_JAPANESE.name())
                .withBoolean(SKeys.advanced.index.deleteDiacritic, true)
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            assertEquals("ABCDE", proc.createIndexableName("ABCDE"));

            // Half-width conversion is forcibly executed in this schema.
            assertEquals("ABCDE", proc.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth

            // Diacritic
            assertDeleteDiacritic();
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.ROMANIZED_JAPANESE.name())
                .withBoolean(SKeys.advanced.index.deleteDiacritic, false)
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));
            assertRemainDiacritic();
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.WithoutJp
        @Test
        void c03() throws ExecutionException {

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                .withBoolean(SKeys.advanced.index.ignoreFullWidth, true)
                .withBoolean(SKeys.advanced.index.deleteDiacritic, true)
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            assertEquals("DJ FUMI★YEAH!", proc.createIndexableName("DJ FUMI★YEAH!")); // no change
            assertEquals("ABCDE", proc.createIndexableName("ABCDE")); // no change
            assertEquals("ｱｲｳｴｵ", proc.createIndexableName("アイウエオ")); // to Halfwidth
            assertEquals("ｧｨｩｪｫ", proc.createIndexableName("ァィゥェォ")); // to Halfwidth
            assertEquals("ｧｨｩｪｫ", proc.createIndexableName("ｧｨｩｪｫ")); // no change
            assertEquals("ｱｲｳｴｵ", proc.createIndexableName("ｱｲｳｴｵ")); // no change
            assertEquals("つんく♂", proc.createIndexableName("つんく♂")); // no change
            assertEquals("あいうえお", proc.createIndexableName("あいうえお")); // no change
            assertEquals("ｺﾞｳﾋﾛﾐ", proc.createIndexableName("ゴウヒロミ")); // to Halfwidth
            assertEquals("ぱみゅぱみゅ", proc.createIndexableName("ぱみゅぱみゅ")); // NFD
            assertEquals("ｺｳﾀﾞｸﾐ", proc.createIndexableName("コウダクミ")); // to Halfwidth

            // Halfwidth
            assertEquals("ABCDE", proc.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                .withBoolean(SKeys.advanced.index.ignoreFullWidth, false)
                .withBoolean(SKeys.advanced.index.deleteDiacritic, true)
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            assertEquals("ＡＢＣＤＥ", proc.createIndexableName("ＡＢＣＤＥ")); // no change

            // Diacritic
            assertDeleteDiacritic();

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                .withBoolean(SKeys.advanced.index.ignoreFullWidth, false)
                .withBoolean(SKeys.advanced.index.deleteDiacritic, false)
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            assertRemainDiacritic();
        }
    }

    @Documented
    private @interface CreateIndexableNameArtistDecisions {
        @interface Conditions {

            @interface IndexScheme {
                @interface NativeJapanese {
                }

                @interface WithoutJp {
                }
            }

            @interface Artist {
                @interface Reading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                            @interface EqName {

                            }

                            @interface NeName {

                            }
                        }
                    }
                }

                @interface Name {
                    @interface NotJapanese {

                    }

                    @interface Japanese {

                    }
                }
            }
        }

        @interface Result {
            @interface IndexableName {
                @interface NameDerived {
                }

                @interface ReadingDerived {
                }
            }
        }
    }

    @Order(12)
    @Nested
    class CreateIndexableNameArtistTest {

        String name = "nameDerived";
        String reading = "reagingDerived";

        private Artist createArtist(String name, String sort) {
            Artist artist = new Artist();
            artist.setName(name);
            artist.setSort(sort);
            return artist;
        }

        @CreateIndexableNameArtistDecisions.Conditions.IndexScheme.WithoutJp
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c01() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            Artist artist = createArtist(name, null);
            assertEquals(name, proc.createIndexableName(artist));
        }

        @CreateIndexableNameArtistDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Reading.Empty.True
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c02() {
            Artist artist = createArtist(name, null);
            assertEquals(name, proc.createIndexableName(artist));
        }

        @CreateIndexableNameArtistDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Reading.Empty.False.EqName
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c03() {
            Artist artist = createArtist(name, null);
            artist.setReading(name);
            assertEquals(name, proc.createIndexableName(artist));
        }

        @CreateIndexableNameArtistDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Reading.Empty.False.NeName
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.NotJapanese
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c04() {
            Artist artist = createArtist(name, null);
            artist.setReading(reading);
            assertEquals(name, proc.createIndexableName(artist));
        }

        @CreateIndexableNameArtistDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Reading.Empty.False.NeName
        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.Japanese
        @CreateIndexableNameArtistDecisions.Result.IndexableName.ReadingDerived
        @Test
        void c05() {
            Artist artist = createArtist("ニホンゴメイ", null);
            artist.setReading(reading);
            assertEquals(reading, proc.createIndexableName(artist));
        }
    }

    @Documented
    private @interface CreateIndexableNameMediaFileDecisions {
        @interface Conditions {

            @interface IndexScheme {
                @interface NativeJapanese {
                }

                @interface WithoutJp {
                }
            }

            @interface MediaFile {
                @interface MediaType {
                    @interface NeDirectory {

                    }

                    @interface EqDirectory {

                    }
                }

                @interface ArtistReading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                            @interface EqName {

                            }

                            @interface NeName {

                            }
                        }
                    }
                }

                @interface Name {
                    @interface NotJapanese {

                    }

                    @interface Japanese {

                    }
                }

            }
        }

        @interface Result {
            @interface IndexableName {
                @interface PathDerived {
                }

                @interface SortTagDerived {
                }
            }
        }
    }

    @Order(13)
    @Nested
    class CreateIndexableNameMediaFile {

        String name = "name";
        String pathDerived = "root/pathDerived";

        private MediaFile createMediaFile(String name, String sort, String path) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setArtist(name);
            mediaFile.setArtistSort(sort);
            mediaFile.setPathString(path);
            return mediaFile;
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.WithoutJp
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c01() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, "ja")
                .withString(I18nSKeys.localeCountry, "ja")
                .withString(SKeys.advanced.index.indexSchemeName,
                        IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                .build();
            proc = new JapaneseReadingProcessor(settingsFacade,
                    new JapaneseReadingUtils(settingsFacade));

            MediaFile mediaFile = createMediaFile(name, null, pathDerived);
            proc.analyze(mediaFile);
            assertEquals("pathDerived", proc.createIndexableName(mediaFile));
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.MediaType.NeDirectory
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c02() {
            MediaFile mediaFile = createMediaFile(name, null, pathDerived);
            assertEquals("pathDerived", proc.createIndexableName(mediaFile));
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.MediaType.EqDirectory
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.True
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c03() {
            MediaFile mediaFile = createMediaFile(name, null, pathDerived);
            mediaFile.setMediaType(MediaType.DIRECTORY);
            assertNull(mediaFile.getArtistReading());
            assertEquals("pathDerived", proc.createIndexableName(mediaFile));
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.MediaType.EqDirectory
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.False.EqName
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c04() {
            MediaFile mediaFile = createMediaFile("pathDerived", null, pathDerived);
            mediaFile.setMediaType(MediaType.DIRECTORY);
            proc.analyze(mediaFile);
            assertEquals("pathDerived", mediaFile.getArtistReading());
            assertEquals("pathDerived", proc.createIndexableName(mediaFile));
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.MediaType.EqDirectory
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.False.NeName
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.NotJapanese
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c05() {
            MediaFile mediaFile = createMediaFile(name, "sortTagDerived", pathDerived);
            mediaFile.setMediaType(MediaType.DIRECTORY);
            proc.analyze(mediaFile);
            // assertEquals("sortTagDerived", mediaFile.getArtistReading());
            assertEquals("pathDerived", proc.createIndexableName(mediaFile));
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.IndexScheme.NativeJapanese
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.MediaType.EqDirectory
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.False.NeName
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.Japanese
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.SortTagDerived
        @Test
        void c06() {
            MediaFile mediaFile = createMediaFile("日本語名", "sortTagDerived", "root/日本語名");
            mediaFile.setMediaType(MediaType.DIRECTORY);
            proc.analyze(mediaFile);
            assertEquals("sortTagDerived", mediaFile.getArtistReading());
            assertEquals("sortTagDerived", proc.createIndexableName(mediaFile));
        }
    }
}
