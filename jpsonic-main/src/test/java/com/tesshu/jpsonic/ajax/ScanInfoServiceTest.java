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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Optional;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.service.scanner.ScannerProcedureService;
import com.tesshu.jpsonic.service.scanner.ScannerProcedureService.ScanPhaseInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;

class ScanInfoServiceTest {

    private ScannerProcedureService scannerProcedureService;
    private ScanInfoService scanInfoService;

    @BeforeEach
    public void setup() {
        StatusService statusService = mock(StatusService.class);
        MediaFile file = new MediaFile();
        file.setId(0);
        Player player = new Player();
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        PlayStatus playStatus = new PlayStatus(file, player, now());
        Mockito.when(statusService.getPlayStatuses()).thenReturn(Arrays.asList(playStatus));
        scannerProcedureService = mock(ScannerProcedureService.class);
        scanInfoService = new ScanInfoService(mock(ScannerStateService.class), scannerProcedureService);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testgetScanningStatus() {
        assertEquals(-1, scanInfoService.getScanningStatus().getPhase());

        ScanPhaseInfo info = new ScanPhaseInfo(0, 10, "phaseName", 1);
        Mockito.when(scannerProcedureService.getScanPhaseInfo()).thenReturn(Optional.of(info));
        assertEquals(0, scanInfoService.getScanningStatus().getPhase());
    }
}
