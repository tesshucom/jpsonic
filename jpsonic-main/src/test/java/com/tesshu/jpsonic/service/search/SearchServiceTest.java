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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class SearchServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceTest.class);

    @Nested
    @Order(1)
    class GeneralTest extends AbstractNeedsScan {

        @Autowired
        private AlbumDao albumDao;

        private final MetricRegistry metrics = new MetricRegistry();

        @Autowired
        private MusicFolderDao musicFolderDao;

        @Autowired
        private SearchService searchService;

        @Autowired
        private IndexManager indexManager;

        @Autowired
        private SearchCriteriaDirector director;

        @BeforeEach
        public void setup() {
            populateDatabaseOnlyOnce();
        }

        @SuppressWarnings("PMD.NPathComplexity")
        /*
         * #876 The process from starting the test database to creating Lucene index data includes asynchronous IO. When
         * Travis was used, this case was very sensitive to the heavy load of CI. The method with relatively few
         * troubles was prioritized, and one case became bloated. This case may be split in the future as Junit updates
         * can improve a bit.
         */
        @Test
        void testSearchTypical() throws IOException {

            /*
             * A simple test that is expected to easily detect API syntax differences when updating lucene. Complete
             * route coverage and data coverage in this case alone are not conscious.
             */

            List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();
            assertEquals(3, allMusicFolders.size());

            // *** testSearch() ***

            int offset = 0;
            int count = Integer.MAX_VALUE;

            /*
             * _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble Should find any version of Lucene.
             */
            SearchCriteria searchCriteria = director.construct("Sarah Walker", offset, count, false, allMusicFolders,
                    IndexType.ALBUM);
            SearchResult result = searchService.search(searchCriteria);
            assertEquals(1, result.getTotalHits(),
                    "(0) Specify '" + searchCriteria.getQuery() + "' as query, total Hits is");
            assertEquals(0, result.getArtists().size(),
                    "(1) Specify artist '" + searchCriteria.getQuery() + "' as query. Artist SIZE is");
            assertEquals(0, result.getAlbums().size(),
                    "(2) Specify artist '" + searchCriteria.getQuery() + "' as query. Album SIZE is");
            assertEquals(1, result.getMediaFiles().size(),
                    "(3) Specify artist '" + searchCriteria.getQuery() + "' as query, MediaFile SIZE is");
            assertEquals(MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType(), "(4) ");
            assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getMediaFiles().get(0).getArtist(),
                    "(5) Specify artist '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ");
            assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getMediaFiles().get(0).getAlbumName(),
                    "(6) Specify artist '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ");

            /*
             * _ID3_ALBUM_ Ravel - Chamber Music With Voice Should find any version of Lucene.
             */
            searchCriteria = director.construct("music", offset, count, false, allMusicFolders, IndexType.ALBUM_ID3);
            result = searchService.search(searchCriteria);
            assertEquals(1, result.getTotalHits(),
                    "Specify '" + searchCriteria.getQuery() + "' as query, total Hits is");
            assertEquals(0, result.getArtists().size(),
                    "(7) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. Artist SIZE is ");
            assertEquals(1, result.getAlbums().size(),
                    "(8) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. Album SIZE is ");
            assertEquals(0, result.getMediaFiles().size(),
                    "(9) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. MediaFile SIZE is ");
            assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getAlbums().get(0).getArtist(),
                    "(9) Specify '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ");
            assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getAlbums().get(0).getName(),
                    "(10) Specify '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ");

            /*
             * _ID3_ALBUM_ Ravel - Chamber Music With Voice Should find any version of Lucene.
             */
            searchCriteria = director.construct("Ravel - Chamber Music", offset, count, false, allMusicFolders,
                    IndexType.SONG);
            result = searchService.search(searchCriteria);

            assertEquals(0, // XXX
                    // Legacy
                    // ->
                    // Phrase
                    // Fix
                    // for
                    // missing
                    // cases
                    // (#491)
                    result.getTotalHits(),
                    "(11) Specify album '" + searchCriteria.getQuery() + "' as query, total Hits is");
            assertEquals(0, result.getArtists().size(),
                    "(12) Specify album '" + searchCriteria.getQuery() + "', and get a song. Artist SIZE is");
            assertEquals(0, result.getAlbums().size(),
                    "(13) Specify album '" + searchCriteria.getQuery() + "', and get a song. Album SIZE is");
            assertEquals(0, result.getMediaFiles().size(),
                    "(14) Specify album '" + searchCriteria.getQuery() + "', and get a song. MediaFile SIZE is");
            // XXX Legacy -> Phrase Fix for missing cases (#491)
            // assertEquals("(15) Specify album '" + searchCriteria.getQuery() + "', and get songs. The first song is
            // ", "01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(0).getTitle());
            // assertEquals("(16) Specify album '" + searchCriteria.getQuery() + "', and get songs. The second song
            // is ", "02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getTitle());
            // if ("01 - Gaspard de la Nuit - i. Ondine".equals(result.getMediaFiles().get(0).getName())) {
            // assertEquals("02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getName());
            // } else if ("02 - Gaspard de la Nuit - ii. Le Gibet".equals(result.getMediaFiles().get(0).getName())) {
            // assertEquals("01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(1).getName());
            // } else {
            // fail("Search results are not correct.");
            // }

            // *** testGetRandomSongs() ***

            /*
             * Regardless of the Lucene version, RandomSearchCriteria can specify null and means the maximum range. 11
             * should be obtainable.
             */
            RandomSearchCriteria randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    null, // genre,
                    null, // fromYear
                    null, // toYear
                    allMusicFolders // musicFolders
            );
            List<MediaFile> allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
            assertEquals(11, allRandomSongs.size(),
                    "(22) Specify MAX_VALUE as the upper limit, and randomly acquire songs.");

            /*
             * Regardless of the Lucene version, 7 should be obtainable.
             */
            randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    null, // genre,
                    1900, // fromYear
                    null, // toYear
                    allMusicFolders // musicFolders
            );
            allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
            assertEquals(7, allRandomSongs.size(), "(23) Specify 1900 as 'fromYear', and randomly acquire songs.");

            /*
             * Regardless of the Lucene version, It should be 0 because it is a non-existent genre.
             */
            randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    Arrays.asList("Chamber Music"), // genre,
                    null, // fromYear
                    null, // toYear
                    allMusicFolders // musicFolders
            );
            allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
            assertEquals(0, allRandomSongs.size(), "(24) Specify music as 'genre', and randomly acquire songs.");

            /*
             * Genre including blank. Regardless of the Lucene version, It should be 2.
             */
            randomSearchCriteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    Arrays.asList("Baroque Instrumental"), // genre,
                    null, // fromYear
                    null, // toYear
                    allMusicFolders // musicFolders
            );
            allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
            assertEquals(2, allRandomSongs.size(), "(25) Search by specifying genres including spaces and hyphens.");

            // *** testGetRandomAlbums() ***

            /*
             * Acquisition of maximum number(5).
             */
            List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, allMusicFolders);
            assertEquals(5, allAlbums.size(), "(26) Get all albums with Dao.");
            List<MediaFile> allRandomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
            assertEquals(5, allRandomAlbums.size(),
                    "(27) Specify Integer.MAX_VALUE as the upper limit," + "and randomly acquire albums(file struct).");

            /*
             * Acquisition of maximum number(5).
             */
            List<Album> allRandomAlbumsId3 = searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
            assertEquals(5, allRandomAlbumsId3.size(),
                    "(28) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire albums(ID3).");

            /*
             * Total is 4.
             */
            searchCriteria = director.construct("ID 3 ARTIST", offset, count, false, allMusicFolders,
                    IndexType.ARTIST_ID3);
            result = searchService.search(searchCriteria);
            assertEquals(3, // XXX Legacy(4) ->
                    // Phrase(3) Accuracy
                    // was improved by
                    // considering the
                    // word order
                    result.getTotalHits(), "(29) Specify '" + searchCriteria.getQuery() + "', total Hits is");
            assertEquals(3, // XXX
                            // Legacy(4)
                            // ->
                            // Phrase(3)
                            // Accuracy
                            // was
                            // improved
                            // by
                            // considering
                            // the
                            // word
                            // order
                    result.getArtists().size(),
                    "(30) Specify '" + searchCriteria.getQuery() + "', and get an artists. Artist SIZE is ");
            assertEquals(0, result.getAlbums().size(),
                    "(31) Specify '" + searchCriteria.getQuery() + "', and get a artists. Album SIZE is ");
            assertEquals(0, result.getMediaFiles().size(),
                    "(32) Specify '" + searchCriteria.getQuery() + "', and get a artists. MediaFile SIZE is ");

            /*
             * Three hits to the artist. ALBUMARTIST is not registered with these. Therefore, the registered value of
             * ARTIST is substituted in ALBUMARTIST.
             */
            long l = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ARTIST_")).count();
            assertEquals(3L, l, "(33) Artist whose name contains \\\"_ID3_ARTIST_\\\" is 3 records.");

            /*
             * The structure of "01 - Sonata Violin & Cello I. Allegro.ogg" ARTIST -> _ID3_ARTIST_ Sarah Walker/Nash
             * Ensemble ALBUMARTIST -> _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble (The result must not contain
             * duplicates. And ALBUMARTIST must be returned correctly.)
             */
            l = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ALBUMARTIST_")).count();
            assertEquals(0L, l, "(34) Artist whose name is \"_ID3_ARTIST_\" is 1 records."); // XXX Legacy(1L) ->
                                                                                             // Phrase(0L) Any
                                                                                             // deficiencies in the
                                                                                             // test? Healthy on the
                                                                                             // web.

            /*
             * Below is a simple loop test. How long is the total time?
             */
            int countForEachMethod = 500;
            String[] randomWords4Search = createRandomWords(countForEachMethod);

            Timer globalTimer = metrics.timer(MetricRegistry.name(SearchServiceTest.class, "Timer.global"));

            try (Timer.Context globalTimerContext = globalTimer.time()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("--- Random search (" + countForEachMethod * 4 + " times) ---");
                }

                // testSearch()
                for (String word : randomWords4Search) {
                    searchCriteria = director.construct(word, offset, count, false, allMusicFolders, IndexType.ALBUM);
                    searchService.search(searchCriteria);
                }

                // testGetRandomSongs()
                RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                        null, // genre,
                        null, // fromYear
                        null, // toYear
                        allMusicFolders // musicFolders
                );
                for (int i = 0; i < countForEachMethod; i++) {
                    searchService.getRandomSongs(criteria);
                }

                // testGetRandomAlbums()
                for (int i = 0; i < countForEachMethod; i++) {
                    searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
                }

                // testGetRandomAlbumsId3()
                for (int i = 0; i < countForEachMethod; i++) {
                    searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
                }

                globalTimerContext.stop();
            }

            /*
             * Whether or not IndexReader is exhausted.
             */
            searchCriteria = director.construct("Sarah Walker", offset, count, false, allMusicFolders, IndexType.ALBUM);
            result = searchService.search(searchCriteria);
            assertEquals(0, result.getArtists().size(), "(35) Can the normal case be implemented.");
            assertEquals(0, result.getAlbums().size(), "(36) Can the normal case be implemented.");
            assertEquals(1, result.getMediaFiles().size(), "(37) Can the normal case be implemented.");
            assertEquals(MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType(),
                    "(38) Can the normal case be implemented.");
            assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getMediaFiles().get(0).getArtist(),
                    "(39) Can the normal case be implemented.");

            if (LOG.isInfoEnabled()) {
                LOG.info("--- SUCCESS ---");
            }

            try (ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS).build()) {
                reporter.report();
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("End. ");
            }
        }

        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (word) Not reusable.
        private String[] createRandomWords(int count) {
            String[] randomStrings = new String[count];
            Random random = new Random();
            for (int i = 0; i < count; i++) {
                char[] word = new char[random.nextInt(8) + 3];
                for (int j = 0; j < word.length; j++) {
                    word[j] = (char) ('a' + random.nextInt(26));
                }
                randomStrings[i] = new String(word);
            }
            return randomStrings;
        }

        @Test
        @SuppressWarnings("PMD.CognitiveComplexity")
        void testGenre() {

            List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();

            // #### song ####
            String genre = "Baroque Instrumental";
            List<MediaFile> songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(2, songs.size(), "song - genre : " + genre);

            genre = "Impressionist Era";
            songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(2, songs.size(), "song - genre : " + genre);

            genre = "Gothik Folk Psychobilly";
            songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(3, songs.size(), "song - genre : " + genre);

            genre = "Metal";
            songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, songs.size(), "song - genre : " + genre);
            assertEquals("_ID3_ALBUM_ Chrome Hoof", songs.get(0).getAlbumName(), "album name");

            genre = "Alternative/Indie";
            songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, songs.size(), "song - genre : " + genre);
            assertEquals("_ID3_ALBUM_ Chrome Hoof", songs.get(0).getAlbumName(), "album name");

            // #### file struct album ####
            genre = "Baroque Instrumental";
            List<MediaFile> albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albums.size(), "albums - genre : " + genre);

            genre = "Impressionist Era";
            albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albums.size(), "albums - genre : " + genre);

            genre = "Gothik Folk Psychobilly";
            albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albums.size(), "albums - genre : " + genre);

            genre = "Metal";
            albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            long countOfMetal = albums.size();
            if (countOfMetal == 0) {
                // https://github.com/tesshucom/jpsonic/issues/797
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "In this environment, the genre count of the album may be partially different from other environments.");
                }
            } else {
                // Originally. Or on Windows.
                assertEquals(1, countOfMetal, "albums - genre : " + genre);
                assertEquals("_ID3_ALBUM_ Chrome Hoof", albums.get(0).getAlbumName(), "album name" + genre);
            }

            /* "Metal" and "Alternative/Indie" exist in child of _ID3_ALBUM_ Chrome Hoof */
            genre = "Alternative/Indie";
            albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            long countOfIndie = albums.size();
            /* https://github.com/tesshucom/jpsonic/issues/797 */
            long countOfIndieMagic = 1;
            if (countOfIndie == countOfIndieMagic) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "In this environment, the genre count of the album may be partially different from other environments.");
                }
            } else {
                // Originally. Or on Windows.
                assertEquals(0, albums.size(), "albums - genre : " + genre);
            }

            genre = "TestAlbum";
            albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(0, albums.size(), "albums - genre : " + genre);

            // #### id3 album ####
            genre = "Baroque Instrumental";
            List<Album> albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albumid3s.size(), "albums - genre : " + genre);

            genre = "Impressionist Era";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albumid3s.size(), "albums - genre : " + genre);

            genre = "Gothik Folk Psychobilly";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(1, albumid3s.size(), "albums - genre : " + genre);

            genre = "Metal";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            countOfMetal = albumid3s.size();
            if (countOfMetal == 0) {
                // https://github.com/tesshucom/jpsonic/issues/797
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "In this environment, the genre count of the album may be partially different from other environments.");
                }
            } else {
                // Originally. Or on Windows.
                assertEquals(1, countOfMetal, "albums - genre : " + genre);
                assertEquals("_ID3_ALBUM_ Chrome Hoof", albumid3s.get(0).getName(), "album name" + genre);
            }

            /* "Metal" and "Alternative/Indie" exist in child of _ID3_ALBUM_ Chrome Hoof */
            genre = "Alternative/Indie";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            countOfIndie = albumid3s.size();
            // https://github.com/tesshucom/jpsonic/issues/797
            if (countOfIndie == countOfIndieMagic) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "In this environment, the genre count of the album may be partially different from other environments.");
                }
            } else {
                // Originally. Or on Windows.
                assertEquals(0, albumid3s.size(), "albums - genre : " + genre);
            }

            genre = "TestAlbum";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(0, albumid3s.size(), "albums - genre : " + genre);

            genre = "Rock & Roll";
            albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
            assertEquals(0, albumid3s.size(), "albums - genre : " + genre);

        }

        @Test
        void testGenreMaster() {

            List<Genre> genres = searchService.getGenres(false);
            assertEquals(5, genres.size(), "Song genre size");
            assertEquals(1L, genres.stream().filter(g -> "Gothik Folk Psychobilly".equals(g.getName())).count(),
                    "Song genre: Gothik Folk Psychobilly");
            assertEquals(1L, genres.stream().filter(g -> "Impressionist Era".equals(g.getName())).count(),
                    "Song genre: Impressionist Era");
            assertEquals(1L, genres.stream().filter(g -> "Baroque Instrumental".equals(g.getName())).count(),
                    "Song genre: Baroque Instrumental");
            assertEquals(1L, genres.stream().filter(g -> "Alternative/Indie".equals(g.getName())).count(),
                    "Song genre: Alternative/Indie");
            assertEquals(1L, genres.stream().filter(g -> "Metal".equals(g.getName())).count(), "Song genre: Metal");

            genres = searchService.getGenres(true);
            assertEquals(4, genres.size(), "Album genre size");
            assertEquals(1L, genres.stream().filter(g -> "Gothik Folk Psychobilly".equals(g.getName())).count(),
                    "Album genre: Gothik Folk Psychobilly");
            assertEquals(1L, genres.stream().filter(g -> "Impressionist Era".equals(g.getName())).count(),
                    "Album genre: Impressionist Era");
            assertEquals(1L, genres.stream().filter(g -> "Baroque Instrumental".equals(g.getName())).count(),
                    "Album genre: Baroque Instrumental");
            long countOfMetal = genres.stream().filter(g -> "Metal".equals(g.getName())).count();
            if (countOfMetal == 0) {
                // https://github.com/tesshucom/jpsonic/issues/797
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "In this environment, the genre count of the album may be partially different from other environments.");
                }
            } else {
                // Originally. Or on Windows.
                assertEquals(1L, countOfMetal, "Album genre: Metal");
            }
        }

        @Test
        void testToPreAnalyzedGenres() {

            List<String> preAnalyzedGenres = indexManager
                    .toPreAnalyzedGenres(Arrays.asList("Baroque Instrumental", "Gothik Folk Psychobilly"));
            assertEquals(2, preAnalyzedGenres.size(), "size");
            assertEquals("Baroque Instrumental", preAnalyzedGenres.get(0), "genre1");
            assertEquals("Gothik Folk Psychobilly", preAnalyzedGenres.get(1), "genre2");

            preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList("Baroque Instrumental"));
            assertEquals(1, preAnalyzedGenres.size(), "size");
            assertEquals("Baroque Instrumental", preAnalyzedGenres.get(0), "genre");

            preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList("Baroque"));
            assertEquals(0, preAnalyzedGenres.size(), "size");

        }
    }

    /**
     * Tests to prove what kind of strings/chars can be used in the genre field.
     */
    @Nested
    @Order(2)
    class SpecialGenreTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private SearchService searchService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (isEmpty(musicFolders)) {
                musicFolders = new ArrayList<>();
                File musicDir = new File(resolveBaseMediaPath("Search/SpecialGenre"));
                musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
            }
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            populateDatabase();
        }

        /*
         * There are 19 files in src/test/resources/MEDIAS/Search/SpecialGenre/ARTIST1/ALBUM_A. In FILE01 to FILE16,
         * Special strings for Lucene syntax are stored as tag values ​​of Genre.
         *
         * Legacy can not search all these genres. (Strictly speaking, the genre field is not created at index
         * creation.)
         *
         * // XXX 3.x -> 8.x : Do the process more strictly.
         *
         * - Values ​​that can be cross-referenced with DB are stored in the index. - Search is also possible with
         * user's readable value (file tag value). - However, there is an exception in parentheses.
         */
        @Test
        void testQueryEscapeRequires() {

            Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(
                    Integer.MAX_VALUE, // count
                    Arrays.asList(s), // genre,
                    null, // fromYear
                    null, // toYear
                    getMusicFolders() // musicFolders
            );

            List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("+"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("+", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 1", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("-"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("-", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 2", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("&&"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("&&", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 3", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("||"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("||", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 4", songs.get(0).getTitle());

            /*
             * // XXX 3.x -> 8.x : Brackets ()
             *
             * Lucene can handle these. However, brackets are specially parsed before the index creation process.
             *
             * This string is never stored in the index. This is the only exception.
             */
            songs = searchService.getRandomSongs(simpleStringCriteria.apply(" (")); // space & bracket
            assertEquals(0, songs.size());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply(")"));
            assertEquals(0, songs.size());

            /*
             * // XXX 3.x -> 8.x : Brackets {}[]
             *
             * Lucene can handle these. However, brackets are specially parsed before the index creation process.
             *
             * This can be done with a filter that performs the reverse process on the input values ​​when searching. As
             * a result, the values ​​stored in the file can be retrieved by search.
             *
             * @see AnalyzerFactory <<<<<<<
             * HEAD:jpsonic-main/src/test/java/org/airsonic/player/service/search/SearchServiceSpecialGenreTestCase.java
             *
             *
             * =======
             *
             * >>>>> >>>>>>>
             * ec426fc:airsonic-main/src/test/java/org/airsonic/player/service/search/SearchServiceSpecialGenreTestCase.
             * java
             */
            songs = searchService.getRandomSongs(simpleStringCriteria.apply("{}"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            /*
             * This is the result of the tag parser and domain value. It is different from the tag value in file.
             */
            assertEquals("{ }", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 7", songs.get(0).getTitle());
            songs = searchService.getRandomSongs(simpleStringCriteria.apply("{ }"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("Query Escape Requires 7", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("[]"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            /*
             * This is the result of the tag parser and domain value. It is different from the tag value in file.
             */
            assertEquals("[ ]", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
            songs = searchService.getRandomSongs(simpleStringCriteria.apply("[ ]"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("Query Escape Requires 8", songs.get(0).getTitle());
            // <<<<<

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("^"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("^", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 9", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("\""));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("\"", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 10", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("~"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("~", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 11", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("*"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("*", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 12", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("?"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("?", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 13", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply(":"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals(":", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 14", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("\\"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("\\", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 15", songs.get(0).getTitle());

            songs = searchService.getRandomSongs(simpleStringCriteria.apply("/"));
            assertEquals(1, songs.size()); // XXX 3.x -> 8.x : Searchable
            assertEquals("/", songs.get(0).getGenre());
            assertEquals("Query Escape Requires 16", songs.get(0).getTitle());

        }

        /*
         * Jaudiotagger applies special treatment to bracket (FILE17). XXX 3.x -> 8.x : Specification of genre search
         * became more natural.
         */
        @Test
        void testBrackets() {

            Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(
                    Integer.MAX_VALUE, // count
                    Arrays.asList(s), // genre,
                    null, // fromYear
                    null, // toYear
                    getMusicFolders() // musicFolders
            );

            // -(GENRE)- is registered as genre of FILE17.

            /*
             * Search by genre string registered in file.
             *
             * The value stored in the index is different from legacy. Domain value is kept as it is.
             */
            List<MediaFile> songs = searchService.getRandomSongs(simpleStringCriteria.apply("-(GENRE)-"));
            assertEquals(1, songs.size());
            assertEquals("-GENRE -", songs.get(0).getGenre());
            assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

            /*
             * Search by Domain value.
             */
            songs = searchService.getRandomSongs(simpleStringCriteria.apply("-GENRE -"));
            assertEquals(1, songs.size());
            assertEquals("-GENRE -", songs.get(0).getGenre());
            assertEquals("Consistency with Tag Parser 1", songs.get(0).getTitle());

            /*
             * Legacy genre search
             */
            songs = searchService.getRandomSongs(simpleStringCriteria.apply(" genre"));
            // Strong unique parsing rules have been removed.
            assertEquals(0, songs.size());

        }

        /*
         * Jaudiotagger applies special treatment to numeric. (FILE18)
         */
        @Test
        void testNumericMapping() {

            List<MusicFolder> folders = getMusicFolders();

            RandomSearchCriteria criteria = new RandomSearchCriteria(Integer.MAX_VALUE, // count
                    Arrays.asList("Rock"), // genre,
                    null, // fromYear
                    null, // toYear
                    folders // musicFolders
            );

            List<MediaFile> songs = searchService.getRandomSongs(criteria);
            assertEquals(1, songs.size());
            assertEquals("Numeric mapping specification of genre 1", songs.get(0).getTitle());

            // The value registered in the file is 17
            assertEquals("Rock", songs.get(0).getGenre());

        }

        /*
         * Other special strings. (FILE19)
         *
         * {'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's
         *
         * Legacy stores with Analyze, so searchable characters are different.
         *
         */
        @Test
        void testOthers() {

            Function<String, RandomSearchCriteria> simpleStringCriteria = s -> new RandomSearchCriteria(
                    Integer.MAX_VALUE, // count
                    Arrays.asList(s), // genre,
                    null, // fromYear
                    null, // toYear
                    getMusicFolders() // musicFolders
            );

            // XXX 3.x -> 8.x : Do the process more strictly.
            List<MediaFile> songs = searchService
                    .getRandomSongs(simpleStringCriteria.apply("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's"));
            assertEquals(1, songs.size());
            assertEquals(1, songs.size());
            assertEquals("Other special strings 1", songs.get(0).getTitle());
            assertEquals("{'“『【【】】[︴○◎@ $〒→+]ＦＵＬＬ－ＷＩＤＴＨCæsar's", songs.get(0).getGenre());

            /*
             * Legacy kept "widthcaesar" using their own rules. The previous rule has been discarded.
             */
            songs = searchService.getRandomSongs(simpleStringCriteria.apply("widthcaesar"));
            assertEquals(0, songs.size());

        }
    }

    /**
     * Test cases related to #1139. Confirming whether shuffle search can be performed correctly in MusicFolder
     * containing special strings.
     *
     * (Since the query of getRandomAlbums consists of folder paths only, this verification is easy to perform.)
     *
     * This test case is a FalsePattern for search, but there may be problems with the data flow prior to creating the
     * search index.
     */
    @Nested
    @Order(3)
    class SpecialPathTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private SearchService searchService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (isEmpty(musicFolders)) {
                musicFolders = new ArrayList<>();

                File musicDir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible"));
                musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));

                File music2Dir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible's"));
                musicFolders.add(new MusicFolder(2, music2Dir, "accessible's", true, new Date()));

                File music3Dir = new File(resolveBaseMediaPath("Search/SpecialPath/accessible+s"));
                musicFolders.add(new MusicFolder(3, music3Dir, "accessible+s", true, new Date()));
            }
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            populateDatabase();
        }

        @Test
        void testSpecialCharactersInDirName() {

            List<MusicFolder> folders = getMusicFolders();

            // ALL Songs
            List<MediaFile> randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folders);
            assertEquals(3, randomAlbums.size(), "ALL Albums ");

            // dir - accessible
            List<MusicFolder> folder01 = folders.stream().filter(m -> "accessible".equals(m.getName()))
                    .collect(Collectors.toList());
            randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder01);
            assertEquals(1, randomAlbums.size(), "Albums in \"accessible\" ");

            // dir - accessible's
            List<MusicFolder> folder02 = folders.stream().filter(m -> "accessible's".equals(m.getName()))
                    .collect(Collectors.toList());
            randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder02);
            assertEquals(1, randomAlbums.size(), "Albums in \"accessible's\" ");

            // dir - accessible+s
            List<MusicFolder> folder03 = folders.stream().filter(m -> "accessible+s".equals(m.getName()))
                    .collect(Collectors.toList());
            randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder03);
            assertEquals(1, randomAlbums.size(), "Albums in \"accessible+s\" ");
        }
    }

    /**
     * Test cases related to #1142. The filter is not properly applied when analyzing the query,
     *
     * In the process of hardening the Analyzer implementation, this problem is solved side by side.
     */
    @Nested
    @Order(4)
    class StartWithStopwardsTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private SearchService searchService;

        @Autowired
        private SearchCriteriaDirector director;

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (isEmpty(musicFolders)) {
                musicFolders = new ArrayList<>();
                File musicDir = new File(resolveBaseMediaPath("Search/StartWithStopwards"));
                musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, new Date()));
            }
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            populateDatabase();
        }

        @Test
        void testStartWithStopwards() throws IOException {

            int offset = 0;
            int count = Integer.MAX_VALUE;
            List<MusicFolder> folders = getMusicFolders();

            SearchCriteria criteria = director.construct("will", offset, count, false, folders, IndexType.ARTIST_ID3);
            SearchResult result = searchService.search(criteria);
            // Will hit because Airsonic's stopword is defined(#1235) => This case does not hit because it is a phrase
            // search rather than a term prefix match.
            assertEquals(0, result.getTotalHits(), "Williams hit by \"will\" ");

            // XXX legacy -> phrase
            criteria = director.construct("williams", offset, count, false, folders, IndexType.ARTIST_ID3);
            result = searchService.search(criteria);
            assertEquals(1, result.getTotalHits(), "Williams hit by \"williams\" ");

            criteria = director.construct("the", offset, count, false, folders, IndexType.SONG);
            result = searchService.search(criteria);
            // XXX 3.x -> 8.x : The filter is properly applied to the input(Stopward)
            assertEquals(0, result.getTotalHits(), "Theater hit by \"the\" ");

            criteria = director.construct("willi", offset, count, false, folders, IndexType.ARTIST_ID3);
            result = searchService.search(criteria);
            // XXX 3.x -> 8.x : Normal forward matching => This case does not hit because it is a phrase search rather
            // than
            // a term prefix match.
            assertEquals(0, result.getTotalHits(), "Williams hit by \"Williams\" ");

            criteria = director.construct("thea", offset, count, false, folders, IndexType.SONG);
            result = searchService.search(criteria);
            // XXX 3.x -> 8.x : Normal forward matching
            assertEquals(0, result.getTotalHits(), "Theater hit by \"thea\" "); // => This case does not hit
                                                                                // because
            // it is a phrase search rather than
            // a term prefix match.

            // XXX legacy -> phrase
            criteria = director.construct("theater", offset, count, false, folders, IndexType.SONG);
            result = searchService.search(criteria);
            assertEquals(1, result.getTotalHits(), "Theater hit by \"theater\" ");
        }
    }
}
