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
 * (C) 2013 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;

/**
 * UPnP configuration which uses Apache HttpComponents. Needed to make UPnP work when deploying on Tomcat.
 *
 * @author Sindre Mehus
 */
public class ApacheUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    // @SuppressWarnings("rawtypes")
    // @Override
    // public StreamClient createStreamClient() {
    // return new StreamClientImpl(new StreamClientConfigurationImpl(Executors.newCachedThreadPool()));
    // }
    //
    // @SuppressWarnings("rawtypes")
    // @Override
    // public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
    // int listenPort = SettingsService.getDefaultUPnPPort();
    // return new StreamServerImpl(new StreamServerConfigurationImpl(
    // listenPort < 1 ? networkAddressFactory.getStreamListenPort() : listenPort));
    // }
}
