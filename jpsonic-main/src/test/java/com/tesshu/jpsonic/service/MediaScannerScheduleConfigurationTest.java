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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.TestCaseUtils;
import org.junit.AfterClass;
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

class MediaScannerScheduleConfigurationTest {

    private SettingsService settingsService;
    private MediaScannerService mediaScannerService;
    private MediaScannerScheduleConfiguration configuration;

    private LocalDateTime now;

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        mediaScannerService = mock(MediaScannerService.class);
        configuration = new MediaScannerScheduleConfiguration(mock(TaskScheduler.class), settingsService,
                mediaScannerService);
        now = LocalDateTime.now();
        TestCaseUtils.setLogLevel(MediaScannerScheduleConfiguration.class, Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        TestCaseUtils.setLogLevel(MediaScannerScheduleConfiguration.class, Level.WARN);
    }

    @AfterClass
    public static void afterClass() throws URISyntaxException {
        System.setProperty("jpsonic.scan.onboot", "false");
    }

    @Test
    void testCreateFirstTime() {
        LocalDateTime firstTime = configuration.createFirstTime().atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(now.plus(1, ChronoUnit.DAYS).getDayOfMonth(), firstTime.getDayOfMonth());
        assertEquals(0, firstTime.getHour());
        assertEquals(0, firstTime.getMinute());

        // Date verify is simplified (coverage may not be available depending on system time)
        int hour = 23;
        Mockito.when(settingsService.getIndexCreationHour()).thenReturn(hour);
        firstTime = configuration.createFirstTime().atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(
                now.plus(now.compareTo(now.withHour(hour).withMinute(0).withSecond(0)) > 0 ? 1 : 0, ChronoUnit.DAYS)
                        .getDayOfMonth(),
                firstTime.get(ChronoField.DAY_OF_MONTH));
        assertEquals(11, firstTime.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(23, firstTime.get(ChronoField.HOUR_OF_DAY));
        assertEquals(0, firstTime.get(ChronoField.MINUTE_OF_HOUR));
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
            Mockito.verify(mediaScannerService, Mockito.times(1)).scanLibrary();
            Mockito.clearInvocations(mediaScannerService);

            Trigger trigger = task.getTrigger();
            TriggerContext triggerContext = mock(TriggerContext.class);

            // Operation check at the first startup
            Date firstTime = trigger.nextExecutionTime(triggerContext);
            LocalDateTime firstDateTime = Instant.ofEpochMilli(firstTime.getTime()).atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            assertEquals(now.plus(1, ChronoUnit.DAYS).getDayOfMonth(), firstDateTime.getDayOfMonth());
            assertEquals(0, firstDateTime.getHour());
            assertEquals(0, firstDateTime.getMinute());

            int hour = 23;
            Mockito.when(settingsService.getIndexCreationHour()).thenReturn(hour);
            firstTime = trigger.nextExecutionTime(triggerContext);
            firstDateTime = Instant.ofEpochMilli(firstTime.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            assertEquals(
                    now.plus(now.compareTo(now.withHour(hour).withMinute(0).withSecond(0)) > 0 ? 1 : 0, ChronoUnit.DAYS)
                            .getDayOfMonth(),
                    firstDateTime.getDayOfMonth());
            assertEquals(hour, firstDateTime.getHour());
            assertEquals(0, firstDateTime.getMinute());

            // Operation check at the second and subsequent startups
            Mockito.when(triggerContext.lastCompletionTime()).thenReturn(firstTime);
            LocalDateTime secondDateTime = Instant.ofEpochMilli(trigger.nextExecutionTime(triggerContext).getTime())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

            assertEquals(hour, secondDateTime.getHour());
            assertEquals(0, secondDateTime.getMinute());

            // Whether the date is one day ahead
            assertEquals(firstDateTime.plus(1, ChronoUnit.DAYS).getDayOfMonth(), secondDateTime.getDayOfMonth());
            Mockito.verify(mediaScannerService, Mockito.never()).scanLibrary();
        }

        /**
         * Unlike legacy servers, Jpsonic has an optional boot scan. The default is false because there are cases where
         * problems occur when starting Docker.
         */
        @Test
        void testScanOnBoot() {
            System.setProperty("jpsonic.scan.onboot", "true");
            ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
            configuration.configureTasks(registrar);
            Trigger trigger = registrar.getTriggerTaskList().get(0).getTrigger();

            TriggerContext triggerContext = mock(TriggerContext.class);
            Mockito.when(mediaScannerService.neverScanned()).thenReturn(false);
            trigger.nextExecutionTime(triggerContext);
            Mockito.verify(mediaScannerService, Mockito.never()).scanLibrary();

            Mockito.when(mediaScannerService.neverScanned()).thenReturn(true);
            trigger.nextExecutionTime(triggerContext);
            Mockito.verify(mediaScannerService, Mockito.times(1)).scanLibrary();
        }
    }
}
