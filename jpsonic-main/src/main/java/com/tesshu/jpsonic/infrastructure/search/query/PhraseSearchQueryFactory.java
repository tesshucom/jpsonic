package com.tesshu.jpsonic.infrastructure.search.query;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.infrastructure.search.index.IndexType;
import jakarta.annotation.Nonnull;
import org.apache.lucene.search.Query;

/**
 * Provides query construction for phrase-based media searches.
 * <p>
 * This interface represents the subset of query construction used by
 * phrase-based searches and may be shared by different search entry points.
 */
public interface PhraseSearchQueryFactory {

    /**
     * Creates a query that restricts the search to the specified music folders.
     * <p>
     * When {@code isId3} is {@code true}, the ID3-based folder fields are used;
     * otherwise, path-based folder fields are used.
     *
     * @param isId3   whether to use ID3-based folder fields
     * @param folders music folders to include in the query
     * @return a query matching the specified folders
     */
    @Nonnull
    Query createFolderQuery(@Nonnull boolean isId3, @Nonnull Collection<MusicFolder> folders);

    /**
     * Creates a multi-field phrase query for the specified search input.
     * <p>
     * The fields and boosts used for the query are determined by the specified
     * {@link IndexType}. Composer fields may also be included according to the
     * current search configuration.
     *
     * @param targetFields fields to search
     * @param queryString  phrase to search for
     * @param indexType    index type that determines field boosts and search
     *                     behavior
     * @return a matching query, or an empty {@link Optional} if no query can be
     *         created
     * @throws IOException if the query cannot be constructed during analysis
     */
    Optional<Query> createPhraseQuery(List<String> targetFields, @Nonnull String queryString,
            @Nonnull IndexType indexType) throws IOException;
}
