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
import java.util.concurrent.locks.ReentrantLock;

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

    private final DefaultStreamServerConf conf;
    private HttpServer server;
    public final ReentrantLock serverLock = new ReentrantLock();

    public StreamServerImpl(DefaultStreamServerConf conf) {
        this.conf = conf;
    }

    @Override
    public void init(InetAddress address, Router router) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, conf.getListenPort());
        serverLock.lock();
        try {
            server = HttpServer.create(socketAddress, conf.tcpConnectionBacklog());
            server.createContext("/", new RequestHttpHandler(router, conf.serverClientTokens()));
            server.setExecutor(router.getConfiguration().getStreamServerExecutorService());
        } catch (IOException e) {
            throw new InitializationException("Could not initialize UPnP server.", e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public DefaultStreamServerConf getConfiguration() {
        return conf;
    }

    @Override
    public void run() {
        serverLock.lock();
        try {
            server.start();
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    public void stop() {
        serverLock.lock();
        try {
            if (server != null) {
                server.stop(0);
            }
        } finally {
            serverLock.unlock();
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
            case GET, POST, SUBSCRIBE, UNSUBSCRIBE -> true;
            case MSEARCH, NOTIFY, UNKNOWN  -> false;
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
