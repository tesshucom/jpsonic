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

package com.tesshu.jpsonic.feature.upnp.content;

/**
 * Defines the count-limiting contract for UPnP content processors.
 *
 * <p>
 * Some content processors can produce or query an effectively unbounded number
 * of results, such as search and random-selection processors. UPnP requests,
 * however, define the requested range using an offset and count.
 * Implementations of this interface must therefore ensure that the amount of
 * data retrieved is bounded by the applicable server-side limit.
 * </p>
 *
 * <p>
 * Implementing this interface is also an explicit indication that the processor
 * requires care when handling result sizes. The limit must be applied before
 * retrieving an unnecessarily large result set, rather than only after the data
 * has been obtained.
 * </p>
 */
public interface CountLimitProc {

    /**
     * Calculates the number of items to retrieve from a paged UPnP request,
     * constrained by a server-side maximum.
     *
     * <p>
     * This calculation must be applied before retrieving the result set. Processors
     * that can produce an unbounded result set must use this contract to honor the
     * requested offset and count while enforcing the server-side limit.
     * </p>
     *
     * @param requestOffset starting index requested by the control point
     * @param requestMax    maximum number of items requested by the control point
     * @param serverSideMax maximum number of items that may be retrieved
     * @return the number of items that should actually be retrieved
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
