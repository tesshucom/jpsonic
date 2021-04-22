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

package com.tesshu.jpsonic.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.airsonic.player.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
public class ShortThreadPoolConfigurationTest {

    @Autowired
    private ShortTaskPoolConfiguration configuration;

    @Test
    void testGetCorePoolSize() {
        assertEquals(4, configuration.getCorePoolSize());
    }

    @Test
    void testGetQueueCapacity() {
        assertEquals(20, configuration.getQueueCapacity());
    }

    @Test
    void testGetMaxPoolSize() {
        assertEquals(7, configuration.getMaxPoolSize());
    }
}
