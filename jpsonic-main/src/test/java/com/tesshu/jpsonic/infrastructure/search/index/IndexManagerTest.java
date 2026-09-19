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

package com.tesshu.jpsonic.infrastructure.search.index;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.ServerLocaleProvider;
import com.tesshu.jpsonic.domain.type.GenreMasterScope;
import com.tesshu.jpsonic.domain.type.GenreMasterSort;
import com.tesshu.jpsonic.infrastructure.language.I18nSKeys;
import com.tesshu.jpsonic.infrastructure.language.JapaneseReadingProcessor;
import com.tesshu.jpsonic.infrastructure.language.JapaneseReadingUtils;
import com.tesshu.jpsonic.infrastructure.locale.ServerLocaleManager;
import com.tesshu.jpsonic.infrastructure.search.LegacySearch;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.search.criteria.HttpSearchCriteria;
import com.tesshu.jpsonic.infrastructure.search.criteria.HttpSearchCriteriaDirector;
import com.tesshu.jpsonic.infrastructure.search.legacy.LegacySearchResult;
import com.tesshu.jpsonic.infrastructure.search.legacy.SearchServiceUtilities;
import com.tesshu.jpsonic.infrastructure.search.query.QueryFactory;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.persistence.NeedsDB;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import com.tesshu.jpsonic.persistence.api.repository.RatingDao;
import com.tesshu.jpsonic.persistence.base.TemplateWrapper;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.scanner.DirectoryScanProcedure;
import com.tesshu.jpsonic.service.scanner.Id3MetadataScanProcedure;
import com.tesshu.jpsonic.service.scanner.ScanContext;
import net.sf.ehcache.Ehcache;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals",
        "PMD.UseUtilityClass" })
class IndexManagerTest {

    @Documented
    private @interface GetGenresDecisions {
        @interface Conditions {
            @interface Settings {
                @interface IsSortGenresByAlphabet {
                    @interface FALSE {
                    }

                    @interface TRUE {
                    }
                }
            }

            @interface Params {
                @interface SortByAlbum {
                    @interface FALSE {
                    }

                    @interface TRUE {
                    }
                }
            }
        }

        @interface Result {
            @interface GenreSort {
                @interface SongCount {

                }

                @interface AlbumCount {

                }

                @interface SongAlphabetical {
                }

                @interface AlbumAlphabetical {
                }
            }
        }
    }

    /*
     * The implementation is poor and difficult to assert. This is a coverage test
     * for later fix.
     */
    @Nested
    @NeedsDB
    @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
    class GetGenresTest {

        private SettingsFacade settingsFacade;
        private IndexManager indexManager;

        @BeforeEach
        void setup() {
            settingsFacade = SettingsFacadeBuilder.create().build();
            init();
        }

        @Ignore
        void init() {
            QueryFactory queryFactory = new QueryFactory(settingsFacade, null);
            SearchServiceUtilities utils = new SearchServiceUtilities(mock(ArtistDao.class),
                    mock(AlbumDao.class), mock(ArtistProvider.class), mock(AlbumProvider.class),
                    mock(Ehcache.class), null, mock(MediaFileService.class),
                    mock(MediaFileProvider.class));
            JapaneseReadingUtils readingUtils = new JapaneseReadingUtils(settingsFacade);
            JapaneseReadingProcessor proc = new JapaneseReadingProcessor(settingsFacade,
                    readingUtils);
            ServerLocaleProvider serverLocaleProvider = new ServerLocaleManager(settingsFacade);
            JpsonicComparators comparators = new JpsonicComparators(settingsFacade,
                    serverLocaleProvider, proc);

            indexManager = new IndexManager(null, null, queryFactory, utils, comparators,
                    settingsFacade, null, null, null, null);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.FALSE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.FALSE
        @GetGenresDecisions.Result.GenreSort.SongCount
        @Test
        void c00() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withBoolean(SKeys.general.sort.genresByAlphabet, false)
                .build();
            init();

            indexManager.getGenres(false);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.FALSE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.TRUE
        @GetGenresDecisions.Result.GenreSort.AlbumCount
        @Test
        void c01() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withBoolean(SKeys.general.sort.genresByAlphabet, false)
                .build();
            init();

            indexManager.getGenres(true);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.TRUE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.FALSE
        @GetGenresDecisions.Result.GenreSort.SongAlphabetical
        @Test
        void c02() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, Locale.ROOT.getLanguage())
                .withString(I18nSKeys.localeCountry, Locale.ROOT.getCountry())
                .withString(I18nSKeys.localeVariant, Locale.ROOT.getVariant())
                .withBoolean(SKeys.general.sort.genresByAlphabet, true)
                .build();
            init();

