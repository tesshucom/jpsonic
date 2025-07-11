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

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.controller.MainController;
import com.tesshu.jpsonic.dao.PlaylistDao;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.MusicIndexServiceImpl;
import com.tesshu.jpsonic.service.search.HttpSearchCriteria;
import com.tesshu.jpsonic.service.search.HttpSearchCriteriaDirector;
import com.tesshu.jpsonic.service.search.IndexType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * JpsonicComparators unit test. Jpsonic does not change the behavior of legacy
 * test specifications. This is because the range not defined in the legacy test
 * specification has been expanded.
 */
@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = "com.tesshu.jpsonic")
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports",
        "PMD.TestClassWithoutTestCases" })
class JpsonicComparatorsTest extends AbstractNeedsScan {

    protected static final Logger LOG = LoggerFactory.getLogger(JpsonicComparatorsTest.class);

    protected static final List<MusicFolder> MUSIC_FOLDERS = Arrays
        .asList(new MusicFolder(1, resolveBaseMediaPath("Sort" + File.separator + "Compare"),
                "test date for sorting", true, now(), 1, false));

    protected static final List<String> INDEX_LIST = Collections
        .unmodifiableList(Arrays
            .asList("abcde", "abcいうえおあ", // Turn
                                         // over
                                         // by
                    // reading
                    "abc亜伊鵜絵尾", // Turn over by reading
                    "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC", "the eabcd", "episode 1", "episode 2", "episode 19",
                    "亜伊鵜絵尾", "αβγ", "いうえおあ", "ｴｵｱｲｳ", "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る",
                    "はるなつあきふゆ", "10", // # Num
                    "20", // # Num
                    "50", // # Num
                    "60", // # Num
                    "70", // # Num
                    "98", // # Num
                    "99", // # Num
                    "ゥェォァィ", // # SmallKana (Not used at the beginning of a word/Generally
                             // prohibited characters in index)
                    "ｪｫｧｨｩ", // # SmallKana
                    "ぉぁぃぅぇ", // # SmallKana
                    "♂くんつ") // # Symbol
        );

    @Autowired
    private JpsonicComparatorsTestUtils testUtils;

    @Autowired
    protected JpsonicComparators comparators;

    @Autowired
    protected SettingsService settingsService;

    @SuppressWarnings("PMD.ClassNamingConventions")
    @Documented
    private @interface ComparatorsDecisions {
        @interface Conditions {
            @interface isProhibitSortVarious {
            } // GlobalOption

            @interface isSortAlbumsByYear {
            } // GlobalOption

            @interface isSortAlphanum {
            } // GlobalOption

            @interface Target {
                @interface Artist {
                }

                @interface Album {
                }

                @interface MediaFile {
                    @interface mediaType {
                        @interface ARTIST {
                        }

                        @interface ALBUM {
                        }

                        @interface Ignore {
                            @interface FieldOrderBy {
                                @interface Artist {
                                }

                                @interface Album {
                                }

                                @interface Track {
                                }
                            }
                        }
                    }

                    @interface parent {
                        @interface isEmpty {
                        }

                        @interface isVariablePrefix {
                        }
                    }
                }

                @interface Playlist {
                }

                @interface Genre {
                    @interface isSortByAlbum {
                    }
                }
            }
        }

        @interface Actions {
            @interface artistOrderByAlpha {
            }

            @interface albumOrderByAlpha {
            }

            @interface mediaFileOrder {
            }

            @interface mediaFileOrderBy {
            }

            @interface mediaFileOrderByAlpha {
            }

            @interface playlistOrder {
            }

            @interface genreOrder {
            }

            @interface genreOrderByAlpha {
            }
        }
    }

