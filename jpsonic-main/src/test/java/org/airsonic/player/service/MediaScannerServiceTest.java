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

package org.airsonic.player.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.HomeRule;
import org.airsonic.player.util.MusicFolderTestDataUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

/**
 * A unit test class to test the MediaScannerService.
 * <p>
 * This class uses the Spring application context configuration present in the
 * /org/airsonic/player/service/mediaScannerServiceTestCase/ directory.
 * <p>
 * The media library is found in the /MEDIAS directory. It is composed of 2 musicFolders (Music and Music2) and several
 * little weight audio files.
 * <p>
 * At runtime, the subsonic_home dir is set to
 * target/test-classes/org/airsonic/player/service/mediaScannerServiceTestCase. An empty database is created on the fly.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MediaScannerServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerServiceTest.class);

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
    private MediaFileService mediaFileService;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Tests the MediaScannerService by scanning the test media library into an empty database.
     */
    @Test
    public void testScanLibrary() {
        musicFolderDao.getAllMusicFolders()
                .forEach(musicFolder -> musicFolderDao.deleteMusicFolder(musicFolder.getId()));
        MusicFolderTestDataUtils.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
        settingsService.clearMusicFolderCache();

        Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTest.class, "Timer.global"));

        Timer.Context globalTimerContext = globalTimer.time();
        TestCaseUtils.execScan(mediaScannerService);
        globalTimerContext.stop();

        if (LOG.isInfoEnabled()) {
            LOG.info("--- Report of records count per table ---");
        }
        Map<String, Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
        records.keySet().forEach(tableName -> {
            if (LOG.isInfoEnabled()) {
                LOG.info(tableName + " : " + records.get(tableName).toString());
            }
        });

        if (LOG.isInfoEnabled()) {
            LOG.info("--- *********************** ---");
        }

        // Music Folder Music must have 3 children
        List<MediaFile> listeMusicChildren = mediaFileDao
                .getChildrenOf(new File(MusicFolderTestDataUtils.resolveMusicFolderPath()).getPath());
        Assert.assertEquals(3, listeMusicChildren.size());
        // Music Folder Music2 must have 1 children
        List<MediaFile> listeMusic2Children = mediaFileDao
                .getChildrenOf(new File(MusicFolderTestDataUtils.resolveMusic2FolderPath()).getPath());
        Assert.assertEquals(1, listeMusic2Children.size());

        if (LOG.isInfoEnabled()) {
            LOG.info("--- List of all artists ---");
            LOG.info("artistName#albumCount");
        }

        List<Artist> allArtists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE,
                musicFolderDao.getAllMusicFolders());
        allArtists.forEach(artist -> {
            if (LOG.isInfoEnabled()) {
                LOG.info(artist.getName() + "#" + artist.getAlbumCount());
            }
        });

        if (LOG.isInfoEnabled()) {
            LOG.info("--- *********************** ---");
            LOG.info("--- List of all albums ---");
            LOG.info("name#artist");
        }

        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true,
                musicFolderDao.getAllMusicFolders());
        allAlbums.forEach(album -> {
            if (LOG.isInfoEnabled()) {
                LOG.info(album.getName() + "#" + album.getArtist());
            }
        });
        Assert.assertEquals(5, allAlbums.size());

        if (LOG.isInfoEnabled()) {
            LOG.info("--- *********************** ---");
        }

        List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental", 0, 0,
                musicFolderDao.getAllMusicFolders());
        Assert.assertEquals(2, listeSongs.size());

        // display out metrics report
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
        reporter.report();

        if (LOG.isInfoEnabled()) {
            LOG.info("End");
        }

    }

    @Test
    public void testSpecialCharactersInFilename() throws Exception {
        InputStream resource = MediaScannerServiceTest.class.getClassLoader().getResourceAsStream("MEDIAS/piano.mp3");
        assert resource != null;
        String directoryName = "Muff1nman\u2019s \uFF0FMusic"; // Muff1nman’s ／Music
        String fileName = "Muff1nman\u2019s\uFF0FPiano.mp3"; // Muff1nman’s／Piano.mp3
        File artistDir = temporaryFolder.newFolder(directoryName);
        File musicFile = artistDir.toPath().resolve(fileName).toFile();
        IOUtils.copy(resource, Files.newOutputStream(Paths.get(musicFile.toURI())));

        MusicFolder musicFolder = new MusicFolder(1, temporaryFolder.getRoot(), "Music", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        settingsService.clearMusicFolderCache();
        TestCaseUtils.execScan(mediaScannerService);
        MediaFile mediaFile = mediaFileService.getMediaFile(musicFile);
        assertEquals(mediaFile.getFile().toString(), musicFile.toString());
        if (LOG.isInfoEnabled()) {
            LOG.info(mediaFile.getFile().getPath());
        }
        assertNotNull(mediaFile);
    }

    @Test
    public void testNeverScanned() {

        mediaScannerService.neverScanned();
    }

    @Test
    public void testMusicBrainzReleaseIdTag() {

        // Add the "Music3" folder to the database
        File musicFolderFile = new File(MusicFolderTestDataUtils.resolveMusic3FolderPath());
        MusicFolder musicFolder = new MusicFolder(1, musicFolderFile, "Music3", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        settingsService.clearMusicFolderCache();
        TestCaseUtils.execScan(mediaScannerService);

        // Retrieve the "Music3" folder from the database to make
        // sure that we don't accidentally operate on other folders
        // from previous tests.
        musicFolder = musicFolderDao.getMusicFolderForPath(musicFolder.getPath().getPath());
        List<MusicFolder> folders = new ArrayList<>();
        folders.add(musicFolder);

        // Test that the artist is correctly imported
        List<Artist> allArtists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        Assert.assertEquals(1, allArtists.size());
        Artist artist = allArtists.get(0);
        Assert.assertEquals("TestArtist", artist.getName());
        Assert.assertEquals(1, artist.getAlbumCount());

        // Test that the album is correctly imported, along with its MusicBrainz release ID
        List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, folders);
        Assert.assertEquals(1, allAlbums.size());
        Album album = allAlbums.get(0);
        Assert.assertEquals("TestAlbum", album.getName());
        Assert.assertEquals("TestArtist", album.getArtist());
        Assert.assertEquals(1, album.getSongCount());
        Assert.assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", album.getMusicBrainzReleaseId());
        Assert.assertEquals(musicFolderFile.toPath().resolve("TestAlbum").toString(), album.getPath());

        // Test that the music file is correctly imported, along with its MusicBrainz release ID and recording ID
        List<MediaFile> albumFiles = mediaFileDao.getChildrenOf(allAlbums.get(0).getPath());
        Assert.assertEquals(1, albumFiles.size());
        MediaFile file = albumFiles.get(0);
        Assert.assertEquals("Aria", file.getTitle());
        Assert.assertEquals("flac", file.getFormat());
        Assert.assertEquals("TestAlbum", file.getAlbumName());
        Assert.assertEquals("TestArtist", file.getArtist());
        Assert.assertEquals("TestArtist", file.getAlbumArtist());
        Assert.assertEquals(1, (long) file.getTrackNumber());
        Assert.assertEquals(2001, (long) file.getYear());
        Assert.assertEquals(album.getPath(), file.getParentPath());
        Assert.assertEquals(new File(album.getPath()).toPath().resolve("01 - Aria.flac").toString(), file.getPath());
        Assert.assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", file.getMusicBrainzReleaseId());
        Assert.assertEquals("831586f4-56f9-4785-ac91-447ae20af633", file.getMusicBrainzRecordingId());
    }
}
