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

package com.tesshu.jpsonic.service.search;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals",
        "PMD.UseUtilityClass" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexManagerTest {

    @BeforeAll
    public static void setUpOnce() throws InterruptedException {
        SettingsService.setDevelopmentMode(true);
    }

    @AfterAll
    public static void tearDownOnce() throws InterruptedException {
        SettingsService.setDevelopmentMode(false);
    }

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
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Nested
    @ExtendWith(NeedsHome.class)
    class GetGenresTest {

        private SettingsService settingsService;
        private IndexManager indexManager;

        @BeforeEach
        public void setup() {
            settingsService = Mockito.mock(SettingsService.class);
            QueryFactory queryFactory = new QueryFactory(settingsService, null);
            SearchServiceUtilities utils = new SearchServiceUtilities(null, null,
                    Mockito.mock(Ehcache.class), null, null);
            JapaneseReadingUtils readingUtils = new JapaneseReadingUtils(settingsService);
            JpsonicComparators comparators = new JpsonicComparators(settingsService, readingUtils);
            indexManager = new IndexManager(new LuceneUtils(), null, null, queryFactory, utils,
                    comparators, settingsService, null, null, null);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.FALSE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.FALSE
        @GetGenresDecisions.Result.GenreSort.SongCount
        @Test
        void c00() {
            Mockito.when(settingsService.isSortGenresByAlphabet()).thenReturn(false);
            indexManager.getGenres(false);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.FALSE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.TRUE
        @GetGenresDecisions.Result.GenreSort.AlbumCount
        @Test
        void c01() {
            Mockito.when(settingsService.isSortGenresByAlphabet()).thenReturn(false);
            indexManager.getGenres(true);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.TRUE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.FALSE
        @GetGenresDecisions.Result.GenreSort.SongAlphabetical
        @Test
        void c02() {

            Mockito.when(settingsService.getLocale()).thenReturn(Locale.ROOT);
            Mockito.when(settingsService.isSortGenresByAlphabet()).thenReturn(true);
            indexManager.getGenres(false);
        }

        @GetGenresDecisions.Conditions.Settings.IsSortGenresByAlphabet.TRUE
        @GetGenresDecisions.Conditions.Params.SortByAlbum.TRUE
        @GetGenresDecisions.Result.GenreSort.AlbumAlphabetical
        @Test
        void c03() {

            Mockito.when(settingsService.getLocale()).thenReturn(Locale.ROOT);
            Mockito.when(settingsService.isSortGenresByAlphabet()).thenReturn(true);
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
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Nested
    class CreateGenreMasterTest extends AbstractNeedsScan {

        @Autowired
        private IndexManager indexManager;

        private final List<MusicFolder> musicFolders = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true,
                    now(), 1, false));

        private static boolean populated;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            if (!populated) {
                populateDatabase();
                populated = true;
            }
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c00() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                    Sort.NAME);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals("GENRE_K", genres.get(12).getName());
            assertEquals("GENRE_L", genres.get(13).getName());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.AlbumCount
        @Test
        void c01() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                    Sort.ALBUM_COUNT);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("GENRE_D", genres.get(0).getName());
            assertEquals("GENRE_K", genres.get(1).getName());
            assertEquals("GENRE_L", genres.get(2).getName());
            assertEquals("Audiobook - Historical", genres.get(3).getName());
            assertEquals("Audiobook - Sports", genres.get(4).getName());
            assertEquals("GENRE_A", genres.get(5).getName());
            assertEquals("GENRE_B", genres.get(6).getName());
            assertEquals("GENRE_C", genres.get(7).getName());
            assertEquals("GENRE_E", genres.get(8).getName());
            assertEquals("GENRE_F", genres.get(9).getName());
            assertEquals("GENRE_G", genres.get(10).getName());
            assertEquals("GENRE_H", genres.get(11).getName());
            assertEquals("GENRE_I", genres.get(12).getName());
            assertEquals("GENRE_J", genres.get(13).getName());
            assertEquals(2, genres.get(0).getAlbumCount());
            assertEquals(2, genres.get(1).getAlbumCount());
            assertEquals(2, genres.get(2).getAlbumCount());
            assertEquals(1, genres.get(3).getAlbumCount());
            assertEquals(1, genres.get(4).getAlbumCount());
            assertEquals(1, genres.get(5).getAlbumCount());
            assertEquals(1, genres.get(6).getAlbumCount());
            assertEquals(1, genres.get(7).getAlbumCount());
            assertEquals(1, genres.get(8).getAlbumCount());
            assertEquals(1, genres.get(9).getAlbumCount());
            assertEquals(1, genres.get(10).getAlbumCount());
            assertEquals(1, genres.get(11).getAlbumCount());
            assertEquals(1, genres.get(12).getAlbumCount());
            assertEquals(1, genres.get(13).getAlbumCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.SongCount
        @Test
        void c02() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                    Sort.SONG_COUNT);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(14, genres.size());
            assertEquals("GENRE_A", genres.get(0).getName());
            assertEquals("GENRE_D", genres.get(1).getName());
            assertEquals("GENRE_E", genres.get(2).getName());
            assertEquals("GENRE_F", genres.get(3).getName());
            assertEquals("GENRE_K", genres.get(4).getName());
            assertEquals("GENRE_L", genres.get(5).getName());
            assertEquals("Audiobook - Historical", genres.get(6).getName());
            assertEquals("Audiobook - Sports", genres.get(7).getName());
            assertEquals("GENRE_B", genres.get(8).getName());
            assertEquals("GENRE_C", genres.get(9).getName());
            assertEquals("GENRE_G", genres.get(10).getName());
            assertEquals("GENRE_H", genres.get(11).getName());
            assertEquals("GENRE_I", genres.get(12).getName());
            assertEquals("GENRE_J", genres.get(13).getName());
            assertEquals(2, genres.get(0).getSongCount());
            assertEquals(2, genres.get(1).getSongCount());
            assertEquals(2, genres.get(2).getSongCount());
            assertEquals(2, genres.get(3).getSongCount());
            assertEquals(2, genres.get(4).getSongCount());
            assertEquals(2, genres.get(5).getSongCount());
            assertEquals(1, genres.get(6).getSongCount());
            assertEquals(1, genres.get(7).getSongCount());
            assertEquals(1, genres.get(8).getSongCount());
            assertEquals(1, genres.get(9).getSongCount());
            assertEquals(1, genres.get(10).getSongCount());
            assertEquals(1, genres.get(11).getSongCount());
            assertEquals(1, genres.get(12).getSongCount());
            assertEquals(1, genres.get(13).getSongCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c03() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.SONG,
                    Sort.NAME, MediaType.MUSIC);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(13, genres.size());
            assertEquals("GENRE_A", genres.get(0).getName());
            assertEquals("GENRE_B", genres.get(1).getName());
            assertEquals("GENRE_C", genres.get(2).getName());
            assertEquals("GENRE_D", genres.get(3).getName());
            assertEquals("GENRE_E", genres.get(4).getName());
            assertEquals("GENRE_F", genres.get(5).getName());
            assertEquals("GENRE_G", genres.get(6).getName());
            assertEquals("GENRE_H", genres.get(7).getName());
            assertEquals("GENRE_I", genres.get(8).getName());
            assertEquals("GENRE_J", genres.get(9).getName());
            assertEquals("GENRE_K", genres.get(10).getName());
            assertEquals("GENRE_L", genres.get(11).getName());
            assertEquals("NO_ALBUM", genres.get(12).getName());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.SongCount
        @Test
        void c04() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.SONG,
                    Sort.SONG_COUNT, MediaType.MUSIC);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(13, genres.size());
            assertEquals("GENRE_A", genres.get(0).getName());
            assertEquals("GENRE_D", genres.get(1).getName());
            assertEquals("GENRE_E", genres.get(2).getName());
            assertEquals("GENRE_F", genres.get(3).getName());
            assertEquals("GENRE_K", genres.get(4).getName());
            assertEquals("GENRE_L", genres.get(5).getName());
            assertEquals("GENRE_B", genres.get(6).getName());
            assertEquals("GENRE_C", genres.get(7).getName());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals("NO_ALBUM", genres.get(12).getName());
            assertEquals(2, genres.get(0).getSongCount());
            assertEquals(2, genres.get(1).getSongCount());
            assertEquals(2, genres.get(2).getSongCount());
            assertEquals(2, genres.get(3).getSongCount());
            assertEquals(2, genres.get(4).getSongCount());
            assertEquals(2, genres.get(5).getSongCount());
            assertEquals(1, genres.get(6).getSongCount());
            assertEquals(1, genres.get(7).getSongCount());
            assertEquals(1, genres.get(8).getSongCount());
            assertEquals(1, genres.get(9).getSongCount());
            assertEquals(1, genres.get(10).getSongCount());
            assertEquals(1, genres.get(11).getSongCount());
            assertEquals(1, genres.get(12).getSongCount());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Name
        @Test
        void c05() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.SONG,
                    Sort.NAME, MediaType.MUSIC, MediaType.AUDIOBOOK);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(15, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals("GENRE_K", genres.get(12).getName());
            assertEquals("GENRE_L", genres.get(13).getName());
            assertEquals("NO_ALBUM", genres.get(14).getName());
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Album
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Frequency
        @Test
        void c06() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                    Sort.FREQUENCY, MediaType.MUSIC, MediaType.AUDIOBOOK);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(14, genres.size());
            for (int i = 0; i < genres.size(); i++) {
                if (i < 3) {
                    assertTrue("GENRE_D".equals(genres.get(i).getName())
                            || "GENRE_K".equals(genres.get(i).getName())
                            || "GENRE_L".equals(genres.get(i).getName()));
                    assertEquals(2, genres.get(i).getAlbumCount());
                } else {
                    assertEquals(1, genres.get(i).getAlbumCount());
                }
            }
        }

        @CreateGenreMasterDecisions.Conditions.Criteria.Scope.Song
        @CreateGenreMasterDecisions.Conditions.Criteria.Sort.Frequency
        @Test
        void c07() {
            GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.SONG,
                    Sort.FREQUENCY, MediaType.MUSIC, MediaType.AUDIOBOOK);
            List<Genre> genres = indexManager.createGenreMaster(criteria);
            assertEquals(15, genres.size());
            for (int i = 0; i < genres.size(); i++) {
                if (i < 6) {
                    assertTrue("GENRE_A".equals(genres.get(i).getName())
                            || "GENRE_D".equals(genres.get(i).getName())
                            || "GENRE_E".equals(genres.get(i).getName())
                            || "GENRE_F".equals(genres.get(i).getName())
                            || "GENRE_K".equals(genres.get(i).getName())
                            || "GENRE_L".equals(genres.get(i).getName()));
                    assertEquals(2, genres.get(i).getSongCount());
                } else {
                    assertEquals(1, genres.get(i).getSongCount());
                }
            }
        }
    }

    @Nested
    class UnitTest {

        private ArtistDao artistDao;
        private IndexManager indexManager;

        @BeforeEach
        public void setup() {
            SettingsService settingsService = Mockito.mock(SettingsService.class);
            artistDao = mock(ArtistDao.class);
            indexManager = new IndexManager(new LuceneUtils(), null, null, null, null, null,
                    settingsService, null, artistDao, null);
        }

        @AfterEach
        public void trarDown() {
            System.clearProperty("jpsonic.home");
        }

        @Test
        void testIndexDirectoryAlreadyExists(@TempDir Path tempDir) throws IOException {
            System.setProperty("jpsonic.home", tempDir.toString());
            Files.createDirectories(indexManager.getRootIndexDirectory());
            indexManager.initializeIndexDirectory();
            Mockito.verify(artistDao, Mockito.never()).deleteAll();
        }

        @Test
        void testIndexDirectoryNotExists(@TempDir Path tempDir) {
            System.setProperty("jpsonic.home", tempDir.toString());
            indexManager.initializeIndexDirectory();
            Mockito.verify(artistDao, Mockito.times(1)).deleteAll();
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private SearchService searchService;

        @Autowired
        private IndexManager indexManager;

        @Autowired
        private HttpSearchCriteriaDirector director;

        @Autowired
        private MediaScannerService mediaScannerService;

        @Autowired
        private TemplateWrapper template;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Autowired
        private RatingDao ratingDao;

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
        public void setup() {
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

        @Test
        @Order(1)
        void testExpunge() throws IOException {

            int offset = 0;
            int count = Integer.MAX_VALUE;

            final HttpSearchCriteria criteriaArtist = director
                .construct("_DIR_ Ravel", offset, count, false, musicFolders, IndexType.ARTIST);
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
            SearchResult result = searchService.search(criteriaArtist);
            assertEquals(1, result.getMediaFiles().size());
            assertEquals("_DIR_ Ravel", result.getMediaFiles().get(0).getName());

            List<Integer> candidates = mediaFileDao.getArtistExpungeCandidates();
            assertEquals(0, candidates.size());

            result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

            candidates = mediaFileDao.getArtistExpungeCandidates();
            assertEquals(1, candidates.size());

            // album
            result = searchService.search(criteriaAlbum);
            assertEquals(1, result.getMediaFiles().size());
            assertEquals("_DIR_ Ravel - Complete Piano Works",
                    result.getMediaFiles().get(0).getName());

            candidates = mediaFileDao.getAlbumExpungeCandidates();
            assertEquals(0, candidates.size());

            result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

            candidates = mediaFileDao.getAlbumExpungeCandidates();
            assertEquals(1, candidates.size());

            // song
            result = searchService.search(criteriaSong);
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
            result = searchService.search(criteriaArtistId3);
            assertEquals(1, result.getArtists().size());
            assertEquals("_DIR_ Ravel", result.getArtists().get(0).getName());

            // albumId3
            result = searchService.search(criteriaAlbumId3);
            assertEquals(1, result.getAlbums().size());
            assertEquals("Complete Piano Works", result.getAlbums().get(0).getName());

            /* Does not scan, only expunges the index. */
            mediaScannerService.expunge();

            /*
             * Subsequent search results. Results can also be confirmed with Luke.
             */

            result = searchService.search(criteriaArtist);
            assertEquals(0, result.getMediaFiles().size());

            result = searchService.search(criteriaAlbum);
            assertEquals(0, result.getMediaFiles().size());

            result = searchService.search(criteriaSong);
            assertEquals(0, result.getMediaFiles().size());

            result = searchService.search(criteriaArtistId3);
            assertEquals(0, result.getArtists().size());

            result = searchService.search(criteriaAlbumId3);
            assertEquals(0, result.getAlbums().size());

            // See this#setup
            assertEquals(3, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders),
                    "Because one album has been deleted.");
            int ratingsCount = template
                .getJdbcTemplate()
                .queryForObject("select count(*) from user_rating where user_rating.username = ?",
                        Integer.class, USER_NAME);
            assertEquals(3, ratingsCount, "Will be removed, including oldPath");
        }

        @Test
        @Order(2)
        void testDeleteLegacyFiles() throws ExecutionException, IOException {
            // Remove the index used in the early days of Airsonic(Close to Subsonic)
            Path legacyFile = Path.of(SettingsService.getJpsonicHome().toString(), "lucene2");
            if (Files.createFile(legacyFile) != null) {
                assertTrue(Files.exists(legacyFile));
            } else {
                Assertions.fail();
            }
            Path legacyDir = Path.of(SettingsService.getJpsonicHome().toString(), "lucene3");
            FileUtil.createDirectories(legacyDir);

            indexManager.deleteLegacyFiles();
            assertFalse(Files.exists(legacyFile));
            assertFalse(Files.exists(legacyDir));
        }

        @Test
        @Order(3)
        void testDeleteOldFiles() throws ExecutionException, IOException {
            // If the index version does not match, delete it
            Path oldDir = Path.of(SettingsService.getJpsonicHome().toString(), "index-JP22");
            if (FileUtil.createDirectories(oldDir) != null) {
                assertTrue(Files.exists(oldDir));
            } else {
                Assertions.fail();
            }
            indexManager.deleteOldFiles();
            assertFalse(Files.exists(oldDir));
        }
    }
}
