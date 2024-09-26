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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.base.DaoHelper;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import com.tesshu.jpsonic.service.metadata.VideoParser;
import com.tesshu.jpsonic.service.search.IndexManager;
import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.search.SearchCriteriaDirector;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

    @Documented
    private @interface TryCancelDecisions {
        @interface Conditions {
            @interface IsScanning {
                @interface True {
                    @interface OwnLock {
                    }

                    @interface OthersLock {
                    }
                }

                @interface False {
                }
            }
        }

        @interface Result {
            @interface False {
            }

            @interface True {
            }
        }
    }

    @Documented
    private @interface IsOptionalProcessSkippableDecisions {

        @interface Conditions {
            @interface IgnoreFileTimestamps {
                @interface False {
                }

                @interface True {
                }
            }

            @interface LastScanEventType {
                @interface NotPresent {

                }

                @interface Present {
                    @interface ScanEventType {
                        @interface EqFinished {
                        }

                        @interface NeFinished {
                        }
                    }
                }
            }

            @interface FolderChangedSinceLastScan {
                @interface True {
                }

                @interface False {
                }
            }

        }

        @interface Result {
            @interface False {
            }

            @interface True {
            }
        }

    }

    @SuppressWarnings("PMD.SingularField") // pmd/pmd#4616
    @Nested
    class UnitTest {

        private SettingsService settingsService;
        private IndexManager indexManager;
        private ArtistDao artistDao;
        private AlbumDao albumDao;
        private MediaFileService mediaFileService;
        private MediaFileDao mediaFileDao;
        private StaticsDao staticsDao;
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
            staticsDao = mock(StaticsDao.class);
            scannerStateService = new ScannerStateServiceImpl(staticsDao);
            writableMediaFileService = new WritableMediaFileService(mediaFileDao, scannerStateService, mediaFileService,
                    albumDao, mock(MediaFileCache.class), mock(MusicParser.class), mock(VideoParser.class),
                    settingsService, mock(SecurityService.class), null, mock(IndexManager.class),
                    mock(MusicIndexServiceImpl.class));
            scannerProcedureService = new ScannerProcedureService(settingsService, mock(MusicFolderServiceImpl.class),
                    indexManager, mediaFileService, writableMediaFileService, mock(PlaylistService.class),
                    mock(TemplateWrapper.class), mediaFileDao, artistDao, albumDao, staticsDao, utils,
                    scannerStateService, mock(MusicIndexServiceImpl.class), mock(MediaFileCache.class),
                    mock(JapaneseReadingUtils.class), mock(JpsonicComparators.class),
                    mock(ThreadPoolTaskExecutor.class));
            mediaScannerService = new MediaScannerServiceImpl(settingsService, scannerStateService,
                    scannerProcedureService, mock(ExpungeService.class), staticsDao, executor);
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
            mediaScannerService = new MediaScannerServiceImpl(settingsService, scannerStateService,
                    scannerProcedureService, mock(ExpungeService.class), mock(StaticsDao.class),
                    mock(ThreadPoolTaskExecutor.class));
            mediaScannerService.scanLibrary();
        }

        @Nested
        class SetCancelTest {

            @Test
            @TryCancelDecisions.Conditions.IsScanning.False
            @TryCancelDecisions.Result.False
            void c01() {
                assertFalse(mediaScannerService.isCancel());
                mediaScannerService.tryCancel();
                assertFalse(mediaScannerService.isCancel());
            }

            @Test
            @TryCancelDecisions.Conditions.IsScanning.True.OwnLock
            @TryCancelDecisions.Result.True
            void c02() {
                scannerStateService.setReady();
                assertTrue(scannerStateService.tryScanningLock());
                assertTrue(mediaScannerService.isScanning());
                assertFalse(mediaScannerService.isCancel());
                mediaScannerService.tryCancel();
                assertTrue(mediaScannerService.isCancel());
            }

            @Test
            @TryCancelDecisions.Conditions.IsScanning.True.OthersLock
            @TryCancelDecisions.Result.True
            void c03() throws InterruptedException {
                scannerStateService.setReady();
                ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                executor.initialize();
                executor.submit(scannerStateService::tryScanningLock);
                Thread.sleep(50);
                assertTrue(scannerStateService.isScanning());
                assertFalse(mediaScannerService.isCancel());
                mediaScannerService.tryCancel();
                assertTrue(mediaScannerService.isCancel());
                executor.shutdown();
            }
        }

        @Nested
        class GetLastScanEventTypeTest {

            /* If scanning is in progress, recovery is expected, so previous results are not returned. */
            @Test
            void testWithScanning() {
                scannerStateService.setReady();
                assertTrue(scannerStateService.tryScanningLock());
                assertTrue(mediaScannerService.getLastScanEventType().isEmpty());
            }

            /* No results are returned if a scan has not been run. */
            @Test
            void testNeverScanned() {
                assertFalse(mediaScannerService.isScanning());
                Mockito.when(staticsDao.isNeverScanned()).thenReturn(true);
                assertTrue(mediaScannerService.getLastScanEventType().isEmpty());
            }

            /* A record may not have been recorded if the scan was interrupted by an unchecked exception. */
            @Test
            void testNoRecords() {
                assertFalse(mediaScannerService.isScanning());
                Mockito.when(staticsDao.isNeverScanned()).thenReturn(false);
                assertFalse(mediaScannerService.getLastScanEventType().isEmpty());
                assertEquals(ScanEventType.FAILED, mediaScannerService.getLastScanEventType().get());
            }

            /* If the scan ran to completion, it would have logged FINISHED, DESTROYED, or CANCELED. */
            @Test
            void testWithRecords() {
                assertFalse(mediaScannerService.isScanning());
                Mockito.when(staticsDao.isNeverScanned()).thenReturn(false);
                ScanEvent success = new ScanEvent(null, null, ScanEventType.SUCCESS, 0L, 0L, 0L, null, null);
                Mockito.when(staticsDao.getLastScanAllStatuses()).thenReturn(Arrays.asList(success));
                assertFalse(mediaScannerService.getLastScanEventType().isEmpty());
                assertEquals(ScanEventType.SUCCESS, mediaScannerService.getLastScanEventType().get());
            }
        }

        @Nested
        class IsOptionalProcessSkippableTest {

            @Test
            @IsOptionalProcessSkippableDecisions.Conditions.IgnoreFileTimestamps.True
            @IsOptionalProcessSkippableDecisions.Result.False
            void c01() {
                Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
                assertFalse(mediaScannerService.isOptionalProcessSkippable());
            }

            @Test
            @IsOptionalProcessSkippableDecisions.Conditions.IgnoreFileTimestamps.False
            @IsOptionalProcessSkippableDecisions.Conditions.LastScanEventType.NotPresent
            @IsOptionalProcessSkippableDecisions.Result.False
            void c02() {
                Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
                Mockito.when(staticsDao.isNeverScanned()).thenReturn(true);
                assertFalse(mediaScannerService.isOptionalProcessSkippable());
            }

            @Test
            @IsOptionalProcessSkippableDecisions.Conditions.IgnoreFileTimestamps.False
            @IsOptionalProcessSkippableDecisions.Conditions.LastScanEventType.Present.ScanEventType.NeFinished
            @IsOptionalProcessSkippableDecisions.Result.False
            void c03() {
                Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
                Mockito.when(staticsDao.getLastScanAllStatuses()).thenReturn(
                        Arrays.asList(new ScanEvent(null, null, ScanEventType.CANCELED, null, null, null, null, null)));
                assertFalse(mediaScannerService.isOptionalProcessSkippable());
            }

            @Test
            @IsOptionalProcessSkippableDecisions.Conditions.IgnoreFileTimestamps.False
            @IsOptionalProcessSkippableDecisions.Conditions.LastScanEventType.Present.ScanEventType.EqFinished
            @IsOptionalProcessSkippableDecisions.Conditions.FolderChangedSinceLastScan.True
            @IsOptionalProcessSkippableDecisions.Result.False
            void c04() {
                Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
                Mockito.when(staticsDao.getLastScanAllStatuses()).thenReturn(
                        Arrays.asList(new ScanEvent(null, null, ScanEventType.SUCCESS, null, null, null, null, null)));
                Mockito.when(staticsDao.isfolderChangedSinceLastScan()).thenReturn(true);
                assertFalse(mediaScannerService.isOptionalProcessSkippable());
            }

            @Test
            @IsOptionalProcessSkippableDecisions.Conditions.IgnoreFileTimestamps.False
            @IsOptionalProcessSkippableDecisions.Conditions.LastScanEventType.Present.ScanEventType.EqFinished
            @IsOptionalProcessSkippableDecisions.Conditions.FolderChangedSinceLastScan.False
            @IsOptionalProcessSkippableDecisions.Result.True
            void c05() {
                Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
                Mockito.when(staticsDao.getLastScanAllStatuses()).thenReturn(
                        Arrays.asList(new ScanEvent(null, null, ScanEventType.SUCCESS, null, null, null, null, null)));
                Mockito.when(staticsDao.isfolderChangedSinceLastScan()).thenReturn(false);
                assertTrue(mediaScannerService.isOptionalProcessSkippable());
            }
        }
    }

    /*
     * Used if NIO2 fails
     */
    private boolean copy(Path in, Path out) {
        try (InputStream is = Files.newInputStream(in); OutputStream os = Files.newOutputStream(out);) {
            byte[] buf = new byte[256];
            while (is.read(buf) != -1) {
                os.write(buf);
            }
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
        return true;
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
                        new MusicFolder(1, resolveBaseMediaPath("Scan/Id3LIFO"), "alphaBeticalProps", true, now(), 0,
                                false),
                        new MusicFolder(2, resolveBaseMediaPath("Scan/Null"), "noTagFirstChild", true, now(), 1, false),
                        new MusicFolder(3, resolveBaseMediaPath("Scan/Reverse"), "fileAndPropsNameInReverse", true,
                                now(), 2, false));
            }
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            populateDatabase();
        }

        /*
         * If data with the same name exists in both albums in the file structure/Id3, the tag of the first child file
         * takes precedence. The Sonic server relies on his NIO for this "first child", so the result of the first-fetch
         * depends on the OS filesystem. Jpsonic has solved this problem in v111.6.0, and the same analysis is now
         * performed on all platforms.
         */
        @Test
        void testUpdateAlbum() {

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
            assertEquals(2001, album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, folder);
            Map<String, Album> albumId3Map = albumId3s.stream().collect(Collectors.toMap(Album::getArtist, a -> a));

            assertEquals(2, albumId3s.size());
            Album albumA = albumId3Map.get("albumArtistA");
            assertEquals("albumA", albumA.getName());
            assertEquals("albumArtistA", albumA.getArtist());
            assertEquals("genreA", albumA.getGenre());
            assertEquals(2001, albumA.getYear());
            assertNull(albumA.getMusicBrainzReleaseId());
            Album albumB = albumId3Map.get("albumArtistB");
            assertEquals("albumA", albumB.getName());
            assertEquals("albumArtistB", albumB.getArtist());
            assertEquals("genreB", albumB.getGenre());
            assertEquals(2002, albumB.getYear());
            assertNull(albumB.getMusicBrainzReleaseId());

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

            albumA = albumId3s.get(0);
            assertEquals("ALBUM2", albumA.getName());
            assertEquals("ARTIST2", albumA.getArtist());
            assertNull(albumA.getGenre());
            assertNull(albumA.getYear());
            assertNull(albumA.getMusicBrainzReleaseId());

            albumA = albumId3s.get(1);
            assertEquals("albumC", albumA.getName());
            assertEquals("albumArtistC", albumA.getArtist());
            assertEquals("genreC", albumA.getGenre());
            assertEquals(2002, albumA.getYear());
            assertNull(albumA.getMusicBrainzReleaseId());

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
            assertEquals(2001, album.getYear());
            assertNull(album.getMusicBrainzReleaseId());
            assertNull(album.getMusicBrainzRecordingId());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
            assertEquals(2, albumId3s.size());

            albumA = albumId3s.get(0);
            assertEquals("albumD", albumA.getName());
            assertEquals("albumArtistD", albumA.getArtist());
            assertEquals("genreD", albumA.getGenre());
            assertEquals(2001, albumA.getYear());
            assertNull(albumA.getMusicBrainzReleaseId());

            albumA = albumId3s.get(1);
            assertEquals("albumE", albumA.getName());
            assertEquals("albumArtistE", albumA.getArtist());
            assertEquals("genreE", albumA.getGenre());
            assertEquals(2002, albumA.getYear());
            assertNull(albumA.getMusicBrainzReleaseId());
        }
    }

    @Nested
    class GenreCRUDTest extends AbstractNeedsScan {

        @Autowired
        private SearchService searchService;

        @TempDir
        private Path tempDir;
        private List<MusicFolder> folders;

        @Override
        public List<MusicFolder> getMusicFolders() {
            if (ObjectUtils.isEmpty(folders)) {
                folders = Arrays.asList(new MusicFolder(1, tempDir.toString(), "MultiGenre", true, now(), 0, false));
            }
            return folders;
        }

        @BeforeEach
        public void setup() throws IOException {
            FileUtils.copyDirectory(new File(resolveBaseMediaPath("MultiGenre")), tempDir.toFile());
            populateDatabase();
        }

        @Test
        void testCRUD() throws IOException {

            // Test CR
            GenreMasterCriteria criteria = new GenreMasterCriteria(folders, Scope.ALBUM, Sort.NAME);
            List<Genre> genres = searchService.getGenres(criteria, 0, Integer.MAX_VALUE);
            assertEquals(14, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals(1, genres.get(0).getAlbumCount());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals(1, genres.get(1).getAlbumCount());
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals(1, genres.get(2).getAlbumCount());
            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals(1, genres.get(3).getAlbumCount());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals(1, genres.get(4).getAlbumCount());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals(2, genres.get(5).getAlbumCount());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals(1, genres.get(6).getAlbumCount());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals(1, genres.get(7).getAlbumCount());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals(1, genres.get(8).getAlbumCount());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals(1, genres.get(9).getAlbumCount());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals(1, genres.get(10).getAlbumCount());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals(1, genres.get(11).getAlbumCount());
            assertEquals("GENRE_K", genres.get(12).getName());
            assertEquals(2, genres.get(12).getAlbumCount());
            assertEquals("GENRE_L", genres.get(13).getName());
            assertEquals(2, genres.get(13).getAlbumCount());

            // ### Test D(File Delete)
            Files.delete(Path.of(tempDir.toString(), "ARTIST1/ALBUM1/FILE02.mp3"));
            Files.delete(Path.of(tempDir.toString(), "ARTIST1/ALBUM3/FILE05.mp3"));
            Files.delete(Path.of(tempDir.toString(), "ARTIST1/ALBUM6/FILE10.mp3"));
            TestCaseUtils.execScan(mediaScannerService);

            // Deleting a file will reduce the number of genres by 2.
            genres = searchService.getGenres(criteria, 0, Integer.MAX_VALUE);
            assertEquals(12, genres.size());

            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals(1, genres.get(0).getAlbumCount());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals(1, genres.get(1).getAlbumCount());

            // Even if FILE02 is deleted,
            // the number of albums will not change because FILE01 still exists.
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals(1, genres.get(2).getAlbumCount()); // (1->1)

            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals(1, genres.get(3).getAlbumCount());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals(1, genres.get(4).getAlbumCount());

            // FILE05 has been deleted.
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals(1, genres.get(5).getAlbumCount()); // (2->1)

            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals(1, genres.get(6).getAlbumCount());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals(1, genres.get(7).getAlbumCount());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals(1, genres.get(8).getAlbumCount());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals(1, genres.get(9).getAlbumCount());

            // GENRE_I, GENRE_J has been deleted.

            assertEquals("GENRE_K", genres.get(10).getName());
            assertEquals(2, genres.get(10).getAlbumCount());
            assertEquals("GENRE_L", genres.get(11).getName());
            assertEquals(2, genres.get(11).getAlbumCount());

            // ### Test UD(Tag Update&Delete)
            Files.delete(Path.of(tempDir.toString(), "ARTIST1/ALBUM1/FILE01.mp3"));
            copy(Path.of(resolveBaseMediaPath("Scan/MultiGenreCRUD/ARTIST1/ALBUM1/FILE01.mp3")),
                    Path.of(tempDir.toString(), "ARTIST1/ALBUM1/FILE01.mp3"));
            Files.delete(Path.of(tempDir.toString(), "ARTIST1/ALBUM7/FILE11.mp3"));
            copy(Path.of(resolveBaseMediaPath("Scan/MultiGenreCRUD/ARTIST1/ALBUM7/FILE11.mp3")),
                    Path.of(tempDir.toString(), "ARTIST1/ALBUM7/FILE11.mp3"));
            TestCaseUtils.execScan(mediaScannerService);

            // Deleting a file will reduce the number of genres by 2.
            genres = searchService.getGenres(criteria, 0, Integer.MAX_VALUE);
            assertEquals(13, genres.size());

            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals(1, genres.get(0).getAlbumCount());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals(1, genres.get(1).getAlbumCount());

            assertEquals("GENRE_A-CHANGED", genres.get(2).getName());
            assertEquals(1, genres.get(2).getAlbumCount());

            // GENRE_A -> GENRE_A-CHANGED

            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals(1, genres.get(3).getAlbumCount());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals(1, genres.get(4).getAlbumCount());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals(1, genres.get(5).getAlbumCount());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals(1, genres.get(6).getAlbumCount());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals(1, genres.get(7).getAlbumCount());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals(1, genres.get(8).getAlbumCount());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals(1, genres.get(9).getAlbumCount());
            assertEquals("GENRE_K", genres.get(10).getName());
            assertEquals(2, genres.get(10).getAlbumCount());

            assertEquals("GENRE_L", genres.get(11).getName());
            assertEquals(1, genres.get(11).getAlbumCount()); // (2->1)

            // Some of the multi-genres have been changed.
            assertEquals("GENRE_L-CHANGED", genres.get(12).getName());
            assertEquals(1, genres.get(12).getAlbumCount());
        }
    }

    /*
     * Confirm that the Genre Count does not increase or decrease during Normal-Scan and Scan with IgnoreTimestamp.
     */
    @Nested
    class GenrePersistenceTest extends AbstractNeedsScan {

        @Autowired
        private SearchService searchService;

        private final List<MusicFolder> folders = List
                .of(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true, now(), 0, false));

        @Override
        public List<MusicFolder> getMusicFolders() {
            return folders;
        }

        @BeforeEach
        public void setup() throws IOException {
            populateDatabase();
        }

        @Test
        void testGenreCountPersistence() throws IOException {
            GenreMasterCriteria albumGenreCriteria = new GenreMasterCriteria(folders, Scope.ALBUM, Sort.NAME);
            GenreMasterCriteria songGenreCriteria = new GenreMasterCriteria(folders, Scope.SONG, Sort.NAME);
            assertTrue(assertAlbumGenreCount(searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
            assertTrue(assertSongGenreCount(searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));

            // Run a scan
            TestCaseUtils.execScan(mediaScannerService);
            assertTrue(assertAlbumGenreCount(searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
            assertTrue(assertSongGenreCount(searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));

            // Scan with IgnoreFileTimestamps enabled
            settingsService.setIgnoreFileTimestamps(true);
            settingsService.save();
            TestCaseUtils.execScan(mediaScannerService);
            assertTrue(assertAlbumGenreCount(searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
            assertTrue(assertSongGenreCount(searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));
        }

        private boolean assertAlbumGenreCount(List<Genre> genres) {
            assertEquals(14, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals(1, genres.get(0).getAlbumCount());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals(1, genres.get(1).getAlbumCount());
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals(1, genres.get(2).getAlbumCount());
            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals(1, genres.get(3).getAlbumCount());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals(1, genres.get(4).getAlbumCount());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals(2, genres.get(5).getAlbumCount());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals(1, genres.get(6).getAlbumCount());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals(1, genres.get(7).getAlbumCount());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals(1, genres.get(8).getAlbumCount());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals(1, genres.get(9).getAlbumCount());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals(1, genres.get(10).getAlbumCount());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals(1, genres.get(11).getAlbumCount());
            assertEquals("GENRE_K", genres.get(12).getName());
            assertEquals(2, genres.get(12).getAlbumCount());
            assertEquals("GENRE_L", genres.get(13).getName());
            assertEquals(2, genres.get(13).getAlbumCount());
            return true;
        }

        private boolean assertSongGenreCount(List<Genre> genres) {
            assertEquals(15, genres.size());
            assertEquals("Audiobook - Historical", genres.get(0).getName());
            assertEquals(1, genres.get(0).getSongCount());
            assertEquals("Audiobook - Sports", genres.get(1).getName());
            assertEquals(1, genres.get(1).getSongCount());
            assertEquals("GENRE_A", genres.get(2).getName());
            assertEquals(2, genres.get(2).getSongCount());
            assertEquals("GENRE_B", genres.get(3).getName());
            assertEquals(1, genres.get(3).getSongCount());
            assertEquals("GENRE_C", genres.get(4).getName());
            assertEquals(1, genres.get(4).getSongCount());
            assertEquals("GENRE_D", genres.get(5).getName());
            assertEquals(2, genres.get(5).getSongCount());
            assertEquals("GENRE_E", genres.get(6).getName());
            assertEquals(2, genres.get(6).getSongCount());
            assertEquals("GENRE_F", genres.get(7).getName());
            assertEquals(2, genres.get(7).getSongCount());
            assertEquals("GENRE_G", genres.get(8).getName());
            assertEquals(1, genres.get(8).getSongCount());
            assertEquals("GENRE_H", genres.get(9).getName());
            assertEquals(1, genres.get(9).getSongCount());
            assertEquals("GENRE_I", genres.get(10).getName());
            assertEquals(1, genres.get(10).getSongCount());
            assertEquals("GENRE_J", genres.get(11).getName());
            assertEquals(1, genres.get(11).getSongCount());
            assertEquals("GENRE_K", genres.get(12).getName());
            assertEquals(2, genres.get(12).getSongCount());
            assertEquals("GENRE_L", genres.get(13).getName());
            assertEquals(2, genres.get(13).getSongCount());
            assertEquals("NO_ALBUM", genres.get(14).getName());
            assertEquals(1, genres.get(14).getSongCount());
            return true;
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
        private IndexManager indexManager;
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
            this.musicFolders = Arrays
                    .asList(new MusicFolder(1, tempDir.toString(), "musicFolder", true, now(), 1, false));

            // Copy the song file from the test resource. No tags are registered in this file.
            Path sample = Path.of(MediaScannerServiceImplTest.class
                    .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sample.mp3").toURI());
            this.song = Path.of(this.album.toString(), "sample.mp3");
            assertNotNull(Files.copy(sample, song));
            assertTrue(Files.exists(song));

            // Exec scan
            populateDatabase();
        }

        /*
         * On platforms with Timestamp disabled the result will be different from the case. Windows for home use will
         * work fine.
         */
        @DisabledOnOs(OS.WINDOWS) // Windows Server 2022 & JDK17
        @Test
        void testBehavioralSpecForTagReflesh() throws URISyntaxException, IOException, InterruptedException {

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
             * Note that the name of getChildrenOf is get, but the actual process is get & Update, in legacy sonic
             * servers. Jpsonic analyzes files only when scanning.
             */
            Instant scanStart = now();
            indexManager.startIndexing();
            albums = writableMediaFileService.getChildrenOf(scanStart, artist, false);
            assertEquals(1, albums.size());
            songs = writableMediaFileService.getChildrenOf(scanStart, album, true);
            assertEquals(1, songs.size());
            indexManager.stopIndexing();

            // Artist and Album are not subject to the update process
            artist = mediaFileDao.getMediaFile(this.artist.toString());
            assertEquals(this.artist, artist.toPath());
            assertEquals("ARTIST", artist.getName());
            album = mediaFileDao.getMediaFile(this.album.toString());
            assertEquals(this.album, album.toPath());
            assertEquals("ALBUM", album.getName());

            song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertEquals("Edited artist!", song.getArtist());
            assertEquals("Edited album!", song.getAlbumName());

            /*
             * Not reflected in the search at this point. (It's a expected behavior)
             */
            SearchResult result = searchService.search(
                    criteriaDirector.construct("Edited", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
            assertEquals(1, result.getMediaFiles().size());
            result = searchService.search(
                    criteriaDirector.construct("sample", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
            assertEquals(0, result.getMediaFiles().size());
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
        private SettingsService settingsService;
        @Autowired
        private MusicFolderServiceImpl musicFolderService;
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
        @Autowired
        private StaticsDao staticsDao;

        private MediaScannerService mediaScannerService;

        @BeforeEach
        public void setup() {
            ThreadPoolTaskExecutor scanExecutor = ServiceMockUtils.mockNoAsyncTaskExecutor();
            mediaScannerService = new MediaScannerServiceImpl(settingsService, scannerStateService, procedure,
                    expungeService, staticsDao, scanExecutor);
        }

        /**
         * Tests the MediaScannerService by scanning the test media library into an empty database.
         */
        @Test
        void testScanLibrary() {

            // Test case ported from a legacy server.

            musicFolderDao.getAllMusicFolders()
                    .forEach(musicFolder -> musicFolderDao.deleteMusicFolder(musicFolder.getId()));
            MusicFolderTestDataUtils.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
            musicFolderService.clearMusicFolderCache();

            Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceImplTest.class, "Timer.global"));

            try (Timer.Context globalTimerContext = globalTimer.time()) {
                TestCaseUtils.execScan(mediaScannerService);
                globalTimerContext.stop();
            }
            assertEquals(ScanEventType.SUCCESS, mediaScannerService.getLastScanEventType().get());

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

            List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre(Arrays.asList("Baroque Instrumental"), 0, 0,
                    musicFolderDao.getAllMusicFolders(), Arrays.asList(MediaType.MUSIC));
            assertEquals(2, listeSongs.size());

            // display out metrics report
            try (ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS).build()) {
                reporter.report();
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("End");
            }

            // Add below with Jpsonic. Whether MediaFile#folder is registered correctly.
            Map<String, MusicFolder> folders = musicFolderDao.getAllMusicFolders().stream()
                    .collect(Collectors.toMap(MusicFolder::getName, m -> m));
            Map<String, MediaFile> albums = mediaFileDao
                    .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolderDao.getAllMusicFolders()).stream()
                    .collect(Collectors.toMap(MediaFile::getName, m -> m));
            assertEquals(5, albums.size());
            assertEquals(folders.get("Music").getPathString(),
                    albums.get("_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]")
                            .getFolder());
            assertEquals(folders.get("Music").getPathString(),
                    albums.get("_DIR_ Ravel - Chamber Music With Voice").getFolder());
            assertEquals(folders.get("Music").getPathString(),
                    albums.get("_DIR_ Ravel - Complete Piano Works").getFolder());
            assertEquals(folders.get("Music").getPathString(), albums.get("_DIR_ Sackcloth 'n' Ashes").getFolder());
            assertEquals(folders.get("Music2").getPathString(), albums.get("_DIR_ chrome hoof - 2004").getFolder());
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

            MusicFolder musicFolder = new MusicFolder(1, tempDirPath.toString(), "Music", true, now(), 1, false);
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

        @SuppressWarnings("PMD.DetachedTestCase")
        @Test
        void testMusicBrainzReleaseIdTag() {

            // Add the "Music3" folder to the database
            Path musicFolderPath = Path.of(MusicFolderTestDataUtils.resolveMusic3FolderPath());
            MusicFolder musicFolder = new MusicFolder(1, musicFolderPath.toString(), "Music3", true, now(), 1, false);
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

    @Nested
    class ChangeFolderTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;
        private Path artist;
        private Path album;
        private Path song;
        private @TempDir Path tempDir1;
        private @TempDir Path tempDir2;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() throws IOException, URISyntaxException {
            artist = Path.of(tempDir1.toString(), "ARTIST");
            assertNotNull(FileUtil.createDirectories(artist));
            this.album = Path.of(artist.toString(), "ALBUM");
            assertNotNull(FileUtil.createDirectories(album));
            this.musicFolders = Arrays.asList(
                    new MusicFolder(1, tempDir1.toString(), "musicFolder1", true, now(), 0, false),
                    new MusicFolder(2, tempDir2.toString(), "musicFolder2", true, now(), 1, false));

            Path sample = Path.of(MediaScannerServiceImplTest.class
                    .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sample.mp3").toURI());
            this.song = Path.of(this.album.toString(), "sample.mp3");
            assertNotNull(Files.copy(sample, song));
            assertTrue(Files.exists(song));

            populateDatabase();
        }

        @DisabledOnJre(JRE.JAVA_11) // #1840
        @Test
        void testChangeFolder() throws URISyntaxException, IOException, InterruptedException {

            MediaFile artist = mediaFileDao.getMediaFile(this.artist.toString());
            assertEquals(this.artist, artist.toPath());
            assertEquals("ARTIST", artist.getName());
            MediaFile album = mediaFileDao.getMediaFile(this.album.toString());
            assertEquals(this.album, album.toPath());
            assertEquals("ALBUM", album.getName());
            MediaFile song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());

            Map<String, MusicFolder> folders = musicFolders.stream()
                    .collect(Collectors.toMap(MusicFolder::getName, mf -> mf));
            assertEquals(song.getFolder(), folders.get("musicFolder1").getPathString());

            // Create a directory and move the files there
            Path artist2 = Path.of(tempDir2.toString(), "ARTIST2");
            assertNotNull(FileUtil.createDirectories(artist2));
            Path album2 = Path.of(artist2.toString(), "ALBUM2");
            assertNotNull(FileUtil.createDirectories(album2));
            Path movedSong = Path.of(album2.toString(), "sample.mp3");
            Files.move(this.song, movedSong);
            assertFalse(Files.exists(this.song));
            assertTrue(Files.exists(movedSong));

            // Exec scan
            TestCaseUtils.execScan(mediaScannerService);

            assertNull(mediaFileDao.getMediaFile(this.song.toString()));
            song = mediaFileDao.getMediaFile(movedSong.toString());
            assertNotNull(song);
            assertEquals(song.getFolder(), folders.get("musicFolder2").getPathString());
        }
    }

    @Nested
    class FolderEnabledTest extends AbstractNeedsScan {

        private List<MusicFolder> musicFolders;
        private Path song;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup(@TempDir Path tempDir) throws IOException, URISyntaxException {
            Path artist = Path.of(tempDir.toString(), "ARTIST");
            assertNotNull(FileUtil.createDirectories(artist));
            Path album = Path.of(artist.toString(), "ALBUM");
            assertNotNull(FileUtil.createDirectories(album));
            this.musicFolders = Arrays
                    .asList(new MusicFolder(1, tempDir.toString(), "musicFolder1", true, now(), 1, false));
            Path sample = Path.of(MediaScannerServiceImplTest.class
                    .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sample.mp3").toURI());
            this.song = Path.of(album.toString(), "sample.mp3");
            assertNotNull(Files.copy(sample, song));
            assertTrue(Files.exists(song));
            populateDatabase();
        }

        /**
         * Scan after Music folder is set to enable=false and rescan with enable=true before cleanup. In this case, the
         * previous record that has already been registered but not deleted is reused. In this case the previous record
         * that was already registered but not deleted (enable=false) is reused. Retains play counts etc, but becomes a
         * performance barrier.
         */
        @Test
        void testRestoreUpdate() throws URISyntaxException, IOException, InterruptedException, ExecutionException {

            MediaFile song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertTrue(song.isPresent());

            MusicFolder folder = musicFolders.get(0);
            folder.setEnabled(false);

            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.initialize();
            executor.submit(() -> musicFolderService.updateMusicFolder(now(), folder)).get();

            TestCaseUtils.execScan(mediaScannerService);

            assertNull(mediaFileDao.getMediaFile(this.song.toString()));

            folder.setEnabled(true);
            executor.submit(() -> musicFolderService.updateMusicFolder(now(), folder)).get();
            TestCaseUtils.execScan(mediaScannerService);

            song = mediaFileDao.getMediaFile(this.song.toString());
            assertEquals(this.song, song.toPath());
            assertTrue(song.isPresent());

            executor.shutdown();
        }
    }

    @Nested
    class StrictSortTest {

        private SettingsService settingsService;
        private ArtistDao artistDao;
        private MediaFileDao mediaFileDao;
        private ScannerStateServiceImpl scannerStateService;
        private SortProcedureService sortProcedureService;
        private MusicFolderServiceImpl musicFolderService;
        private MediaScannerServiceImpl mediaScannerService;
        private JpsonicComparators comparators;

        @BeforeEach
        public void setup() {
            settingsService = mock(SettingsService.class);
            mediaFileDao = mock(MediaFileDao.class);
            artistDao = mock(ArtistDao.class);
            sortProcedureService = mock(SortProcedureService.class);
            scannerStateService = mock(ScannerStateServiceImpl.class);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            AlbumDao albumDao = mock(AlbumDao.class);
            WritableMediaFileService writableMediaFileService = new WritableMediaFileService(mediaFileDao,
                    scannerStateService, mediaFileService, albumDao, mock(MediaFileCache.class),
                    mock(MusicParser.class), mock(VideoParser.class), settingsService, mock(SecurityService.class),
                    null, mock(IndexManager.class), mock(MusicIndexServiceImpl.class));
            musicFolderService = mock(MusicFolderServiceImpl.class);
            comparators = mock(JpsonicComparators.class);
            StaticsDao staticsDao = mock(StaticsDao.class);
            IndexManager indexManager = mock(IndexManager.class);
            ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
            ScannerProcedureService scannerProcedureService = new ScannerProcedureService(settingsService,
                    musicFolderService, indexManager, mediaFileService, writableMediaFileService,
                    mock(PlaylistService.class), mock(TemplateWrapper.class), mediaFileDao, artistDao, albumDao,
                    staticsDao, sortProcedureService, scannerStateService, mock(MusicIndexServiceImpl.class),
                    mock(MediaFileCache.class), mock(JapaneseReadingUtils.class), comparators,
                    mock(ThreadPoolTaskExecutor.class));
            mediaScannerService = new MediaScannerServiceImpl(settingsService, scannerStateService,
                    scannerProcedureService, mock(ExpungeService.class), staticsDao, executor);
        }

        @Test
        void testDoScanLibraryWithSortStrict() {

            Mockito.when(musicFolderService.getAllMusicFolders())
                    .thenReturn(Arrays.asList(new MusicFolder(0, "path", "name", true, null, -1, false)));
            Mockito.when(artistDao.getAlbumCounts()).thenReturn(Arrays.asList(new Artist()));

            Mockito.when(scannerStateService.isEnableCleansing()).thenReturn(true);
            Mockito.when(scannerStateService.tryScanningLock()).thenReturn(true);
            Mockito.when(settingsService.isSortStrict()).thenReturn(true);

            mediaScannerService.doScanLibrary();

            // parseAlbum
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedAlbums(Mockito.anyInt(), Mockito.anyList());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnparsedAlbums(Mockito.anyInt(), Mockito.anyList());

            // updateSortOfAlbum
            Mockito.verify(sortProcedureService, Mockito.times(1)).mergeSortOfAlbum(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.times(1)).copySortOfAlbum(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.times(1)).compensateSortOfAlbum(Mockito.anyList());

            // (updateOrderOfAlbum/updateSortOfArtist)
            Mockito.verify(comparators, Mockito.times(2)).mediaFileOrderByAlpha();
            Mockito.verify(comparators, Mockito.never()).mediaFileOrderBy(OrderBy.ALBUM);
            Mockito.verify(comparators, Mockito.never()).mediaFileOrderBy(OrderBy.ARTIST);

            Mockito.verify(sortProcedureService, Mockito.times(1)).mergeSortOfArtist(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.times(1)).copySortOfArtist(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.times(1)).compensateSortOfArtist(Mockito.anyList());

            // refleshAlbumId3
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedId3Albums(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnregisteredId3Albums(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());

            // (updateOrderOfAlbumId3)
            // Mockito.verify(scannerProcedureService, Mockito.times(1)).updateOrderOfAlbumID3();
            Mockito.verify(comparators, Mockito.times(1)).albumOrderByAlpha();

            // refleshArtistId3
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedId3Artists(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnregisteredId3Artists(Mockito.anyInt(),
                    Mockito.anyList(), Mockito.anyBoolean());

            // (updateOrderOfArtistId3)
            // Mockito.verify(scannerProcedureService, Mockito.times(1)).updateOrderOfArtistID3();
            Mockito.verify(comparators, Mockito.times(1)).artistOrderByAlpha();

            // updateAlbumCounts
            Mockito.verify(artistDao, Mockito.times(1)).updateAlbumCount(Mockito.anyInt(), Mockito.anyInt());
        }

        /**
         * From v112.0.0 onwards, updateOrder will be a mandatory process when data is updated. Because it is used for
         * fetch in incremental updates.
         */
        @Test
        void testDoScanLibraryWithoutSortStrict() {

            Mockito.when(musicFolderService.getAllMusicFolders())
                    .thenReturn(Arrays.asList(new MusicFolder(0, "path", "name", true, null, -1, false)));
            Mockito.when(artistDao.getAlbumCounts()).thenReturn(Arrays.asList(new Artist()));

            Mockito.when(scannerStateService.isEnableCleansing()).thenReturn(true);
            Mockito.when(scannerStateService.tryScanningLock()).thenReturn(true);
            Mockito.when(settingsService.isSortStrict()).thenReturn(false);

            mediaScannerService.doScanLibrary();

            // parseAlbum
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedAlbums(Mockito.anyInt(), Mockito.anyList());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnparsedAlbums(Mockito.anyInt(), Mockito.anyList());

            // updateSortOfAlbum
            Mockito.verify(sortProcedureService, Mockito.never()).mergeSortOfAlbum(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.never()).copySortOfAlbum(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.never()).compensateSortOfAlbum(Mockito.anyList());

            // updateSortOfArtist
            Mockito.verify(sortProcedureService, Mockito.never()).mergeSortOfArtist(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.never()).copySortOfArtist(Mockito.anyList());
            Mockito.verify(sortProcedureService, Mockito.never()).compensateSortOfArtist(Mockito.anyList());

            // (updateOrderOfAlbum/updateOrderOfArtist)
            Mockito.verify(comparators, Mockito.times(2)).mediaFileOrderByAlpha();
            Mockito.verify(comparators, Mockito.never()).mediaFileOrderBy(OrderBy.ALBUM);
            Mockito.verify(comparators, Mockito.never()).mediaFileOrderBy(OrderBy.ARTIST);

            // refleshAlbumId3
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedId3Albums(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnregisteredId3Albums(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());

            // (updateOrderOfAlbumId3)
            // Mockito.verify(scannerProcedureService, Mockito.times(1)).updateOrderOfAlbumID3();
            Mockito.verify(comparators, Mockito.times(1)).albumOrderByAlpha();

            // refleshArtistId3
            Mockito.verify(mediaFileDao, Mockito.times(1)).getChangedId3Artists(Mockito.anyInt(), Mockito.anyList(),
                    Mockito.anyBoolean());
            Mockito.verify(mediaFileDao, Mockito.times(1)).getUnregisteredId3Artists(Mockito.anyInt(),
                    Mockito.anyList(), Mockito.anyBoolean());

            // (updateOrderOfArtistId3)
            // Mockito.verify(scannerProcedureService, Mockito.times(1)).updateOrderOfArtistID3();
            Mockito.verify(comparators, Mockito.times(1)).artistOrderByAlpha();

            // updateAlbumCounts
            Mockito.verify(artistDao, Mockito.times(1)).updateAlbumCount(Mockito.anyInt(), Mockito.anyInt());
        }
    }
}
