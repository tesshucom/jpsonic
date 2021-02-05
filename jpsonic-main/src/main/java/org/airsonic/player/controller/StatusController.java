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

package org.airsonic.player.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Controller for the status page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/status")
public class StatusController {

    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    private static final long LIMIT_OF_HISTORY_TO_BE_PRESENTED = 60L;

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TransferStatusHolder) Not reusable
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        List<TransferStatus> streamStatuses = statusService.getAllStreamStatuses();
        List<TransferStatus> downloadStatuses = statusService.getAllDownloadStatuses();
        List<TransferStatus> uploadStatuses = statusService.getAllUploadStatuses();

        Locale locale = RequestContextUtils.getLocale(request);
        List<TransferStatusHolder> transferStatuses = new ArrayList<>();

        for (int i = 0; i < streamStatuses.size(); i++) {
            long minutesAgo = streamStatuses.get(i).getMillisSinceLastUpdate() / 1000L / 60L;
            if (minutesAgo < LIMIT_OF_HISTORY_TO_BE_PRESENTED) {
                transferStatuses.add(new TransferStatusHolder(streamStatuses.get(i), true, false, false, i, locale));
            }
        }
        for (int i = 0; i < downloadStatuses.size(); i++) {
            transferStatuses.add(new TransferStatusHolder(downloadStatuses.get(i), false, true, false, i, locale));
        }
        for (int i = 0; i < uploadStatuses.size(); i++) {
            transferStatuses.add(new TransferStatusHolder(uploadStatuses.get(i), false, false, true, i, locale));
        }
        return new ModelAndView("status", "model",
                LegacyMap.of("brand", settingsService.getBrand(), "admin",
                        securityService.isAdmin(securityService.getCurrentUser(request).getUsername()), "showStatus",
                        settingsService.isShowStatus(), "transferStatuses", transferStatuses, "chartWidth",
                        StatusChartController.IMAGE_WIDTH, "chartHeight", StatusChartController.IMAGE_HEIGHT));
    }

    public static class TransferStatusHolder {
        private TransferStatus transferStatus;
        private boolean stream;
        private boolean download;
        private boolean upload;
        private int index;
        private Locale locale;

        TransferStatusHolder(TransferStatus transferStatus, boolean isStream, boolean isDownload, boolean isUpload,
                int index, Locale locale) {
            this.transferStatus = transferStatus;
            this.stream = isStream;
            this.download = isDownload;
            this.upload = isUpload;
            this.index = index;
            this.locale = locale;
        }

        public boolean isStream() {
            return stream;
        }

        public boolean isDownload() {
            return download;
        }

        public boolean isUpload() {
            return upload;
        }

        public int getIndex() {
            return index;
        }

        public Player getPlayer() {
            return transferStatus.getPlayer();
        }

        public String getPlayerType() {
            Player player = transferStatus.getPlayer();
            return player == null ? null : player.getType();
        }

        public String getUsername() {
            Player player = transferStatus.getPlayer();
            return player == null ? null : player.getUsername();
        }

        public String getPath() {
            return FileUtil.getShortPath(transferStatus.getFile());
        }

        public String getBytes() {
            return StringUtil.formatBytes(transferStatus.getBytesTransfered(), locale);
        }
    }

}
