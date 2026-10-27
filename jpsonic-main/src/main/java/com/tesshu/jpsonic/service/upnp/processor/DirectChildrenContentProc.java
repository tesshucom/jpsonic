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
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.StorageFolder;

public abstract class DirectChildrenContentProc<P, C> implements UPnPContentProcessor<P, C> {

    private String title;

    @Override
    public final String getProcTitle() {
        return title;
    }

    @Override
    public final void setProcTitle(String title) {
        this.title = title;
    }

    @Override
    public final Container createRootContainer() {
        Container container = new StorageFolder();
        container.setId(getProcId().getValue());
        container.setTitle(getProcTitle());
        container.setChildCount(getDirectChildrenCount());
        container.setParentID(ProcId.ROOT.getValue());
        return container;
    }
}
