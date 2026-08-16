package com.tesshu.jpsonic.feature.upnp.content;

import com.tesshu.jpsonic.domain.model.SearchResult;
import org.jupnp.support.model.BrowseResult;

/**
 * Defines the capability to convert search results into a UPnP browse result.
 *
 * <p>
 * A processor implementing this interface can handle the conversion of a
 * {@link ParamSearchResult} produced by the search layer into the
 * {@link BrowseResult} representation required by the UPnP ContentDirectory.
 * </p>
 *
 * @param <T> type of search result data handled by the processor
 */
@FunctionalInterface
public interface SearchResultProcessor<T> {

    BrowseResult toBrowseResult(SearchResult<T> searchResult);
}
