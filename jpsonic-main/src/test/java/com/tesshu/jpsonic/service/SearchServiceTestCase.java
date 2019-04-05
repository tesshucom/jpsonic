/*
 This file is part of Jpsonic.
 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.
 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service;

import org.airsonic.player.dao.*;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.MediaScannerServiceTestCase;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.util.HomeRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.subsonic.restapi.ArtistID3;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

@ContextConfiguration(locations = {
    "/applicationContext-service.xml",
    "/applicationContext-cache.xml",
    "/applicationContext-testdb.xml",
    "/applicationContext-mockSonos.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SearchServiceTestCase {

  @ClassRule
  public static final SpringClassRule classRule = new SpringClassRule() {
    HomeRule airsonicRule = new HomeRule();

    @Override
    public Statement apply(Statement base, Description description) {
      Statement spring = super.apply(base, description);
      return airsonicRule.apply(spring, description);
    }
  };

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  private final MetricRegistry metrics = new MetricRegistry();

  @Autowired
  private MediaScannerService mediaScannerService;

  @Autowired
  private MediaFileDao mediaFileDao;

  @Autowired
  private MusicFolderDao musicFolderDao;

  @Autowired
  private DaoHelper daoHelper;

  @Autowired
  private AlbumDao albumDao;

  @Autowired
  private SearchService searchService;

  @Autowired
  private SettingsService settingsService;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Autowired
  ResourceLoader resourceLoader;

  @Before
  public void warmUp() {

  }

  @Test
  public void testSearchTypical() {

      /* It seems that there is a case that does not work well
       * if you test immediately after initialization in 1 method.
       * It may be improved later.
       */
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      MusicFolderTestData.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
      settingsService.clearMusicFolderCache();
      TestCaseUtils.execScan(mediaScannerService);
      
      System.out.println("--- Report of records count per table ---");
      Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
      records.keySet().stream()
          .filter(s -> s.equals("MEDIA_FILE") // 20
              | s.equals("ARTIST") // 5
              | s.equals("MUSIC_FOLDER")// 3
              | s.equals("ALBUM"))// 5
          .forEach(tableName
              -> System.out.println("\t" + tableName + " : " + records.get(tableName).toString()));

      // Music Folder Music must have 3 children
      List<MediaFile> listeMusicChildren = mediaFileDao.getChildrenOf(new File(MusicFolderTestData.resolveMusicFolderPath()).getPath());
      Assert.assertEquals(3, listeMusicChildren.size());
      // Music Folder Music2 must have 1 children
      List<MediaFile> listeMusic2Children = mediaFileDao.getChildrenOf(new File(MusicFolderTestData.resolveMusic2FolderPath()).getPath());
      Assert.assertEquals(1, listeMusic2Children.size());
      System.out.println("--- *********************** ---");

    /*
     * A simple test that is expected to easily detect
     * API syntax differences when updating lucene.
     * 
     * Complete route coverage and data coverage in this case alone
     * are not conscious.
     */

    List<MusicFolder> allMusicFolders = musicFolderDao.getAllMusicFolders();
    Assert.assertEquals(3, allMusicFolders.size());

    //  testSearch()

    String query = "Sarah Walker";
    final SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.setQuery(query);
    searchCriteria.setCount(Integer.MAX_VALUE);
    searchCriteria.setOffset(0);
    SearchResult result = searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM);
    Assert.assertEquals("(0) Specify '" + query + "' as query, total Hits is", 1, result.getTotalHits());
    Assert.assertEquals("(1) Specify artist '" + query + "' as query. Artist SIZE is", 0, result.getArtists().size());
    Assert.assertEquals("(2) Specify artist '" + query + "' as query. Album SIZE is", 0, result.getAlbums().size());
    Assert.assertEquals("(3) Specify artist '" + query + "' as query, MediaFile SIZE is", 1, result.getMediaFiles().size());
    Assert.assertEquals("(4) ", MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType());
    Assert.assertEquals(
        "(5) Specify artist '" + query + "' as query, and get a album. The album artist name is ",
        "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getMediaFiles().get(0).getArtist());
    Assert.assertEquals(
        "(6) Specify artist '" + query + "' as query, and get a album. The album name is ",
        "_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getMediaFiles().get(0).getAlbumName());

    query = "music";
    searchCriteria.setQuery(query);
    result = searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM_ID3);
    Assert.assertEquals("Specify '" + query + "' as query, total Hits is", 1, result.getTotalHits());
    Assert.assertEquals("(7) Specify '" + query + "' as query, and get a song. Artist SIZE is ", 0, result.getArtists().size());
    Assert.assertEquals("(8) Specify '" + query + "' as query, and get a song. Album SIZE is ", 1, result.getAlbums().size());
    Assert.assertEquals("(9) Specify '" + query + "' as query, and get a song. MediaFile SIZE is ", 0, result.getMediaFiles().size());
    Assert.assertEquals(
        "(9) Specify '" + query + "' as query, and get a album. The album artist name is ",
        "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getAlbums().get(0).getArtist());
    Assert.assertEquals(
        "(10) Specify '" + query + "' as query, and get a album. The album name is ",
        "_ID3_ALBUM_ Ravel - Chamber Music With Voice", result.getAlbums().get(0).getName());

    query = "Ravel - Chamber Music";
    searchCriteria.setQuery(query);
    result = searchService.search(searchCriteria, allMusicFolders, IndexType.SONG);
    Assert.assertEquals("(11) Specify album '" + query + "' as query, total Hits is", 2, result.getTotalHits());
    Assert.assertEquals("(12) Specify album '" + query + "' as query, and get a song. Artist SIZE is", 0, result.getArtists().size());
    Assert.assertEquals("(13) Specify album '" + query + "' as query, and get a song. Album SIZE is", 0, result.getAlbums().size());
    Assert.assertEquals("(14) Specify album '" + query + "' as query, and get a song. MediaFile SIZE is", 2, result.getMediaFiles().size());
    Assert.assertEquals(
        "(15) Specify album '" + query + "' as query, and get songs. The first song title is ",
        "01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(0).getTitle());
    Assert.assertEquals(
        "(16) Specify album '" + query + "' as query, and get songs. The second song title is ",
        "02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getTitle());

    //  testSearchByName()
    String albumName = "Sackcloth 'n' Ashes";
    ParamSearchResult<Album> albumResult = searchService.searchByName(albumName, 0, Integer.MAX_VALUE, allMusicFolders, Album.class);
    Assert.assertEquals(
        "(17) Specify album name '" + albumName + "' as the name, and get an album.", 1, albumResult.getItems().size());
    Assert.assertEquals(
        "(18) Specify '" + albumName + "' as the name, The album name is ",
        "_ID3_ALBUM_ Sackcloth 'n' Ashes", albumResult.getItems().get(0).getName());

    Assert.assertEquals(
        "(19) Whether the acquired album contains data of the specified album name.",
        1L, albumResult.getItems().stream()
        .filter(r -> "_ID3_ALBUM_ Sackcloth \'n\' Ashes".equals(r.getName())).count());

    ParamSearchResult<ArtistID3>
    artistId3Result = searchService.searchByName("lker/Nash", 0, Integer.MAX_VALUE, allMusicFolders, ArtistID3.class);
    Assert.assertEquals(
        "(20) Specify 'lker/Nash' as the name, and get an artist.", 0, artistId3Result.getItems().size());

    ParamSearchResult<Artist>
    artistResult = searchService.searchByName("lker/Nash", 0, Integer.MAX_VALUE, allMusicFolders, Artist.class);
    Assert.assertEquals(
        "(21) Specify 'lker/Nash' as the name, and get an artist.", 1, artistResult.getItems().size());

    //  testGetRandomSongs()

    RandomSearchCriteria randomSearchCriteria = new RandomSearchCriteria(
         Integer.MAX_VALUE, // count
         null, // genre,
         null, // fromYear
         null, // toYear
         allMusicFolders // musicFolders
    );
    List<MediaFile> allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
    Assert.assertEquals(
       "(22) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire songs.", 11, allRandomSongs.size());

    randomSearchCriteria = new RandomSearchCriteria(
         Integer.MAX_VALUE, // count
         null, // genre,
         1900, // fromYear
         null, // toYear
         allMusicFolders // musicFolders
    );
    allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
    Assert.assertEquals(
        "(23) Specify 1900 as 'fromYear', and randomly acquire songs.", 7, allRandomSongs.size());

    randomSearchCriteria = new RandomSearchCriteria(
        Integer.MAX_VALUE, // count
        "Chamber Music", // genre,
        null, // fromYear
        null, // toYear
        allMusicFolders // musicFolders
        );
    allRandomSongs = searchService.getRandomSongs(randomSearchCriteria);
    Assert.assertEquals(
        "(24) Specify music as 'genre', and randomly acquire songs.", 0, allRandomSongs.size());

    List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, 0, true, true, allMusicFolders);
    Assert.assertEquals(
        "(25) Get all albums with Dao.", 5,  allAlbums.size());

    //  testGetRandomAlbums()

    List<MediaFile> allRandomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
    Assert.assertEquals(
        "(26) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire albums(file struct).", 5, allRandomAlbums.size());

    //  testGetRandomAlbumsId3()

    query = "artist:Sarah Walker*";
    List<Album> allRandomAlbumsId3 = searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
    Assert.assertEquals(
        "(27) Specify Integer.MAX_VALUE as the upper limit, and randomly acquire albums(ID3).", 5, allRandomAlbumsId3.size());

    query = "ID 3 ARTIST";
    searchCriteria.setQuery(query);
    result = searchService.search(searchCriteria, allMusicFolders, IndexType.ARTIST_ID3);
    Assert.assertEquals("(28) Specify '" + query + "' as query, total Hits is", 4, result.getTotalHits());
    Assert.assertEquals("(29) Specify '" + query + "' as query, and get an artists. Artist SIZE is ", 4, result.getArtists().size());
    Assert.assertEquals("(30) Specify '" + query + "' as query, and get a artists. Album SIZE is ", 0, result.getAlbums().size());
    Assert.assertEquals("(31) Specify '" + query + "' as query, and get a artists. MediaFile SIZE is ", 0, result.getMediaFiles().size());

    long count = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ARTIST_")).count();
    Assert.assertEquals("(32) Artist whose name contains \\\"_ID3_ARTIST_\\\" is 3 records.", 3L, count);
    count = result.getArtists().stream().filter(a -> a.getName().startsWith("_ID3_ALBUMARTIST_")).count();
    Assert.assertEquals("(33) Artist whose name is \"_ID3_ARTIST_\" is 1 records.", 1L, count);

    /*
     * Below is a simple loop test.
     * Whether or not IndexReader is exhausted.
     */
    int countForEachMethod = 500;
    String[] randomWords4Search = createRandomWords(countForEachMethod);
    String[] randomWords4SearchByName = createRandomWords(countForEachMethod);

    Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTestCase.class, "Timer.global"));
    Timer.Context globalTimerContext = globalTimer.time();

    System.out.println("--- Random search (" + countForEachMethod * 5 + " times) ---");

    // testSearch()
    Arrays.stream(randomWords4Search).forEach(w -> {
      searchCriteria.setQuery(w);
      searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM);
    });

    // testSearchByName()
    Arrays.stream(randomWords4SearchByName).forEach(w -> {
      searchService.searchByName(w, 0, Integer.MAX_VALUE, allMusicFolders, Artist.class);
    });

    // testGetRandomSongs()
    RandomSearchCriteria criteria = new RandomSearchCriteria(
        Integer.MAX_VALUE, // count
        null, // genre,
        null, // fromYear
        null, // toYear
        allMusicFolders // musicFolders
    );
    for(int i = 0; i < countForEachMethod ; i++) {
      searchService.getRandomSongs(criteria);
    }

    // testGetRandomAlbums()
   for(int i = 0; i < countForEachMethod ; i++) {
     searchService.getRandomAlbums(Integer.MAX_VALUE, allMusicFolders);
   }

   // testGetRandomAlbumsId3()
   for(int i = 0; i < countForEachMethod ; i++) {
     searchService.getRandomAlbumsId3(Integer.MAX_VALUE, allMusicFolders);
   }

   globalTimerContext.stop();

   query = "Sarah Walker";
   searchCriteria.setQuery(query);
   result = searchService.search(searchCriteria, allMusicFolders, IndexType.ALBUM);
   Assert.assertEquals("(35) Can the normal case be implemented.", 0, result.getArtists().size());
   Assert.assertEquals("(36) Can the normal case be implemented.", 0, result.getAlbums().size());
   Assert.assertEquals("(37) Can the normal case be implemented.", 1, result.getMediaFiles().size());
   Assert.assertEquals("(38) Can the normal case be implemented.", MediaType.ALBUM, result.getMediaFiles().get(0).getMediaType());
   Assert.assertEquals("(39) Can the normal case be implemented.",
       "_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", result.getMediaFiles().get(0).getArtist());

   System.out.println("--- SUCCESS ---");

   ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
       .convertRatesTo(TimeUnit.SECONDS)
       .convertDurationsTo(TimeUnit.MILLISECONDS)
       .build();
   reporter.report();

   System.out.println("End. ");
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

}
