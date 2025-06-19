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

package com.tesshu.jpsonic.service.upnp;

import com.tesshu.jpsonic.service.upnp.transport.RouterImpl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.transport.Router;

@SuppressWarnings("PMD.MissingOverride") // false positive
public class UpnpServiceImpl extends org.jupnp.UpnpServiceImpl {

    private final String filteredIp;

    public UpnpServiceImpl(UpnpServiceConfiguration configuration, @Nullable String filteredIp) {
        super(configuration);
        this.filteredIp = filteredIp;
    }

    protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
        return new RouterImpl(getConfiguration(), protocolFactory, filteredIp);
    }
}
