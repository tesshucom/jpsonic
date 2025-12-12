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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CacheDisposerTest {

    private EhcacheConfiguration.CacheDisposer disposer;

    @BeforeEach
    void setUp() {
        disposer = new EhcacheConfiguration.CacheDisposer();
    }

    /**
     * Decision Table annotations for CacheDisposer paths.
     */
    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE })
    @interface CacheDisposerDecision {

        @interface Conditions {
            @interface Running {
                @interface True {
                }

                @interface False {
                }
            }
        }

        @interface Result {
            @interface CallbackRun {
            }
        }
    }

    @Nested
    @CacheDisposerDecision.Conditions.Running.False
    @CacheDisposerDecision.Result.CallbackRun
    class StopImmediate {

        @Test
        void testStopWhenNotRunning() {
            assertFalse(disposer.isRunning());

            disposer.stop();

            assertFalse(disposer.isRunning());
        }
    }

    @Nested
    @CacheDisposerDecision.Conditions.Running.True
    @CacheDisposerDecision.Result.CallbackRun
    class StopAfterStart {

        @Test
        void testStopAfterStart() {
            disposer.start();
            assertTrue(disposer.isRunning());

            disposer.stop();

            assertFalse(disposer.isRunning());
        }
    }

    @Nested
    @CacheDisposerDecision.Conditions.Running.False
    @CacheDisposerDecision.Result.CallbackRun
    class StopWithCallback {

        @Test
        void testStopWithCallback() {
            Runnable callback = () -> {
                /* callback executed */ };
            disposer.stop(callback);

            assertFalse(disposer.isRunning());
        }
    }

    @Nested
    class StartTest {

        @Test
        void testStart() {
            disposer.start();
            assertTrue(disposer.isRunning());
        }
    }

    @Nested
    class IsRunningTest {

        @Test
        void testIsRunning() {
            assertFalse(disposer.isRunning());
            disposer.start();
            assertTrue(disposer.isRunning());
            disposer.stop();
            assertFalse(disposer.isRunning());
        }
    }

    @Nested
    class GetPhaseTest {

        @Test
        void testGetPhase() {
            assertEquals(LifecyclePhase.CACHE.value, disposer.getPhase());
        }
    }
}