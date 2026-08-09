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

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;

/**
 * Defines the processing boundary for one logical UPnP ContentDirectory
 * hierarchy.
 *
 * <p>
 * A processor owns the retrieval and representation of one subtree of the
 * ContentDirectory. It works with a branch type {@code P} and its child type
 * {@code C}, while the common browse flow is provided by this interface.
 * </p>
 *
 * <p>
 * The processor is responsible for obtaining the domain data and supplying the
 * DIDL objects that represent it. Cross-hierarchy processor resolution is
 * deliberately not part of this contract; a processor should remain
 * self-contained once it has been selected for a request.
 * </p>
 *
 * @param <P> type of branch entities represented as UPnP containers
 * @param <C> type of child entities represented as UPnP content
 */
public interface UPnPContentProcessor<P, C> {

    ProcId getProcId();

    String getProcTitle();

    void setProcTitle(String procTitle);

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // DIDLParser#generate
    default BrowseResult createBrowseResult(DIDLContent parent, int count, int totalMatches)
            throws ExecutionException {
        String result;
        try {
            result = new DIDLParser().generate(parent);
        } catch (Exception e) {
            throw new ExecutionException("Unable to generate XML representation of content model.",
                    e);
        }
        return new BrowseResult(result, count, totalMatches);
    }

    Container createRootContainer();

    default BrowseResult browseMetadata() throws ExecutionException {
        return createBrowseResult(new DIDLContent().addContainer(createRootContainer()), 1, 1);
    }

    Container createContainer(P entity);

    default void addDirectChild(DIDLContent parent, P entity) {
        parent.addContainer(createContainer(entity));
    }

    List<P> getDirectChildren(long offset, long maxLength);

    int getDirectChildrenCount();

    default BrowseResult browseRoot(String filter, long offset, long maxLength)
            throws ExecutionException {
        DIDLContent parent = new DIDLContent();
        getDirectChildren(offset, maxLength).forEach(child -> addDirectChild(parent, child));
        return createBrowseResult(parent, (int) parent.getCount(), getDirectChildrenCount());
    }

    P getDirectChild(String id);

    default BrowseResult browseDirectChildren(String id) throws ExecutionException {
        P entity = getDirectChild(id);
        DIDLContent parent = new DIDLContent();
        addDirectChild(parent, entity);
        return createBrowseResult(parent, 1, 1);
    }

    List<C> getChildren(P entity, long offset, long maxLength);

    int getChildSizeOf(P entity);

    void addChild(DIDLContent parent, C entity);

    default BrowseResult browseLeaf(String id, String filter, long offset, long maxLength)
            throws ExecutionException {
        P branch = getDirectChild(id);
        List<C> leaves = getChildren(branch, offset, maxLength);
        DIDLContent parent = new DIDLContent();
        for (C leaf : leaves) {
            addChild(parent, leaf);
        }
        return createBrowseResult(parent, leaves.size(), getChildSizeOf(branch));
    }
}
