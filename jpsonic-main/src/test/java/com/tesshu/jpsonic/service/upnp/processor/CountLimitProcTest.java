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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;

import org.junit.jupiter.api.Test;

class CountLimitProcTest {

    private final CountLimitProc proc = new CountLimitProc() {
    };

    @Documented
    private @interface ToCountDecisions {
        @interface Conditions {
            @interface RequestOffset {
                @interface GtServerSideMax {
                }

                @interface EqServerSideMax {
                }

                @interface LtServerSideMax {
                }
            }

            @interface RequestOffsetPlusRequestMax {
                @interface GtServerSideMax {
                }

                @interface EqServerSideMax {
                }

                @interface LtServerSideMax {
                }
            }
        }

        @interface Results {
            @interface Zero {
            }

            @interface RequestMax {
            }

            @interface ServerSideMaxMinusRequestOffset {
            }

        }
    }

    @ToCountDecisions.Conditions.RequestOffset.GtServerSideMax
    @ToCountDecisions.Results.Zero
    @Test
    void c1() {
        assertEquals(0, proc.toCount(20, 100, 10));
    }

    @ToCountDecisions.Conditions.RequestOffset.EqServerSideMax
    @ToCountDecisions.Results.Zero
    @Test
    void c2() {
        assertEquals(0, proc.toCount(10, 100, 10));
    }

    @ToCountDecisions.Conditions.RequestOffset.LtServerSideMax
    @ToCountDecisions.Conditions.RequestOffsetPlusRequestMax.GtServerSideMax
    @ToCountDecisions.Results.ServerSideMaxMinusRequestOffset
    @Test
    void c3() {
        assertEquals(10, proc.toCount(0, 100, 10));
        assertEquals(9, proc.toCount(1, 100, 10));
        assertEquals(9, proc.toCount(0, 100, 9));
    }

    @ToCountDecisions.Conditions.RequestOffset.LtServerSideMax
    @ToCountDecisions.Conditions.RequestOffsetPlusRequestMax.EqServerSideMax
    @ToCountDecisions.Results.RequestMax
    @Test
    void c4() {
        assertEquals(10, proc.toCount(0, 10, 10));
    }

    @ToCountDecisions.Conditions.RequestOffset.LtServerSideMax
    @ToCountDecisions.Conditions.RequestOffsetPlusRequestMax.LtServerSideMax
    @ToCountDecisions.Results.RequestMax
    @Test
    void c5() {
        assertEquals(9, proc.toCount(0, 9, 10));
        assertEquals(8, proc.toCount(1, 8, 10));
        assertEquals(8, proc.toCount(2, 8, 10));
    }
}
