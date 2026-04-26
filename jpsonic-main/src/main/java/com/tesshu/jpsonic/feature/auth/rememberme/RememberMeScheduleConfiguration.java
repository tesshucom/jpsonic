package com.tesshu.jpsonic.feature.auth.rememberme;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Provides a 24-hour periodic pulse to the RememberMeKeyManager. The strict
 * rotation logic is encapsulated within the manager itself.
 */
@Configuration
@EnableScheduling
public class RememberMeScheduleConfiguration implements SchedulingConfigurer {

    private final TaskScheduler taskScheduler;
    private final RememberMeKeyManager rememberMeKeyManager;

    public RememberMeScheduleConfiguration(TaskScheduler taskScheduler,
            RememberMeKeyManager rememberMeKeyManager) {
        super();
        this.taskScheduler = taskScheduler;
        this.rememberMeKeyManager = rememberMeKeyManager;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskScheduler);

        // A simple "24-hour heartbeat" task
        scheduledTaskRegistrar
            .addTriggerTask(rememberMeKeyManager::rotateIfNecessary, new DailyMidnightTrigger());
    }

    /**
     * Anchors the pulse to the next 0:00 (midnight) and repeats every 24 hours.
     */
    static class DailyMidnightTrigger implements Trigger {

        @Override
        public Instant nextExecution(TriggerContext triggerContext) {
            Instant lastTime = triggerContext.lastCompletion();

            // If it's the first run, target the very next midnight.
            // Otherwise, target 24 hours after the last pulse.
            if (lastTime == null) {
                return LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            return lastTime.plus(24, ChronoUnit.HOURS);
        }

        // Backward compatibility for Spring versions using Date
        @Override
        public Date nextExecutionTime(TriggerContext triggerContext) {
            Instant next = nextExecution(triggerContext);
            return (next != null) ? Date.from(next) : null;
        }
    }
}
