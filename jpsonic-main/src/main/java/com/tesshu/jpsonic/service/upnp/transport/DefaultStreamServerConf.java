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

import org.jupnp.model.ServerClientTokens;
import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * Default settings for the UPnP Server used by Jpsonic.
 */
public record DefaultStreamServerConf(int listenPort, int tcpConnectionBacklog,
        ServerClientTokens serverClientTokens) implements StreamServerConfiguration {

    public DefaultStreamServerConf(int listenPort, ServerClientTokens serverClientTokens) {
        this(listenPort, 0, serverClientTokens);
    }

    public DefaultStreamServerConf(ServerClientTokens serverClientTokens) {
        this(0, serverClientTokens);
    }

    @Override
    public int getListenPort() {
        return listenPort;
    }
}
