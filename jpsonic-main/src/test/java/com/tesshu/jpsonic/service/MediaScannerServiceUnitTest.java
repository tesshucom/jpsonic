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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.search.IndexManager;
import net.sf.ehcache.Ehcache;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SuppressWarnings("PMD.TooManyStaticImports")
class MediaScannerServiceUnitTest {

    private IndexManager indexManager;
    private ArtistDao artistDao;
    private AlbumDao albumDao;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private MediaScannerService mediaScannerService;

    @BeforeEach
    public void setup() {
        indexManager = mock(IndexManager.class);
        mediaFileService = mock(MediaFileService.class);
        mediaFileDao = mock(MediaFileDao.class);
        artistDao = mock(ArtistDao.class);
        albumDao = mock(AlbumDao.class);
        mediaScannerService = new MediaScannerService(mock(SettingsService.class), mock(MusicFolderService.class),
                indexManager, mock(PlaylistService.class), mock(MediaFileCache.class), mediaFileService, mediaFileDao,
                artistDao, albumDao, mock(Ehcache.class), mock(MediaScannerServiceUtils.class),
                mock(ThreadPoolTaskExecutor.class));
    }

    @Test
    void testNeverScanned() {
        Mockito.when(indexManager.getStatistics()).thenReturn(null);
        assertTrue(mediaScannerService.neverScanned());

        Mockito.when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }

    @Nested
    class UpdateAlbumTest {

        private MediaFile createSong() {
            // nonull
            MediaFile song = new MediaFile();
            song.setAlbumName("albumName");
            song.setParentPath("parentPath");
            song.setMediaType(MediaType.MUSIC);
            song.setArtist("artist");

            // nullable
            song.setLastScanned(new Date());
            return song;
        }

        private MediaLibraryStatistics createStatistics() {
            return new MediaLibraryStatistics(DateUtils.truncate(new Date(), Calendar.SECOND));
        }

        private MusicFolder createMusicFolder() {
            return new MusicFolder(Integer.valueOf(1), new File(""), "", true, new Date());
        }

        @Test
        void testIsNotAlbumUpdatable() {

            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            MediaFile song = createSong();
            song.setAlbumName(null);
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setParentPath(null);
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setMediaType(MediaType.DIRECTORY);
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setAlbumArtist(null);
            song.setArtist(null);
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.never()).createOrUpdateAlbum(Mockito.any(Album.class));

            song = createSong();
            song.setAlbumArtist("albumArtist");
            song.setArtist(null);
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(Mockito.any(Album.class));

            Mockito.clearInvocations(albumDao);
            song = createSong();
            song.setAlbumArtist(null);
            song.setArtist("artist");
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(Mockito.any(Album.class));

            Mockito.clearInvocations(albumDao);
            song = createSong();
            song.setAlbumArtist("albumArtist");
            song.setArtist("artist");
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
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
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

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
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

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
            mediaScannerService.updateAlbum(song1, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());
            Album registeredAlbum = albumCap.getValue();
            assertEquals(registeredAlbum.getLastScanned(), statistics.getScanDate());

            final MediaFile song2 = createSong();
            song2.setYear(2222);
            song2.setGenre("Genre2");
            Mockito.when(albumDao.getAlbumForFile(song2)).thenReturn(registeredAlbum);

            // ## Second run
            albumCap = ArgumentCaptor.forClass(Album.class);
            mediaScannerService.updateAlbum(song2, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(2)).createOrUpdateAlbum(albumCap.capture());
            assertEquals(2, albumCap.getAllValues().size());
            registeredAlbum = albumCap.getAllValues().get(1);

            assertEquals(Integer.valueOf(1111), registeredAlbum.getYear());
            assertEquals("Genre1", registeredAlbum.getGenre());
        }

