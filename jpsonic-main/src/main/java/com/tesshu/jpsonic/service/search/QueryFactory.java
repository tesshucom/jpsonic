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
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.param.RandomSearchCriteria;
import com.tesshu.jpsonic.service.SettingsService;
import jakarta.annotation.Nonnull;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Factory class for constructing Lucene queries for music search. This class
 * delegates Lucene-specific query construction to {@link LuceneQueryBuilder},
 * and manages application-specific concerns like index scheme and composer
 * field inclusion.
 */
@Component
public class QueryFactory {

    private final SettingsService settingsService;
    private final LuceneQueryBuilder luceneQueryBuilder;
    private final AnalyzerFactory analyzerFactory;

    private static final String MEDIA_TYPE = FieldNamesConstants.MEDIA_TYPE;

    /**
     * Constructs a QueryFactory.
     *
     * @param settingsService the settings service
     * @param analyzerFactory the analyzer factory
     */
    public QueryFactory(SettingsService settingsService, AnalyzerFactory analyzerFactory) {
        this.settingsService = settingsService;
        this.analyzerFactory = analyzerFactory;
        this.luceneQueryBuilder = new LuceneQueryBuilder(analyzerFactory, settingsService);
    }

    /**
     * Creates a query that matches the given folders using ID3 or path.
     *
     * @param isId3   true to use ID3 folder, false to use path
     * @param folders the list of music folders
     * @return folder query
     */
    @Nonnull
    public Query createFolderQuery(@Nonnull Boolean isId3, @Nonnull List<MusicFolder> folders) {
        return luceneQueryBuilder.buildFolderQuery(folders, isId3);
    }

    /**
     * Creates a year range query.
     *
     * @param from the start year (nullable)
     * @param to   the end year (nullable)
     * @return range query
     */
    @Nonnull
    public Query createYearRangeQuery(@Nullable Integer from, @Nullable Integer to) {
        return luceneQueryBuilder.buildYearRangeQuery(from, to);
    }

    /**
     * Creates a phrase query with optional composer fields.
     *
     * @param targetFields the fields to search
     * @param queryString  the phrase to search
     * @param indexType    the index type
     * @return an optional Lucene query
     * @throws IOException if tokenization fails
     */
    public Optional<Query> createPhraseQuery(List<String> targetFields, @Nonnull String queryString,
            @Nonnull IndexType indexType) throws IOException {
        boolean includeComposer = settingsService.isSearchComposer();
        return luceneQueryBuilder
            .buildMultiFieldQueryWithBoost(targetFields, queryString, indexType.getBoosts(),
                    includeComposer);
    }

    /**
     * Constructs a Lucene query for phrase-based search with folder filtering.
     *
     * @param searchInput     user input
     * @param includeComposer whether to include composer fields
     * @param musicFolders    target folders
     * @param indexType       index type
     * @return Lucene query
     * @throws IOException if query construction fails
     */
    public Query searchByPhrase(@Nonnull String searchInput, boolean includeComposer,
            @Nonnull List<MusicFolder> musicFolders, @Nonnull IndexType indexType)
            throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        boolean composerIncluded = includeComposer || settingsService.isSearchComposer();

        Optional<Query> textQuery = luceneQueryBuilder
            .buildMultiFieldQueryWithBoost(indexType.getFields(), searchInput,
                    indexType.getBoosts(), composerIncluded);
        textQuery.ifPresent(q -> builder.add(q, BooleanClause.Occur.MUST));

        boolean isId3 = indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3;
        Query folderQuery = luceneQueryBuilder.buildFolderQuery(musicFolders, isId3);
        builder.add(folderQuery, BooleanClause.Occur.MUST);

