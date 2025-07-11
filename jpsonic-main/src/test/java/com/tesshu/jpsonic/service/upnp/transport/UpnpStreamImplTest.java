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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.transport;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertArrayEquals; // NOPMD pmd/pmd#4432

import java.nio.charset.StandardCharsets; // NOPMD pmd/pmd#4432

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolFactory;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class UpnpStreamImplTest {

    private UpnpStreamImpl upnpStream;

    @BeforeEach
    public void setup() {
        upnpStream = new UpnpStreamImpl(mock(ProtocolFactory.class), mock(HttpExchange.class),
                mock(ServerClientTokens.class));
    }

    @Nested
    class GetBodyBytesTest {

        @Test
        void testNobody() {
            StreamResponseMessage responseMessage = mock(StreamResponseMessage.class);
            assertArrayEquals(new byte[0], upnpStream.getBodyBytesOf(responseMessage));
        }

        @Test
        void testString() {
            StreamResponseMessage responseMessage = new StreamResponseMessage("ACE");
            assertArrayEquals("ACE".getBytes(StandardCharsets.UTF_8),
                    upnpStream.getBodyBytesOf(responseMessage));
            responseMessage = new StreamResponseMessage("ÂĈÊ");
            assertArrayEquals("ÂĈÊ".getBytes(StandardCharsets.UTF_8),
                    upnpStream.getBodyBytesOf(responseMessage));
        }

        @Test
        void testOyherThanString() {
            byte[] expected = { (byte) 1, (byte) 2, (byte) 3 };
            StreamResponseMessage responseMessage = new StreamResponseMessage(expected);
            assertArrayEquals(expected, upnpStream.getBodyBytesOf(responseMessage));
        }
    }

}
