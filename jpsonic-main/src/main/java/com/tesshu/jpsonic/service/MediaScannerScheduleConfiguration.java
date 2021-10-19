/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class MediaScannerScheduleConfiguration implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerScheduleConfiguration.class);

    private final TaskScheduler taskScheduler;
    private final SettingsService settingsService;
    private final MediaScannerService mediaScannerService;

    public MediaScannerScheduleConfiguration(TaskScheduler taskScheduler, SettingsService settingsService,
            MediaScannerService mediaScannerService) {
        super();
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
    }

    final Date createFirstTime() {
        int hour = getSettingsService().getIndexCreationHour();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }
        long initialDelay = ChronoUnit.MILLIS.between(now, nextRun);
        return Date.from(now.plus(initialDelay, ChronoUnit.MILLIS).atZone(ZoneId.systemDefault()).toInstant());
    }

    private SettingsService getSettingsService() {
        return settingsService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setScheduler(taskScheduler);

        Trigger trigger = (triggerContext) -> {
            Date lastTime = triggerContext.lastCompletionTime();
            Date nextTime = lastTime == null ? createFirstTime()
                    : Date.from(lastTime.toInstant().plus(1L, ChronoUnit.DAYS));
            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Daily auto library scan was scheduled. (Next {})",
                        new SimpleDateFormat("yyyy/MM/dd HH:mm", settingsService.getLocale()).format(nextTime));
            }

            // In addition, create index immediately if it doesn't exist on disk.
            if (SettingsService.isScanOnBoot() && mediaScannerService.neverScanned()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Media library never scanned. Doing it now.");
                }
                mediaScannerService.scanLibrary();
            }
            return nextTime;
        };

        registrar.addTriggerTask(new ScanLibraryTask(mediaScannerService), trigger);
    }

    private static class ScanLibraryTask implements Runnable {

        private MediaScannerService mediaScannerService;

        public ScanLibraryTask(MediaScannerService mediaScannerService) {
            super();
            this.mediaScannerService = mediaScannerService;
        }

        @Override
        public void run() {
            mediaScannerService.scanLibrary();
        }
    }
}
