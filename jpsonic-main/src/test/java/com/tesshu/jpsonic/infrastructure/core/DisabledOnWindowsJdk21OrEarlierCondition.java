package com.tesshu.jpsonic.infrastructure.core;

import java.util.Locale;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnWindowsJdk21OrEarlierCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
        .enabled("Supported platform");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        boolean windows = System
            .getProperty("os.name")
            .toLowerCase(Locale.ROOT)
            .contains("windows");

        int javaVersion = Runtime.version().feature();

        if (windows && javaVersion <= 21) {
            return ConditionEvaluationResult
                .disabled("Disabled on Windows with JDK 21 or earlier due to flaky behavior");
        }

        return ENABLED;
    }
}
