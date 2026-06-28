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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit.MAX_128;
import static com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit.MAX_96;
import static com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit.OFF;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class TranscodingServiceTranscodeSchemeTest {

    private final TranscodingService service = new TranscodingService(null, null, null, null, null,
            null);

    @Test
    void testAirsonicEsqueStrictest() {
        assertSame(OFF, service.airsonicEsqueStrictest(OFF, null));
        assertSame(OFF, service.airsonicEsqueStrictest(OFF, OFF));
        assertSame(MAX_96, service.airsonicEsqueStrictest(OFF, MAX_96));
        assertSame(MAX_96, service.airsonicEsqueStrictest(MAX_96, null));
        assertSame(MAX_96, service.airsonicEsqueStrictest(MAX_96, OFF));
        assertSame(MAX_96, service.airsonicEsqueStrictest(MAX_96, MAX_128));
        assertSame(MAX_96, service.airsonicEsqueStrictest(MAX_128, MAX_96));
    }
}
