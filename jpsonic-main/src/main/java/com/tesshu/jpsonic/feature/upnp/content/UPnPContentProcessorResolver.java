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

package com.tesshu.jpsonic.feature.upnp.content;

/**
 * Resolves processor capabilities responsible for a UPnP content hierarchy.
 *
 * <p>
 * This abstraction is intentionally used only at the top level of the
 * ContentDirectory. A {@link ProcId} is resolved to the processor capability
 * required for the current operation, after which processing remains within
 * that processor.
 * </p>
 *
 * <p>
 * Restricting resolution to the entry point keeps the processing graph
 * one-directional and prevents processors from using processor lookup as a
 * general-purpose dependency mechanism. In particular, processors must not
 * resolve other processors through this abstraction.
 * </p>
 */
public interface UPnPContentProcessorResolver {

    /**
     * Resolves processor capabilities for a UPnP content hierarchy.
     *
     * <p>
     * This method is intended to be called only by the top-level ContentDirectory
     * entry point. Processors must not use it to resolve or invoke other
     * processors.
     * </p>
     *
     * @param id content hierarchy to resolve
     * @return the processor responsible for the hierarchy
     */
    UPnPContentProcessor<?, ?> findProcessor(ProcId id);

    /**
     * Resolves the processor capable of converting search results for the specified
     * content hierarchy.
     *
     * @param id content hierarchy to resolve
     * @return the processor capable of converting search results
     */
    SearchResultProcessor<?> findSearchResultProcessor(ProcId id);

    /**
     * Resolves the processor capable of handling specialized search filters.
     *
     * <p>
     * The resolved processor handles search requests that require specialized
     * processing based on the UPnP search filter.
     * </p>
     *
     * @return the processor capable of handling specialized search filters
     */
    SearchFilterProcessor findSearchFilterProcessor();
}
