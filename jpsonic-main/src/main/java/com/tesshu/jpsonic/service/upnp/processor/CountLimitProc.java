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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

/**
 * This is an interface for a processor that handles infinitely long lists and requires a certain size limit.
 */
public interface CountLimitProc {

    /**
     * Returns the boundary value when searching from the request value from UPnP and the Jpsonic setting item value.
     *
     * @param requestOffset
     *            Request value from UPnP
     * @param requestMax
     *            Request value from UPnP
     * @param serverSideMax
     *            Maximum value determined by Jpsonic
     *
     * @return Count value used for search
     */
    default int toCount(long requestOffset, long requestMax, int serverSideMax) {
        if (serverSideMax <= requestOffset) {
            return 0;
        } else if (serverSideMax < requestOffset + requestMax) {
            return (int) (serverSideMax - requestOffset);
        }
        return (int) requestMax;
    }
}
