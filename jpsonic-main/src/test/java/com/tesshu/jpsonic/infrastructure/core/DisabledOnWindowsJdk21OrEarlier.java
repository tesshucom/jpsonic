package com.tesshu.jpsonic.infrastructure.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnWindowsJdk21OrEarlierCondition.class)
public @interface DisabledOnWindowsJdk21OrEarlier {
}
