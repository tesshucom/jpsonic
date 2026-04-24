package com.tesshu.jpsonic.feature.auth.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Tests for RememberMeScheduleConfiguration. Focuses on the accuracy of the
 * 24-hour heartbeat and midnight anchoring.
 */
class RememberMeScheduleConfigurationTest {

    private RememberMeScheduleConfiguration config;

    @BeforeEach
    void setup() {
        RememberMeKeyManager keyManager = mock(RememberMeKeyManager.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        config = new RememberMeScheduleConfiguration(taskScheduler, keyManager);
    }

    /**
     * Verify that the configuration correctly registers the trigger task to the
     * Spring task registrar.
     */
    @Test
    void configureTasksRegistersTask() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        config.configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(1);
    }

    /**
     * Verify the "First Time" logic. On the first run, the trigger must anchor to
     * the next midnight (0:00).
     */
    @Test
    void triggerCalculatesNextMidnightOnFirstRun() {
        RememberMeScheduleConfiguration.DailyMidnightTrigger trigger = new RememberMeScheduleConfiguration.DailyMidnightTrigger();
        TriggerContext context = mock(TriggerContext.class);

        // Scenario: Initial run where no previous completion exists
        when(context.lastCompletion()).thenReturn(null);

        Instant next = trigger.nextExecution(context);

        // Expected: Tomorrow at 00:00:00.000
        Instant expected = LocalDate
            .now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();

        assertThat(next).isEqualTo(expected);
    }

    /**
     * Verify the "Cycle" logic. Subsequent runs must occur exactly 24 hours after
     * the last completion.
     */
    @Test
    void triggerCalculatesNextDayOnSubsequentRun() {
        RememberMeScheduleConfiguration.DailyMidnightTrigger trigger = new RememberMeScheduleConfiguration.DailyMidnightTrigger();
        TriggerContext context = mock(TriggerContext.class);

        // Scenario: Assume the last run finished at exactly today's midnight
        Instant lastMidnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        when(context.lastCompletion()).thenReturn(lastMidnight);

        Instant next = trigger.nextExecution(context);

        // Expected: 24 hours later
        assertThat(next).isEqualTo(lastMidnight.plus(24, ChronoUnit.HOURS));
    }
}
