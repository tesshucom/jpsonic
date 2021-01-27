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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import org.airsonic.player.service.ApacheCommonsConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by remi on 17/01/17.
 */
@Service
public class MetricsManager {

    @Autowired
    private ApacheCommonsConfigurationService configurationService;

    // Main metrics registry
    private static final MetricRegistry METRICS = new MetricRegistry();

    private static AtomicBoolean metricsActivatedByConfiguration = new AtomicBoolean(false);
    private static Object lock = new Object();

    // Potential metrics reporters
    private static JmxReporter reporter;

    private void configureMetricsActivation() {
        if (configurationService.containsKey("Metrics")) {
            metricsActivatedByConfiguration.set(true);

            // Start a Metrics JMX reporter
            reporter = JmxReporter.forRegistry(METRICS).convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS).build();
            reporter.start();
        } else {
            metricsActivatedByConfiguration.set(false);
        }
    }

    private boolean isMetricsActivatedByConfiguration() {
        if (!metricsActivatedByConfiguration.get()) {
            synchronized (lock) {
                if (!metricsActivatedByConfiguration.get()) {
                    configureMetricsActivation();
                }
            }
        }
        return metricsActivatedByConfiguration.get();
    }

    /**
     * Creates a {@link Timer} whose name is based on a class name and a qualified name.
     */
    public Timer timer(Class<?> clazz, String name) {
        if (isMetricsActivatedByConfiguration()) {
            return new TimerBuilder().timer(clazz, name);
        } else {
            return NULL_TIMER_SINGLETON;
        }
    }

    /**
     * Creates a {@link Timer} whose name is based on an object's class name and a qualified name.
     */
    public Timer timer(Object ref, String name) {
        return timer(ref.getClass(), name);
    }

    /**
     * Initiate a {@link TimerBuilder} using a condition. If the condition is false, a void {@link Timer} will finally
     * be built thus no timer will be registered in the Metrics registry.
     */
    public TimerBuilder condition(boolean ifTrue) {
        if (isMetricsActivatedByConfiguration()) {
            if (!ifTrue) {
                return CONDITION_FALSE_TIMER_BUILDER_SINGLETON;
            }
            return new TimerBuilder();
        } else {
            return NULL_TIMER_BUILDER_SINGLETON;
        }
    }

    public void setConfigurationService(ApacheCommonsConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * A class that builds a {@link Timer}
     */
    public static class TimerBuilder {

        public Timer timer(Class<?> clazz, String name) {
            com.codahale.metrics.Timer t = METRICS.timer(MetricRegistry.name(clazz, name));
            com.codahale.metrics.Timer.Context tContext = t.time();
            return new Timer(tContext);
        }

        public Timer timer(Object ref, String name) {
            return timer(ref.getClass(), name);
        }

    }

    /**
     * A class that holds a Metrics timer context implementing {@link AutoCloseable} thus it can be used in a
     * try-with-resources statement.
     */
    public static class Timer implements AutoCloseable {

        private com.codahale.metrics.Timer.Context timerContext;

        protected Timer(com.codahale.metrics.Timer.Context timerContext) {
            this.timerContext = timerContext;
        }

        @Override
        public void close() {
            timerContext.stop();
        }

    }

    // -----------------------------------------------------------------
    // Convenient singletons to avoid creating useless objects instances
    // -----------------------------------------------------------------
    private static final NullTimer NULL_TIMER_SINGLETON = new NullTimer(null);
    private static final NullTimerBuilder CONDITION_FALSE_TIMER_BUILDER_SINGLETON = new NullTimerBuilder();
    private static final NullTimerBuilder NULL_TIMER_BUILDER_SINGLETON = new NullTimerBuilder();

    private static class NullTimer extends Timer {

        protected NullTimer(com.codahale.metrics.Timer.Context timerContext) {
            super(timerContext);
        }

        @Override
        public void close() {
            // Does nothing
        }
    }

    private static class NullTimerBuilder extends TimerBuilder {
        @Override
        public Timer timer(Class<?> clazz, String name) {
            return NULL_TIMER_SINGLETON;
        }
    }

}
