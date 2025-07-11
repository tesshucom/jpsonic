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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.TestCaseUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;

@SuppressWarnings("PMD.TooManyStaticImports")
class PodcastScheduleConfigurationTest {

    private SettingsService settingsService;
    private PodcastService podcastService;
    private PodcastScheduleConfiguration configuration;
    private ScannerStateService scannerStateService;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        podcastService = mock(PodcastService.class);
        scannerStateService = mock(ScannerStateService.class);
        configuration = new PodcastScheduleConfiguration(mock(TaskScheduler.class), settingsService,
                podcastService, scannerStateService);
        TestCaseUtils.setLogLevel(PodcastScheduleConfiguration.class, Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        TestCaseUtils.setLogLevel(PodcastScheduleConfiguration.class, Level.WARN);
    }

    @Test
    void testCreateFirstTime() {
        LocalDateTime firstTimeExpected = now()
            .plus(5, ChronoUnit.MINUTES)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        LocalDateTime firstTime = PodcastScheduleConfiguration
            .createFirstTime()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        assertEquals(firstTimeExpected.getDayOfMonth(), firstTime.getDayOfMonth());
        assertEquals(firstTimeExpected.getHour(), firstTime.getHour());
        assertEquals(firstTimeExpected.getMinute(), firstTime.getMinute());
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
            Mockito
                .verify(podcastService, Mockito.times(1))
                .refreshAllChannels(Mockito.anyBoolean());
            Mockito.clearInvocations(podcastService);

            Trigger trigger = task.getTrigger();
            TriggerContext triggerContext = mock(TriggerContext.class);

            // Do nothing
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(-1);
            assertNull(trigger.nextExecution(triggerContext));

            int hourOfweek = 24 * 7;
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(hourOfweek);

            // Operation check at the first startup
            LocalDateTime firstTimeExpected = LocalDateTime.now().plus(5, ChronoUnit.MINUTES);

            Instant firstTime = trigger.nextExecution(triggerContext);
            LocalDateTime firstDateTime = firstTime
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            assertEquals(firstTimeExpected.getDayOfMonth(), firstDateTime.getDayOfMonth());
            assertEquals(firstTimeExpected.getHour(), firstDateTime.getHour());
            assertEquals(firstTimeExpected.getMinute(), firstDateTime.getMinute());

            // Operation check at the second and subsequent startups
            Mockito.when(triggerContext.lastCompletion()).thenReturn(firstTime);
            LocalDateTime secondDateTime = Instant
                .ofEpochMilli(trigger.nextExecution(triggerContext).toEpochMilli())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            LocalDateTime secondExpected = firstTimeExpected.plus(hourOfweek, ChronoUnit.HOURS);
            assertEquals(secondExpected.getDayOfMonth(), secondDateTime.getDayOfMonth());
            assertEquals(secondExpected.getHour(), secondDateTime.getHour());
            assertEquals(secondExpected.getMinute(), secondDateTime.getMinute());

            Mockito
                .verify(podcastService, Mockito.never())
                .refreshAllChannels(Mockito.anyBoolean());
        }

        /*
         * If isScanning is true, refreshAllChannels will not run. Also
         * nextExecutionTime is recalculated using createFirstTime (ie after 5 minutes).
         */
        @Test
        void testExecutionTimeDuringScan() {

            Mockito.when(scannerStateService.isScanning()).thenReturn(true);

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

            Mockito
                .verify(podcastService, Mockito.never())
                .refreshAllChannels(Mockito.anyBoolean());

            Trigger trigger = task.getTrigger();
            TriggerContext triggerContext = mock(TriggerContext.class);

            // Do nothing
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(-1);
            assertNull(trigger.nextExecution(triggerContext));

            int hourOfweek = 24 * 7;
            Mockito.when(settingsService.getPodcastUpdateInterval()).thenReturn(hourOfweek);

            // Operation check at the first startup
            LocalDateTime firstTimeExpected = LocalDateTime.now().plus(5, ChronoUnit.MINUTES);

            Instant firstTime = trigger.nextExecution(triggerContext);
            LocalDateTime firstDateTime = firstTime
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            assertEquals(firstTimeExpected.getDayOfMonth(), firstDateTime.getDayOfMonth());
            assertEquals(firstTimeExpected.getHour(), firstDateTime.getHour());
            assertEquals(firstTimeExpected.getMinute(), firstDateTime.getMinute());

            // Operation check at the second and subsequent startups
            Mockito.when(triggerContext.lastCompletion()).thenReturn(firstTime);
            LocalDateTime secondDateTime = Instant
                .ofEpochMilli(trigger.nextExecution(triggerContext).toEpochMilli())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

            LocalDateTime secondExpected = LocalDateTime.now().plus(5, ChronoUnit.MINUTES);
            assertEquals(secondExpected.getDayOfMonth(), secondDateTime.getDayOfMonth());
            assertEquals(secondExpected.getHour(), secondDateTime.getHour());
            assertEquals(secondExpected.getMinute(), secondDateTime.getMinute());

            Mockito
                .verify(podcastService, Mockito.never())
                .refreshAllChannels(Mockito.anyBoolean());
        }

    }
}
