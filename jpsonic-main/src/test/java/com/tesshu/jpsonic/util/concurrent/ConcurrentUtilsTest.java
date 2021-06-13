/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = "com.tesshu.jpsonic")
@ExtendWith(NeedsHome.class)
class ConcurrentUtilsTest {

    @Test
    void testHandleCauseUnchecked() {

        assertThrows(StackOverflowError.class,
                () -> ConcurrentUtils.handleCauseUnchecked(new ExecutionException(new StackOverflowError())));
        assertThrows(IllegalArgumentException.class,
                () -> ConcurrentUtils.handleCauseUnchecked(new ExecutionException(new IllegalArgumentException())));

        ConcurrentUtils.handleCauseUnchecked(new ExecutionException(new ClassNotFoundException()));

    }
}
