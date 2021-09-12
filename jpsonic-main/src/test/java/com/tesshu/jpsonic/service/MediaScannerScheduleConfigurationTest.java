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

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

class MediaScannerScheduleConfigurationTest {

    private MediaScannerScheduleConfiguration configuration;

    @BeforeEach
    public void setup() throws URISyntaxException {
        configuration = new MediaScannerScheduleConfiguration(mock(TaskScheduler.class), mock(SettingsService.class),
                mock(MediaScannerService.class));
    }

    @Test
    void testConfigureTasks() {
        ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
        assertNull(scheduledTaskRegistrar.getScheduler());
        configuration.configureTasks(scheduledTaskRegistrar);
        assertNotNull(scheduledTaskRegistrar.getScheduler());
    }
}
