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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.feature.search.UPnPSearchMethod;
import com.tesshu.jpsonic.feature.upnp.content.processor.UPnPProcessorUtil;
import com.tesshu.jpsonic.infrastructure.concurrent.ConcurrentUtils;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.search.criteria.UPnPSearchCriteria;
import com.tesshu.jpsonic.infrastructure.search.criteria.UPnPSearchCriteriaDirector;
import com.tesshu.jpsonic.infrastructure.search.query.QueryFactory;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.contentdirectory.AbstractContentDirectoryService;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.SortCriterion;
import org.springframework.stereotype.Service;

/**
 * Entry point for Jpsonic's UPnP ContentDirectory processing.
 *
 * <p>
 * This service forms the boundary between the UPnP protocol and the
 * content-processing architecture. It interprets incoming Object IDs, resolves
 * the processor responsible for the requested content hierarchy, and delegates
 * the actual content processing to that processor.
 * </p>
 *
 * <p>
 * Content-specific browsing logic is intentionally kept outside this class.
 * This keeps the protocol-facing service independent of the individual content
 * hierarchies and allows those hierarchies to be implemented as independent
 * {@link UPnPContentProcessor} components.
 * </p>
 */
@Service("contentDirectoryService")
class ContentDirectoryService extends AbstractContentDirectoryService implements CountLimitProc {

    private static final int SEARCH_COUNT_MAX = 50;

    private final UPnPContentProcessorResolver uPnPContentProcessorResolver;
    private final UPnPProcessorUtil uPnPProcessorUtil;
    private final QueryFactory queryFactory;
    private final MediaSearchProvider mediaSearchProvider;

    ContentDirectoryService(UPnPContentProcessorResolver uPnPContentProcessorResolver,
            UPnPProcessorUtil uPnPProcessorUtil, QueryFactory queryFactory,
            MediaSearchProvider mediaSearchProvider) {
        super(Arrays.asList("*"), Collections.emptyList());
        this.uPnPContentProcessorResolver = uPnPContentProcessorResolver;
        this.uPnPProcessorUtil = uPnPProcessorUtil;
        this.queryFactory = queryFactory;
        this.mediaSearchProvider = mediaSearchProvider;
    }

    ProcId getProcId(@NonNull String objectId) {
        int i = objectId.indexOf(ProcId.CID_SEPA);
        return ProcId.of(i == -1 ? objectId : objectId.substring(0, i));
    }

    @Nullable
    String getItemId(@NonNull String objectId) {
        int i = objectId.indexOf(ProcId.CID_SEPA);
        return i == -1 ? null : objectId.substring(i + 1);
    }

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag, String filter,
            long firstResult, final long maxResults, SortCriterion[] orderBy)
            throws ContentDirectoryException {
        if (isEmpty(objectId)) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS,
                    "objectId is null");
        }

        ProcId procId = getProcId(objectId);
        UPnPContentProcessor<?, ?> processor = uPnPContentProcessorResolver.findProcessor(procId);
        String itemId = getItemId(objectId);
        long max = maxResults == 0 ? Long.MAX_VALUE : maxResults;

        try {
            if (isEmpty(itemId)) {
                return browseFlag == BrowseFlag.METADATA ? processor.browseMetadata()
                        : processor.browseRoot(filter, firstResult, max);
            }
            return browseFlag == BrowseFlag.METADATA ? processor.browseDirectChildren(itemId)
                    : processor.browseLeaf(itemId, filter, firstResult, max);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS.getCode(),
                    ContentDirectoryErrorCode.CANNOT_PROCESS.getDescription(), e);
        }
    }

    @Override
    public BrowseResult search(String containerId, String upnpSearchQuery, String filter,
            long firstResult, long maxResults, SortCriterion[] orderBy)
            throws ContentDirectoryException {

        // For known filters, delegation processing
        SearchFilterProcessor searchFilter = uPnPContentProcessorResolver
            .findSearchFilterProcessor();

        if (searchFilter.isAvailable(filter)) {
            BrowseResult wmpResult = searchFilter
                .getBrowseResult(upnpSearchQuery, filter, maxResults, firstResult);
            if (!isEmpty(wmpResult)) {
                return wmpResult;
            }
        }

        // General UPnP search
        int offset = (int) firstResult;
        int count = toCount(firstResult, maxResults, SEARCH_COUNT_MAX);
        UPnPSearchMethod searchMethod = uPnPProcessorUtil.getUPnPSearchMethod();
        List<MusicFolder> folders = uPnPProcessorUtil.getGuestFolders();
        UPnPSearchCriteriaDirector director = new UPnPSearchCriteriaDirector(searchMethod, folders,
                queryFactory);
        UPnPSearchCriteria criteria = director.construct(offset, count, upnpSearchQuery);

        ProcId searchResultProcId = switch (criteria.targetType()) {
        case ARTIST, ALBUM, SONG -> ProcId.MEDIA_FILE;
        case ALBUM_ID3 -> ProcId.ALBUM_ID3;
        case ARTIST_ID3 -> ProcId.ARTIST;
        case GENRE, ALBUM_ID3_GENRE -> throw new AssertionError("Unreachable code.");
        };
        SearchResultProcessor<?> searchResultProcessor = uPnPContentProcessorResolver
            .findSearchResultProcessor(searchResultProcId);

        return searchResultProcessor.toBrowseResult(mediaSearchProvider.search(criteria));
    }
}