            indexManager.getGenres(false);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.TRUE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.TRUE
        @GetGenresDecisions.Result.GenreSort.AlbumAlphabetical
        @Test
        void c03() {
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withString(I18nSKeys.localeLanguage, Locale.ROOT.getLanguage())
                .withString(I18nSKeys.localeCountry, Locale.ROOT.getCountry())
                .withString(I18nSKeys.localeVariant, Locale.ROOT.getVariant())
                .withBoolean(SKeys.general.sort.genresByAlphabet, true)
                .build();
            init();

            indexManager.getGenres(true);
        }
    }

    @Documented
    private @interface CreateGenreMasterDecisions {
        @interface Conditions {
            @interface Criteria {
                @interface Scope {
                    @interface Album {
                    }

                    @interface Song {
                    }
                }

                @interface Sort {
                    @interface Frequency {
                    }

                    @interface Name {
                    }

                    @interface AlbumCount {
                    }

                    @interface SongCount {
                    }
                }
            }
        }
    }

    /*
     * The implementation is poor and difficult to assert. This is a coverage test
     * for later fix.
     */
    @Nested
    class CreateGenreMasterTest extends AbstractNeedsScan {

        @Autowired
        private IndexManager indexManager;

        private final List<MusicFolder> musicFolders = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true,
                    now(), 1, false));
        private final List<com.tesshu.jpsonic.domain.model.MusicFolder> domainMusicFolders = Arrays
            .asList(new com.tesshu.jpsonic.domain.model.MusicFolder(1,
                    resolveBaseMediaPath("MultiGenre"), "MultiGenre", true, now(), 1, false));

        private static boolean populated;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        void setup() {
            if (!populated) {
                populateDatabase();
                populated = true;
            }
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c00() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.ALBUM, GenreMasterSort.NAME);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).name());
            assertEquals("Audiobook - Sports", genres.get(1).name());
            assertEquals("GENRE_A", genres.get(2).name());
            assertEquals("GENRE_B", genres.get(3).name());
            assertEquals("GENRE_C", genres.get(4).name());
            assertEquals("GENRE_D", genres.get(5).name());
            assertEquals("GENRE_E", genres.get(6).name());
            assertEquals("GENRE_F", genres.get(7).name());
            assertEquals("GENRE_G", genres.get(8).name());
            assertEquals("GENRE_H", genres.get(9).name());
            assertEquals("GENRE_I", genres.get(10).name());
            assertEquals("GENRE_J", genres.get(11).name());
            assertEquals("GENRE_K", genres.get(12).name());
            assertEquals("GENRE_L", genres.get(13).name());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.AlbumCount
        @Test
        void c01() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.ALBUM, GenreMasterSort.ALBUM_COUNT);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("GENRE_D", genres.get(0).name());
            assertEquals("GENRE_K", genres.get(1).name());
            assertEquals("GENRE_L", genres.get(2).name());
            assertEquals("Audiobook - Historical", genres.get(3).name());
            assertEquals("Audiobook - Sports", genres.get(4).name());
            assertEquals("GENRE_A", genres.get(5).name());
            assertEquals("GENRE_B", genres.get(6).name());
            assertEquals("GENRE_C", genres.get(7).name());
            assertEquals("GENRE_E", genres.get(8).name());
            assertEquals("GENRE_F", genres.get(9).name());
            assertEquals("GENRE_G", genres.get(10).name());
            assertEquals("GENRE_H", genres.get(11).name());
            assertEquals("GENRE_I", genres.get(12).name());
            assertEquals("GENRE_J", genres.get(13).name());
            assertEquals(2, genres.get(0).albumCount());
            assertEquals(2, genres.get(1).albumCount());
            assertEquals(2, genres.get(2).albumCount());
            assertEquals(1, genres.get(3).albumCount());
            assertEquals(1, genres.get(4).albumCount());
            assertEquals(1, genres.get(5).albumCount());
            assertEquals(1, genres.get(6).albumCount());
            assertEquals(1, genres.get(7).albumCount());
            assertEquals(1, genres.get(8).albumCount());
            assertEquals(1, genres.get(9).albumCount());
            assertEquals(1, genres.get(10).albumCount());
            assertEquals(1, genres.get(11).albumCount());
            assertEquals(1, genres.get(12).albumCount());
            assertEquals(1, genres.get(13).albumCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.SongCount
        @Test
        void c02() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.ALBUM, GenreMasterSort.SONG_COUNT);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("GENRE_A", genres.get(0).name());
            assertEquals("GENRE_D", genres.get(1).name());
            assertEquals("GENRE_E", genres.get(2).name());
            assertEquals("GENRE_F", genres.get(3).name());
            assertEquals("GENRE_K", genres.get(4).name());
            assertEquals("GENRE_L", genres.get(5).name());
            assertEquals("Audiobook - Historical", genres.get(6).name());
            assertEquals("Audiobook - Sports", genres.get(7).name());
            assertEquals("GENRE_B", genres.get(8).name());
            assertEquals("GENRE_C", genres.get(9).name());
            assertEquals("GENRE_G", genres.get(10).name());
            assertEquals("GENRE_H", genres.get(11).name());
            assertEquals("GENRE_I", genres.get(12).name());
            assertEquals("GENRE_J", genres.get(13).name());
            assertEquals(2, genres.get(0).songCount());
            assertEquals(2, genres.get(1).songCount());
            assertEquals(2, genres.get(2).songCount());
            assertEquals(2, genres.get(3).songCount());
            assertEquals(2, genres.get(4).songCount());
            assertEquals(2, genres.get(5).songCount());
            assertEquals(1, genres.get(6).songCount());
            assertEquals(1, genres.get(7).songCount());
            assertEquals(1, genres.get(8).songCount());
            assertEquals(1, genres.get(9).songCount());
            assertEquals(1, genres.get(10).songCount());
            assertEquals(1, genres.get(11).songCount());
            assertEquals(1, genres.get(12).songCount());
            assertEquals(1, genres.get(13).songCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c03() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.SONG, GenreMasterSort.NAME,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.MUSIC);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(13, genres.size());
            assertEquals("GENRE_A", genres.get(0).name());
            assertEquals("GENRE_B", genres.get(1).name());
            assertEquals("GENRE_C", genres.get(2).name());
            assertEquals("GENRE_D", genres.get(3).name());
            assertEquals("GENRE_E", genres.get(4).name());
            assertEquals("GENRE_F", genres.get(5).name());
            assertEquals("GENRE_G", genres.get(6).name());
            assertEquals("GENRE_H", genres.get(7).name());
            assertEquals("GENRE_I", genres.get(8).name());
            assertEquals("GENRE_J", genres.get(9).name());
            assertEquals("GENRE_K", genres.get(10).name());
            assertEquals("GENRE_L", genres.get(11).name());
            assertEquals("NO_ALBUM", genres.get(12).name());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.SongCount
        @Test
        void c04() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.SONG, GenreMasterSort.SONG_COUNT,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.MUSIC);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(13, genres.size());
            assertEquals("GENRE_A", genres.get(0).name());
            assertEquals("GENRE_D", genres.get(1).name());
            assertEquals("GENRE_E", genres.get(2).name());
            assertEquals("GENRE_F", genres.get(3).name());
            assertEquals("GENRE_K", genres.get(4).name());
            assertEquals("GENRE_L", genres.get(5).name());
            assertEquals("GENRE_B", genres.get(6).name());
            assertEquals("GENRE_C", genres.get(7).name());
            assertEquals("GENRE_G", genres.get(8).name());
            assertEquals("GENRE_H", genres.get(9).name());
            assertEquals("GENRE_I", genres.get(10).name());
            assertEquals("GENRE_J", genres.get(11).name());
            assertEquals("NO_ALBUM", genres.get(12).name());
            assertEquals(2, genres.get(0).songCount());
            assertEquals(2, genres.get(1).songCount());
            assertEquals(2, genres.get(2).songCount());
            assertEquals(2, genres.get(3).songCount());
            assertEquals(2, genres.get(4).songCount());
            assertEquals(2, genres.get(5).songCount());
            assertEquals(1, genres.get(6).songCount());
            assertEquals(1, genres.get(7).songCount());
            assertEquals(1, genres.get(8).songCount());
            assertEquals(1, genres.get(9).songCount());
            assertEquals(1, genres.get(10).songCount());
            assertEquals(1, genres.get(11).songCount());
            assertEquals(1, genres.get(12).songCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c05() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.SONG, GenreMasterSort.NAME,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.MUSIC,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.AUDIOBOOK);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(15, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).name());
            assertEquals("Audiobook - Sports", genres.get(1).name());
            assertEquals("GENRE_A", genres.get(2).name());
            assertEquals("GENRE_B", genres.get(3).name());
            assertEquals("GENRE_C", genres.get(4).name());
            assertEquals("GENRE_D", genres.get(5).name());
            assertEquals("GENRE_E", genres.get(6).name());
            assertEquals("GENRE_F", genres.get(7).name());
            assertEquals("GENRE_G", genres.get(8).name());
            assertEquals("GENRE_H", genres.get(9).name());
            assertEquals("GENRE_I", genres.get(10).name());
            assertEquals("GENRE_J", genres.get(11).name());
            assertEquals("GENRE_K", genres.get(12).name());
            assertEquals("GENRE_L", genres.get(13).name());
            assertEquals("NO_ALBUM", genres.get(14).name());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Frequency
        @Test
        void c06() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.ALBUM, GenreMasterSort.FREQUENCY,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.MUSIC,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.AUDIOBOOK);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(14, genres.size());
            for (int i = 0; i < genres.size(); i++) {
                if (i < 3) {
                    assertTrue("GENRE_D".equals(genres.get(i).name())
                            || "GENRE_K".equals(genres.get(i).name())
                            || "GENRE_L".equals(genres.get(i).name()));
                    assertEquals(2, genres.get(i).albumCount());
                } else {
                    assertEquals(1, genres.get(i).albumCount());
                }
            }
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Frequency
        @Test
        void c07() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(domainMusicFolders,
                    GenreMasterScope.SONG, GenreMasterSort.FREQUENCY,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.MUSIC,
                    com.tesshu.jpsonic.domain.model.MediaFile.Type.AUDIOBOOK);
            List<com.tesshu.jpsonic.domain.model.Genre> genres = indexManager
                .createGenreMaster(criteria);
            assertEquals(15, genres.size());
            for (int i = 0; i < genres.size(); i++) {
                if (i < 6) {
                    assertTrue("GENRE_A".equals(genres.get(i).name())
                            || "GENRE_D".equals(genres.get(i).name())
                            || "GENRE_E".equals(genres.get(i).name())
                            || "GENRE_F".equals(genres.get(i).name())
                            || "GENRE_K".equals(genres.get(i).name())
                            || "GENRE_L".equals(genres.get(i).name()));
                    assertEquals(2, genres.get(i).songCount());
                } else {
                    assertEquals(1, genres.get(i).songCount());
                }
            }
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private LegacySearch legacySearch;

        @Autowired
        private IndexManager indexManager;

        @Autowired
        private HttpSearchCriteriaDirector director;

        @Autowired
        private TemplateWrapper template;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Autowired
        private RatingDao ratingDao;

        @Autowired
        private DirectoryScanProcedure directoryScanProcedure;

        @Autowired
        private Id3MetadataScanProcedure id3MetadataScanProcedure;

        private static final String USER_NAME = "admin";

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (ObjectUtils.isEmpty(musicFolders)) {
                musicFolders = Arrays
                    .asList(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(),
                            1, false));
            }
            return musicFolders;
        }

        @BeforeEach
        void setup() {
            populateDatabaseOnlyOnce(() -> {
                return true;
            }, () -> {

                // #1842 Airsonic does not implement Rating expunge

                List<MediaFile> albums = mediaFileDao
                    .getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, getMusicFolders());
                assertEquals(4, albums.size());

                albums.forEach(m -> ratingDao.setRatingForUser(USER_NAME, m, 1));
                assertEquals(4, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders));
                int ratingsCount = template
                    .getJdbcTemplate()
                    .queryForObject(
                            "select count(*) from user_rating where user_rating.username = ?",
                            Integer.class, USER_NAME);
                assertEquals(4, ratingsCount, "Because explicitly registered 4 Ratings.");

                // Register a dummy rate (reproduce old path data by moving files)
                MediaFile dummyMediaFile = new MediaFile();
                dummyMediaFile.setPathString("oldPath");
                ratingDao.setRatingForUser(USER_NAME, dummyMediaFile, 1);

                assertEquals(4, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders),
                        "Because the SELECT condition only references real paths.");
                ratingsCount = template
                    .getJdbcTemplate()
                    .queryForObject(
                            "select count(*) from user_rating where user_rating.username = ?",
                            Integer.class, USER_NAME);
                assertEquals(5, ratingsCount,
                        "Because counted directly, including non-existent paths.");

                return true;
            });

        }

        /**
         * This test was originally created to cover the now-obsolete expungeService,
         * which has already been removed. Although the expungeService no longer exists,
         * an equivalent process is expected to run automatically as part of the scan.
         * Therefore, in the current implementation, invoking a scan under similar
         * conditions should result in the removal of unnecessary data from both the
         * database and the Lucene index.
         */
        @Order(1)
        @Test
        void testExpunge() throws IOException {

            int offset = 0;
            int count = Integer.MAX_VALUE;

            final HttpSearchCriteria criteriaArtist = director
                .construct("_DIR_ Ravel", offset, count, false, getMusicFolders(),
                        IndexType.ARTIST);
            final HttpSearchCriteria criteriaAlbum = director
                .construct("Complete Piano Works", offset, count, false, musicFolders,
                        IndexType.ALBUM);
            final HttpSearchCriteria criteriaSong = director
                .construct("Gaspard", offset, count, false, musicFolders, IndexType.SONG);
            final HttpSearchCriteria criteriaArtistId3 = director
                .construct("_DIR_ Ravel", offset, count, false, musicFolders, IndexType.ARTIST_ID3);
            final HttpSearchCriteria criteriaAlbumId3 = director
                .construct("Complete Piano Works", offset, count, false, musicFolders,
                        IndexType.ALBUM_ID3);

            /* Delete DB record. */

            // artist
            LegacySearchResult result = legacySearch.search(criteriaArtist);
            assertEquals(1, result.getMediaFiles().size());
            assertEquals("_DIR_ Ravel", result.getMediaFiles().get(0).getName());

            List<Integer> candidates = mediaFileDao.getArtistExpungeCandidates();
            assertEquals(0, candidates.size());

            result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

            candidates = mediaFileDao.getArtistExpungeCandidates();
            assertEquals(1, candidates.size());

            // album
            result = legacySearch.search(criteriaAlbum);
            assertEquals(1, result.getMediaFiles().size());
            assertEquals("_DIR_ Ravel - Complete Piano Works",
                    result.getMediaFiles().get(0).getName());

            candidates = mediaFileDao.getAlbumExpungeCandidates();
            assertEquals(0, candidates.size());

            result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

            candidates = mediaFileDao.getAlbumExpungeCandidates();
            assertEquals(1, candidates.size());

            // song
            result = legacySearch.search(criteriaSong);
            assertEquals(2, result.getMediaFiles().size());
            if ("01 - Gaspard de la Nuit - i. Ondine"
                .equals(result.getMediaFiles().get(0).getName())) {
                assertEquals("02 - Gaspard de la Nuit - ii. Le Gibet",
                        result.getMediaFiles().get(1).getName());
            } else if ("02 - Gaspard de la Nuit - ii. Le Gibet"
                .equals(result.getMediaFiles().get(0).getName())) {
                assertEquals("01 - Gaspard de la Nuit - i. Ondine",
                        result.getMediaFiles().get(1).getName());
            } else {
                Assertions.fail("Search results are not correct.");
            }

            candidates = mediaFileDao.getSongExpungeCandidates();
            assertEquals(0, candidates.size());

            result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

            candidates = mediaFileDao.getSongExpungeCandidates();
            assertEquals(2, candidates.size());

            // artistid3
            result = legacySearch.search(criteriaArtistId3);
            assertEquals(1, result.getArtists().size());
            assertEquals("_DIR_ Ravel", result.getArtists().get(0).getName());

            // albumId3
            result = legacySearch.search(criteriaAlbumId3);
            assertEquals(1, result.getAlbums().size());
            assertEquals("Complete Piano Works", result.getAlbums().get(0).getName());

            // The following reproduces the behavior of the expungeService.
            // Note the processing order.
            // ->
            ScanContext scanContext = new ScanContext(now(), false, USER_NAME, false, false, 0, 0,
                    false, false);
            indexManager.startIndexing();
            id3MetadataScanProcedure.iterateAlbumId3(scanContext, false);
            id3MetadataScanProcedure.iterateArtistId3(scanContext, false);
            directoryScanProcedure.iterateFileStructure(scanContext);
            indexManager.stopIndexing();
            ratingDao.expunge();
            template.checkpoint();
            // <-

            result = legacySearch.search(criteriaArtist);
            assertEquals(0, result.getMediaFiles().size());

            result = legacySearch.search(criteriaAlbum);
            assertEquals(0, result.getMediaFiles().size());

            result = legacySearch.search(criteriaSong);
            assertEquals(0, result.getMediaFiles().size());

            result = legacySearch.search(criteriaArtistId3);
            assertEquals(0, result.getArtists().size());

            result = legacySearch.search(criteriaAlbumId3);
            assertEquals(0, result.getAlbums().size());

            // See this#setup
            assertEquals(0, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders),
                    "Because one album has been deleted.");
            int ratingsCount = template
                .getJdbcTemplate()
                .queryForObject("select count(*) from user_rating where user_rating.username = ?",
                        Integer.class, USER_NAME);
            assertEquals(0, ratingsCount, "Will be removed, including oldPath");
        }
    }
}
