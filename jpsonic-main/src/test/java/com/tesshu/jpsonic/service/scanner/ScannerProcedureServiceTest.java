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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import com.tesshu.jpsonic.service.metadata.VideoParser;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class ScannerProcedureServiceTest {

    private SettingsService settingsService;
    private MusicFolderServiceImpl musicFolderServiceImpl;
    private StaticsDao staticsDao;
    private ScannerProcedureService scannerProcedureService;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        MediaFileService mediaFileService = mock(MediaFileService.class);
        musicFolderServiceImpl = mock(MusicFolderServiceImpl.class);
        MediaFileDao mediaFileDao = mock(MediaFileDao.class);
        AlbumDao albumDao = mock(AlbumDao.class);
        staticsDao = mock(StaticsDao.class);
        WritableMediaFileService writableMediaFileService = new WritableMediaFileService(mediaFileDao, null,
                mediaFileService, albumDao, null, mock(MusicParser.class), mock(VideoParser.class), settingsService,
                mock(SecurityService.class), mock(JapaneseReadingUtils.class), mock(IndexManager.class),
                mock(MusicIndexServiceImpl.class));
        scannerProcedureService = new ScannerProcedureService(settingsService, musicFolderServiceImpl,
                mock(IndexManager.class), mediaFileService, writableMediaFileService, mock(PlaylistService.class),
                mock(TemplateWrapper.class), mediaFileDao, mock(ArtistDao.class), albumDao, staticsDao,
                mock(SortProcedureService.class), new ScannerStateServiceImpl(staticsDao),
                mock(MusicIndexServiceImpl.class), mock(MediaFileCache.class), mock(JapaneseReadingUtils.class),
                mock(JpsonicComparators.class), mock(ThreadPoolTaskExecutor.class));
    }

    @Nested
    class CheckMudicFoldersTest {

        @Test
        void testExistenceCheck() throws URISyntaxException {
            MusicFolder existingFolder = new MusicFolder(1,
                    Path.of(ScannerProcedureServiceTest.class.getResource("/MEDIAS/Music").toURI()).toString(),
                    "Existing", true, now(), 1, false);
            List<MusicFolder> folders = Arrays.asList(existingFolder);
            Mockito.when(musicFolderServiceImpl.getAllMusicFolders(false, true)).thenReturn(folders);
            Instant startDate = now();
            scannerProcedureService.checkMudicFolders(startDate);
            Mockito.verify(musicFolderServiceImpl, Mockito.never()).updateMusicFolder(startDate, existingFolder);

            MusicFolder notExistingFolder = new MusicFolder(2, existingFolder.getPathString() + "99", "Not existing",
                    true, now(), 2, false);
            folders = Arrays.asList(existingFolder, notExistingFolder);
            Mockito.when(musicFolderServiceImpl.getAllMusicFolders(false, true)).thenReturn(folders);
            scannerProcedureService.checkMudicFolders(startDate);
            Mockito.verify(musicFolderServiceImpl, Mockito.never()).updateMusicFolder(startDate, existingFolder);
            Mockito.verify(musicFolderServiceImpl, Mockito.times(1)).updateMusicFolder(startDate, notExistingFolder);
            Mockito.clearInvocations(musicFolderServiceImpl);

            MusicFolder existingFile = new MusicFolder(3,
                    Path.of(ScannerProcedureServiceTest.class.getResource("/MEDIAS/piano.mp3").toURI()).toString(),
                    "Existing file", true, now(), 3, false);
            folders = Arrays.asList(existingFolder, notExistingFolder, existingFile);
            Mockito.when(musicFolderServiceImpl.getAllMusicFolders(false, true)).thenReturn(folders);
            scannerProcedureService.checkMudicFolders(startDate);
            Mockito.verify(musicFolderServiceImpl, Mockito.never()).updateMusicFolder(startDate, existingFolder);
            Mockito.verify(musicFolderServiceImpl, Mockito.times(1)).updateMusicFolder(startDate, notExistingFolder);
            Mockito.verify(musicFolderServiceImpl, Mockito.times(1)).updateMusicFolder(startDate, existingFile);
        }

        @Test
        void testOrderCheck() throws URISyntaxException {
            MusicFolder orderedFolder = new MusicFolder(1,
                    Path.of(ScannerProcedureServiceTest.class.getResource("/MEDIAS/Music").toURI()).toString(),
                    "Ordered", true, now(), 1, false);
            List<MusicFolder> folders = Arrays.asList(orderedFolder);
            Mockito.when(musicFolderServiceImpl.getAllMusicFolders(false, true)).thenReturn(folders);
            Instant startDate = now();
            scannerProcedureService.checkMudicFolders(startDate);
            Mockito.verify(musicFolderServiceImpl, Mockito.never()).updateMusicFolder(startDate, orderedFolder);
            Mockito.clearInvocations(musicFolderServiceImpl);

            MusicFolder notOrderedFolder1 = new MusicFolder(2,
                    Path.of(ScannerProcedureServiceTest.class.getResource("/MEDIAS/Music2").toURI()).toString(),
                    "Music2", true, now(), -1, false);
            MusicFolder notOrderedFolder2 = new MusicFolder(3,
                    Path.of(ScannerProcedureServiceTest.class.getResource("/MEDIAS/Music3").toURI()).toString(),
                    "Music3", true, now(), -1, false);
            folders = Arrays.asList(orderedFolder, notOrderedFolder1, notOrderedFolder2);
            Mockito.when(musicFolderServiceImpl.getAllMusicFolders(false, true)).thenReturn(folders);
            ArgumentCaptor<MusicFolder> folderCaptor = ArgumentCaptor.forClass(MusicFolder.class);
            Mockito.doNothing().when(musicFolderServiceImpl).updateMusicFolder(Mockito.any(Instant.class),
                    folderCaptor.capture());
            scannerProcedureService.checkMudicFolders(startDate);
            assertEquals(3, folderCaptor.getAllValues().size());
            assertEquals(1, folderCaptor.getAllValues().get(0).getFolderOrder());
            assertEquals(2, folderCaptor.getAllValues().get(1).getFolderOrder());
            assertEquals(3, folderCaptor.getAllValues().get(2).getFolderOrder());
        }
    }

    @Test
    void testCreateScanEvent() {
        Instant startDate = now();

        // Log only mandary events if useScanEvents=false
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(false);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.SUCCESS, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.CANCELED, null);
        Mockito.verify(staticsDao, Mockito.times(2)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.DESTROYED, null);
        Mockito.verify(staticsDao, Mockito.times(3)).createScanEvent(Mockito.any(ScanEvent.class));
        Mockito.clearInvocations(staticsDao);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
        Mockito.verify(staticsDao, Mockito.never()).createScanEvent(Mockito.any(ScanEvent.class));

        // Log all events if useScanEvents=true
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(true);
        Mockito.clearInvocations(staticsDao);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.SUCCESS, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.CANCELED, null);
        Mockito.verify(staticsDao, Mockito.times(2)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.DESTROYED, null);
        Mockito.verify(staticsDao, Mockito.times(3)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
        Mockito.verify(staticsDao, Mockito.times(4)).createScanEvent(Mockito.any(ScanEvent.class));

        // No memory metering
        Mockito.when(settingsService.isMeasureMemory()).thenReturn(false);
        ArgumentCaptor<ScanEvent> eventCap = ArgumentCaptor.forClass(ScanEvent.class);
        Mockito.doNothing().when(staticsDao).createScanEvent(eventCap.capture());
        scannerProcedureService.createScanEvent(startDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
        assertEquals(-1, eventCap.getValue().getFreeMemory());

        // With memory metering
        Mockito.when(settingsService.isMeasureMemory()).thenReturn(true);
        eventCap = ArgumentCaptor.forClass(ScanEvent.class);
        Mockito.doNothing().when(staticsDao).createScanEvent(eventCap.capture());
        scannerProcedureService.createScanEvent(startDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
        assertNotEquals(-1, eventCap.getValue().getFreeMemory());
    }

    @Test
    void testInvokeUpdateOrder() {

        MediaFile m1 = new MediaFile();
        m1.setPathString("path1");
        m1.setPresent(true);
        m1.setOrder(1);
        MediaFile m2 = new MediaFile();
        m2.setPathString("path2");
        m2.setPresent(true);
        m2.setOrder(2);
        MediaFile m3 = new MediaFile();
        m3.setPathString("path3");
        m3.setPresent(true);
        m3.setOrder(3);

        JapaneseReadingUtils readingUtils = mock(JapaneseReadingUtils.class);
        JpsonicComparators comparators = new JpsonicComparators(mock(SettingsService.class), readingUtils);
        WritableMediaFileService wmfs = mock(WritableMediaFileService.class);

        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);

        int count = scannerProcedureService.invokeUpdateOrder(Arrays.asList(m2, m3, m1), comparators.songsDefault(),
                (song) -> wmfs.updateOrder(song));
        assertEquals(3, count);

        List<MediaFile> result = captor.getAllValues();
        assertEquals(3, result.size());
        assertEquals("path1", result.get(0).getPathString());
        assertEquals("path2", result.get(1).getPathString());
        assertEquals("path3", result.get(2).getPathString());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scannerProcedureService.invokeUpdateOrder(result, comparators.songsDefault(), (song) -> wmfs.updateOrder(song));
        assertEquals(0, captor.getAllValues().size());

        m1.setOrder(3);
        m2.setOrder(2);
        m3.setOrder(1);
        count = scannerProcedureService.invokeUpdateOrder(Arrays.asList(m1, m2, m3), comparators.songsDefault(),
                (song) -> wmfs.updateOrder(song));
        assertEquals(2, count);
        result = captor.getAllValues();
        assertEquals(2, result.size());
        assertEquals("path1", result.get(0).getPathString());
        assertEquals(1, result.get(0).getOrder());
        assertEquals("path3", result.get(1).getPathString());
        assertEquals(3, result.get(1).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scannerProcedureService.invokeUpdateOrder(result, comparators.songsDefault(), (song) -> wmfs.updateOrder(song));
        result = captor.getAllValues();
        assertEquals(1, result.size());
        assertEquals("path3", result.get(0).getPathString());
        assertEquals(2, result.get(0).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scannerProcedureService.invokeUpdateOrder(result, comparators.songsDefault(), (song) -> wmfs.updateOrder(song));
        result = captor.getAllValues();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scannerProcedureService.invokeUpdateOrder(result, comparators.songsDefault(), (song) -> wmfs.updateOrder(song));
        result = captor.getAllValues();
        assertEquals(0, result.size());
    }

    @Test
    void testGetScanPhaseInfo() {
        ScannerStateServiceImpl scannerStateService = mock(ScannerStateServiceImpl.class);
        scannerProcedureService = new ScannerProcedureService(settingsService, musicFolderServiceImpl,
                mock(IndexManager.class), mock(MediaFileService.class), mock(WritableMediaFileService.class),
                mock(PlaylistService.class), mock(TemplateWrapper.class), mock(MediaFileDao.class),
                mock(ArtistDao.class), mock(AlbumDao.class), mock(StaticsDao.class), mock(SortProcedureService.class),
                scannerStateService, mock(MusicIndexServiceImpl.class), mock(MediaFileCache.class),
                mock(JapaneseReadingUtils.class), mock(JpsonicComparators.class), mock(ThreadPoolTaskExecutor.class));

        Mockito.when(scannerStateService.isScanning()).thenReturn(false);
        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.UNKNOWN);
        assertFalse(scannerProcedureService.getScanPhaseInfo().isPresent());

        Mockito.when(scannerStateService.isScanning()).thenReturn(true);
        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.MUSIC_FOLDER_CHECK);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(2, scanPhaseInfo.phase());
            assertEquals(22, scanPhaseInfo.phaseMax());
            assertEquals("PARSE_FILE_STRUCTURE", scanPhaseInfo.phaseName());
            assertEquals(0, scanPhaseInfo.thread());
        });

        Mockito.when(scannerStateService.isScanning()).thenReturn(true);
        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.SCANNED_COUNT);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(2, scanPhaseInfo.phase());
            assertEquals(22, scanPhaseInfo.phaseMax());
            assertEquals("PARSE_FILE_STRUCTURE", scanPhaseInfo.phaseName());
            assertEquals(0, scanPhaseInfo.thread());
        });

        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.CHECKPOINT);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(21, scanPhaseInfo.phase());
            assertEquals(22, scanPhaseInfo.phaseMax());
            assertEquals("AFTER_SCAN", scanPhaseInfo.phaseName());
            assertEquals(0, scanPhaseInfo.thread());
        });

        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.AFTER_SCAN);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(21, scanPhaseInfo.phase());
            assertEquals(22, scanPhaseInfo.phaseMax());
            assertEquals("AFTER_SCAN", scanPhaseInfo.phaseName());
            assertEquals(0, scanPhaseInfo.thread());
        });

        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.AFTER_SCAN);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(21, scanPhaseInfo.phase());
            assertEquals(22, scanPhaseInfo.phaseMax());
            assertEquals("AFTER_SCAN", scanPhaseInfo.phaseName());
            assertEquals(0, scanPhaseInfo.thread());
        });

        Mockito.when(scannerStateService.getLastEvent()).thenReturn(ScanEventType.CANCELED);
        Mockito.when(staticsDao.getScanEvents(Mockito.nullable(Instant.class))).thenReturn(Collections.emptyList());
        assertTrue(scannerProcedureService.getScanPhaseInfo().isPresent());
        scannerProcedureService.getScanPhaseInfo().ifPresent(scanPhaseInfo -> {
            assertEquals(-1, scanPhaseInfo.phase());
            assertEquals(-1, scanPhaseInfo.phaseMax());
            assertEquals("Semi Scan Proc", scanPhaseInfo.phaseName());
            assertEquals(-1, scanPhaseInfo.thread());
        });
    }
}
