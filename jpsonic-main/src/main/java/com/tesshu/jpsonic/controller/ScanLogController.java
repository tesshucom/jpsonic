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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.domain.ScanLog;
import com.tesshu.jpsonic.domain.ScanLog.ScanLogType;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.StringUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping({ "/scanlog", "/scanlog.view" })
public class ScanLogController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ScannerStateService scannerStateService;
    private final StaticsDao staticsDao;

    public ScanLogController(SettingsService settingsService, SecurityService securityService,
            ScannerStateService scannerStateService, StaticsDao staticsDao) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.scannerStateService = scannerStateService;
        this.staticsDao = staticsDao;
    }

    @GetMapping
    protected ModelAndView get(HttpServletRequest request,
            @RequestParam(value = Attributes.Request.NameConstants.START_DATE, required = false) String reqStartDate,
            @RequestParam(value = Attributes.Request.NameConstants.SHOW_SCANNED_COUNT, required = false) String reqShowCount) {

        Map<String, Object> model = LegacyMap.of();

        model.put("brand", SettingsService.getBrand());
        model.put("admin", securityService.isAdmin(securityService.getCurrentUserStrict(request).getUsername()));
        model.put("scanning", scannerStateService.isScanning());
        model.put("showStatus", settingsService.isShowStatus());

        List<ScanLogVO> scanLogs = staticsDao.getScanLog(ScanLogType.SCAN_ALL).stream().map(ScanLogVO::new)
                .collect(Collectors.toList());
        LocalDateTime lastStartDate = scanLogs.get(0).getStartDate();
        if (!scanLogs.isEmpty()) {
            scanLogs.forEach(scanLog -> setStatus(lastStartDate, scanLog));
        }
        model.put("scanLogs", scanLogs);

        LocalDateTime selectedStartDate = isEmpty(reqStartDate) ? lastStartDate : LocalDateTime.parse(reqStartDate);
        model.put("startDate", selectedStartDate);

        User user = securityService.getCurrentUserStrict(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        if (userSettings.isShowScannedCount() != Boolean.valueOf(reqShowCount)) {
            userSettings.setShowScannedCount(Boolean.valueOf(reqShowCount));
            securityService.updateUserSettings(userSettings);
        }
        boolean showScannedCount = userSettings.isShowScannedCount();
        model.put("showScannedCount", showScannedCount);

        if (!scanLogs.isEmpty()) {
            List<ScanEventVO> scanEvents = createScanEvents(selectedStartDate, showScannedCount);
            model.put("scanEvents", scanEvents);

            if (!scanEvents.isEmpty()) {
                String scanEventsDuration = getDurationString(selectedStartDate,
                        scanEvents.get(scanEvents.size() - 1).getExecuted());
                model.put("scanEventsDuration", scanEventsDuration);
            }
        }

        return new ModelAndView("scanLog", "model", model);
    }

    private List<ScanEventVO> createScanEvents(@NonNull LocalDateTime selectedStartDate, boolean showScannedCount) {
        @SuppressWarnings("deprecation")
        List<ScanEventVO> scanEvents = staticsDao
                .getScanEvents(selectedStartDate.atZone(ZoneOffset.systemDefault()).toInstant()).stream()
                .filter(scanEvent -> showScannedCount || scanEvent.getType() != ScanEventType.SCANNED_COUNT
                        && scanEvent.getType() != ScanEventType.PARSED_COUNT)
                .map(ScanEventVO::new).collect(Collectors.toList());
        setDurations(selectedStartDate, scanEvents);
        return scanEvents;
    }

    @SuppressWarnings("deprecation")
    private void setStatus(@NonNull LocalDateTime lastStartDate, ScanLogVO scanLog) {
        final ScanEventType lastEventType = staticsDao
                .getLastScanEventType(scanLog.getStartDate().atZone(ZoneOffset.systemDefault()).toInstant());
        switch (lastEventType) {
        case SUCCESS:
        case FAILED:
        case DESTROYED:
        case CANCELED:
        case FINISHED:
            scanLog.setStatus(lastEventType.name());
            break;
        default:
            if (lastStartDate.equals(scanLog.getStartDate()) && scannerStateService.isScanning()) {
                scanLog.setStatus("SCANNING");
            } else {
                scanLog.setStatus(ScanEventType.UNKNOWN.name());
            }
            break;
        }
    }

    @SuppressWarnings("PMD.ForLoopCanBeForeach") // false positive
    private void setDurations(@NonNull LocalDateTime startDate, List<ScanEventVO> scanEvents) {
        if (scanEvents.isEmpty()) {
            return;
        }
        scanEvents.get(0).setDuration(getDurationString(startDate, scanEvents.get(0).getExecuted()));
        for (int i = 0; i < scanEvents.size(); i++) {
            if (scanEvents.get(i).getDuration() != null) {
                continue;
            }
            scanEvents.get(i).setDuration(
                    getDurationString(scanEvents.get(i - 1).getExecuted(), scanEvents.get(i).getExecuted()));
        }
    }

    private String getDurationString(LocalDateTime from, LocalDateTime to) {
        Duration duration = Duration.between(from, to);
        return StringUtil.formatDurationHMMSS(duration.toSeconds()) + "."
                + String.format("%03d", duration.toMillis() % 1_000);
    }

    public static class ScanLogVO {

        private final LocalDateTime startDate;
        private final ScanLogType type;
        private String status;

        ScanLogVO(@NonNull ScanLog scanLog) {
            super();
            this.startDate = LocalDateTime.ofInstant(scanLog.getStartDate(), ZoneId.systemDefault());
            this.type = scanLog.getType();
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public ScanLogType getType() {
            return type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ScanEventVO {

        private final LocalDateTime executed;
        private final ScanEventType type;
        private final String maxMemory;
        private final String totalMemory;
        private final String usedMemory;
        private final String comment;
        private String duration;

        public ScanEventVO(@NonNull ScanEvent scanEvent) {
            super();
            this.executed = LocalDateTime.ofInstant(scanEvent.getExecuted(), ZoneId.systemDefault());
            this.type = scanEvent.getType();
            this.maxMemory = scanEvent.getMaxMemory() > 0 ? FileUtil.byteCountToDisplaySize(scanEvent.getMaxMemory())
                    : "-";
            this.totalMemory = scanEvent.getTotalMemory() > 0
                    ? FileUtil.byteCountToDisplaySize(scanEvent.getTotalMemory()) : "-";
            this.usedMemory = scanEvent.getFreeMemory() > 0
                    ? FileUtil.byteCountToDisplaySize(scanEvent.getTotalMemory() - scanEvent.getFreeMemory()) : "-";
            this.comment = scanEvent.getComment();
        }

        public LocalDateTime getExecuted() {
            return executed;
        }

        public ScanEventType getType() {
            return type;
        }

        public String getMaxMemory() {
            return maxMemory;
        }

        public String getTotalMemory() {
            return totalMemory;
        }

        public String getUsedMemory() {
            return usedMemory;
        }

        public String getComment() {
            return comment;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }
}
