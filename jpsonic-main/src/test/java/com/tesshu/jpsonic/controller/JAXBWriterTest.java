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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
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

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.PlayerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.subsonic.restapi.Response;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JAXBWriterTest {

    private SettingsService settingsService;
    private JAXBWriter writer;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        writer = new JAXBWriter(settingsService);
    }

    @WithMockUser(username = "admin")
    @Test
    void testConvertDate() throws Exception {

        Instant dateInDB = ZonedDateTime
            .of(2001, 12, 31, 23, 59, 59, 999, ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.MILLIS)
            .toInstant();
        XMLGregorianCalendar converted = writer.convertDate(dateInDB);

        assertEquals(dateInDB.toEpochMilli(), converted.toGregorianCalendar().getTimeInMillis());

        // The results below will vary depending on your system's default timezone
        // (No normalization required. To ensure readability and allow client apps to
        // decide how to display the date.)

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

        // With modern Jackson parsers, it doesn't really matter whether the
        // intermediate format is normalized or not.
        XMLGregorianCalendar parsedLocal = PlayerUtils.OBJECT_MAPPER
            .convertValue(converted.toXMLFormat(), XMLGregorianCalendar.class);

        // The format is different. Simply because Jackson defaults to nanoseconds.
        assertNotEquals(dateInDB, parsedLocal); // 2001-12-31T23:59:59Z 2001-12-31T23:59:59.000Z

        // Most are treated in milliseconds, so they are effectively equivalent
        assertEquals(dateInDB.toEpochMilli(),
                parsedLocal.toGregorianCalendar().getTime().getTime());

        assertEquals("2001-12-31 23:59:59",
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneOffset.UTC)
                    .format(parsedLocal.toGregorianCalendar().toInstant()));
        assertEquals("2002-01-01 08:59:59",
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Japan"))
                    .format(parsedLocal.toGregorianCalendar().toInstant()));
    }

    @Nested
    class WriteResponseTest {

        @Test
        void testContentTypeWithXml() {
            HttpServletRequest request = mock(MockHttpServletRequest.class);
            HttpServletResponse httpResponse = new MockHttpServletResponse();
            Response response = writer.createResponse(true);

            // No format
            writer.writeResponse(request, httpResponse, response);
            assertEquals(httpResponse.getContentType(), "text/xml;charset=UTF-8");

            // format=xml
            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("xml");
            writer.writeResponse(request, httpResponse, response);
            assertEquals("text/xml;charset=UTF-8", httpResponse.getContentType());
        }

        @Test
        void testContentTypeWithJson() {
            HttpServletRequest request = mock(MockHttpServletRequest.class);
            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("json");
            HttpServletResponse httpResponse = new MockHttpServletResponse();
            Response response = writer.createResponse(true);
            writer.writeResponse(request, httpResponse, response);
            assertEquals("application/json;charset=UTF-8", httpResponse.getContentType());
        }

        @Test
        void testContentTypeWithJsonp() {
            Mockito.when(settingsService.isUseJsonp()).thenReturn(true);
            HttpServletRequest request = mock(MockHttpServletRequest.class);
            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("jsonp");
            Mockito
                .when(request.getParameter(Attributes.Request.CALLBACK.value()))
                .thenReturn("testJsonp");
            HttpServletResponse httpResponse = new MockHttpServletResponse();
            Response response = writer.createResponse(true);
            writer.writeResponse(request, httpResponse, response);
            assertEquals("text/javascript;charset=UTF-8", httpResponse.getContentType());
        }

        @Test
        void testContentTypeWithoutJsonp() {
            // Jsonp cannot be used unless all conditions are met
            HttpServletRequest request = mock(MockHttpServletRequest.class);
            HttpServletResponse httpResponse = new MockHttpServletResponse();
            Response response = writer.createResponse(true);

            writer = new JAXBWriter(null);
            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("jsonp");
            writer.writeResponse(request, httpResponse, response);
            assertEquals("text/xml;charset=UTF-8", httpResponse.getContentType());

            writer = new JAXBWriter(settingsService);
            Mockito.when(settingsService.isUseJsonp()).thenReturn(true);
            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("xml");
            writer.writeResponse(request, httpResponse, response);
            assertEquals("text/xml;charset=UTF-8", httpResponse.getContentType());

            Mockito.when(request.getParameter(Attributes.Request.F.value())).thenReturn("jsonp");
            writer.writeResponse(request, httpResponse, response);
            assertEquals("text/xml;charset=UTF-8", httpResponse.getContentType());
        }
    }
}
