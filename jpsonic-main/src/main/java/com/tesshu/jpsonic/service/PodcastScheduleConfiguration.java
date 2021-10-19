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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
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

    public PodcastScheduleConfiguration(TaskScheduler taskScheduler, SettingsService settingsService,
            PodcastService podcastService) {
        super();
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.podcastService = podcastService;
    }

    /*
     * The first execution is 5 minutes after the server starts.
     */
    final Date createFirstTime() {
        return Date.from(Instant.now().plus(5L, ChronoUnit.MINUTES));
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler);
        scheduledTaskRegistrar.addTriggerTask(() -> podcastService.refreshAllChannels(true), (context) -> {

            int hoursBetween = settingsService.getPodcastUpdateInterval();
            if (hoursBetween == -1) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Automatic Podcast update disabled.");
                }
                return null;
            }

            Date lastTime = context.lastCompletionTime();
            Date nextTime = lastTime == null ? createFirstTime()
                    : Date.from(lastTime.toInstant().plus(hoursBetween, ChronoUnit.HOURS));

            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Auto Podcast update every {} hours was scheduled. (Next {})", +hoursBetween,
                        new SimpleDateFormat("yyyy/MM/dd HH:mm", settingsService.getLocale()).format(nextTime));
            }
            return nextTime;
        });
    }
}
