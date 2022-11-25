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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class PodcastScheduleConfiguration implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(PodcastScheduleConfiguration.class);

    private final TaskScheduler taskScheduler;
    private final SettingsService settingsService;
    private final PodcastService podcastService;
    private final ScannerStateService scannerStateService;

    public PodcastScheduleConfiguration(TaskScheduler taskScheduler, SettingsService settingsService,
            PodcastService podcastService, ScannerStateService scannerStateService) {
        super();
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.podcastService = podcastService;
        this.scannerStateService = scannerStateService;
    }

    /*
     * The first execution is 5 minutes after the server starts.
     */
    static final Instant createFirstTime() {
        return now().plus(5, ChronoUnit.MINUTES);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {

        scheduledTaskRegistrar.setScheduler(taskScheduler);

        scheduledTaskRegistrar.addTriggerTask(() -> {
            if (scannerStateService.isScanning()) {
                LOG.info("Is scanning. Automatic podcast updates will not be performed.");
            } else {
                LOG.info("Auto Podcast update will be performed.");
                podcastService.refreshAllChannels(true);
            }
        }, new PodcastUpdateTrigger(settingsService, scannerStateService));
    }

    static class PodcastUpdateTrigger implements Trigger {

        private final SettingsService settingsService;
        private final ScannerStateService scannerStateService;

        public PodcastUpdateTrigger(SettingsService settingsService, ScannerStateService scannerStateService) {
            super();
            this.settingsService = settingsService;
            this.scannerStateService = scannerStateService;
        }

        @Override
        public java.util.Date nextExecutionTime(TriggerContext triggerContext) {

            int hoursBetween = this.settingsService.getPodcastUpdateInterval();
            if (hoursBetween == -1) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Automatic Podcast update disabled.");
                }
                return null;
            }

            Instant lastTime = Optional.ofNullable(triggerContext.lastCompletionTime()).filter(Objects::nonNull)
                    .map(d -> d.toInstant()).orElse(null);
            boolean isReschedule = lastTime == null || this.scannerStateService.isScanning();
            Instant nextTime = isReschedule ? createFirstTime() : lastTime.plus(hoursBetween, ChronoUnit.HOURS);

            String nextTimeString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
                    .format(nextTime);

            String msg;
            if (this.scannerStateService.isScanning()) {
                msg = "Auto Podcast update has been rescheduled because being scanning. (Next {})";
            } else {
                msg = "Auto Podcast update every " + hoursBetween + " hours was scheduled. (Next {})";
            }

            if (this.settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info(msg, nextTimeString);
            }

            return java.util.Date.from(nextTime);
        }
    }
}
