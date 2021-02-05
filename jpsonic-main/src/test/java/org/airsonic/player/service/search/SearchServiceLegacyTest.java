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

package org.airsonic.player.service.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchServiceLegacyTest extends AbstractAirsonicHomeTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceLegacyTest.class);

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

    @Autowired
    private SettingsService settingsService;

    @Before
    public void setup() {
        settingsService.setSearchMethodLegacy(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testSearchTypical() throws IOException {

        /*
         * A simple test that is expected to easily detect API syntax differences when updating lucene. Complete route
         * coverage and data coverage in this case alone are not conscious.
         */

        List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();
        Assert.assertEquals(3, allMusicFolders.size());

        // *** testSearch() ***

        int offset = 0;
        int count = Integer.MAX_VALUE;
        boolean includeComposer = false;

        /*
         * _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble Should find any version of Lucene.
         */
        SearchCriteria searchCriteria = director.construct("Sarah Walker", offset, count, includeComposer,
                allMusicFolders, IndexType.ALBUM);
        SearchResult result = searchService.search(searchCriteria);
        Assert.assertEquals("(0) Specify '" + searchCriteria.getQuery() + "' as query, total Hits is", 1,
                result.getTotalHits());
        Assert.assertEquals("(1) Specify artist '" + searchCriteria.getQuery() + "' as query. Artist SIZE is", 0,
                result.getArtists().size());
        Assert.assertEquals("(2) Specify artist '" + searchCriteria.getQuery() + "' as query. Album SIZE is", 0,
                result.getAlbums().size());
        Assert.assertEquals("(3) Specify artist '" + searchCriteria.getQuery() + "' as query, MediaFile SIZE is", 1,
                result.getMediaFiles().size());
        Assert.assertEquals("(4) ", MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType());
        Assert.assertEquals(
                "(5) Specify artist '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ",
                "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getMediaFiles().get(0).getArtist());
        Assert.assertEquals(
                "(6) Specify artist '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ",
                "_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getMediaFiles().get(0).getAlbumName());

        /*
         * _ID3_ALBUM_ Ravel - Chamber Music With Voice Should find any version of Lucene.
         */
        searchCriteria = director.construct("music", offset, count, includeComposer, allMusicFolders,
                IndexType.ALBUM_ID3);
        result = searchService.search(searchCriteria);
        Assert.assertEquals("Specify '" + searchCriteria.getQuery() + "' as query, total Hits is", 1,
                result.getTotalHits());
        Assert.assertEquals("(7) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. Artist SIZE is ",
                0, result.getArtists().size());
        Assert.assertEquals("(8) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. Album SIZE is ",
                1, result.getAlbums().size());
        Assert.assertEquals(
                "(9) Specify '" + searchCriteria.getQuery() + "' as query, and get a song. MediaFile SIZE is ", 0,
                result.getMediaFiles().size());
        Assert.assertEquals("(9) Specify '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ",
                "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getAlbums().get(0).getArtist());
        Assert.assertEquals("(10) Specify '" + searchCriteria.getQuery() + "' as query, and get a album. Name is ",
                "_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getAlbums().get(0).getName());

        /*
         * _ID3_ALBUM_ Ravel - Chamber Music With Voice Should find any version of Lucene.
         */
        searchCriteria = director.construct("Ravel - Chamber Music", offset, count, includeComposer, allMusicFolders,
                IndexType.SONG);
        result = searchService.search(searchCriteria);
        Assert.assertEquals("(11) Specify album '" + searchCriteria.getQuery() + "' as query, total Hits is", 2,
                result.getTotalHits());
        Assert.assertEquals("(12) Specify album '" + searchCriteria.getQuery() + "', and get a song. Artist SIZE is", 0,
                result.getArtists().size());
        Assert.assertEquals("(13) Specify album '" + searchCriteria.getQuery() + "', and get a song. Album SIZE is", 0,
                result.getAlbums().size());
        Assert.assertEquals("(14) Specify album '" + searchCriteria.getQuery() + "', and get a song. MediaFile SIZE is",
                2, result.getMediaFiles().size());
        // Assert.assertEquals("(15) Specify album '" + searchCriteria.getQuery() + "', and get songs. The first song is
        // ", "01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(0).getTitle());
        // Assert.assertEquals("(16) Specify album '" + searchCriteria.getQuery() + "', and get songs. The second song
        // is ", "02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getTitle());
        if ("01 - Gaspard de la Nuit - i. Ondine".equals(result.getMediaFiles().get(0).getName())) {
            assertEquals("02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getName());
        } else if ("02 - Gaspard de la Nuit - ii. Le Gibet".equals(result.getMediaFiles().get(0).getName())) {
            assertEquals("01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(1).getName());
        } else {
            fail("Search results are not correct.");
        }

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
        Assert.assertEquals("(22) Specify MAX_VALUE as the upper limit, and randomly acquire songs.", 11,
                allRandomSongs.size());

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
        Assert.assertEquals("(23) Specify 1900 as 'fromYear', and randomly acquire songs.", 7, allRandomSongs.size());

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
        Assert.assertEquals("(24) Specify music as 'genre', and randomly acquire songs.", 0, allRandomSongs.size());

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
        Assert.assertEquals("(25) Search by specifying genres including spaces and hyphens.", 2, allRandomSongs.size());

        // *** testGetRandomAlbums() ***

        /*
         * Acquisition of maximum number(5).
         */
        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, allMusicFolders);
        Assert.assertEquals("(26) Get all albums with Dao.", 5, allAlbums.size());
        List<MediaFile> allRandomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals(
                "(27) Specify Integer.MAX_VALUE as the upper limit," + "and randomly acquire albums(file struct).", 5,
                allRandomAlbums.size());

        /*
         * Acquisition of maximum number(5).
         */
        List<Album> allRandomAlbumsId3 = searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("(28) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire albums(ID3).", 5,
                allRandomAlbumsId3.size());

        /*
         * Total is 4.
         */
        searchCriteria = director.construct("ID 3 ARTIST", offset, count, includeComposer, allMusicFolders,
                IndexType.ARTIST_ID3);
        result = searchService.search(searchCriteria);
        Assert.assertEquals("(29) Specify '" + searchCriteria.getQuery() + "', total Hits is", 4,
                result.getTotalHits());
        Assert.assertEquals("(30) Specify '" + searchCriteria.getQuery() + "', and get an artists. Artist SIZE is ", 4,
                result.getArtists().size());
        Assert.assertEquals("(31) Specify '" + searchCriteria.getQuery() + "', and get a artists. Album SIZE is ", 0,
                result.getAlbums().size());
        Assert.assertEquals("(32) Specify '" + searchCriteria.getQuery() + "', and get a artists. MediaFile SIZE is ",
                0, result.getMediaFiles().size());

        /*
         * Three hits to the artist. ALBUMARTIST is not registered with these. Therefore, the registered value of ARTIST
         * is substituted in ALBUMARTIST.
         */
        long l = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ARTIST_")).count();
        Assert.assertEquals("(33) Artist whose name contains \\\"_ID3_ARTIST_\\\" is 3 records.", 3L, l);

        /*
         * The structure of "01 - Sonata Violin & Cello I. Allegro.ogg" ARTIST -> _ID3_ARTIST_ Sarah Walker/Nash
         * Ensemble ALBUMARTIST -> _ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble (The result must not contain duplicates.
         * And ALBUMARTIST must be returned correctly.)
         */
        l = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ALBUMARTIST_")).count();
        Assert.assertEquals("(34) Artist whose name is \"_ID3_ARTIST_\" is 1 records.", 1L, l);

        /*
         * Below is a simple loop test. How long is the total time?
         */
        int countForEachMethod = 500;
        String[] randomWords4Search = createRandomWords(countForEachMethod);

        Timer globalTimer = metrics.timer(MetricRegistry.name(SearchServiceLegacyTest.class, "Timer.global"));
        final Timer.Context globalTimerContext = globalTimer.time();

        if (LOG.isInfoEnabled()) {
            LOG.info("--- Random search (" + countForEachMethod * 4 + " times) ---");
        }

        // testSearch()
        for (String word : randomWords4Search) {
            searchCriteria = director.construct(word, offset, count, includeComposer, allMusicFolders, IndexType.ALBUM);
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

        /*
         * Whether or not IndexReader is exhausted.
         */
        searchCriteria = director.construct("Sarah Walker", offset, count, includeComposer, allMusicFolders,
                IndexType.ALBUM);
        result = searchService.search(searchCriteria);
        Assert.assertEquals("(35) Can the normal case be implemented.", 0, result.getArtists().size());
        Assert.assertEquals("(36) Can the normal case be implemented.", 0, result.getAlbums().size());
        Assert.assertEquals("(37) Can the normal case be implemented.", 1, result.getMediaFiles().size());
        Assert.assertEquals("(38) Can the normal case be implemented.", MediaType.ALBUM,
                result.getMediaFiles().get(0).getMediaType());
        Assert.assertEquals("(39) Can the normal case be implemented.", "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble",
                result.getMediaFiles().get(0).getArtist());

        if (LOG.isInfoEnabled()) {
            LOG.info("--- SUCCESS ---");
        }

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
        reporter.report();

        if (LOG.isInfoEnabled()) {
            LOG.info("End. ");
        }
    }

    private static String[] createRandomWords(int count) {
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
    public void testGenre() {

        List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();

        // #### song ####
        String genre = "Baroque Instrumental";
        List<MediaFile> songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("song - genre : " + genre, 2, songs.size());

        genre = "Impressionist Era";
        songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("song - genre : " + genre, 2, songs.size());

        genre = "Gothik Folk Psychobilly";
        songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("song - genre : " + genre, 3, songs.size());

        genre = "Metal";
        songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("song - genre : " + genre, 1, songs.size());
        Assert.assertEquals("album name", "_ID3_ALBUM_ Chrome Hoof", songs.get(0).getAlbumName());

        genre = "Alternative/Indie";
        songs = searchService.getSongsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("song - genre : " + genre, 1, songs.size());
        Assert.assertEquals("album name", "_ID3_ALBUM_ Chrome Hoof", songs.get(0).getAlbumName());

        // #### file struct album ####
        genre = "Baroque Instrumental";
        List<MediaFile> albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albums.size());

        genre = "Impressionist Era";
        albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albums.size());

        genre = "Gothik Folk Psychobilly";
        albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albums.size());

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
            Assert.assertEquals("albums - genre : " + genre, 1, countOfMetal);
            Assert.assertEquals("album name" + genre, "_ID3_ALBUM_ Chrome Hoof", albums.get(0).getAlbumName());
        }

        /* "Metal" and "Alternative/Indie" exist in child of _ID3_ALBUM_ Chrome Hoof */
        genre = "Alternative/Indie";
        albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        long countOfIndie = albums.size();
        if (countOfIndie == 1) {
            // https://github.com/tesshucom/jpsonic/issues/797
            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "In this environment, the genre count of the album may be partially different from other environments.");
            }
        } else {
            // Originally. Or on Windows.
            Assert.assertEquals("albums - genre : " + genre, 0, albums.size());
        }

        genre = "TestAlbum";
        albums = searchService.getAlbumsByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 0, albums.size());

        // #### id3 album ####
        genre = "Baroque Instrumental";
        List<Album> albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albumid3s.size());

        genre = "Impressionist Era";
        albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albumid3s.size());

        genre = "Gothik Folk Psychobilly";
        albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 1, albumid3s.size());

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
            Assert.assertEquals("albums - genre : " + genre, 1, countOfMetal);
            Assert.assertEquals("album name" + genre, "_ID3_ALBUM_ Chrome Hoof", albumid3s.get(0).getName());
        }

        /* "Metal" and "Alternative/Indie" exist in child of _ID3_ALBUM_ Chrome Hoof */
        genre = "Alternative/Indie";
        albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        countOfIndie = albumid3s.size();
        if (countOfIndie == 1) {
            // https://github.com/tesshucom/jpsonic/issues/797
            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "In this environment, the genre count of the album may be partially different from other environments.");
            }
        } else {
            // Originally. Or on Windows.
            Assert.assertEquals("albums - genre : " + genre, 0, albumid3s.size());
        }

        genre = "TestAlbum";
        albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 0, albumid3s.size());

        genre = "Rock & Roll";
        albumid3s = searchService.getAlbumId3sByGenres(genre, 0, Integer.MAX_VALUE, allMusicFolders);
        Assert.assertEquals("albums - genre : " + genre, 0, albumid3s.size());

    }

    @Test
    public void testGenreMaster() {

        List<Genre> genres = searchService.getGenres(false);
        Assert.assertEquals("Song genre size", 5, genres.size());
        assertEquals("Song genre: Gothik Folk Psychobilly", 1L,
                genres.stream().filter(g -> "Gothik Folk Psychobilly".equals(g.getName())).count());
        assertEquals("Song genre: Impressionist Era", 1L,
                genres.stream().filter(g -> "Impressionist Era".equals(g.getName())).count());
        assertEquals("Song genre: Baroque Instrumental", 1L,
                genres.stream().filter(g -> "Baroque Instrumental".equals(g.getName())).count());
        assertEquals("Song genre: Alternative/Indie", 1L,
                genres.stream().filter(g -> "Alternative/Indie".equals(g.getName())).count());
        assertEquals("Song genre: Metal", 1L, genres.stream().filter(g -> "Metal".equals(g.getName())).count());

        genres = searchService.getGenres(true);
        Assert.assertEquals("Album genre size", 4, genres.size());
        assertEquals("Album genre: Gothik Folk Psychobilly", 1L,
                genres.stream().filter(g -> "Gothik Folk Psychobilly".equals(g.getName())).count());
        assertEquals("Album genre: Impressionist Era", 1L,
                genres.stream().filter(g -> "Impressionist Era".equals(g.getName())).count());
        assertEquals("Album genre: Baroque Instrumental", 1L,
                genres.stream().filter(g -> "Baroque Instrumental".equals(g.getName())).count());
        long countOfMetal = genres.stream().filter(g -> "Metal".equals(g.getName())).count();
        if (countOfMetal == 0) {
            // https://github.com/tesshucom/jpsonic/issues/797
            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "In this environment, the genre count of the album may be partially different from other environments.");
            }
        } else {
            // Originally. Or on Windows.
            assertEquals("Album genre: Metal", 1L, countOfMetal);
        }
    }

    @Test
    public void testToPreAnalyzedGenres() {

        List<String> preAnalyzedGenres = indexManager
                .toPreAnalyzedGenres(Arrays.asList("Baroque Instrumental", "Gothik Folk Psychobilly"));
        Assert.assertEquals("size", 2, preAnalyzedGenres.size());
        Assert.assertEquals("genre1", "Baroque Instrumental", preAnalyzedGenres.get(0));
        Assert.assertEquals("genre2", "Gothik Folk Psychobilly", preAnalyzedGenres.get(1));

        preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList("Baroque Instrumental"));
        Assert.assertEquals("size", 1, preAnalyzedGenres.size());
        Assert.assertEquals("genre", "Baroque Instrumental", preAnalyzedGenres.get(0));

        preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList("Baroque"));
        Assert.assertEquals("size", 0, preAnalyzedGenres.size());

    }

}
