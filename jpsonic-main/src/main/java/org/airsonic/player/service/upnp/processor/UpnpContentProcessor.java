/*
  This file is part of Airsonic.

  Airsonic is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Airsonic is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;

import java.util.List;

public abstract class UpnpContentProcessor<T extends Object, U extends Object> {

    private final UpnpProcessDispatcher dispatcher;

    private final UpnpProcessorUtil util;

    private String rootTitle;

    private String rootId;

    public UpnpContentProcessor(UpnpProcessDispatcher dispatcher, UpnpProcessorUtil util) {
        super();
        this.dispatcher = dispatcher;
        this.util = util;
    }

    /**
     * Browses the root metadata for a type.
     */
    public BrowseResult browseRootMetadata() throws Exception {
        DIDLContent didl = new DIDLContent();
        didl.addContainer(createRootContainer());
        return createBrowseResult(didl, 1, 1);
    }

    protected Container createRootContainer() {
        Container container = new StorageFolder();
        container.setId(getRootId());
        container.setTitle(getRootTitle());
        int childCount = getItemCount();
        container.setChildCount(childCount);
        container.setParentID(UpnpProcessDispatcher.CONTAINER_ID_ROOT);
        return container;
    }

    /**
     * Browses the top-level content of a type.
     */
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion... orderBy)
            throws Exception {
        DIDLContent didl = new DIDLContent();
        List<T> selectedItems = getItems(firstResult, maxResults);
        for (T item : selectedItems) {
            addItem(didl, item);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    /**
     * Browses metadata for a child.
     */
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        T item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addItem(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    /**
     * Browses a child of the container.
     */
    public BrowseResult browseObject(String id, String filter, long firstResult, long maxResults,
            SortCriterion... orderBy) throws Exception {
        T item = getItemById(id);
        List<U> selectedChildren = getChildren(item, firstResult, maxResults);
        DIDLContent didl = new DIDLContent();
        for (U child : selectedChildren) {
            addChild(didl, child);
        }
        return createBrowseResult(didl, selectedChildren.size(), getChildSizeOf(item));
    }

    protected final BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws Exception {
        return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
    }

    protected final UpnpProcessDispatcher getDispatcher() {
        return dispatcher;
    }

    public void addItem(DIDLContent didl, T item) {
        didl.addContainer(createContainer(item));
    }

    public abstract Container createContainer(T item);

    /**
     * Returns the count result in dao instead of service layer.
     */
    public abstract int getItemCount();

    public abstract List<T> getItems(long offset, long maxResults);

    public abstract T getItemById(String id);

    /**
     * Returns the count result in dao instead of service layer.
     */
    public abstract int getChildSizeOf(T item);

    public abstract List<U> getChildren(T item, long offset, long maxResults);

    public abstract void addChild(DIDLContent didl, U child);

    public final String getRootTitle() {
        return rootTitle;
    }

    public abstract void initTitle();

    protected final void setRootTitleWithResource(String key) {
        setRootTitle(util.getResource(key));
    }

    protected final void setRootTitle(String rootTitle) {
        this.rootTitle = rootTitle;
    }

    public final String getRootId() {
        return rootId;
    }

    protected final void setRootId(String rootId) {
        this.rootId = rootId;
    }

}
