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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * A unified builder for constructing Lucene {@link Query} objects used in
 * music-related search functionality.
 * <p>
 * This builder encapsulates logic for various common query patterns, and is
 * designed to reduce the responsibility of the {@code QueryFactory} class,
 * improve modularity, and make testing easier.
 * </p>
 *
 * <h2>Main Features:</h2>
 * <ul>
 * <li><b>Genre Query</b>: Tokenizes and builds an OR query across multiple
 * genre strings.</li>
 * <li><b>Field Query</b>: Builds phrase or n-gram queries for a given field and
 * query string.</li>
 * <li><b>Folder Query</b>: Builds an OR query for folder IDs or folder
 * paths.</li>
 * <li><b>Year Range Query</b>: Constructs a Lucene {@link IntPoint} range query
 * for the year field.</li>
 * <li><b>Boosted Multi-Field Query</b>: Supports multiple fields with optional
 * per-field boost values. Composer-related fields can be conditionally
 * excluded.</li>
 * </ul>
 */
public class LuceneQueryBuilder {

    private final AnalyzerFactory analyzerFactory;
    private final SettingsService settingsService;

    private static final float DEFAULT_BOOST_MULTIPLIER = 2.0f;

    private static final Set<String> COMPOSER_FIELDS = Set
        .of(FieldNamesConstants.COMPOSER, FieldNamesConstants.COMPOSER_READING,
                FieldNamesConstants.COMPOSER_READING_ROMANIZED);

    private static final Set<String> ROMANIZED_ONLY_FIELDS = Set
        .of(FieldNamesConstants.ARTIST_READING_ROMANIZED,
                FieldNamesConstants.COMPOSER_READING_ROMANIZED);

    /**
     * Constructs a new LuceneQueryBuilder.
     *
     * @param analyzerFactory the analyzer factory
     * @param settingsService the settings service
     */
    public LuceneQueryBuilder(AnalyzerFactory analyzerFactory, SettingsService settingsService) {
        this.analyzerFactory = analyzerFactory;
        this.settingsService = settingsService;
    }

    /**
     * Builds a folder query using either folder ID or folder path.
     *
     * @param folders list of music folders
     * @param isId3   whether to use folder ID (true) or path (false)
     * @return a BooleanQuery containing the folder conditions
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Query buildFolderQuery(@Nonnull List<MusicFolder> folders, boolean isId3) {
        String field = isId3 ? FieldNamesConstants.FOLDER_ID : FieldNamesConstants.FOLDER;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        folders
            .stream()
            .map(folder -> isId3 ? folder.getId().toString() : folder.getPathString())
            .map(value -> new TermQuery(new Term(field, value)))
            .forEach(query -> builder.add(query, BooleanClause.Occur.SHOULD));
        return builder.build();
    }

    /**
     * Builds a range query for the year field.
     *
     * @param from the start year (inclusive) or null
     * @param to   the end year (inclusive) or null
     * @return a range query on the year field
     */
    public Query buildYearRangeQuery(@Nullable Integer from, @Nullable Integer to) {
        int lower = from == null ? Integer.MIN_VALUE : from;
        int upper = to == null ? Integer.MAX_VALUE : to;
        return IntPoint.newRangeQuery(FieldNamesConstants.YEAR, lower, upper);
    }

