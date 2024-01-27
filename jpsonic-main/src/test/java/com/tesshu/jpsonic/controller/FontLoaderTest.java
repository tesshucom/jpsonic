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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Font;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import net.sf.ehcache.CacheManager;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class FontLoaderTest {

    private CacheManager manager;
    private FontLoader fontLoader;

    @BeforeEach
    public void setup() throws URISyntaxException {
        Path path = Path.of(FontLoaderTest.class.getResource("/ehcache.xml").toURI());
        manager = CacheManager.newInstance(path.toString());
        fontLoader = new FontLoader(manager.getCache("fontCache"));
    }

    @AfterEach
    public void tearDown() {
        manager.shutdown();
        System.clearProperty("jpsonic.embeddedfont");
    }

    @Test
    void testGetDefaultFont() throws Exception {
        assertNotNull(fontLoader.getFont(10));
    }

    @Test
    void testGetDefaultFontFromCache() throws Exception {
        assertNotNull(fontLoader.getFont(10));
        assertNotNull(fontLoader.getFont(10));
    }

    @Test
    void testGetEmbed() throws Exception {
        System.setProperty("jpsonic.embeddedfont", "true");
        assertNotNull(fontLoader.getFont(10));
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testLock() throws Exception {

        int threadsCount = 1_000;

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(true); // To handle Stream
        executor.setAwaitTerminationMillis(1_000);
        executor.setQueueCapacity(threadsCount);
        executor.setCorePoolSize(threadsCount);
        executor.setMaxPoolSize(threadsCount);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setDaemon(true);
        executor.initialize();

        MetricRegistry metrics = new MetricRegistry();
        List<Future<Font>> futures = new ArrayList<>();
        Timer globalTimer = metrics.timer(MetricRegistry.name(FontLoaderTest.class, "SpeedOfFontRetrieval"));

        for (int i = 0; i < threadsCount; i++) {
            futures.add(executor.submit(() -> {
                Font font;
                try (Timer.Context globalTimerContext = globalTimer.time()) {
                    font = fontLoader.getFont(10);
                } catch (IllegalArgumentException e) {
                    throw new UncheckedException(e);
                }
                return font;
            }));
        }

        assertEquals(threadsCount, futures.stream().mapToInt(future -> {
            try {
                assertNotNull(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new UncheckedException(e);
            }
            return 1;
        }).sum());
        executor.shutdown();

        ConsoleReporter.Builder builder = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS);
        try (ConsoleReporter reporter = builder.build()) {
            // to be none
        }
    }
}
