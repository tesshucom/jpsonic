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
import java.util.Date;
import java.util.Optional;
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
public class PodcastScheduleConfiguration implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(PodcastScheduleConfiguration.class);

    private final TaskScheduler taskScheduler;
    private final SettingsService settingsService;
    private final PodcastService podcastService;

    /*
     * The first execution is 5 minutes after the server starts.
     */
    private final Supplier<Date> firstTime = () -> new Date(System.currentTimeMillis() + 5L * 60L * 1000L);

    public PodcastScheduleConfiguration(TaskScheduler taskScheduler, SettingsService settingsService,
            PodcastService podcastService) {
        super();
        this.taskScheduler = taskScheduler;
        this.settingsService = settingsService;
        this.podcastService = podcastService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler);

        scheduledTaskRegistrar.addTriggerTask(() -> {

            if (LOG.isInfoEnabled()) {
                LOG.info("Starting scheduled Podcast refresh.");
            }
            podcastService.refreshAllChannels(true);
            if (LOG.isInfoEnabled()) {
                LOG.info("Completed scheduled Podcast refresh.");
            }

        }, (context) -> {

            int hoursBetween = settingsService.getPodcastUpdateInterval();
            if (hoursBetween == -1) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Automatic Podcast update disabled.");
                }
                return null;
            }

            long periodMillis = hoursBetween * 60L * 60L * 1000L;
            Optional<Date> lastCompletionTime = Optional.ofNullable(context.lastCompletionTime());
            Instant nextExecutionTime = lastCompletionTime.orElseGet(firstTime).toInstant().plusMillis(periodMillis);
            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Automatic Podcast update scheduled to run every " + hoursBetween + " hour(s), starting at "
                        + firstTime.get());
            }

            return Date.from(nextExecutionTime);
        });
    }
}
