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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpRequest.Method;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete class for stream server using vanilla HttpServer.
 * 
 * Jpsonic supports both tomcat and jetty.
 * This class doesn't depend on any of those server-libs.
 */
public final class StreamServerImpl implements StreamServer<DefaultStreamServerConf> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamServerImpl.class);
    public static final Object SERVER_LOCK = new Object();

    private final DefaultStreamServerConf conf;
    private HttpServer server;

    public StreamServerImpl(DefaultStreamServerConf conf) {
        this.conf = conf;
    }

    @Override
    public void init(InetAddress address, Router router) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, conf.getListenPort());
        synchronized (SERVER_LOCK) {
            try {
                server = HttpServer.create(socketAddress, conf.tcpConnectionBacklog());
                server.createContext("/", new RequestHttpHandler(router, conf.serverClientTokens()));
                server.setExecutor(router.getConfiguration().getStreamServerExecutorService());
            } catch (IOException e) {
                throw new InitializationException("Could not initialize UPnP server.", e);
            }
        }
    }

    @Override
    public int getPort() {
        synchronized (SERVER_LOCK) {
            return server.getAddress().getPort();
        }
    }

    @Override
    public DefaultStreamServerConf getConfiguration() {
        return conf;
    }

    @Override
    public void run() {
        synchronized (SERVER_LOCK) {
            server.start();
        }
    }

    @Override
    public void stop() {
        synchronized (SERVER_LOCK) {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private static void traceIfEnabled(String msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(msg);
        }
    }

    private record RequestInfo(Method method, URI requestURI, InetSocketAddress remoteAddress) {
    }

    private static RequestInfo parse(HttpExchange exchange) {
        return new RequestInfo(UpnpRequest.Method.valueOf(exchange.getRequestMethod()),
                exchange.getRequestURI(), exchange.getRemoteAddress());
    }

    private static boolean isAcceptable(RequestInfo requestInfo) {
        return switch (requestInfo.method) {
            case GET, POST -> true;
            case MSEARCH, NOTIFY, SUBSCRIBE, UNKNOWN, UNSUBSCRIBE -> false;
        };
    }

    private static class RequestHttpHandler implements HttpHandler {

        private final Router router;
        private final ServerClientTokens tokens;

        public RequestHttpHandler(Router router, ServerClientTokens tokens) {
            this.router = router;
            this.tokens = tokens;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            RequestInfo requestInfo = parse(exchange);
            if (isAcceptable(requestInfo)) {
                traceIfEnabled("Received: %s".formatted(requestInfo));
            } else {
                traceIfEnabled("Rejected: %s".formatted(requestInfo));
                exchange.close();
                if (requestInfo.method == UpnpRequest.Method.UNKNOWN && LOG.isWarnEnabled()) {
                    LOG.warn("Method not supported: %s".formatted(UpnpRequest.Method.UNKNOWN));
                }
                return;
            }
            router.received(new UpnpStreamImpl(router.getProtocolFactory(), exchange, tokens));
        }
    }
}
