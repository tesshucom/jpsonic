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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.UncheckedException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage.BodyType;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpRequest.Method;

@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.JUnitTestsShouldIncludeAssert",
    "PMD.AvoidDuplicateLiterals", "PMD.AvoidCatchingGenericException"})
@TestInstance(Lifecycle.PER_CLASS)
class StreamClientImplTest {

    private ExecutorService executor;
    private StreamClientImpl streamClient;

    @BeforeAll
    void beforeAll() {
        executor = Executors.newSingleThreadExecutor();
    }

    @BeforeEach
    void setUp() {
        DefaultStreamClientConf cnf = new DefaultStreamClientConf(executor);
        streamClient = new StreamClientImpl(cnf);
    }

    @AfterAll
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void testStop() {
        streamClient.stop();
    }

    @Test
    void testGetConfiguration() {
        DefaultStreamClientConf cnf = streamClient.getConfiguration();
        assertEquals(executor, cnf.executorService());
        assertEquals(2, cnf.defaultMaxPerRoute());
        assertEquals(20, cnf.maxTotal());
        assertEquals(8094, cnf.bufferSize());
        assertEquals(10, cnf.socketTimeoutSeconds());
    }

    @Nested
    class CreateRequestTest {

        @Test
        void testGet() {
            StreamRequestMessage message = mock(StreamRequestMessage.class);
            UpnpRequest operation = mock(UpnpRequest.class);
            when(message.getOperation()).thenReturn(operation);
            when(operation.getMethod()).thenReturn(Method.GET);
            when(operation.getURI()).thenReturn(URI.create("https://github.com/jpsonic/jpsonic"));
            UpnpHeaders upnpHeaders = new UpnpHeaders();
            when(message.getHeaders()).thenReturn(upnpHeaders);
            
            HttpUriRequestBase request = streamClient.createRequest(message);
            
            assertEquals("GET", request.getMethod());
            assertEquals("HTTP/1.0", request.getVersion().toString());
        }

        @Test
        void testPost() {
            StreamRequestMessage message = mock(StreamRequestMessage.class);
            UpnpRequest operation = mock(UpnpRequest.class);
            when(operation.getMethod()).thenReturn(Method.POST);
            when(operation.getURI()).thenReturn(URI.create("https://github.com/jpsonic/jpsonic"));
            when(message.getOperation()).thenReturn(operation);
            UpnpHeaders upnpHeaders = new UpnpHeaders();
            when(message.getHeaders()).thenReturn(upnpHeaders);
            when(message.getBodyType()).thenReturn(BodyType.STRING);
            when(message.getBodyString()).thenReturn("test");
            
            HttpUriRequestBase request = streamClient.createRequest(message);
            
            assertEquals("POST", request.getMethod());
            assertEquals("HTTP/1.0", request.getVersion().toString());
        }

        @Test
        void testUnknown() {
            StreamRequestMessage message = mock(StreamRequestMessage.class);
            UpnpRequest operation = mock(UpnpRequest.class);
            when(operation.getMethod()).thenReturn(Method.MSEARCH);
            when(message.getOperation()).thenReturn(operation);

            assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> streamClient.createRequest(message));
        }
    }

    @Test
    void testCreateCallable() throws URISyntaxException  {
        StreamRequestMessage message = mock(StreamRequestMessage.class);
        HttpUriRequestBase request = mock(HttpUriRequestBase.class);
        when(request.getUri()).thenReturn(URI.create("https://github.com/jpsonic/jpsonic"));
        when(request.headerIterator()).thenReturn(Collections.emptyIterator());
        when(request.getMethod()).thenReturn("GET");

        Callable<StreamResponseMessage> callable = streamClient.createCallable(message, request);
        assertNotNull(callable);
        try {
            callable.call();
        } catch (Exception e) {
            fail();
            throw new UncheckedException(e);
        }
    }

    @Test
    void testAbort() throws URISyntaxException {
        HttpGet request = new HttpGet("https://github.com/jpsonic/jpsonic");
        assertFalse(request.isAborted());
        streamClient.abort(request);
        assertTrue(request.isAborted());
    }

    @Test
    void testLogExecutionExceptionThrowable() {
        assertTrue(streamClient.logExecutionException(new IllegalStateException("test")));
        assertTrue(streamClient.logExecutionException(new NoHttpResponseException("test")));
        assertFalse(streamClient.logExecutionException(new IllegalArgumentException("test")));
    }
}
