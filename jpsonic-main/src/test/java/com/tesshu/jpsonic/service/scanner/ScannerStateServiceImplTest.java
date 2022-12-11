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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ScannerStateServiceImplTest {

    private IndexManager indexManager;
    private ScannerStateServiceImpl scannerStateService;

    @BeforeEach
    public void setup() {
        indexManager = mock(IndexManager.class);
        scannerStateService = new ScannerStateServiceImpl(indexManager);
    }

    @Test
    void testNeverScanned() {
        Mockito.when(indexManager.getStatistics()).thenReturn(null);
        assertTrue(scannerStateService.neverScanned());

        Mockito.when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(scannerStateService.neverScanned());
    }

    @Test
    void testScanCount() {
        assertEquals(0, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(1, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(2, scannerStateService.getScanCount());

        scannerStateService.tryScanningLock();
        assertEquals(0, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(1, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(2, scannerStateService.getScanCount());
        scannerStateService.unlockScanning();

        assertEquals(0, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(1, scannerStateService.getScanCount());
        scannerStateService.incrementScanCount();
        assertEquals(2, scannerStateService.getScanCount());
    }
}