    protected static boolean validateIndexList(List<String> l) {
        return INDEX_LIST.equals(l);
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @Nested
    class ComparatorsUnitTest {

        @ComparatorsDecisions.Actions.artistOrderByAlpha
        @ComparatorsDecisions.Conditions.Target.Artist
        @Test
        void c00() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Artist> artists = testUtils.createReversedArtists();
            artists.sort(comparators.artistOrderByAlpha());
            JpsonicComparatorsTestUtils.assertArtistOrder(artists, 14, 15, 16);
            assertEquals("episode 1", artists.get(14).getName());
            assertEquals("episode 19", artists.get(15).getName());
            assertEquals("episode 2", artists.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.Artist
        @ComparatorsDecisions.Actions.artistOrderByAlpha
        @Test
        void c01() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Artist> artists = testUtils.createReversedArtists();
            artists.sort(comparators.artistOrderByAlpha());
            JpsonicComparatorsTestUtils.assertArtistOrder(artists, 14, 15, 16);
            assertEquals("episode 1", artists.get(14).getName());
            assertEquals("episode 2", artists.get(15).getName());
            assertEquals("episode 19", artists.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.Album
        @ComparatorsDecisions.Actions.albumOrderByAlpha
        @Test
        void c02() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Album> albums = testUtils.createReversedAlbums();
            albums.sort(comparators.albumOrderByAlpha());
            JpsonicComparatorsTestUtils.assertAlbumOrder(albums, 14, 15, 16);
            assertEquals("episode 1", albums.get(14).getName());
            assertEquals("episode 19", albums.get(15).getName());
            assertEquals("episode 2", albums.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.Album
        @ComparatorsDecisions.Actions.albumOrderByAlpha
        @Test
        void c03() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Album> albums = testUtils.createReversedAlbums();
            albums.sort(comparators.albumOrderByAlpha());
            JpsonicComparatorsTestUtils.assertAlbumOrder(albums, 14, 15, 16);
            assertEquals("episode 1", albums.get(14).getName());
            assertEquals("episode 2", albums.get(15).getName());
            assertEquals("episode 19", albums.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c16() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c17() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c18() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c19() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c20() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c21() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c22() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c23() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c24() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c25() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c26() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            assertEquals("98", files.get(0).getName());
            assertEquals("99", files.get(1).getName());
            assertEquals("10", files.get(2).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c27() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(new MediaFile()));
            assertEquals("98", files.get(0).getName());
            assertEquals("99", files.get(1).getName());
            assertEquals("10", files.get(2).getName());
        }

        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c28() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c29() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c30() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.isProhibitSortVarious
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
        @ComparatorsDecisions.Actions.mediaFileOrder
        @Test
        void c31() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(true);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            files.sort(comparators.mediaFileOrder(testUtils.createVariousMedifile()));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c32() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c33() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            files.sort(comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c34() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c35() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediArtists();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c36() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c37() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c38() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.isSortAlbumsByYear
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
        @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
        @Test
        void c39() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(true);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediAlbums();
            Collections.sort(files, comparators.mediaFileOrderByAlpha());
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Artist
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c40() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections
                .sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ARTIST));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Artist
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c41() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections
                .sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ARTIST));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Album
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c42() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections.sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ALBUM));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Album
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c43() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections.sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.ALBUM));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Track
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c44() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections.sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.TRACK));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 19", files.get(15).getName());
            assertEquals("episode 2", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.MediaFile
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
        @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Track
        @ComparatorsDecisions.Actions.mediaFileOrderBy
        void c45() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<MediaFile> files = testUtils.createReversedMediaSongs();
            Collections.sort(files, comparators.mediaFileOrderBy(JpsonicComparators.OrderBy.TRACK));
            JpsonicComparatorsTestUtils.assertMediafileOrder(files, 14, 15, 16);
            assertEquals("episode 1", files.get(14).getName());
            assertEquals("episode 2", files.get(15).getName());
            assertEquals("episode 19", files.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.Playlist
        @ComparatorsDecisions.Actions.playlistOrder
        @Test
        void c46() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Playlist> playlists = testUtils.createReversedPlaylists();
            Collections.sort(playlists, comparators.playlistOrder());
            JpsonicComparatorsTestUtils.assertPlaylistOrder(playlists, 8, 9, 14, 15, 16);

            // Playlist can not be specified reading with tag (so alphabetical)
            assertEquals("abc亜伊鵜絵尾", playlists.get(8).getName());
            assertEquals("abcいうえおあ", playlists.get(9).getName());

            assertEquals("episode 1", playlists.get(14).getName());
            assertEquals("episode 19", playlists.get(15).getName());
            assertEquals("episode 2", playlists.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.Playlist
        @ComparatorsDecisions.Actions.playlistOrder
        @Test
        void c47() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Playlist> playlists = testUtils.createReversedPlaylists();
            Collections.sort(playlists, comparators.playlistOrder());
            JpsonicComparatorsTestUtils.assertPlaylistOrder(playlists, 8, 9, 14, 15, 16);

            // Playlist can not be specified reading with tag (so alphabetical)
            assertEquals("abc亜伊鵜絵尾", playlists.get(8).getName());
            assertEquals("abcいうえおあ", playlists.get(9).getName());

            assertEquals("episode 1", playlists.get(14).getName());
            assertEquals("episode 2", playlists.get(15).getName());
            assertEquals("episode 19", playlists.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.Target.Genre
        @ComparatorsDecisions.Actions.genreOrder
        @Test
        void c48() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Genre> genres = testUtils.createReversedGenres();
            Collections.sort(genres, comparators.genreOrder(false));

            JpsonicComparatorsTestUtils.assertGenreOrder(genres, 8, 9);

            // Genre can not be specified reading with tag (so count)
            assertEquals("abcいうえおあ", genres.get(8).getName());
            assertEquals("abc亜伊鵜絵尾", genres.get(9).getName());
        }

        @ComparatorsDecisions.Conditions.Target.Genre.isSortByAlbum
        @ComparatorsDecisions.Conditions.Target.Genre
        @ComparatorsDecisions.Actions.genreOrder
        @Test
        void c49() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Genre> genres = testUtils.createReversedGenres();
            Collections.sort(genres, comparators.genreOrder(true));

            JpsonicComparatorsTestUtils.assertGenreOrder(genres, 8, 9);
            // Genre can not be specified reading with tag (so count)
            assertEquals("abcいうえおあ", genres.get(8).getName());
            assertEquals("abc亜伊鵜絵尾", genres.get(9).getName());
        }

        @ComparatorsDecisions.Conditions.Target.Genre
        @ComparatorsDecisions.Actions.genreOrderByAlpha
        @Test
        void c50() {
            settingsService.setSortAlphanum(false);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Genre> genres = testUtils.createReversedGenres();
            Collections.sort(genres, comparators.genreOrderByAlpha());

            JpsonicComparatorsTestUtils.assertGenreOrder(genres, 8, 9, 14, 15, 16);

            // Genre can not be specified reading with tag (so alphabetical)
            assertEquals("abc亜伊鵜絵尾", genres.get(8).getName());
            assertEquals("abcいうえおあ", genres.get(9).getName());

            assertEquals("episode 1", genres.get(14).getName());
            assertEquals("episode 19", genres.get(15).getName());
            assertEquals("episode 2", genres.get(16).getName());
        }

        @ComparatorsDecisions.Conditions.isSortAlphanum
        @ComparatorsDecisions.Conditions.Target.Genre
        @ComparatorsDecisions.Actions.genreOrderByAlpha
        @Test
        void c51() {
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            List<Genre> genres = testUtils.createReversedGenres();
            Collections.sort(genres, comparators.genreOrderByAlpha());
            JpsonicComparatorsTestUtils.assertGenreOrder(genres, 8, 9, 14, 15, 16);

            // Genre can not be specified reading with tag (so alphabetical)
            assertEquals("abc亜伊鵜絵尾", genres.get(8).getName());
            assertEquals("abcいうえおあ", genres.get(9).getName());

            assertEquals("episode 1", genres.get(14).getName());
            assertEquals("episode 2", genres.get(15).getName());
            assertEquals("episode 19", genres.get(16).getName());
        }
    }

    @Test
    void testSerialNumbers() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Artist> artists = testUtils.createReversedAlphanum();
        Collections.sort(artists, comparators.artistOrderByAlpha());
        assertTrue(JpsonicComparatorsTestUtils.assertAlphanumArtistOrder(artists));
    }

    /*
     * Quoted from MediaFileComparatorTest Jpsonic does not change the behavior of
     * legacy test specifications.<p> <p> Copyright 2020 (C) tesshu.com<br> Based
     * upon Airsonic, Copyright 2016 (C) Airsonic Authors<br> Based upon Subsonic,
     * Copyright 2009 (C) Sindre Mehus
     */
    @Test
    void testCompareAlbums() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        final MediaFileComparator comparator = comparators.mediaFileOrder(null);

        MediaFile albumA2012 = new MediaFile();
        albumA2012.setMediaType(MediaFile.MediaType.ALBUM);
        albumA2012.setPathString("a");
        albumA2012.setYear(2012);

        MediaFile albumB2012 = new MediaFile();
        albumB2012.setMediaType(MediaFile.MediaType.ALBUM);
        albumB2012.setPathString("b");
        albumB2012.setYear(2012);

        MediaFile album2013 = new MediaFile();
        album2013.setMediaType(MediaFile.MediaType.ALBUM);
        album2013.setPathString("c");
        album2013.setYear(2013);

        MediaFile albumWithoutYear = new MediaFile();
        albumWithoutYear.setMediaType(MediaFile.MediaType.ALBUM);
        albumWithoutYear.setPathString("c");

        assertEquals(0, comparator.compare(albumWithoutYear, albumWithoutYear));
        assertEquals(0, comparator.compare(albumA2012, albumA2012));

        assertEquals(-1, comparator.compare(albumA2012, albumWithoutYear));
        assertEquals(-1, comparator.compare(album2013, albumWithoutYear));
        assertEquals(1, comparator.compare(album2013, albumA2012));

        assertEquals(1, comparator.compare(albumWithoutYear, albumA2012));
        assertEquals(1, comparator.compare(albumWithoutYear, album2013));
        assertEquals(-1, comparator.compare(albumA2012, album2013));

        assertEquals(-1, comparator.compare(albumA2012, albumB2012));
        assertEquals(1, comparator.compare(albumB2012, albumA2012));
    }

    /*
     * Quoted from MediaFileComparatorTest Jpsonic does not change the behavior of
     * legacy test specifications.<p> Copyright 2020 (C) tesshu.com<br> Based upon
     * Airsonic, Copyright 2016 (C) Airsonic Authors<br> Based upon Subsonic,
     * Copyright 2009 (C) Sindre Mehus
     */
    @Test
    void testCompareDiscNumbers() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);

        MediaFile discXtrack1 = new MediaFile();
        discXtrack1.setMediaType(MediaFile.MediaType.MUSIC);
        discXtrack1.setPathString("a");
        discXtrack1.setTrackNumber(1);

        MediaFile discXtrack2 = new MediaFile();
        discXtrack2.setMediaType(MediaFile.MediaType.MUSIC);
        discXtrack2.setPathString("a");
        discXtrack2.setTrackNumber(2);

        MediaFile disc5track1 = new MediaFile();
        disc5track1.setMediaType(MediaFile.MediaType.MUSIC);
        disc5track1.setPathString("a");
        disc5track1.setDiscNumber(5);
        disc5track1.setTrackNumber(1);

        MediaFile disc5track2 = new MediaFile();
        disc5track2.setMediaType(MediaFile.MediaType.MUSIC);
        disc5track2.setPathString("a");
        disc5track2.setDiscNumber(5);
        disc5track2.setTrackNumber(2);

        MediaFile disc6track1 = new MediaFile();
        disc6track1.setMediaType(MediaFile.MediaType.MUSIC);
        disc6track1.setPathString("a");
        disc6track1.setDiscNumber(6);
        disc6track1.setTrackNumber(1);

        MediaFile disc6track2 = new MediaFile();
        disc6track2.setMediaType(MediaFile.MediaType.MUSIC);
        disc6track2.setPathString("a");
        disc6track2.setDiscNumber(6);
        disc6track2.setTrackNumber(2);

        MediaFileComparator comparator = comparators.mediaFileOrder(null);
        assertEquals(0, comparator.compare(discXtrack1, discXtrack1));
        assertEquals(0, comparator.compare(disc5track1, disc5track1));

        assertEquals(-1, comparator.compare(discXtrack1, discXtrack2));
        assertEquals(1, comparator.compare(discXtrack2, discXtrack1));

        assertEquals(-1, comparator.compare(disc5track1, disc5track2));
        assertEquals(1, comparator.compare(disc6track2, disc5track1));

        assertEquals(-1, comparator.compare(disc5track1, disc6track1));
        assertEquals(1, comparator.compare(disc6track1, disc5track1));

        assertEquals(-1, comparator.compare(disc5track2, disc6track1));
        assertEquals(1, comparator.compare(disc6track1, disc5track2));

        assertEquals(-1, comparator.compare(discXtrack1, disc5track1));
        assertEquals(1, comparator.compare(disc5track1, discXtrack1));

        assertEquals(-1, comparator.compare(discXtrack1, disc5track2));
        assertEquals(1, comparator.compare(disc5track2, discXtrack1));
    }

    /*
     * Quoted from SortableArtistTest Jpsonic does not change the behavior of legacy
     * test specifications.<p> Copyright 2020 (C) tesshu.com<br> Based upon
     * Airsonic, Copyright 2016 (C) Airsonic Authors<br> Based upon Subsonic,
     * Copyright 2009 (C) Sindre Mehus
     */
    @Test
    void testCollation() {
        List<MediaFile> artists = new ArrayList<>();
        List.of("p\u00e9ch\u00e9", "peach", "p\u00eache").stream().forEach(name -> { // péché pêche
            MediaFile artist = new MediaFile();
            artist.setArtist(name);
            artist.setMediaType(MediaType.DIRECTORY);
            artist.setPathString("/" + name);
            artists.add(artist);
        });
        Collections.sort(artists, comparators.mediaFileOrderByAlpha());
        assertEquals("peach", artists.get(0).getName());
        assertEquals("p\u00e9ch\u00e9", artists.get(1).getName()); // péché
        assertEquals("p\u00eache", artists.get(2).getName()); // pêche
    }

    /*
     * Quoted from SortableArtistTest Jpsonic does not change the behavior of legacy
     * test specifications.<p> Copyright 2020 (C) tesshu.com<br> Based upon
     * Airsonic, Copyright 2016 (C) Airsonic Authors<br> Based upon Subsonic,
     * Copyright 2009 (C) Sindre Mehus
     */
    @Test
    void testSorting() {
        List<MediaFile> artists = new ArrayList<>();
        List
            .of("ABBA", "Abba", "abba", "ACDC", "acdc", "ACDC", "abc", "ABC")
            .stream()
            .forEach(name -> {
                MediaFile artist = new MediaFile();
                artist.setArtist(name);
                artist.setMediaType(MediaType.DIRECTORY);
                artist.setPathString("/" + name);
                artists.add(artist);
            });
        Collections.shuffle(artists);
        Collections.sort(artists, comparators.mediaFileOrderByAlpha());
        assertEquals("abba", artists.get(0).getName());
        assertEquals("Abba", artists.get(1).getName());
        assertEquals("ABBA", artists.get(2).getName());
        assertEquals("abc", artists.get(3).getName());
        assertEquals("ABC", artists.get(4).getName());
        assertEquals("acdc", artists.get(5).getName());
        assertEquals("ACDC", artists.get(6).getName());
        assertEquals("ACDC", artists.get(7).getName());
    }

    @Nested
    class JpsonicComparatorsIntegrationTest {

        @Autowired
        private MainController mainController;

        @Autowired
        private MusicIndexServiceImpl musicIndexService;

        @Autowired
        private SearchService searchService;

        @Autowired
        private MediaFileService mediaFileService;

        @Autowired
        private PlaylistDao playlistDao;

        @Autowired
        private PlaylistService playlistService;

        @Autowired
        private HttpSearchCriteriaDirector director;

        @BeforeEach
        public void setup() {
            settingsService.setSortStrict(true);
            settingsService.setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setProhibitSortVarious(false);
            settingsService.setDlnaGuestPublish(false);

            Function<String, Playlist> toPlaylist = (title) -> {
                Instant now = now();
                Playlist playlist = new Playlist();
                playlist.setName(title);
                playlist.setUsername("admin");
                playlist.setCreated(now);
                playlist.setChanged(now);
                playlist.setShared(false);
                try {
                    Thread.sleep(200);
                    // Creating a large number of playlists in an instant can be inconsistent with
                    // consistency ...
                } catch (InterruptedException e) {
                    LOG
                        .error("It is possible that the playlist could not be initialized due to high load.",
                                e);
                }
                return playlist;
            };

            populateDatabaseOnlyOnce(() -> {
                if (playlistDao.getAllPlaylists().isEmpty()) {
                    List<String> shallow = new ArrayList<>(
                            JpsonicComparatorsTestUtils.JPSONIC_NATURAL_LIST);
                    Collections.shuffle(shallow);
                    shallow.stream().map(toPlaylist).forEach(playlistDao::createPlaylist);
                }
                return true;
            });

            /*
             * Should be more than 30 elements.
             */
            assertEquals(32, INDEX_LIST.size());
            assertEquals(32, JpsonicComparatorsTestUtils.JPSONIC_NATURAL_LIST.size());
        }

        /**
         * {@link MainController#getMultiFolderChildren}
         */
        @Test
        void testGetMultiFolderChildren() throws IOException {
            HttpSearchCriteria criteria = director
                .construct("10", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS, IndexType.ARTIST);
            SearchResult result = searchService.search(criteria);
            List<MediaFile> artists = mainController.getMultiFolderChildren(result.getMediaFiles());
            List<String> artistNames = artists
                .stream()
                .map(MediaFile::getName)
                .collect(Collectors.toList());
            assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(artistNames));
        }

        /**
         * {@link PlaylistService#getAllPlaylists()}
         */
        @Test
        void testGetAllPlaylists() {
            List<Playlist> all = playlistService.getAllPlaylists();
            List<String> names = all.stream().map(Playlist::getName).collect(Collectors.toList());
            JpsonicComparatorsTestUtils.validateNaturalList(names, 8, 9);
            /*
             * Since the reading of playlist name cannot be registered, it is sorted
             * according to the reading analysis of the server.
             */
            assertEquals("abc亜伊鵜絵尾", names.get(8));
            assertEquals("abcいうえおあ", names.get(9));
        }

        /**
         * {@link MediaFileService#getChildrenOf(MediaFile, boolean, boolean, boolean, boolean)}
         */
        @Test
        void testGetChildrenOf() throws IOException {
            HttpSearchCriteria criteria = director
                .construct("10", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS, IndexType.ARTIST);
            SearchResult result = searchService.search(criteria);
            List<MediaFile> files = mediaFileService
                .getChildrenOf(result.getMediaFiles().get(0), true, true);
            List<String> albums = files
                .stream()
                .map(MediaFile::getName)
                .collect(Collectors.toList());
            assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(albums));
        }

        /**
         * {@link MusicIndexService#getIndexedDirs(List, boolean)}
         */
        @Test
        void testGetIndexedArtists() {
            List<MusicFolder> musicFoldersToUse = Arrays.asList(MUSIC_FOLDERS.get(0));
            SortedMap<MusicIndex, List<MediaFile>> m = musicIndexService
                .getMusicFolderContent(musicFoldersToUse)
                .getIndexedArtists();
            List<String> artists = m
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(MediaFile::getName)
                .collect(Collectors.toList());
            assertTrue(validateIndexList(artists));
        }

        /**
         * {@link PlayQueue#sort(java.util.Comparator)}
         */
        @Test
        void testPlayQueueSort() throws IOException {
            HttpSearchCriteria criteria = director
                .construct("empty", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS, IndexType.SONG);
            SearchResult result = searchService.search(criteria);
            PlayQueue playQueue = new PlayQueue();
            playQueue.addFiles(true, result.getMediaFiles());
            playQueue.shuffle();
            playQueue.sort(comparators.mediaFileOrderBy(ARTIST));
            List<String> artists = playQueue
                .getFiles()
                .stream()
                .map(MediaFile::getArtist)
                .collect(Collectors.toList());
            assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(artists));
        }

    }

}
