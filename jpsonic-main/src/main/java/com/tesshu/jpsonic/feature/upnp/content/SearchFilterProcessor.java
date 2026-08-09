package com.tesshu.jpsonic.feature.upnp.content;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.BrowseResult;

/**
 * Processes UPnP search requests that require specialized handling based on the
 * search filter.
 * <p>
 * Implementations may support search requests issued by specific third-party
 * clients or other non-standard usage patterns of the UPnP ContentDirectory
 * search operation. The client itself is not part of this interface's concern;
 * implementations simply determine whether they can handle a given filter and,
 * if so, produce the corresponding {@link BrowseResult}.
 */
public interface SearchFilterProcessor {

    boolean isAvailable(String filter);

    @Nullable
    BrowseResult getBrowseResult(@NonNull String query, @Nullable String filter, long count,
            long offset);
}
