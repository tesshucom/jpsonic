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

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.MusicFolderTestDataUtils;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.DaoHelper;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.search.SearchCriteriaDirector;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.concurrent.ExecutorConfiguration;
import com.tesshu.jpsonic.util.concurrent.ShortTaskPoolConfiguration;
import net.sf.ehcache.Ehcache;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.ObjectUtils;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class MediaScannerServiceImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerServiceImplTest.class);

    @Nested
    class UnitTest {

        private SettingsService settingsService;
        private IndexManager indexManager;
        private ArtistDao artistDao;
        private AlbumDao albumDao;
        private MediaFileService mediaFileService;
        private MediaFileDao mediaFileDao;
        private ScannerStateServiceImpl scannerStateService;
        private ThreadPoolTaskExecutor executor;
        private SortProcedureService utils;
        private ScannerProcedureService scannerProcedureService;
        private WritableMediaFileService writableMediaFileService;
        private MediaScannerServiceImpl mediaScannerService;

        @BeforeEach
        public void setup() {
            settingsService = mock(SettingsService.class);
            indexManager = mock(IndexManager.class);
            mediaFileService = mock(MediaFileService.class);
            mediaFileDao = mock(MediaFileDao.class);
            artistDao = mock(ArtistDao.class);
            albumDao = mock(AlbumDao.class);
            executor = mock(ThreadPoolTaskExecutor.class);
            utils = mock(SortProcedureService.class);
            scannerStateService = new ScannerStateServiceImpl(mock(StaticsDao.class));
            writableMediaFileService = new WritableMediaFileService(mediaFileDao, scannerStateService, mediaFileService,
                    albumDao, mock(MediaFileCache.class), null, settingsService, mock(SecurityService.class), null);
            scannerProcedureService = new ScannerProcedureService(settingsService, mock(MusicFolderService.class),
                    indexManager, mediaFileService, writableMediaFileService, mock(PlaylistService.class), mediaFileDao,
                    artistDao, albumDao, mock(StaticsDao.class), utils, scannerStateService, mock(Ehcache.class),
                    mock(MediaFileCache.class));
            mediaScannerService = new MediaScannerServiceImpl(scannerStateService, scannerProcedureService,
                    mock(ExpungeService.class), executor);
        }

        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // It doesn't seem to be able to capture
        @Test
        void testPodcast() throws URISyntaxException {
            indexManager = mock(IndexManager.class);
            Mockito.doNothing().when(indexManager).startIndexing();
            Path podcastPath = Path.of(MediaScannerServiceImplTest.class.getResource("/MEDIAS/Scan/Null").toURI());
            Mockito.when(settingsService.getPodcastFolder()).thenReturn(podcastPath.toString());
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(podcastPath.toString());
            Mockito.when(mediaFileService.getMediaFile(podcastPath)).thenReturn(mediaFile);

            ShortTaskPoolConfiguration poolConf = new ShortTaskPoolConfiguration();
            poolConf.setQueueCapacity("0");
            poolConf.setCorePoolSize("1");
            poolConf.setMaxPoolSize("1");
            ExecutorConfiguration executorConf = new ExecutorConfiguration(poolConf);
            final ThreadPoolTaskExecutor executor = executorConf.scanExecutor();

            mediaScannerService = new MediaScannerServiceImpl(scannerStateService, scannerProcedureService,
                    mock(ExpungeService.class), executor);
            mediaScannerService.scanLibrary();
            executor.shutdown();
        }

        @Nested
        class UpdateArtistTest {

            private MediaFile createSong() {
                // nonull
                MediaFile song = new MediaFile();
                // song.setAlbumName("albumName");
                // song.setParentPath("parentPath");
                song.setMediaType(MediaType.MUSIC);
                song.setAlbumArtist("albumArtist");

                // nullable
                // song.setLastScanned(now());
                return song;
            }

            private MusicFolder createMusicFolder() {
                return new MusicFolder(Integer.valueOf(1), "", "", true, now());
            }

            @Test
            void testIsNotUpdatable() {

                MusicFolder musicFolder = createMusicFolder();
                Instant scanDate = now();

                MediaFile song = createSong();
                song.setAlbumArtist(null);
                scannerProcedureService.updateArtist(scanDate, musicFolder, song);
                Mockito.verify(artistDao, Mockito.never()).createOrUpdateArtist(Mockito.any(Artist.class));

                song = createSong();
                song.setMediaType(MediaType.DIRECTORY);
                scannerProcedureService.updateArtist(scanDate, musicFolder, song);
                Mockito.verify(artistDao, Mockito.never()).createOrUpdateArtist(Mockito.any(Artist.class));
            }

            @Test
            void testFirstEncounter() {

                MediaFile song = createSong();
                MusicFolder musicFolder = createMusicFolder();
                Instant scanDate = now();

                // Song dates are never updated
                assertNotEquals(song.getLastScanned(), scanDate);

                // ## First run
                scannerProcedureService.updateArtist(scanDate, musicFolder, song);
                Mockito.verify(artistDao, Mockito.times(1)).createOrUpdateArtist(Mockito.any(Artist.class));

                ArgumentCaptor<Artist> artistCap = ArgumentCaptor.forClass(Artist.class);
                Mockito.verify(artistDao, Mockito.times(1)).createOrUpdateArtist(artistCap.capture());
                Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Artist.class),
                        Mockito.any(MusicFolder.class));

                Artist registeredArtist = artistCap.getValue();
                assertEquals(registeredArtist.getLastScanned(), scanDate);
                assertEquals(0, registeredArtist.getAlbumCount());
                Mockito.when(artistDao.getArtist(registeredArtist.getName())).thenReturn(registeredArtist);

                // ## Second run
                artistCap = ArgumentCaptor.forClass(Artist.class);
                scannerProcedureService.updateArtist(scanDate, musicFolder, song);

                // Currently always executed
                Mockito.verify(artistDao, Mockito.times(2)).createOrUpdateArtist(artistCap.capture());

                // Not executed if already executed
                Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Artist.class),
                        Mockito.any(MusicFolder.class));

                registeredArtist = artistCap.getValue();
                assertEquals(registeredArtist.getLastScanned(), scanDate);
                // As of v111.6.0 there will be no counting during scanning
                assertEquals(0, registeredArtist.getAlbumCount());
            }
        }
    }

    @Nested
    class UpdateAlbumTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Autowired
        private AlbumDao albumDao;

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (ObjectUtils.isEmpty(musicFolders)) {
                musicFolders = Arrays.asList(
                        new MusicFolder(1, resolveBaseMediaPath("Scan/Id3LIFO"), "alphaBeticalProps", true, now()),
                        new MusicFolder(2, resolveBaseMediaPath("Scan/Null"), "noTagFirstChild", true, now()),
                        new MusicFolder(3, resolveBaseMediaPath("Scan/Reverse"), "fileAndPropsNameInReverse", true,
                                now()));
            }
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            populateDatabase();
        }

        /*
         * File structured albums are only affected by the first child of the system file path. FIFO. Only the last
         * registration is valid if the same name exists in the case of Id3 album. LIFO. Depending on the data pattern,
         * different album data of Genre can be created in File structure / Id3.
         *
         * Jpsonic changes DB registration logic of Id3 album to eliminate data inconsistency.
         *
         */
        @Test
        @DisabledOnOs(OS.LINUX)
        void testUpdateAlbumOnWin() {

            // LIFO
            List<MusicFolder> folder = getMusicFolders().stream().filter(f -> "alphaBeticalProps".equals(f.getName()))
                    .collect(Collectors.toList());
            assertEquals(1, folder.size());
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            assertEquals("ALBUM1", album.getName());
            assertEquals("albumArtistA", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertEquals("genreA", album.getGenre());
            assertEquals(Integer.valueOf(2001), album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(1, albumId3s.size());
            Album albumId3 = albumId3s.get(0);
            assertEquals("albumA", albumId3.getName());
            assertEquals("albumArtistA", albumId3.getArtist());

            // [For legacy implementations, the following values ​​are inserted]
            // assertEquals("genreB", albumId3.getGenre());
            // assertEquals(Integer.valueOf(2002), albumId3.getYear());

            // [For Jpsonic, modified to insert the following values]
            // Other than this case, it is the same specification as the legacy.
            assertEquals("genreA", albumId3.getGenre());
            assertEquals(Integer.valueOf(2001), albumId3.getYear());

            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            // Null
            folder = getMusicFolders().stream().filter(f -> "noTagFirstChild".equals(f.getName()))
                    .collect(Collectors.toList());
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            album = albums.get(0);
            assertEquals("ALBUM2", album.getName());
            assertEquals("ARTIST2", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertNull(album.getGenre());
            assertNull(album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(2, albumId3s.size());

            albumId3 = albumId3s.get(0);
            assertEquals("ALBUM2", albumId3.getName());
            assertEquals("ARTIST2", albumId3.getArtist());
            assertNull(albumId3.getGenre());
            assertNull(albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            albumId3 = albumId3s.get(1);
            assertEquals("albumC", albumId3.getName());
            assertEquals("albumArtistC", albumId3.getArtist());
            assertEquals("genreC", albumId3.getGenre());
            assertEquals(Integer.valueOf(2002), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            // Reverse
            folder = getMusicFolders().stream().filter(f -> "fileAndPropsNameInReverse".equals(f.getName()))
                    .collect(Collectors.toList());
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            album = albums.get(0);
            assertEquals("ALBUM3", album.getName());
            assertEquals("albumArtistE", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertEquals("genreE", album.getGenre());
            assertEquals(Integer.valueOf(2002), album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(2, albumId3s.size());

            albumId3 = albumId3s.get(0);
            assertEquals("albumD", albumId3.getName());
            assertEquals("albumArtistD", albumId3.getArtist());
            assertEquals("genreD", albumId3.getGenre());
            assertEquals(Integer.valueOf(2001), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            albumId3 = albumId3s.get(1);
            assertEquals("albumE", albumId3.getName());
            assertEquals("albumArtistE", albumId3.getArtist());
            assertEquals("genreE", albumId3.getGenre());
            assertEquals(Integer.valueOf(2002), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testUpdateAlbumOnLinux() {

            // LIFO
            List<MusicFolder> folder = getMusicFolders().stream().filter(f -> "alphaBeticalProps".equals(f.getName()))
                    .collect(Collectors.toList());
            assertEquals(1, folder.size());
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            assertEquals("ALBUM1", album.getName());
            assertEquals("albumArtistB", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertEquals("genreB", album.getGenre());
            assertEquals(Integer.valueOf(2002), album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(1, albumId3s.size());
            Album albumId3 = albumId3s.get(0);
            assertEquals("albumA", albumId3.getName());
            assertEquals("albumArtistB", albumId3.getArtist());

            // [For legacy implementations, the following values ​​are inserted]
            // assertEquals("genreB", albumId3.getGenre());
            // assertEquals(Integer.valueOf(2002), albumId3.getYear());

            // [For Jpsonic, modified to insert the following values]
            // Other than this case, it is the same specification as the legacy.
            assertEquals("genreB", albumId3.getGenre());
            assertEquals(Integer.valueOf(2002), albumId3.getYear());

            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            // Null
            folder = getMusicFolders().stream().filter(f -> "noTagFirstChild".equals(f.getName()))
                    .collect(Collectors.toList());
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            album = albums.get(0);
            assertEquals("ALBUM2", album.getName());
            assertEquals("albumArtistC", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertEquals("genreC", album.getGenre());
            assertEquals(2002, album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(2, albumId3s.size());

            albumId3 = albumId3s.get(0);
            assertEquals("ALBUM2", albumId3.getName());
            assertEquals("ARTIST2", albumId3.getArtist());
            assertNull(albumId3.getGenre());
            assertNull(albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            albumId3 = albumId3s.get(1);
            assertEquals("albumC", albumId3.getName());
            assertEquals("albumArtistC", albumId3.getArtist());
            assertEquals("genreC", albumId3.getGenre());
            assertEquals(Integer.valueOf(2002), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            // Reverse
            folder = getMusicFolders().stream().filter(f -> "fileAndPropsNameInReverse".equals(f.getName()))
                    .collect(Collectors.toList());
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
            assertEquals(1, albums.size());
            album = albums.get(0);
            assertEquals("ALBUM3", album.getName());
            assertEquals("albumArtistD", album.getArtist());
            assertNull(album.getAlbumArtist());
            assertEquals("genreD", album.getGenre());
            assertEquals(Integer.valueOf(2001), album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(2, albumId3s.size());

            albumId3 = albumId3s.get(0);
            assertEquals("albumD", albumId3.getName());
            assertEquals("albumArtistD", albumId3.getArtist());
            assertEquals("genreD", albumId3.getGenre());
            assertEquals(Integer.valueOf(2001), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());

            albumId3 = albumId3s.get(1);
            assertEquals("albumE", albumId3.getName());
            assertEquals("albumArtistE", albumId3.getArtist());
            assertEquals("genreE", albumId3.getGenre());
            assertEquals(Integer.valueOf(2002), albumId3.getYear());
            assertNull(albumId3.getMusicBrainzReleaseId());
        }
    }

    /**
     * getChildrenOf Integration Test. This method changes the records of repository without scanning. Note that many
     * properties are overwritten in real time.
     */
    @Nested
    class GetChildrenOfTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;
        private Path artist;
        private Path album;
        private Path song;

        @Autowired
        private MediaFileDao mediaFileDao;
        @Autowired
        private WritableMediaFileService writableMediaFileService;

        @Autowired
        private SearchCriteriaDirector criteriaDirector;
        @Autowired
        private SearchService searchService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup(@TempDir Path tempDir) throws IOException, URISyntaxException {

            // Create a musicfolder for verification
            artist = Path.of(tempDir.toString(), "ARTIST");
            assertNotNull(FileUtil.createDirectories(artist));
            this.album = Path.of(artist.toString(), "ALBUM");
            assertNotNull(FileUtil.createDirectories(album));
            this.musicFolders = Arrays.asList(new MusicFolder(1, tempDir.toString(), "musicFolder", true, now()));

            // Copy the song file from the test resource. No tags are registered in this file.
            Path sample = Path.of(MediaScannerServiceImplTest.class
                    .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sample.mp3").toURI());
            this.song = Path.of(this.album.toString(), "sample.mp3");
            assertNotNull(Files.copy(sample, song));
            assertTrue(Files.exists(song));

            // Exec scan
            populateDatabase();
        }

        @DisabledOnOs(OS.WINDOWS) // Flaky in Windows Server 2022 (#1645)
        @Test
        void testSpecialCharactersInDirName() throws URISyntaxException, IOException, InterruptedException {

            MediaFile artist = mediaFileDao.getMediaFile(this.artist.toString());
            assertEquals(this.artist, artist.toPath());
            assertEquals("ARTIST", artist.getName());
            MediaFile album = mediaFileDao.getMediaFile(this.album.toString());
            assertEquals(this.album, album.toPath());
            assertEquals("ALBUM", album.getName());
            MediaFile song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertEquals("ARTIST", song.getArtist());
            assertEquals("ALBUM", song.getAlbumName());

            // Copy the song file from the test resource. Tags are registered in this file.
            FileUtil.deleteIfExists(this.song);
            Path sampleEdited = Path.of(MediaScannerServiceImplTest.class
                    .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sampleEdited.mp3").toURI());
            this.song = Path.of(this.album.toString(), "sample.mp3");
            Files.copy(sampleEdited, this.song);
            assertTrue(song.exists());

            /*
             * If you get it via Dao, you can get the record before had been copied. (It's a expected behavior)
             */
            List<MediaFile> albums = mediaFileDao.getChildrenOf(this.artist.toString());
            assertEquals(1, albums.size());
            List<MediaFile> songs = mediaFileDao.getChildrenOf(this.album.toString());
            assertEquals(1, songs.size());
            song = songs.get(0);
            assertEquals(this.song, song.toPath());
            assertEquals("ARTIST", song.getArtist());
            assertEquals("ALBUM", song.getAlbumName());

            /*
             * Doing the same thing over the service does not give the same result. This is because the timestamp is
             * checked, and if the file is found to change, it will be parsed and the result will be saved in storage.
             * Note that the naming is get, but the actual processing is get & Update.
             */
            Instant scanStart = now();
            albums = writableMediaFileService.getChildrenOf(scanStart, artist, false);
            assertEquals(1, albums.size());
            songs = writableMediaFileService.getChildrenOf(scanStart, album, true);
            assertEquals(1, songs.size());

            /*
             * Note that the update process at this point is partial. Pay attention to the consistency.
             */

            // Artist and Album are not subject to the update process
            artist = mediaFileDao.getMediaFile(this.artist.toString());
            assertEquals(this.artist, artist.toPath());
            assertEquals("ARTIST", artist.getName());
            album = mediaFileDao.getMediaFile(this.album.toString());
            assertEquals(this.album, album.toPath());
            assertEquals("ALBUM", album.getName());

            // Only songs are updated
            song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertEquals("Edited artist!", song.getArtist());
            assertEquals("Edited album!", song.getAlbumName());

            /*
             * Not reflected in the search at this point. (It's a expected behavior)
             */
            SearchResult result = searchService.search(
                    criteriaDirector.construct("Edited", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
            assertEquals(0, result.getMediaFiles().size());

            result = searchService.search(
                    criteriaDirector.construct("sample", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
            assertEquals(1, result.getMediaFiles().size());
            result = searchService.search(
                    criteriaDirector.construct("ALBUM", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ALBUM));
            assertEquals(1, result.getMediaFiles().size());
            result = searchService.search(
                    criteriaDirector.construct("ARTIST", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ARTIST));
            assertEquals(1, result.getMediaFiles().size());

            // Exec scan
            TestCaseUtils.execScan(mediaScannerService);
            // Await for Lucene to finish writing(asynchronous).
            for (int i = 0; i < 5; i++) {
                Thread.sleep(1000);
            }

            artist = mediaFileDao.getMediaFile(this.artist.toString());
            assertEquals(this.artist, artist.toPath());
            assertNull(artist.getTitle());
            assertEquals("ARTIST", artist.getName());
            assertEquals("ARTIST", artist.getArtist());
            album = mediaFileDao.getMediaFile(this.album.toString());
            assertEquals(this.album, album.toPath());
            assertNull(album.getTitle());
            assertEquals("ALBUM", album.getName());
            assertEquals("Edited album!", album.getAlbumName());

            song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertEquals("Edited song!", song.getTitle());
            assertEquals("Edited song!", song.getName());
            assertEquals("Edited artist!", song.getArtist());
            assertEquals("Edited album!", song.getAlbumName());

            result = searchService.search(
                    criteriaDirector.construct("sample", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
            assertEquals(0, result.getMediaFiles().size()); // good (1 -> 0)
            result = searchService.search(criteriaDirector.construct("Edited song!", 0, Integer.MAX_VALUE, false,
                    musicFolders, IndexType.SONG));
            assertEquals(1, result.getMediaFiles().size()); // good (0 -> 1)

            result = searchService.search(criteriaDirector.construct("Edited album!", 0, Integer.MAX_VALUE, false,
                    musicFolders, IndexType.ALBUM));
            assertEquals(1, result.getMediaFiles().size()); // good (0 -> 1)

            /*
             * Not reflected in the artist of file structure. (It's a expected behavior)
             */
            result = searchService.search(criteriaDirector.construct("Edited artist!", 0, Integer.MAX_VALUE, false,
                    musicFolders, IndexType.ARTIST));
            assertEquals(0, result.getMediaFiles().size()); // good
            result = searchService.search(
                    criteriaDirector.construct("ARTIST", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ARTIST));
            assertEquals(1, result.getMediaFiles().size()); // good (1 -> 1)

            result = searchService.search(criteriaDirector.construct("Edited album!", 0, Integer.MAX_VALUE, false,
                    musicFolders, IndexType.ALBUM_ID3));
            assertEquals(1, result.getAlbums().size());
            result = searchService.search(criteriaDirector.construct("Edited artist!", 0, Integer.MAX_VALUE, false,
                    musicFolders, IndexType.ARTIST_ID3));
            assertEquals(1, result.getArtists().size());
        }
    }

    @Nested
    @SpringBootTest
    @ExtendWith(NeedsHome.class)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class IntegrationTest {

        private final MetricRegistry metrics = new MetricRegistry();

        @Autowired
        private MusicFolderDao musicFolderDao;
        @Autowired
        private DaoHelper daoHelper;

        @Autowired
        private MusicFolderService musicFolderService;
        @Autowired
        private MediaFileService mediaFileService;
        @Autowired
        private MediaFileDao mediaFileDao;
        @Autowired
        private ArtistDao artistDao;
        @Autowired
        private AlbumDao albumDao;
        @Autowired
        private ScannerStateServiceImpl scannerStateService;
        @Autowired
        private ScannerProcedureService procedure;
        @Autowired
        private ExpungeService expungeService;

        private MediaScannerService mediaScannerService;

        @BeforeEach
        public void setup() {
            ThreadPoolTaskExecutor scanExecutor = ServiceMockUtils.mockNoAsyncTaskExecutor();
            mediaScannerService = new MediaScannerServiceImpl(scannerStateService, procedure, expungeService,
                    scanExecutor);
        }

        /**
         * Tests the MediaScannerService by scanning the test media library into an empty database.
         */
        @Test
        void testScanLibrary() {
            musicFolderDao.getAllMusicFolders()
                    .forEach(musicFolder -> musicFolderDao.deleteMusicFolder(musicFolder.getId()));
            MusicFolderTestDataUtils.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
            musicFolderService.clearMusicFolderCache();

            Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceImplTest.class, "Timer.global"));

            try (Timer.Context globalTimerContext = globalTimer.time()) {
                TestCaseUtils.execScan(mediaScannerService);
                globalTimerContext.stop();
            }

            logRecords(TestCaseUtils.recordsInAllTables(daoHelper));

            // Music Folder Music must have 3 children
            List<MediaFile> listeMusicChildren = mediaFileDao
                    .getChildrenOf(Path.of(MusicFolderTestDataUtils.resolveMusicFolderPath()).toString());
            assertEquals(3, listeMusicChildren.size());
            // Music Folder Music2 must have 1 children
            List<MediaFile> listeMusic2Children = mediaFileDao
                    .getChildrenOf(Path.of(MusicFolderTestDataUtils.resolveMusic2FolderPath()).toString());
            assertEquals(1, listeMusic2Children.size());

            logArtistsAll();

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
            assertEquals(5, allAlbums.size());

            if (LOG.isInfoEnabled()) {
                LOG.info("--- *********************** ---");
            }

            List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental", 0, 0,
                    musicFolderDao.getAllMusicFolders());
            assertEquals(2, listeSongs.size());

            // display out metrics report
            try (ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS).build()) {
                reporter.report();
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("End");
            }
        }

        private void logRecords(Map<String, Integer> records) {
            if (LOG.isInfoEnabled()) {
                LOG.info("--- Report of records count per table ---");
            }
            records.keySet().forEach(tableName -> {
                if (LOG.isInfoEnabled()) {
                    LOG.info(tableName + " : " + records.get(tableName).toString());
                }
            });
            if (LOG.isInfoEnabled()) {
                LOG.info("--- *********************** ---");
            }
        }

        private void logArtistsAll() {
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
        }

        @Test
        void testSpecialCharactersInFilename(@TempDir Path tempDirPath) throws Exception {

            Path musicPath;
            try (InputStream resource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("MEDIAS/piano.mp3")) {
                assert resource != null;
                String directoryName = "Muff1nman\u2019s \uFF0FMusic"; // Muff1nman’s ／Music
                String fileName = "Muff1nman\u2019s\uFF0FPiano.mp3"; // Muff1nman’s／Piano.mp3

                Path artistDir = Path.of(tempDirPath.toString(), directoryName);
                FileUtil.createDirectories(artistDir);

                musicPath = Path.of(artistDir.toString(), fileName);
                IOUtils.copy(resource, Files.newOutputStream(musicPath));
            }

            MusicFolder musicFolder = new MusicFolder(1, tempDirPath.toString(), "Music", true, now());
            musicFolderDao.createMusicFolder(musicFolder);
            musicFolderService.clearMusicFolderCache();
            TestCaseUtils.execScan(mediaScannerService);
            MediaFile mediaFile = mediaFileService.getMediaFile(musicPath);
            assertEquals(mediaFile.toPath(), musicPath);
            assertNotNull(mediaFile);
        }

        @Test
        void testNeverScanned() {
            assertFalse(mediaScannerService.neverScanned());
        }

        @Test
        void testMusicBrainzReleaseIdTag() {

            // Add the "Music3" folder to the database
            Path musicFolderPath = Path.of(MusicFolderTestDataUtils.resolveMusic3FolderPath());
            MusicFolder musicFolder = new MusicFolder(1, musicFolderPath.toString(), "Music3", true, now());
            musicFolderDao.createMusicFolder(musicFolder);
            musicFolderService.clearMusicFolderCache();
            TestCaseUtils.execScan(mediaScannerService);

            // Retrieve the "Music3" folder from the database to make
            // sure that we don't accidentally operate on other folders
            // from previous tests.
            musicFolder = musicFolderDao.getMusicFolderForPath(musicFolder.getPathString());
            List<MusicFolder> folders = new ArrayList<>();
            folders.add(musicFolder);

            // Test that the artist is correctly imported
            List<Artist> allArtists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
            assertEquals(1, allArtists.size());
            Artist artist = allArtists.get(0);
            assertEquals("TestArtist", artist.getName());
            assertEquals(1, artist.getAlbumCount());

            // Test that the album is correctly imported, along with its MusicBrainz release ID
            List<Album> allAlbums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, true, folders);
            assertEquals(1, allAlbums.size());
            Album album = allAlbums.get(0);
            assertEquals("TestAlbum", album.getName());
            assertEquals("TestArtist", album.getArtist());
            assertEquals(1, album.getSongCount());
            assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", album.getMusicBrainzReleaseId());
            assertEquals(musicFolderPath.resolve("TestAlbum").toString(), album.getPath());

            // Test that the music file is correctly imported, along with its MusicBrainz release ID and recording ID
            List<MediaFile> albumFiles = mediaFileDao.getChildrenOf(allAlbums.get(0).getPath());
            assertEquals(1, albumFiles.size());
            MediaFile file = albumFiles.get(0);
            assertEquals("Aria", file.getTitle());
            assertEquals("flac", file.getFormat());
            assertEquals("TestAlbum", file.getAlbumName());
            assertEquals("TestArtist", file.getArtist());
            assertEquals("TestArtist", file.getAlbumArtist());
            assertEquals(1, (long) file.getTrackNumber());
            assertEquals(2001, (long) file.getYear());
            assertEquals(album.getPath(), file.getParentPathString());
            assertEquals(Path.of(album.getPath()).resolve("01 - Aria.flac"), file.toPath());
            assertEquals("0820752d-1043-4572-ab36-2df3b5cc15fa", file.getMusicBrainzReleaseId());
            assertEquals("831586f4-56f9-4785-ac91-447ae20af633", file.getMusicBrainzRecordingId());
        }
    }
}