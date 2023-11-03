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

package com.tesshu.jpsonic.service.upnp;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;

/**
 * An interface for representing a typical UPnP node tree with domain object.
 *
 * Assume that rather than using redundant processors, any tree can be represented using a chain of entity operation
 * definitions up to three layers , including the root.
 *
 * @param <P>
 *            Entity directly under root. Most are domain objects used for hierarchical expression such as Artist,
 *            Album, MefiaFile, etc. Or their composite object.
 * @param <C>
 *            Grand child entity of root. Child of P.
 */
public interface UPnPContentProcessor<P, C> {

    ProcId getProcId();

    String getProcTitle();

    void setProcTitle(String procTitle);

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // DIDLParser#generate
    default BrowseResult createBrowseResult(DIDLContent parent, int count, int totalMatches) throws ExecutionException {
        String result;
        try {
            result = new DIDLParser().generate(parent);
        } catch (Exception e) {
            throw new ExecutionException("Unable to generate XML representation of content model.", e);
        }
        return new BrowseResult(result, count, totalMatches);
    }

    Container createRootContainer();

    default BrowseResult browseMetadata() throws ExecutionException {
        return createBrowseResult(new DIDLContent().addContainer(createRootContainer()), 1, 1);
    }

    Container createContainer(P entity);

    default void addItem(DIDLContent parent, P entity) {
        parent.addContainer(createContainer(entity));
    }

    List<P> getDirectChildren(long offset, long maxLength);

    int getDirectChildrenCount();

    default BrowseResult browseRoot(String filter, long offset, long maxLength) throws ExecutionException {
        DIDLContent parent = new DIDLContent();
        getDirectChildren(offset, maxLength).forEach(child -> addItem(parent, child));
        return createBrowseResult(parent, (int) parent.getCount(), getDirectChildrenCount());
    }

    P getDirectChild(String id);

    default BrowseResult browseDirectChildren(String id) throws ExecutionException {
        P entity = getDirectChild(id);
        DIDLContent parent = new DIDLContent();
        addItem(parent, entity);
        return createBrowseResult(parent, 1, 1);
    }

    List<C> getChildren(P entity, long offset, long maxLength);

    int getChildSizeOf(P entity);

    void addChild(DIDLContent parent, C entity);

    default BrowseResult browseLeaf(String id, String filter, long offset, long maxLength) throws ExecutionException {
        P branch = getDirectChild(id);
        List<C> leaves = getChildren(branch, offset, maxLength);
        DIDLContent parent = new DIDLContent();
        for (C leaf : leaves) {
            addChild(parent, leaf);
        }
        return createBrowseResult(parent, leaves.size(), getChildSizeOf(branch));
    }
}
