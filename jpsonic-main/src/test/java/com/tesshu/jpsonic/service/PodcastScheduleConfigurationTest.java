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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;

class PodcastScheduleConfigurationTest {

    private SettingsService settingsService;
    private PodcastService podcastService;
    private PodcastScheduleConfiguration configuration;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        podcastService = mock(PodcastService.class);
        configuration = new PodcastScheduleConfiguration(mock(TaskScheduler.class), settingsService, podcastService);
    }

    @Test
    void testCreateFirstTime() {
        LocalDateTime now = LocalDateTime.now();
        Date firstTime = configuration.createFirstTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(firstTime);
        assertEquals(now.getDayOfMonth(), cal.get(Calendar.DATE));
        assertEquals(now.getHour(), cal.get(Calendar.HOUR));
        assertEquals(now.plus(5, ChronoUnit.MINUTES).getMinute(), cal.get(Calendar.MINUTE));
    }

    @Nested
    class ConfigureTasksTest {

        @Test
        void testExecutionTime() {
            ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
            configuration.configureTasks(registrar);
            assertNotNull(registrar.getScheduler());
            assertEquals(0, registrar.getCronTaskList().size());
            assertEquals(0, registrar.getFixedDelayTaskList().size());
            assertEquals(0, registrar.getFixedRateTaskList().size());
            assertEquals(0, registrar.getScheduledTasks().size());
            assertEquals(1, registrar.getTriggerTaskList().size());

            TriggerTask task = registrar.getTriggerTaskList().get(0);
            assertNotNull(task.getRunnable());

            // Confirmation of scan startup
            task.getRunnable().run();
            Mockito.verify(podcastService, Mockito.times(1)).refreshAllChannels(Mockito.anyBoolean());
            Mockito.clearInvocations(podcastService);

            Trigger trigger = task.getTrigger();
            TriggerContext triggerContext = mock(TriggerContext.class);
            Mockito.when(settingsService.isVerboseLogStart()).thenReturn(true);

            // Do nothing
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(-1);
            assertNull(trigger.nextExecutionTime(triggerContext));

            int hourOfweek = 24 * 7;
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(hourOfweek);

            // Operation check at the first startup
            Date firstTime = trigger.nextExecutionTime(triggerContext);
            Calendar firstCal = Calendar.getInstance();
            firstCal.setTime(firstTime);
            LocalDateTime firstExpected = LocalDateTime.now().plus(5L, ChronoUnit.MINUTES);
            assertEquals(firstExpected.getDayOfMonth(), firstCal.get(Calendar.DATE));
            assertEquals(firstExpected.getHour(), firstCal.get(Calendar.HOUR));
            assertEquals(firstExpected.getMinute(), firstCal.get(Calendar.MINUTE));

            // Operation check at the second and subsequent startups
            Mockito.when(triggerContext.lastCompletionTime()).thenReturn(firstTime);
            Date secondTime = trigger.nextExecutionTime(triggerContext);
            Calendar secondCal = Calendar.getInstance();
            secondCal.setTime(secondTime);

            LocalDateTime secondExpected = firstExpected.plus(hourOfweek, ChronoUnit.HOURS);
            assertEquals(secondExpected.getDayOfMonth(), secondCal.get(Calendar.DATE));
            assertEquals(secondExpected.getHour(), secondCal.get(Calendar.HOUR));
            assertEquals(secondExpected.getMinute(), secondCal.get(Calendar.MINUTE));

            // Whether the date is one day ahead
            Mockito.verify(podcastService, Mockito.never()).refreshAllChannels(Mockito.anyBoolean());
        }

    }
}
