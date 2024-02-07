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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.tesshu.jpsonic.domain.Version;
import com.tesshu.jpsonic.service.SettingsService;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Default settings for the UPnP Service used by Jpsonic.
 */
public class JpsonicUpnpServiceConf extends UpnpServiceConfigurationAdapter {

    private final ServerClientTokens tokens;

    public JpsonicUpnpServiceConf(ExecutorService defaultExecutorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor, String brand,
            Version version) {
        super(defaultExecutorService, asyncExecutorService, registryMaintainerExecutor);
        tokens = new ServerClientTokens(brand, version.toString());
    }

    @Override
    public StreamClient<?> createStreamClient() {
        DefaultStreamClientConf conf = new DefaultStreamClientConf(getDefaultExecutorService());
        return new StreamClientImpl(conf);
    }

    @Override
    public StreamServer<?> createStreamServer(NetworkAddressFactory factory) {
        int listenPort = SettingsService.getDefaultUPnPPort();
        if (listenPort <= 0) {
            listenPort = factory.getStreamListenPort();
        }
        DefaultStreamServerConf conf = new DefaultStreamServerConf(listenPort, tokens);
        return new StreamServerImpl(conf);
    }
}
