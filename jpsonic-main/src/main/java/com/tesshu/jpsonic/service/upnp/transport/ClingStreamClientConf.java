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

import java.util.concurrent.TimeUnit;

import org.jupnp.model.ServerClientTokens;
import org.jupnp.transport.spi.AbstractStreamClientConfiguration;
import org.jupnp.transport.spi.StreamClientConfiguration;

/**
 * Derived from
 * org.fourthline.cling.transport.spi.AbstractStreamClientConfiguration.
 * 
 * This class had been also copied to jupnp. However, since Jpsonic uses the
 * Record class for configuration, it has been replaced by interface. The method
 * names and values of this interface will not change in the future to eliminate
 * confusion when matching property names in legacy code. Overrides are only
 * allowed for certain sealed Record classes. (Record class is immutable)
 * 
 * @see AbstractStreamClientConfiguration
 */
public sealed interface ClingStreamClientConf extends StreamClientConfiguration
        permits DefaultStreamClientConf {

    @Override
    default int getTimeoutSeconds() {
        return 10;
    }

    @Override
    default int getRetryIterations() {
        return 5;
    }

    @Override
    default int getLogWarningSeconds() {
        return 5;
    }

    @Override
    default int getRetryAfterSeconds() {
        return (int) TimeUnit.MINUTES.toSeconds(10);
    }

    @Override
    default String getUserAgentValue(int majorVersion, int minorVersion) {
        return new ServerClientTokens(majorVersion, minorVersion).toString();
    }
}
