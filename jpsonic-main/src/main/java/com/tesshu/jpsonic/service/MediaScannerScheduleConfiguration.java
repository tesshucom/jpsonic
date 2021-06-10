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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
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

    private final Supplier<Date> firstTime = () -> {
        int hour = getSettingsService().getIndexCreationHour();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }
        long initialDelay = ChronoUnit.MILLIS.between(now, nextRun);
        return Date.from(now.plus(initialDelay, ChronoUnit.MILLIS).atZone(ZoneId.systemDefault()).toInstant());
    };

    public MediaScannerScheduleConfiguration(TaskScheduler taskScheduler, SettingsService settingsService,
            MediaScannerService mediaScannerService) {
        super();
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
    }

    private SettingsService getSettingsService() {
        return settingsService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler);

        scheduledTaskRegistrar.addTriggerTask(() -> {

            if (LOG.isInfoEnabled()) {
                LOG.info("Starting scheduled Podcast refresh.");
            }
            mediaScannerService.scanLibrary();
            if (LOG.isInfoEnabled()) {
                LOG.info("Completed scheduled Podcast refresh.");
            }

        }, (context) -> {

            long daysBetween = settingsService.getIndexCreationInterval();
            if (daysBetween == -1 && LOG.isInfoEnabled()) {
                LOG.info("Automatic media scanning disabled.");
            }

            long periodMillis = TimeUnit.DAYS.toMillis(daysBetween);
            Optional<Date> lastCompletionTime = Optional.ofNullable(context.lastCompletionTime());
            Instant nextExecutionTime = lastCompletionTime.orElseGet(firstTime).toInstant().plusMillis(periodMillis);
            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Automatic media library scanning scheduled to run every {} day(s), starting at {}",
                        daysBetween, firstTime.get());
            }

            // In addition, create index immediately if it doesn't exist on disk.
            if (SettingsService.isScanOnBoot() && mediaScannerService.neverScanned()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Media library never scanned. Doing it now.");
                }
                mediaScannerService.scanLibrary();
            }

            return Date.from(nextExecutionTime);
        });
    }
}
