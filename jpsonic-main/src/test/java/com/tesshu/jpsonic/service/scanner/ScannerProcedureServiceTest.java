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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.search.IndexManager;
import net.sf.ehcache.Ehcache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class ScannerProcedureServiceTest {

    private SettingsService settingsService;
    private ScannerProcedureService scannerProcedureService;
    private StaticsDao staticsDao;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        MediaFileService mediaFileService = mock(MediaFileService.class);
        MediaFileDao mediaFileDao = mock(MediaFileDao.class);
        AlbumDao albumDao = mock(AlbumDao.class);
        staticsDao = mock(StaticsDao.class);
        WritableMediaFileService writableMediaFileService = new WritableMediaFileService(mediaFileDao, null,
                mediaFileService, albumDao, null, mock(MetaDataParserFactory.class), settingsService,
                mock(SecurityService.class), mock(JapaneseReadingUtils.class), mock(IndexManager.class));
        scannerProcedureService = new ScannerProcedureService(settingsService, mock(MusicFolderService.class),
                mock(IndexManager.class), mediaFileService, writableMediaFileService, mock(PlaylistService.class),
                mediaFileDao, mock(ArtistDao.class), albumDao, staticsDao, mock(SortProcedureService.class),
                new ScannerStateServiceImpl(staticsDao), mock(Ehcache.class), mock(MediaFileCache.class),
                mock(JapaneseReadingUtils.class));
    }

    @Test
    void testCreateScanEvent() {
        Instant startDate = now();

        // Log only mandary events if useScanEvents=false
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(false);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.FINISHED, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.FAILED, null);
        Mockito.verify(staticsDao, Mockito.times(2)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.DESTROYED, null);
        Mockito.verify(staticsDao, Mockito.times(3)).createScanEvent(Mockito.any(ScanEvent.class));
        Mockito.clearInvocations(staticsDao);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
        Mockito.verify(staticsDao, Mockito.never()).createScanEvent(Mockito.any(ScanEvent.class));

        // Log all events if useScanEvents=true
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(true);
        Mockito.clearInvocations(staticsDao);
        scannerProcedureService.createScanEvent(startDate, ScanEventType.FINISHED, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scannerProcedureService.createScanEvent(startDate, ScanEventType.FAILED, null);
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
}
