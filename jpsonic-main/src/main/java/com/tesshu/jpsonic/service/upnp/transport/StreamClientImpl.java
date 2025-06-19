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

import static com.tesshu.jpsonic.util.StringUtil.ENCODING_UTF8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.jupnp.http.Headers;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache implementation of AbstractStreamClient.
 * 
 * Jpsonic supports both tomcat and jetty.
 * So it doesn't depend on any of those client-libs.
 */
public final class StreamClientImpl
        extends AbstractStreamClient<DefaultStreamClientConf, HttpUriRequestBase> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamClientImpl.class);
    private final DefaultStreamClientConf conf;
    private final PoolingHttpClientConnectionManager manager;
    private final CloseableHttpClient httpClient;

    public StreamClientImpl(DefaultStreamClientConf conf) {
        super();
        this.conf = conf;

        Http1Config customHttpConfig = Http1Config.custom()
                .setBufferSize(conf.bufferSize())
                .build();
        HttpConnectionFactory<ManagedHttpClientConnection> connectionFactory =
                ManagedHttpClientConnectionFactory.builder()
                .http1Config(customHttpConfig)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(conf.getTimeoutSeconds()))
                .setSocketTimeout(Timeout.ofSeconds(conf.socketTimeoutSeconds()))
                .build();

        manager = PoolingHttpClientConnectionManagerBuilder.create()
                .setConnectionFactory(connectionFactory)
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnPerRoute(conf.defaultMaxPerRoute())
                .setMaxConnTotal(conf.maxTotal())
                .build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(manager);
        if (conf.getRetryIterations() == 0) {
            httpClientBuilder
                .setRetryStrategy(new NoRetryStrategy(conf.getRetryAfterSeconds()));
        }
        httpClient = httpClientBuilder.build();
    }

    private void traceIfEnabled(String msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(msg);
        }
    }

    @Override
    public void stop() {
        traceIfEnabled("Shutting down HTTP client connection manager/pool");
        manager.close();
    }

    @Override
    public DefaultStreamClientConf getConfiguration() {
        return conf;
    }

    @SuppressWarnings("serial")
    private HttpGet createGet(UpnpRequest.Method method, URI uri) {
        return new HttpGet(uri) {
            @Override
            public String getMethod() {
                return method.getHttpName();
            }
        };
    }

    private HttpPost createPost(StreamRequestMessage message) {
        UpnpRequest operation = message.getOperation();
        @SuppressWarnings("serial")
        HttpPost post = new HttpPost(operation.getURI()) {
            @Override
            public String getMethod() {
                return operation.getMethod().getHttpName();
            }
        };
        try (HttpEntity entity = switch (message.getBodyType()) {
            case BYTES ->
                new ByteArrayEntity(message.getBodyBytes(), ContentType.APPLICATION_OCTET_STREAM);
            case STRING ->
                new StringEntity(message.getBodyString(), Charset.forName(ENCODING_UTF8));
        }) {
            post.setEntity(entity);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return post;
    }

    @Override
    protected HttpUriRequestBase createRequest(StreamRequestMessage message) {
        UpnpRequest operation = message.getOperation();
        HttpUriRequestBase request = switch (operation.getMethod()) {
            case GET, SUBSCRIBE, UNSUBSCRIBE ->
                    createGet(operation.getMethod(), operation.getURI());
            case POST, NOTIFY ->
                    createPost(message);
            case MSEARCH, UNKNOWN ->
                    throw new IllegalArgumentException(
                            "Unknown HTTP method: %s".formatted(operation.getHttpMethodName()));
        };
        if (!message.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
            request.setHeader("User-Agent", getConfiguration()
                    .getUserAgentValue(message.getUdaMajorVersion(), message.getUdaMinorVersion()));
        }
        if (message.getOperation().getHttpMinorVersion() == 0) {
            request.setVersion(HttpVersion.HTTP_1_0);
        } else {
            request.setVersion(HttpVersion.HTTP_1_1);
            request.addHeader("Connection", "close");
        }
        message.getHeaders().entrySet().forEach(entry -> entry.getValue()
                .forEach(value -> request.addHeader(entry.getKey(), value)));
        return request;
    }

    private HttpClientResponseHandler<StreamResponseMessage> createResponseHandler() {
        return (final ClassicHttpResponse httpResponse) -> {
            traceIfEnabled("Received HTTP response: %s %s"
                    .formatted(httpResponse.getCode(), httpResponse.getReasonPhrase()));

            // Status
            UpnpResponse responseOperation =
                    new UpnpResponse(httpResponse.getCode(), httpResponse.getReasonPhrase());

            // Message
            StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

            // Headers
            Headers headers = new Headers();
            Stream.of(httpResponse.getHeaders())
                    .forEach(header -> headers.add(header.getName(), header.getValue()));
            responseMessage.setHeaders(new UpnpHeaders(headers));

            // Body
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null || entity.getContentLength() == 0) {
                traceIfEnabled("HTTP response message has no entity");
                return responseMessage;
            }

            byte[] data = EntityUtils.toByteArray(entity);
            if (data != null) {
                if (responseMessage.isContentTypeMissingOrText()) {
                    traceIfEnabled("HTTP response message contains text entity");
                    responseMessage.setBodyCharacters(data);
                } else {
                    traceIfEnabled("HTTP response message contains binary entity");
                    responseMessage.setBody(UpnpMessage.BodyType.BYTES, data);
                }
            } else {
                traceIfEnabled("HTTP response message has no entity");
            }
            return responseMessage;
        };
    }

    @Override
    protected Callable<StreamResponseMessage> createCallable(StreamRequestMessage requestMessage,
            HttpUriRequestBase request) {
        traceIfEnabled("Sending HTTP request: %s".formatted(requestMessage));
        return () -> httpClient.execute(request, createResponseHandler());
    }

    @Override
    protected void abort(HttpUriRequestBase request) {
        try {
            traceIfEnabled("abort : %s".formatted(request.getUri()));
        } catch (URISyntaxException e) {
            traceIfEnabled("abort failed : %s".formatted(e.getMessage()));
        }
        request.abort();
    }

    @Override
    protected boolean logExecutionException(Throwable t) {
        if (t instanceof IllegalStateException) {
            traceIfEnabled("Illegal state: %s".formatted(t.getMessage()));
            return true;
        } else if (t instanceof NoHttpResponseException) {
            traceIfEnabled("No Http Response: %s".formatted(t.getMessage()));
            return true;
        }
        return false;
    }

    private static class NoRetryStrategy implements HttpRequestRetryStrategy {

        private final int retryAfterSeconds;
        
        public NoRetryStrategy(int retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }

        @Override
        public boolean retryRequest(HttpRequest request, IOException exception, int execCount,
                HttpContext context) {
            return false;
        }

        @Override
        public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
            return false;
        }

        @Override
        public TimeValue getRetryInterval(HttpResponse response, int execCount,
                HttpContext context) {
            return TimeValue.ofSeconds(retryAfterSeconds);
        }
    }
}