    /**
     * Builds a query for a single field based on the query string. Supports both
     * phrase and n-gram queries depending on the field.
     *
     * @param fieldName   the field name
     * @param queryString the user input
     * @return an optional query if tokens are present
     * @throws IOException if tokenization fails
     */
    public Optional<Query> buildFieldQuery(@Nonnull String fieldName, @Nonnull String queryString)
            throws IOException {
        boolean isRomanizedField = FieldNamesConstants.ARTIST_READING_ROMANIZED.equals(fieldName)
                || FieldNamesConstants.COMPOSER_READING_ROMANIZED.equals(fieldName);

        String analyzedField = isRomanizedField ? FieldNamesConstants.ARTIST_READING : fieldName;
        List<String> tokens = tokenize(analyzedField, queryString);

        if (tokens.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(buildQueryFromTokens(tokens, fieldName, isRomanizedField));
    }

    /**
     * Builds a multi-field query with optional boosts and composer filtering.
     *
     * @param fieldNames      the list of field names
     * @param queryString     the user input
     * @param boosts          a map of field boosts
     * @param includeComposer whether to include composer fields
     * @return an optional combined query
     * @throws IOException if field query generation fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Optional<Query> buildMultiFieldQueryWithBoost(List<String> fieldNames,
            @Nonnull String queryString, Map<String, Float> boosts, boolean includeComposer)
            throws IOException {

        boolean effectiveIncludeComposer = includeComposer || settingsService.isSearchComposer();
        List<String> filteredFields = filterFields(fieldNames, effectiveIncludeComposer);
        if (filteredFields.isEmpty()) {
            return Optional.empty();
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String field : filteredFields) {
            Optional<Query> queryOpt = buildFieldQuery(field, queryString);
            queryOpt.ifPresent(q -> {
                if (boosts.containsKey(field)) {
                    builder
                        .add(new BoostQuery(q, boosts.get(field) * DEFAULT_BOOST_MULTIPLIER),
                                BooleanClause.Occur.SHOULD);
                } else {
                    builder.add(q, BooleanClause.Occur.SHOULD);
                }
            });
        }
        return Optional.of(builder.build());
    }

    /**
     * Builds a genre query using Lucene's analyzer.
     *
     * @param genres the list of genre strings
     * @return a BooleanQuery containing analyzed genre terms
     * @throws IOException if tokenization fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Query buildGenreQuery(@Nonnull List<String> genres) throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String genre : genres) {
            if (genre == null || genre.isEmpty()) {
                continue;
            }
            List<String> tokens = tokenize(FieldNamesConstants.GENRE, genre);
            tokens
                .stream()
                .map(token -> new TermQuery(new Term(FieldNamesConstants.GENRE, token)))
                .forEach(query -> builder.add(query, BooleanClause.Occur.SHOULD));
        }
        return builder.build();
    }

    /**
     * Filters field names based on the settings and whether composer fields should
     * be included.
     *
     * @param fields          original list of field names
     * @param includeComposer whether composer fields are allowed
     * @return a filtered list of field names
     */
    List<String> filterFields(List<String> fields, boolean includeComposer) {
        IndexScheme scheme = IndexScheme.of(settingsService.getIndexSchemeName());
        return fields
            .stream()
            .filter(field -> includeComposer || !COMPOSER_FIELDS.contains(field))
            .filter(field -> scheme == IndexScheme.ROMANIZED_JAPANESE
                    || !ROMANIZED_ONLY_FIELDS.contains(field))
            .collect(Collectors.toList());
    }

    /**
     * Tokenizes a given input string using the specified field's analyzer.
     *
     * @param field the field to analyze
     * @param input the input string
     * @return a list of tokens extracted from the analyzer
     * @throws IOException if tokenization fails
     */
    public List<String> tokenize(@Nonnull String field, @Nonnull String input) throws IOException {
        try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field, input)) {
            stream.reset();
            CharTermAttribute termAttr = stream.getAttribute(CharTermAttribute.class);
            List<String> tokens = new ArrayList<>();
            while (stream.incrementToken()) {
                tokens.add(termAttr.toString());
            }
            return tokens;
        }
    }

    /**
     * Builds a Lucene Query from token list.
     *
     * @param tokens           list of tokens to be converted
     * @param fieldName        field name for terms
     * @param isRomanizedField whether the field is romanized
     * @return a Query (PhraseQuery or BooleanQuery) from tokens
     */
    private Query buildQueryFromTokens(List<String> tokens, String fieldName,
            boolean isRomanizedField) {
        if (isRomanizedField) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            tokens
                .stream()
                .map(token -> new TermQuery(new Term(fieldName, token)))
                .forEach(query -> builder.add(query, BooleanClause.Occur.SHOULD));
            return builder.build();
        } else {
            PhraseQuery.Builder builder = new PhraseQuery.Builder();
            tokens.forEach(token -> builder.add(new Term(fieldName, token)));
            builder.setSlop(1);
            return builder.build();
        }
    }
}
