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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.sun.net.httpserver.HttpExchange;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpMessage.BodyType;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.impl.HttpExchangeUpnpStream;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete class for HTTP request/response procedures.
 * Based on {@link HttpExchangeUpnpStream}.
 */
public final class UpnpStreamImpl extends UpnpStream {

    private static final Logger LOG = LoggerFactory.getLogger(UpnpStreamImpl.class);

    private final ServerClientTokens tokens;
    private final HttpExchange httpExchange;

    public UpnpStreamImpl(@NonNull ProtocolFactory protocolFactory,
            @NonNull HttpExchange httpExchange, @NonNull ServerClientTokens tokens) {
        super(protocolFactory);
        this.httpExchange = httpExchange;
        this.tokens = tokens;
    }

    private void traceIfEnabled(String msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(msg);
        }
    }

    StreamRequestMessage createRequestMessage() throws IOException {

        // Status
        StreamRequestMessage requestMessage = new StreamRequestMessage(
                UpnpRequest.Method.getByHttpName(httpExchange.getRequestMethod()),
                httpExchange.getRequestURI());

        // Protocol
        requestMessage.getOperation().setHttpMinorVersion(
                "HTTP/1.1".equals(httpExchange.getProtocol().toUpperCase(Locale.ROOT)) ? 1 : 0);

        // Connection wrapper
        requestMessage.setConnection(new UpnpStreamConnection(httpExchange));
        traceIfEnabled("Created new request message: %s".formatted(requestMessage));

        // Headers
        requestMessage.setHeaders(new UpnpHeaders(httpExchange.getRequestHeaders()));

        // Body
        byte[] bodyBytes;
        try (InputStream is = httpExchange.getRequestBody()) {
            bodyBytes = IO.readBytes(is);
        }
        traceIfEnabled("Reading request body bytes: %s".formatted(bodyBytes.length));

        if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {
            traceIfEnabled(
                    "Request contains textual entity body, converting then setting string on message");
            requestMessage.setBodyCharacters(bodyBytes);
        } else if (bodyBytes.length > 0) {
            traceIfEnabled("Request contains binary entity body, setting bytes on message");
            requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
        } else {
            traceIfEnabled("Request did not contain entity body");
        }
        return requestMessage;
    }

    /**
     * Don't use {@link StreamResponseMessage#getBodyBytes()}. Contains platform-dependent code. If
     * a JdK earlier than Java 18 is used, the platform default is not UTF-8, and the body uses
     * strings other than the platform's default encoding (typically... UTF-8), garbled characters
     * will appear.
     */
    @NonNull
    byte[] getBodyBytesOf(@NonNull StreamResponseMessage responseMessage) {
        if (responseMessage.hasBody()) {
            if (responseMessage.getBodyType().equals(BodyType.STRING)) {
                return responseMessage.getBodyString().getBytes(StandardCharsets.UTF_8);
            }
            return (byte[]) responseMessage.getBody();
        }
        return new byte[0];
    }

    private @Nullable StreamResponseMessage doProcess(StreamRequestMessage requestMessage)
            throws IOException {
        // Process it
        StreamResponseMessage responseMessage = process(requestMessage);

        // Return the response
        if (responseMessage == null) {
            traceIfEnabled(
                    "Sending HTTP response status: %s".formatted(HttpURLConnection.HTTP_NOT_FOUND));
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
        } else {
            // set our own server token
            responseMessage.getHeaders().set(UpnpHeader.Type.SERVER.getHttpName(),
                    tokens.getHttpToken());
            traceIfEnabled("Preparing HTTP response message: %s".formatted(responseMessage));

            // Headers
            httpExchange.getResponseHeaders().putAll(responseMessage.getHeaders());

            // Body
            byte[] responseBodyBytes = getBodyBytesOf(responseMessage);
            int contentLength = responseBodyBytes.length == 0 ? -1 : responseBodyBytes.length;
            traceIfEnabled("Sending HTTP response message: %s with content length: %s"
                    .formatted(responseMessage, contentLength));

            // StatusCode
            httpExchange.sendResponseHeaders(responseMessage.getOperation().getStatusCode(),
                    contentLength);

            if (contentLength > 0) {
                traceIfEnabled("Response message has body, writing bytes to stream...");
                try (OutputStream os = httpExchange.getResponseBody()) {
                    IO.writeBytes(os, responseBodyBytes);
                    os.flush();
                }
            }
        }
        return responseMessage;
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable") // Unchecked Error is rethrown
    @Override
    public void run() {
        try {

            traceIfEnabled("Processing HTTP request: %s %s"
                    .formatted(httpExchange.getRequestMethod(), httpExchange.getRequestURI()));

            StreamRequestMessage requestMessage = createRequestMessage();
            StreamResponseMessage responseMessage = doProcess(requestMessage);
            responseSent(responseMessage);

        } catch (Throwable t) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Exception occurred during UPnP stream processing.", t);
            }
            try {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Couldn't send error response.", e);
                }
            }
            ConcurrentUtils.handleCauseUnchecked(new ExecutionException(t));
            responseException(t);
        }
    }

    private static final class UpnpStreamConnection implements Connection {

        private final HttpExchange exchange;

        public UpnpStreamConnection(HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public InetAddress getRemoteAddress() {
            InetSocketAddress isa = exchange.getRemoteAddress();
            return isa != null ? isa.getAddress() : null;
        }

        @Override
        public InetAddress getLocalAddress() {
            InetSocketAddress la = exchange.getLocalAddress();
            return la != null ? la.getAddress() : null;
        }
    }
}
