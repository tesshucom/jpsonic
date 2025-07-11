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

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.service.MenuItemService;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.WriteStatus;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RootUpnpProc implements UPnPContentProcessor<MenuItem, MenuItem> {

    private final MenuItemService menuItemService;
    private final UpnpProcessDispatcher dispatcher;

    public RootUpnpProc(MenuItemService menuItemService, @Lazy UpnpProcessDispatcher dispatcher) {
        this.menuItemService = menuItemService;
        this.dispatcher = dispatcher;
    }

    private UPnPContentProcessor<?, ?> findProcessor(MenuItem menuItem) {
        return dispatcher.findProcessor(ProcId.from(menuItem.getId()));
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ROOT;
    }

    @Override
    public String getProcTitle() {
        return "Jpsonic Media";
    }

    @Override
    public void setProcTitle(String title) {
        // to be none
    }

    @Override
    public final Container createRootContainer() {
        StorageFolder root = new StorageFolder();
        root.setId(ProcId.ROOT.getValue());
        root.setParentID("-1");
        root.setStorageUsed(-1L);
        root.setTitle(getProcTitle());
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);
        root.setChildCount(getDirectChildrenCount());
        return root;
    }

    @Override
    public Container createContainer(MenuItem parentMenu) {
        StorageFolder container = new StorageFolder();
        container.setId(ProcId.ROOT.getValue() + ProcId.CID_SEPA + parentMenu.getId().value());
        container.setParentID(ProcId.ROOT.getValue());
        container.setStorageUsed(-1L);
        container.setTitle(parentMenu.getName());
        container.setRestricted(true);
        container.setSearchable(true);
        container.setWriteStatus(WriteStatus.NOT_WRITABLE);
        container.setChildCount(getChildSizeOf(parentMenu));
        return container;
    }

    @Override
    public List<MenuItem> getDirectChildren(long offset, long maxResults) {
        return menuItemService.getTopMenuItems(ViewType.UPNP, true, offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return menuItemService.getTopMenuItemCount(ViewType.UPNP);
    }

    @Override
    public MenuItem getDirectChild(String id) {
        return menuItemService.getMenuItem(id);
    }

    @Override
    public int getChildSizeOf(MenuItem parentMenu) {
        int childSize = menuItemService.getChildSizeOf(ViewType.UPNP, parentMenu.getId());
        if (childSize == 1) {
            List<MenuItem> childlen = menuItemService
                .getChildlenOf(ViewType.UPNP, parentMenu.getId(), true, 0, 1);
            if (childlen.size() == 1) {
                return findProcessor(childlen.get(0)).getDirectChildrenCount();
            }
        }
        return childSize;
    }

    @Override
    public List<MenuItem> getChildren(MenuItem parentMenu, long offset, long maxResults) {
        return menuItemService
            .getChildlenOf(ViewType.UPNP, parentMenu.getId(), true, offset, maxResults);
    }

    @Override
    public void addChild(DIDLContent parent, MenuItem child) {
        UPnPContentProcessor<?, ?> proc = findProcessor(child);
        proc.setProcTitle(child.getName());
        parent.addContainer(proc.createRootContainer());
    }

    @Override
    public BrowseResult browseLeaf(String id, String filter, long offset, long maxLength)
            throws ExecutionException {
        MenuItem parentMenu = getDirectChild(id);
        int childSize = menuItemService.getChildSizeOf(ViewType.UPNP, parentMenu.getId());
        if (childSize == 1) {
            List<MenuItem> childlen = menuItemService
                .getChildlenOf(ViewType.UPNP, parentMenu.getId(), true, 0, 1);
            if (childlen.size() == 1) {
                return findProcessor(childlen.get(0)).browseRoot(filter, offset, maxLength);
            }
        }
        return UPnPContentProcessor.super.browseLeaf(id, filter, offset, maxLength);
    }
}
