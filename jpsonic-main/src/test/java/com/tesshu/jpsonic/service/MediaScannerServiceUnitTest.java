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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(NeedsHome.class)
@ExtendWith(MockitoExtension.class)
class MediaScannerServiceUnitTest {

    @InjectMocks
    private MediaScannerService mediaScannerService;

    @Mock
    private IndexManager indexManager;

    @Test
    void testNeverScanned() {
        when(indexManager.getStatistics()).thenReturn(null);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}
