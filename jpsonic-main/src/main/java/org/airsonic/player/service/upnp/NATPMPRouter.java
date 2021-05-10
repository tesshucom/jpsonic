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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service.upnp;

import com.hoodcomputing.natpmp.MapRequestMessage;
import com.hoodcomputing.natpmp.NatPmpDevice;
import com.hoodcomputing.natpmp.NatPmpException;

/**
 * @author Sindre Mehus
 */
public final class NATPMPRouter implements Router {

    private final NatPmpDevice device;

    private NATPMPRouter(NatPmpDevice device) {
        this.device = device;
    }

    public static NATPMPRouter findRouter() {
        try {
            return new NATPMPRouter(new NatPmpDevice(false));
        } catch (NatPmpException x) {
            return null;
        }
    }

    @Override
    public void addPortMapping(int externalPort, int internalPort, final int leaseDuration) {
        int duration = leaseDuration;
        // Use one week if lease duration is "forever".
        if (duration == 0) {
            duration = 7 * 24 * 3600;
        }
        MapRequestMessage map = new MapRequestMessage(true, internalPort, externalPort, duration, null);
        device.enqueueMessage(map);
        device.waitUntilQueueEmpty();
    }

    @Override
    public void deletePortMapping(int externalPort, int internalPort) {
        MapRequestMessage map = new MapRequestMessage(true, internalPort, externalPort, 0, null);
        device.enqueueMessage(map);
        device.waitUntilQueueEmpty();
    }
}
