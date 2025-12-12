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

package com.tesshu.jpsonic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.spring.LifecyclePhase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyStaticImports")
class UPnPDisposerTest {

    @interface UPnPDisposerDecision {

        @interface Conditions {
            @interface IsRunning {
                @interface True {
                }

                @interface False {
                }
            }

            @interface CallbackRun {
                @interface True {
                }

                @interface False {
                }
            }

            @interface StopException {
                @interface Thrown {
                }

                @interface NotThrown {
                }
            }
        }

        @interface Result {
            @interface ServiceStopped {
                @interface True {
                }

                @interface False {
                }
            }

            @interface CallbackExecuted {
                @interface True {
                }

                @interface False {
                }
            }
        }
    }

    @Nested
    class StopMethod {

        @Test
        @UPnPDisposerDecision.Conditions.StopException.NotThrown
        @UPnPDisposerDecision.Result.ServiceStopped.True
        void c00() {
            UPnPService mockService = mock(UPnPService.class);
            UPnPDisposer disposer = new UPnPDisposer(mockService);

            disposer.stop();

            verify(mockService, times(1)).stop(); // verify stop is called once
        }

        @Test
        @UPnPDisposerDecision.Conditions.StopException.NotThrown
        @UPnPDisposerDecision.Conditions.CallbackRun.True
        @UPnPDisposerDecision.Result.ServiceStopped.True
        @UPnPDisposerDecision.Result.CallbackExecuted.True
        void c01() {
            UPnPService mockService = mock(UPnPService.class);
            Runnable mockCallback = mock(Runnable.class);
            UPnPDisposer disposer = new UPnPDisposer(mockService);

            disposer.stop(mockCallback);

            verify(mockService, times(1)).stop(); // verify stop is called once
            verify(mockCallback, times(1)).run(); // verify callback executed
        }
    }

    @Nested
    class IsRunningMethod {

        @Test
        @UPnPDisposerDecision.Conditions.IsRunning.True
        @UPnPDisposerDecision.Result.ServiceStopped.False
        void c02() {
            UPnPService mockService = mock(UPnPService.class);
            when(mockService.isRunning()).thenReturn(true);
            UPnPDisposer disposer = new UPnPDisposer(mockService);

            assertTrue(disposer.isRunning());
            verify(mockService, times(0)).stop(); // ensure stop was not called
        }

        @Test
        @UPnPDisposerDecision.Conditions.IsRunning.False
        @UPnPDisposerDecision.Result.ServiceStopped.False
        void c03() {
            UPnPService mockService = mock(UPnPService.class);
            when(mockService.isRunning()).thenReturn(false);
            UPnPDisposer disposer = new UPnPDisposer(mockService);

            assertFalse(disposer.isRunning());
            verify(mockService, times(0)).stop(); // ensure stop was not called
        }
    }

    @Test
    void testGetPhase() {
        UPnPService mockService = mock(UPnPService.class);
        UPnPDisposer disposer = new UPnPDisposer(mockService);

        assertEquals(LifecyclePhase.UPNP.getValue(), disposer.getPhase());
        verify(mockService, times(0)).stop(); // ensure stop was not called
    }
}