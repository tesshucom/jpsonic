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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP") // for sandbox
public class RouterImpl extends org.jupnp.transport.RouterImpl {

    private static final Logger LOG = LoggerFactory.getLogger(RouterImpl.class);
    private @Nullable String filteredIp;

    public RouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
        super(configuration, protocolFactory);
    }

    public RouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory,
            String filteredIp) {
        this(configuration, protocolFactory);
        this.filteredIp = filteredIp;
    }

    @Override
    public StreamResponseMessage send(StreamRequestMessage msg) throws RouterException {
        lock(readLock);
        try {
            if (enabled) {
                if (streamClient == null) {
                    LOG.debug("No StreamClient available, not sending: {}", msg);
                    return null;
                }

                if (filteredIp != null) {
                    String hostname = msg.getUri().getHost();
                    InetAddress inetAddress;
                    try {
                        inetAddress = InetAddress.getByName(hostname);
                        String address = inetAddress.getHostAddress();
                        if (filteredIp.equals(address)) {
                            LOG.debug("Skipped sent to: {}", hostname);
                            return null;
                        }
                    } catch (UnknownHostException e) {
                        LOG.debug("Unknown Host Name: {}", hostname);
                    }
                }

                LOG.debug("Sending via TCP unicast stream: {}", msg);
                try {
                    return streamClient.sendRequest(msg);
                } catch (InterruptedException e) {
                    throw new RouterException("Sending stream request was interrupted", e);
                }
            } else {
                LOG.debug("Router disabled, not sending stream request: {}", msg);
                return null;
            }
        } finally {
            unlock(readLock);
        }
    }
}
