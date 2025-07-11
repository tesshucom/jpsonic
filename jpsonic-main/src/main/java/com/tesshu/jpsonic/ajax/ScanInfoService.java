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

package com.tesshu.jpsonic.ajax;

import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.scanner.ScannerProcedureService;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for retrieving the currently playing file and
 * directory. This class is used by the DWR framework
 * (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxScanInfoService")
public class ScanInfoService {

    private final ScannerStateService scannerStateService;
    private final ScannerProcedureService scannerProcedureService;

    public ScanInfoService(ScannerStateService scannerStateService,
            ScannerProcedureService scannerProcedureService) {
        super();
        this.scannerStateService = scannerStateService;
        this.scannerProcedureService = scannerProcedureService;
    }

    /**
     * Returns media folder scanning status.
     */
    public ScanInfo getScanningStatus() {
        boolean scanning = scannerStateService.isScanning();
        int scanCount = (int) scannerStateService.getScanCount();
        return scannerProcedureService
            .getScanPhaseInfo()
            .map(phaseInfo -> new ScanInfo(scanning, scanCount, phaseInfo.phase(),
                    phaseInfo.phaseMax(), phaseInfo.phaseName(), phaseInfo.thread()))
            .orElse(new ScanInfo(scanning, scanCount));
    }
}
