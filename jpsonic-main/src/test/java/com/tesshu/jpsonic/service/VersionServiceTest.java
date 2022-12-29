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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionServiceTest {

    private VersionService versionService;

    @BeforeEach
    public void setup() {
        versionService = new VersionService();
    }

    @Test
    void testParseLocalBuildDate() {
        /*
         * Zoned is used for most of the date and time on web pages or outbounds ... upnp, podcast. BuildDate is also
         * used outside of the app (such as CI), so the text input value is used as is without region conversion.
         */
        LocalDate localDate = versionService.parseLocalBuildDate("20010203");
        assertEquals(2001, localDate.getYear());
        assertEquals(2, localDate.getMonthValue());
        assertEquals(3, localDate.getDayOfMonth());
    }
}
