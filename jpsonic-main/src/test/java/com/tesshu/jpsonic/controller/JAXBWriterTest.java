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

package com.tesshu.jpsonic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.tesshu.jpsonic.util.PlayerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class JAXBWriterTest {

    private JAXBWriter writer;

    @BeforeEach
    public void setup() throws ExecutionException {
        writer = new JAXBWriter();
    }

    @Test
    @WithMockUser(username = "admin")
    void testConvertDate() throws Exception {

        Instant dateInDB = ZonedDateTime.of(2001, 12, 31, 23, 59, 59, 999, ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MILLIS).toInstant();
        XMLGregorianCalendar converted = writer.convertDate(dateInDB);

        assertEquals(dateInDB.toEpochMilli(), converted.toGregorianCalendar().getTimeInMillis());

        // The results below will vary depending on your system's default timezone
        // (No normalization required. To ensure readability and allow client apps to decide how to display the date.)

        // Below is a sample for JST(+9)

        // assertEquals(converted.getYear(), 2002);
        // assertEquals(converted.getMonth(), 1);
        // assertEquals(converted.getDay(), 1);
        // assertEquals(converted.getHour(), 8);
        // assertEquals(converted.getMinute(), 59);
        // assertEquals(converted.getSecond(), 59);
        // assertEquals(converted.getMillisecond(), 0);
        // assertEquals("2001-12-31T23:59:59.000+09:00", converted.toXMLFormat());
        // assertEquals("2001-12-31T23:59:59.000+09:00", converted.toString());

        // With modern Jackson parsers, it doesn't really matter whether the intermediate format is normalized or not.
        XMLGregorianCalendar parsedLocal = PlayerUtils.OBJECT_MAPPER.convertValue(converted.toXMLFormat(),
                XMLGregorianCalendar.class);

        // The format is different. Simply because Jackson defaults to nanoseconds.
        assertNotEquals(dateInDB, parsedLocal); // 2001-12-31T23:59:59Z 2001-12-31T23:59:59.000Z

        // Most are treated in milliseconds, so they are effectively equivalent
        assertEquals(dateInDB.toEpochMilli(), parsedLocal.toGregorianCalendar().getTime().getTime());

        assertEquals("2001-12-31 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
                .format(parsedLocal.toGregorianCalendar().toInstant()));
        assertEquals("2002-01-01 08:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Japan")).format(parsedLocal.toGregorianCalendar().toInstant()));
    }
}
