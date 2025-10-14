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

package com.tesshu.jpsonic.service.upnp.transport;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tesshu.jpsonic.domain.Version;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.UpnpServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpRequest.Method;
import org.jupnp.transport.Router;
import org.jupnp.transport.RouterException;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@TestInstance(Lifecycle.PER_CLASS)
class RouterImplTest {

    private ExecutorService executor;
    private Router router;

    @BeforeAll
    void beforeAll() {
        executor = Executors.newSingleThreadExecutor();
    }

    @BeforeEach
    void setUp() throws RouterException {
        UpnpServiceConfiguration conf = new JpsonicUpnpServiceConf(executor, executor, executor,
                SettingsService.getBrand(), new Version("99"));
        UpnpServiceImpl upnpService = new UpnpServiceImpl(conf, "172.17.16.1");
        upnpService.startup();
        router = upnpService.getRouter();
    }

    @AfterAll
    void tearDown() {
        executor.shutdown();
    }

    private StreamRequestMessage createMessage(String uri) {
        StreamRequestMessage message = mock(StreamRequestMessage.class);
        when(message.getUri()).thenReturn(URI.create(uri));
        UpnpRequest operation = mock(UpnpRequest.class);
        when(message.getOperation()).thenReturn(operation);
        when(operation.getMethod()).thenReturn(Method.GET);
        when(operation.getURI()).thenReturn(URI.create(uri));
        UpnpHeaders upnpHeaders = new UpnpHeaders();
        when(message.getHeaders()).thenReturn(upnpHeaders);
        return message;
    }

    @Nested
    class SendTest {
        @Test
        void testSend() throws RouterException {
            // send (-> abort)
            assertNull(router.send(createMessage("http://www.google.com/webhp")));
        }

        @Test
        void testGateway() throws RouterException {
            // Not to be send
            assertNull(router.send(createMessage("http://172.17.16.1/webhp")));
        }

        @Test
        void testUnknownHost() throws RouterException {
            // send (-> abort)
            assertNull(router.send(createMessage("http://UnknownHost/webhp")));
        }
    }
}