        return builder.build();
    }

    /**
     * Returns a genre query.
     *
     * @param genre the genre name
     * @return Lucene query
     * @throws IOException if tokenization fails
     */
    public Query getGenre(@Nonnull String genre) throws IOException {
        return luceneQueryBuilder.buildGenreQuery(List.of(genre));
    }

    public Query getAlbumId3GenreCount(@Nonnull String genre, List<MusicFolder> folders)
            throws IOException {
        return new BooleanQuery.Builder()
            .add(createFolderQuery(true, folders), BooleanClause.Occur.MUST)
            .add(luceneQueryBuilder.buildGenreQuery(List.of(genre)), BooleanClause.Occur.MUST)
            .build();
    }

    public Query getSongGenreCount(@Nonnull String genre, List<MusicFolder> folders,
            MediaType... types) throws IOException {
        return new BooleanQuery.Builder()
            .add(createFolderQuery(false, folders), BooleanClause.Occur.MUST)
            .add(luceneQueryBuilder.buildGenreQuery(List.of(genre)), BooleanClause.Occur.MUST)
            .add(getTypesQuery(types), BooleanClause.Occur.MUST)
            .build();
    }

    public Query getAlbumChildren(Album album, String genre, List<MusicFolder> folders,
            MediaType... types) throws IOException {
        return new BooleanQuery.Builder()
            .add(luceneQueryBuilder.buildGenreQuery(List.of(genre)), BooleanClause.Occur.MUST)
            .add(createFolderQuery(false, folders), BooleanClause.Occur.MUST)
            .add(getTypesQuery(types), BooleanClause.Occur.MUST)
            .build();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Query getTypesQuery(MediaType... types) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (MediaType type : types) {
            builder
                .add(new TermQuery(new Term(MEDIA_TYPE, type.name())), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    public Query getRandomSongs(@Nonnull RandomSearchCriteria criteria) throws IOException {
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        query
            .add(new TermQuery(new Term(MEDIA_TYPE, MediaType.MUSIC.name())),
                    BooleanClause.Occur.MUST);

        if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
            query
                .add(luceneQueryBuilder.buildGenreQuery(criteria.getGenres()),
                        BooleanClause.Occur.MUST);
        }

        if (criteria.getFromYear() != null || criteria.getToYear() != null) {
            query
                .add(createYearRangeQuery(criteria.getFromYear(), criteria.getToYear()),
                        BooleanClause.Occur.MUST);
        }

        query.add(createFolderQuery(false, criteria.getMusicFolders()), BooleanClause.Occur.MUST);

        return query.build();
    }

    public Query getRandomSongs(@Nonnull List<MusicFolder> musicFolders, String... genres)
            throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(MEDIA_TYPE, MediaType.MUSIC.name())),
                    BooleanClause.Occur.MUST)
            .add(createFolderQuery(false, musicFolders), BooleanClause.Occur.MUST);

        if (genres.length > 0) {
            builder
                .add(luceneQueryBuilder.buildGenreQuery(List.of(genres)), BooleanClause.Occur.MUST);
        }

        return builder.build();
    }

    public Query getRandomAlbums(@Nonnull List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder()
            .add(createFolderQuery(false, musicFolders), BooleanClause.Occur.SHOULD)
            .build();
    }

    public Query getRandomAlbumsId3(@Nonnull List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder()
            .add(createFolderQuery(true, musicFolders), BooleanClause.Occur.SHOULD)
            .build();
    }

    public Query getAlbumId3sByGenres(@Nullable String genres, @Nonnull List<MusicFolder> folders)
            throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (genres != null && !genres.isEmpty()) {
            builder
                .add(luceneQueryBuilder.buildGenreQuery(List.of(genres)), BooleanClause.Occur.MUST);
        }

        builder.add(createFolderQuery(true, folders), BooleanClause.Occur.MUST);
        return builder.build();
    }

    public Query getMediasByGenres(@Nullable String genres, @Nonnull List<MusicFolder> folders)
            throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (genres != null && !genres.isEmpty()) {
            builder
                .add(luceneQueryBuilder.buildGenreQuery(List.of(genres)), BooleanClause.Occur.MUST);
        }

        builder.add(createFolderQuery(false, folders), BooleanClause.Occur.MUST);
        return builder.build();
    }

    /**
     * Converts a list of genre strings into a Lucene query using pre-analyzed
     * tokens.
     *
     * @param genres list of genres
     * @return a Lucene query for genres
     * @throws IOException if tokenization fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Query toPreAnalyzedGenres(@Nonnull List<String> genres) throws IOException {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();

        for (String genre : genres) {
            if (genre != null && !genre.isEmpty()) {
                try (TokenStream stream = analyzerFactory
                    .getAnalyzer()
                    .tokenStream(FieldNamesConstants.GENRE, genre)) {
                    stream.reset();
                    CharTermAttribute attr = stream.getAttribute(CharTermAttribute.class);
                    while (stream.incrementToken()) {
                        genreQuery
                            .add(new TermQuery(
                                    new Term(FieldNamesConstants.GENRE, attr.toString())),
                                    BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }

        query.add(genreQuery.build(), BooleanClause.Occur.MUST);
        return query.build();
    }
}
