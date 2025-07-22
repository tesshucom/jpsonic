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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class ScanHelperTest {

    private SettingsService settingsService;
    private ScanHelper scanHelper;
    private StaticsDao staticsDao;

    @BeforeEach
    public void setup() {
        final ScannerStateServiceImpl scannerStateService = mock(ScannerStateServiceImpl.class);
        settingsService = mock(SettingsService.class);
        staticsDao = mock(StaticsDao.class);
        final MediaFileDao mediaFileDao = mock(MediaFileDao.class);
        final IndexManager indexManager = mock(IndexManager.class);
        final WritableMediaFileService wmfs = mock(WritableMediaFileService.class);
        scanHelper = new ScanHelper(scannerStateService, settingsService, staticsDao, mediaFileDao,
                indexManager, wmfs);
    }

    @Test
    void testCreateScanEvent() {
        Instant startDate = now();
        ScanContext context = new ScanContext(startDate, false, settingsService.getPodcastFolder(),
                settingsService.isSortStrict(), settingsService.isUseScanLog(),
                settingsService.getScanLogRetention(), settingsService.getDefaultScanLogRetention(),
                settingsService.isUseScanEvents(), settingsService.isMeasureMemory());

        // Log only mandary events if useScanEvents=false
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(false);
        scanHelper.createScanEvent(context, ScanEventType.SUCCESS, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scanHelper.createScanEvent(context, ScanEventType.CANCELED, null);
        Mockito.verify(staticsDao, Mockito.times(2)).createScanEvent(Mockito.any(ScanEvent.class));
        scanHelper.createScanEvent(context, ScanEventType.DESTROYED, null);
        Mockito.verify(staticsDao, Mockito.times(3)).createScanEvent(Mockito.any(ScanEvent.class));
        Mockito.clearInvocations(staticsDao);
        scanHelper.createScanEvent(context, ScanEventType.PARSE_FILE_STRUCTURE, null);
        Mockito.verify(staticsDao, Mockito.never()).createScanEvent(Mockito.any(ScanEvent.class));

        // Log all events if useScanEvents=true
        Mockito.when(settingsService.isUseScanEvents()).thenReturn(true);
        context = new ScanContext(startDate, false, settingsService.getPodcastFolder(),
                settingsService.isSortStrict(), settingsService.isUseScanLog(),
                settingsService.getScanLogRetention(), settingsService.getDefaultScanLogRetention(),
                settingsService.isUseScanEvents(), settingsService.isMeasureMemory());
        Mockito.clearInvocations(staticsDao);
        scanHelper.createScanEvent(context, ScanEventType.SUCCESS, null);
        Mockito.verify(staticsDao, Mockito.times(1)).createScanEvent(Mockito.any(ScanEvent.class));
        scanHelper.createScanEvent(context, ScanEventType.CANCELED, null);
        Mockito.verify(staticsDao, Mockito.times(2)).createScanEvent(Mockito.any(ScanEvent.class));
        scanHelper.createScanEvent(context, ScanEventType.DESTROYED, null);
        Mockito.verify(staticsDao, Mockito.times(3)).createScanEvent(Mockito.any(ScanEvent.class));
        scanHelper.createScanEvent(context, ScanEventType.PARSE_FILE_STRUCTURE, null);
        Mockito.verify(staticsDao, Mockito.times(4)).createScanEvent(Mockito.any(ScanEvent.class));

        // No memory metering
        Mockito.when(settingsService.isMeasureMemory()).thenReturn(false);
        context = new ScanContext(startDate, false, settingsService.getPodcastFolder(),
                settingsService.isSortStrict(), settingsService.isUseScanLog(),
                settingsService.getScanLogRetention(), settingsService.getDefaultScanLogRetention(),
                settingsService.isUseScanEvents(), settingsService.isMeasureMemory());
        ArgumentCaptor<ScanEvent> eventCap = ArgumentCaptor.forClass(ScanEvent.class);
        Mockito.doNothing().when(staticsDao).createScanEvent(eventCap.capture());
        scanHelper.createScanEvent(context, ScanEventType.PARSE_FILE_STRUCTURE, null);
        assertEquals(-1, eventCap.getValue().getFreeMemory());

        // With memory metering
        Mockito.when(settingsService.isMeasureMemory()).thenReturn(true);
        context = new ScanContext(startDate, false, settingsService.getPodcastFolder(),
                settingsService.isSortStrict(), settingsService.isUseScanLog(),
                settingsService.getScanLogRetention(), settingsService.getDefaultScanLogRetention(),
                settingsService.isUseScanEvents(), settingsService.isMeasureMemory());
        eventCap = ArgumentCaptor.forClass(ScanEvent.class);
        Mockito.doNothing().when(staticsDao).createScanEvent(eventCap.capture());
        scanHelper.createScanEvent(context, ScanEventType.PARSE_FILE_STRUCTURE, null);
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
        JpsonicComparators comparators = new JpsonicComparators(mock(SettingsService.class),
                readingUtils);
        WritableMediaFileService wmfs = mock(WritableMediaFileService.class);

        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);

        int count = scanHelper
            .invokeUpdateOrder(Arrays.asList(m2, m3, m1), comparators.songsDefault(),
                    wmfs::updateOrder);
        assertEquals(3, count);

        List<MediaFile> result = captor.getAllValues();
        assertEquals(3, result.size());
        assertEquals("path1", result.get(0).getPathString());
        assertEquals("path2", result.get(1).getPathString());
        assertEquals("path3", result.get(2).getPathString());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scanHelper.invokeUpdateOrder(result, comparators.songsDefault(), wmfs::updateOrder);
        assertEquals(0, captor.getAllValues().size());

        m1.setOrder(3);
        m2.setOrder(2);
        m3.setOrder(1);
        count = scanHelper
            .invokeUpdateOrder(Arrays.asList(m1, m2, m3), comparators.songsDefault(),
                    wmfs::updateOrder);
        assertEquals(2, count);
        result = captor.getAllValues();
        assertEquals(2, result.size());
        assertEquals("path1", result.get(0).getPathString());
        assertEquals(1, result.get(0).getOrder());
        assertEquals("path3", result.get(1).getPathString());
        assertEquals(3, result.get(1).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scanHelper.invokeUpdateOrder(result, comparators.songsDefault(), wmfs::updateOrder);
        result = captor.getAllValues();
        assertEquals(1, result.size());
        assertEquals("path3", result.get(0).getPathString());
        assertEquals(2, result.get(0).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scanHelper.invokeUpdateOrder(result, comparators.songsDefault(), wmfs::updateOrder);
        result = captor.getAllValues();
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getOrder());

        captor = ArgumentCaptor.forClass(MediaFile.class);
        Mockito.when(wmfs.updateOrder(captor.capture())).thenReturn(1);
        scanHelper.invokeUpdateOrder(result, comparators.songsDefault(), wmfs::updateOrder);
        result = captor.getAllValues();
        assertEquals(0, result.size());
    }
}
