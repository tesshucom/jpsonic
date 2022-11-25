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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.MusicFolderTestDataUtils;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Genres;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MediaFileServiceUtils;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.search.IndexManager;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class ScannerProcedureServiceTest {

    private SettingsService settingsService;
    private IndexManager indexManager;
    private AlbumDao albumDao;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private ScannerProcedureService scannerProcedureService;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        indexManager = mock(IndexManager.class);
        mediaFileService = mock(MediaFileService.class);
        mediaFileDao = mock(MediaFileDao.class);
        albumDao = mock(AlbumDao.class);
        scannerProcedureService = new ScannerProcedureService(settingsService, indexManager, mediaFileService,
                mediaFileDao, mock(ArtistDao.class), albumDao, mock(SortProcedureService.class),
                new ScannerStateServiceImpl(indexManager), mock(Ehcache.class), mock(MediaFileCache.class));
    }

    @Nested
    class ScanFileTest {

        private Path createPath(String path) throws URISyntaxException {
            return Path.of(ScannerProcedureServiceTest.class.getResource(path).toURI());
        }

        @Test
        void testScanFile() throws URISyntaxException, ExecutionException {

            SecurityService securityService = mock(SecurityService.class);
            MetaDataParserFactory metaDataParserFactory = mock(MetaDataParserFactory.class);
            mediaFileService = new MediaFileService(settingsService, mock(MusicFolderService.class), securityService,
                    mock(MediaFileCache.class), mediaFileDao, mock(AlbumDao.class), metaDataParserFactory,
                    mock(MediaFileServiceUtils.class));

            assertTrue(mediaFileService.isSchemeLastModified());

            Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
            Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            Path dir = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004");
            assertTrue(Files.isDirectory(dir));
            MediaFile album = mediaFileService.createMediaFile(dir);

            Path file = createPath("/MEDIAS/Music2/_DIR_ chrome hoof - 2004/10 telegraph hill.mp3");
            assertFalse(Files.isDirectory(file));
            MediaFile child = mediaFileService.createMediaFile(file);
            child.setLastScanned(FAR_PAST);

            MusicFolder musicFolder = new MusicFolder(MusicFolderTestDataUtils.resolveBaseMediaPath().concat("Music2"),
                    "name", false, now());
            Instant scanStart = now();
            MediaLibraryStatistics statistics = new MediaLibraryStatistics(scanStart);
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();
            Genres genres = new Genres();

            List<MediaFile> children = Arrays.asList(child);
            Mockito.when(mediaFileDao.getChildrenOf(album.getPathString())).thenReturn(children);

            scannerProcedureService.scanFile(album, musicFolder, statistics, albumCount, genres, false);
        }
    }

    @Nested
    class UpdateAlbumTest {

        private MediaFile createSong() {
            // nonull
            MediaFile song = new MediaFile();
            song.setAlbumName("albumName");
            song.setParentPathString("parentPath");
            song.setMediaType(MediaType.MUSIC);
            song.setArtist("artist");

            // nullable
            song.setLastScanned(now().minus(1, ChronoUnit.SECONDS));
            return song;
        }

        private MediaLibraryStatistics createStatistics() {
            return new MediaLibraryStatistics(now());
        }

        private MusicFolder createMusicFolder() {
            return new MusicFolder(Integer.valueOf(1), "", "", true, now());
        }

        @Test
        void testIsNotAlbumUpdatable() {

            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            MediaFile song = createSong();
            song.setAlbumName(null);
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setParentPathString(null);
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setMediaType(MediaType.DIRECTORY);
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setAlbumArtist(null);
            song.setArtist(null);
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setAlbumArtist("albumArtist");
            song.setArtist(null);
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(Mockito.any(Album.class));

            Mockito.clearInvocations(albumDao);
            song = createSong();
            song.setAlbumArtist(null);
            song.setArtist("artist");
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(Mockito.any(Album.class));

            Mockito.clearInvocations(albumDao);
            song = createSong();
            song.setAlbumArtist("albumArtist");
            song.setArtist("artist");
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(Mockito.any(Album.class));
        }

        @Test
        void testFirstEncounter() {

            /*
             * Album property determines by date whether it is the first child. In other words, with this method, it is
             * not possible to parallelize all child scans. (The first child must complete the scan first)
             */

            final MediaFile song = createSong();
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            // Song dates are never updated
            assertNotEquals(song.getLastScanned(), statistics.getScanDate());

            // ## First run
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

            ArgumentCaptor<MediaFile> mediaCap = ArgumentCaptor.forClass(MediaFile.class);
            ArgumentCaptor<Album> albumCap = ArgumentCaptor.forClass(Album.class);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());
            Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Album.class));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(mediaCap.capture());

            Album registeredAlbum = albumCap.getValue();
            assertEquals(registeredAlbum.getLastScanned(), statistics.getScanDate());
            Mockito.when(albumDao.getAlbumForFile(Mockito.any(MediaFile.class))).thenReturn(registeredAlbum);

            // Song dates are never updated
            MediaFile registeredMedia = mediaCap.getValue();
            assertNotEquals(registeredMedia.getLastScanned(), statistics.getScanDate());

            // ## Second run
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

            // Currently always executed
            Mockito.verify(albumDao, Mockito.times(2)).createOrUpdateAlbum(Mockito.any(Album.class));

            // Not executed if already executed
            Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Album.class));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(Mockito.any(MediaFile.class));
        }

        @Test
        void testMergeOnFirstEncount() {

            /*
             * Year / Genre will adopt the value of the first song in the album. Jpsonic specifications.
             */

            final MediaFile song1 = createSong();
            song1.setYear(1111);
            song1.setGenre("Genre1");
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            // Song dates are never updated
            assertNotEquals(song1.getLastScanned(), statistics.getScanDate());

            // ## First run
            ArgumentCaptor<Album> albumCap = ArgumentCaptor.forClass(Album.class);
            scannerProcedureService.updateAlbum(song1, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());
            Album registeredAlbum = albumCap.getValue();
            assertEquals(registeredAlbum.getLastScanned(), statistics.getScanDate());

            final MediaFile song2 = createSong();
            song2.setYear(2222);
            song2.setGenre("Genre2");
            Mockito.when(albumDao.getAlbumForFile(song2)).thenReturn(registeredAlbum);

            // ## Second run
            albumCap = ArgumentCaptor.forClass(Album.class);
            scannerProcedureService.updateAlbum(song2, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(2)).createOrUpdateAlbum(albumCap.capture());
            assertEquals(2, albumCap.getAllValues().size());
            registeredAlbum = albumCap.getAllValues().get(1);

            assertEquals(Integer.valueOf(1111), registeredAlbum.getYear());
            assertEquals("Genre1", registeredAlbum.getGenre());
        }

        @Test
        void testGetMergedAlbum() {

            final MediaFile song1 = createSong();
            song1.setCoverArtPathString("coverArtPath1");

            song1.setMusicBrainzReleaseId("musicBrainzReleaseId1");
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            final MediaFile parent = createSong();
            Mockito.when(mediaFileService.getParentOf(Mockito.any(MediaFile.class))).thenReturn(parent);
            parent.setCoverArtPathString("parentCoverArtPath");

            // ## First run
            ArgumentCaptor<Album> albumCap = ArgumentCaptor.forClass(Album.class);
            scannerProcedureService.updateAlbum(song1, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());

            Album registeredAlbum = albumCap.getValue();
            assertEquals("musicBrainzReleaseId1", registeredAlbum.getMusicBrainzReleaseId());
            assertEquals("parentCoverArtPath", registeredAlbum.getCoverArtPath());

            final MediaFile song2 = createSong();
            song2.setCoverArtPathString("coverArtPath2");

            song2.setMusicBrainzReleaseId("musicBrainzReleaseId2");
            Mockito.when(albumDao.getAlbumForFile(song2)).thenReturn(registeredAlbum);

            // ## Second run
            albumCap = ArgumentCaptor.forClass(Album.class);
            scannerProcedureService.updateAlbum(song2, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(2)).createOrUpdateAlbum(albumCap.capture());
            assertEquals(2, albumCap.getAllValues().size());
            registeredAlbum = albumCap.getAllValues().get(1);

            assertEquals("musicBrainzReleaseId2", registeredAlbum.getMusicBrainzReleaseId());
            assertEquals("parentCoverArtPath", registeredAlbum.getCoverArtPath());
        }

        @Test
        void testCumulativeCount() {

            /*
             * albumDao#createOrUpdate The number of times an album is executed is greater than the number of songs. The
             * reason albumDao#createOrUpdateAlbum is always called is the count logic (... can be improved by batch)
             */

            final MediaFile song = createSong();
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            song.setDurationSeconds(60);

            // ## First run
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            ArgumentCaptor<MediaFile> mediaCap = ArgumentCaptor.forClass(MediaFile.class);
            ArgumentCaptor<Album> albumCap = ArgumentCaptor.forClass(Album.class);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());
            Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Album.class));
            Mockito.verify(mediaFileDao, Mockito.times(1)).createOrUpdateMediaFile(mediaCap.capture());

            Album registeredAlbum = albumCap.getValue();
            Mockito.when(albumDao.getAlbumForFile(Mockito.any(MediaFile.class))).thenReturn(registeredAlbum);

            // ### First result
            assertEquals(60, registeredAlbum.getDurationSeconds());
            assertEquals(1, registeredAlbum.getSongCount());

            // ## Second run
            albumCap = ArgumentCaptor.forClass(Album.class);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());
            scannerProcedureService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

            // ### Second result
            registeredAlbum = albumCap.getValue();
            Mockito.when(albumDao.getAlbumForFile(Mockito.any(MediaFile.class))).thenReturn(registeredAlbum);
            assertEquals(120, registeredAlbum.getDurationSeconds());
            assertEquals(2, registeredAlbum.getSongCount());
        }
    }
}