        @Test
        void testGetMergedAlbum() {

            final MediaFile song1 = createSong();
            song1.setCoverArtPath("coverArtPath1");

            song1.setMusicBrainzReleaseId("musicBrainzReleaseId1");
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            final MediaFile parent = createSong();
            Mockito.when(mediaFileService.getParentOf(Mockito.any(MediaFile.class))).thenReturn(parent);
            parent.setCoverArtPath("parentCoverArtPath");

            // ## First run
            ArgumentCaptor<Album> albumCap = ArgumentCaptor.forClass(Album.class);
            mediaScannerService.updateAlbum(song1, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(albumDao, Mockito.times(1)).createOrUpdateAlbum(albumCap.capture());

            Album registeredAlbum = albumCap.getValue();
            assertEquals("musicBrainzReleaseId1", registeredAlbum.getMusicBrainzReleaseId());
            assertEquals("parentCoverArtPath", registeredAlbum.getCoverArtPath());

            final MediaFile song2 = createSong();
            song2.setCoverArtPath("coverArtPath2");

            song2.setMusicBrainzReleaseId("musicBrainzReleaseId2");
            Mockito.when(albumDao.getAlbumForFile(song2)).thenReturn(registeredAlbum);

            // ## Second run
            albumCap = ArgumentCaptor.forClass(Album.class);
            mediaScannerService.updateAlbum(song2, musicFolder, statistics.getScanDate(), albumCount);
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
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);
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
            mediaScannerService.updateAlbum(song, musicFolder, statistics.getScanDate(), albumCount);

            // ### Second result
            registeredAlbum = albumCap.getValue();
            Mockito.when(albumDao.getAlbumForFile(Mockito.any(MediaFile.class))).thenReturn(registeredAlbum);
            assertEquals(120, registeredAlbum.getDurationSeconds());
            assertEquals(2, registeredAlbum.getSongCount());
        }
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
            // song.setLastScanned(new Date());
            return song;
        }

        private MediaLibraryStatistics createStatistics() {
            return new MediaLibraryStatistics(DateUtils.truncate(new Date(), Calendar.SECOND));
        }

        private MusicFolder createMusicFolder() {
            return new MusicFolder(Integer.valueOf(1), new File(""), "", true, new Date());
        }

        @Test
        void testIsNotUpdatable() {

            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            MediaFile song = createSong();
            song.setAlbumArtist(null);
            mediaScannerService.updateArtist(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(artistDao, Mockito.never()).createOrUpdateArtist(Mockito.any(Artist.class));

            song = createSong();
            song.setMediaType(MediaType.DIRECTORY);
            mediaScannerService.updateArtist(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(artistDao, Mockito.never()).createOrUpdateArtist(Mockito.any(Artist.class));
        }

        @Test
        void testFirstEncounter() {

            MediaFile song = createSong();
            MusicFolder musicFolder = createMusicFolder();
            MediaLibraryStatistics statistics = createStatistics();
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();

            // Song dates are never updated
            assertNotEquals(song.getLastScanned(), statistics.getScanDate());

            // ## First run
            mediaScannerService.updateArtist(song, musicFolder, statistics.getScanDate(), albumCount);
            Mockito.verify(artistDao, Mockito.times(1)).createOrUpdateArtist(Mockito.any(Artist.class));

            ArgumentCaptor<Artist> artistCap = ArgumentCaptor.forClass(Artist.class);
            Mockito.verify(artistDao, Mockito.times(1)).createOrUpdateArtist(artistCap.capture());
            Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Artist.class),
                    Mockito.any(MusicFolder.class));

            Artist registeredArtist = artistCap.getValue();
            assertEquals(registeredArtist.getLastScanned(), statistics.getScanDate());
            assertEquals(0, registeredArtist.getAlbumCount());
            Mockito.when(artistDao.getArtist(registeredArtist.getName())).thenReturn(registeredArtist);

            albumCount.putIfAbsent(registeredArtist.getName(), 99);

            // ## Second run
            artistCap = ArgumentCaptor.forClass(Artist.class);
            mediaScannerService.updateArtist(song, musicFolder, statistics.getScanDate(), albumCount);

            // Currently always executed
            Mockito.verify(artistDao, Mockito.times(2)).createOrUpdateArtist(artistCap.capture());

            // Not executed if already executed
            Mockito.verify(indexManager, Mockito.times(1)).index(Mockito.any(Artist.class),
                    Mockito.any(MusicFolder.class));

            registeredArtist = artistCap.getValue();
            assertEquals(registeredArtist.getLastScanned(), statistics.getScanDate());
            assertEquals(99, registeredArtist.getAlbumCount());
        }
    }
}
