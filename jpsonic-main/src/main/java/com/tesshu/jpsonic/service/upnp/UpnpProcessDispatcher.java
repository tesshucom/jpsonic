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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp;

/**
 * Interface for searching UPnPContentProcessor. Each UPnPContentProcessor will need to hold the ID defined here and be
 * able to identify. This Dispatcher will only be used at the Top Level hierarchy in the UPnP ContentDirectory.
 */
public interface UpnpProcessDispatcher {

    /**
     * Returns the UPnPContentProcessor specified by ID. This method is used only at the top level of a UPnP
     * ContentDirectory. Do not use it anywhere else to avoid circular references.
     */
    UPnPContentProcessor<?, ?> findProcessor(ProcId id);
}
