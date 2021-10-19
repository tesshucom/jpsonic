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
import java.util.Calendar;
import java.util.Date;

import org.junit.AfterClass;
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

    @BeforeEach
    public void setup() throws URISyntaxException {
        settingsService = mock(SettingsService.class);
        mediaScannerService = mock(MediaScannerService.class);
        configuration = new MediaScannerScheduleConfiguration(mock(TaskScheduler.class), settingsService,
                mediaScannerService);
    }

    @AfterClass
    public static void afterClass() throws URISyntaxException {
        System.setProperty("jpsonic.scan.onboot", "false");
    }

    @Test
    void testCreateFirstTime() {
        Date firstTime = configuration.createFirstTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(firstTime);
        assertEquals(0, cal.get(Calendar.HOUR));
        assertEquals(0, cal.get(Calendar.MINUTE));

        Mockito.when(settingsService.getIndexCreationHour()).thenReturn(23);
        firstTime = configuration.createFirstTime();
        cal.setTime(firstTime);
        assertEquals(11, cal.get(Calendar.HOUR));
        assertEquals(0, cal.get(Calendar.MINUTE));
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
            Mockito.when(settingsService.isVerboseLogStart()).thenReturn(true);

            // Operation check at the first startup
            Date executionTime = trigger.nextExecutionTime(triggerContext);
            Calendar cal = Calendar.getInstance();
            cal.setTime(executionTime);
            assertEquals(0, cal.get(Calendar.HOUR));
            assertEquals(0, cal.get(Calendar.MINUTE));

            Mockito.when(settingsService.getIndexCreationHour()).thenReturn(15);
            executionTime = trigger.nextExecutionTime(triggerContext);
            cal.setTime(executionTime);
            assertEquals(3, cal.get(Calendar.HOUR));
            assertEquals(0, cal.get(Calendar.MINUTE));

            // Operation check at the second and subsequent startups
            Mockito.when(triggerContext.lastCompletionTime()).thenReturn(executionTime);
            cal.setTime(trigger.nextExecutionTime(triggerContext));
            assertEquals(3, cal.get(Calendar.HOUR));
            assertEquals(0, cal.get(Calendar.MINUTE));

            cal.setTime(trigger.nextExecutionTime(triggerContext));
            assertEquals(3, cal.get(Calendar.HOUR));
            assertEquals(0, cal.get(Calendar.MINUTE));

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
