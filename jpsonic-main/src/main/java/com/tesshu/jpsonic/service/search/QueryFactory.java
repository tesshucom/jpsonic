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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * Factory class of Lucene Query. This class is an extract of the functionality that was once part of SearchService. It
 * is for maintainability and verification. Each corresponds to the SearchService method. The API syntax for query
 * generation depends on the lucene version. verification with query grammar is possible. On the other hand, the
 * generated queries are relatively small by version. Therefore, test cases of this class are useful for large version
 * upgrades.
 **/
@SuppressWarnings("PMD.CloseResource")
/*
 * Analysers are the factory class for TokenStreams and thread-safe. Loaded only once at startup and used for scanning
 * and searching. Do not explicitly close in this class.
 */
@Component
public class QueryFactory {

    private static final String ASTERISK = "*";

    private final AnalyzerFactory analyzerFactory;
    private final SearchServiceUtilities util;

    private final Function<MusicFolder, Query> toFolderIdQuery = (folder) -> {
        // Unanalyzed field
        return new TermQuery(new Term(FieldNamesConstants.FOLDER_ID, folder.getId().toString()));
    };

    private final Function<MusicFolder, Query> toFolderPathQuery = (folder) -> {
        // Unanalyzed field
        return new TermQuery(new Term(FieldNamesConstants.FOLDER, folder.getPath().getPath()));
    };

    /*
     * XXX 3.x -> 8.x : "SpanOr" has been changed to "Or". - Path comparison is more appropriate with "Or". - If
     * "SpanOr" is maintained, the DOC design needs to be changed.
     */
    public final BiFunction<@NonNull Boolean, @NonNull List<MusicFolder>, @NonNull Query> toFolderQuery = (isId3,
            folders) -> {
        BooleanQuery.Builder mfQuery = new BooleanQuery.Builder();
        folders.stream().map(isId3 ? toFolderIdQuery : toFolderPathQuery).forEach(t -> mfQuery.add(t, Occur.SHOULD));
        return mfQuery.build();
    };

    /*
     * XXX 3.x -> 8.x : RangeQuery has been changed to not allow null.
     */
    private final BiFunction<@Nullable Integer, @Nullable Integer, @NonNull Query> toYearRangeQuery = (from,
            to) -> IntPoint.newRangeQuery(FieldNamesConstants.YEAR, isEmpty(from) ? Integer.MIN_VALUE : from,
                    isEmpty(to) ? Integer.MAX_VALUE : to);

    public QueryFactory(AnalyzerFactory analyzerFactory, SearchServiceUtilities util) {
        super();
        this.analyzerFactory = analyzerFactory;
        this.util = util;
    }

    /*
     * XXX 3.x -> 8.x : In order to support wildcards, MultiFieldQueryParser has been replaced by the following process.
     * 
     * - There is also an override of MultiFieldQueryParser, but it is known to be high cost. - MultiFieldQueryParser
     * was created before Java API was modernized. - The spec of Parser has changed from time to time. Using parser does
     * not reduce library update risk. - Self made parser process reduces one library dependency. - It is easy to make
     * corrections later when changing the query to improve search accuracy.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (ArrayList, WildcardQuery, Term, BoostQuery,
    // BooleanQuery) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public final Query createMultiFieldWildQuery(@NonNull String[] fieldNames, @NonNull String queryString,
            @NonNull IndexType indexType) throws IOException {

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        List<List<Query>> fieldsQuerys = new ArrayList<>();
        Analyzer analyzer = analyzerFactory.getQueryAnalyzer();

        // TODO Support for single token fields

        /* Wildcard applies to all tokens. **/
        for (String fieldName : fieldNames) {
            try (TokenStream stream = analyzer.tokenStream(fieldName, queryString)) {
                stream.reset();
                List<Query> fieldQuerys = new ArrayList<>();
                while (stream.incrementToken()) {
                    String token = stream.getAttribute(CharTermAttribute.class).toString();
                    WildcardQuery wildcardQuery = new WildcardQuery(new Term(fieldName, token.concat(ASTERISK)));
                    if (indexType.getBoosts().containsKey(fieldName)) {
                        fieldQuerys.add(new BoostQuery(wildcardQuery, indexType.getBoosts().get(fieldName)));
                    } else {
                        fieldQuerys.add(wildcardQuery);
                    }
                }
                fieldsQuerys.add(fieldQuerys);
            }
        }

        /* If Field's Tokenizer is different, token's length may not match. **/
        int maxTermLength = fieldsQuerys.stream().map(List::size).max(Integer::compare).orElse(0);

        if (0 < fieldsQuerys.size()) {
            for (int i = 0; i < maxTermLength; i++) {
                BooleanQuery.Builder fieldsQuery = new BooleanQuery.Builder();
                for (List<Query> fieldQuerys : fieldsQuerys) {
                    if (i < fieldQuerys.size()) {
                        fieldsQuery.add(fieldQuerys.get(i), Occur.SHOULD);
                    }
                }
                mainQuery.add(fieldsQuery.build(), Occur.SHOULD);
            }
        }

        return mainQuery.build();

    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (PhraseQuery, Term, BoostQuery) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public final Query createPhraseQuery(@NonNull String[] fieldNames, @NonNull String queryString,
            @NonNull IndexType indexType) throws IOException {

        Analyzer analyzer = analyzerFactory.getQueryAnalyzer();
        BooleanQuery.Builder fieldQuerys = new BooleanQuery.Builder();

        for (String fieldName : fieldNames) {
            PhraseQuery.Builder phrase = new PhraseQuery.Builder();
            boolean exists = false;
            try (TokenStream stream = analyzer.tokenStream(fieldName, queryString)) {
                stream.reset();
                while (stream.incrementToken()) {
                    String token = stream.getAttribute(CharTermAttribute.class).toString();
                    if (!isEmpty(token)) {
                        phrase.add(new Term(fieldName, token));
                        exists = true;
                    }
                }
            }
            if (exists) {
                phrase.setSlop(1);
                if (indexType.getBoosts().containsKey(fieldName)) {
                    fieldQuerys.add(new BoostQuery(phrase.build(), indexType.getBoosts().get(fieldName) * 2),
                            Occur.SHOULD);
                } else {
                    fieldQuerys.add(phrase.build(), Occur.SHOULD);
                }
            }
        }
        return fieldQuerys.build();

    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#search(SearchCriteria, List, IndexType)}.
     * 
     * @param searchInput
     *            searchInput
     * @param includeComposer
     *            includeComposer
     * @param musicFolders
     *            musicFolders
     * @param indexType
     *            {@link IndexType}
     * 
     * @return Query
     * 
     * @throws IOException
     *             When parsing of MultiFieldQueryParser fails
     */
    public Query search(String searchInput, boolean includeComposer, List<MusicFolder> musicFolders,
            IndexType indexType) throws IOException {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        String[] fields = util.filterComposer(indexType.getFields(), includeComposer);
        Query multiFieldQuery = createMultiFieldWildQuery(fields, searchInput, indexType);
        mainQuery.add(multiFieldQuery, Occur.MUST);

        boolean isId3 = indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3;
        Query folderQuery = toFolderQuery.apply(isId3, musicFolders);
        mainQuery.add(folderQuery, Occur.MUST);

        return mainQuery.build();

    }

    public Query searchByPhrase(String searchInput, boolean includeComposer, List<MusicFolder> musicFolders,
            IndexType indexType) throws IOException {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        String[] fields = util.filterComposer(indexType.getFields(), includeComposer);
        Query multiFieldQuery = createPhraseQuery(fields, searchInput, indexType);
        mainQuery.add(multiFieldQuery, Occur.MUST);

        boolean isId3 = indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3;
        Query folderQuery = toFolderQuery.apply(isId3, musicFolders);
        mainQuery.add(folderQuery, Occur.MUST);

        return mainQuery.build();

    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomSongs(RandomSearchCriteria)}.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public Query getRandomSongs(RandomSearchCriteria criteria) throws IOException {

        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // Unanalyzed field
        query.add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name())), Occur.MUST);

        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
        Analyzer queryAnalyzer = analyzerFactory.getQueryAnalyzer();
        if (!isEmpty(criteria.getGenres())) {
            for (String genre : criteria.getGenres()) {
                if (!isEmpty(criteria.getGenres()) && !isEmpty(genre)) {
                    try (TokenStream stream = queryAnalyzer.tokenStream(FieldNamesConstants.GENRE, genre)) {
                        stream.reset();
                        while (stream.incrementToken()) {
                            String token = stream.getAttribute(CharTermAttribute.class).toString();
                            genreQuery.add(new TermQuery(new Term(FieldNamesConstants.GENRE, token)), Occur.SHOULD);
                        }
                    }
                }
            }
            query.add(genreQuery.build(), Occur.MUST);
        }

        if (!(isEmpty(criteria.getFromYear()) && isEmpty(criteria.getToYear()))) {
            query.add(toYearRangeQuery.apply(criteria.getFromYear(), criteria.getToYear()), Occur.MUST);
        }

        query.add(toFolderQuery.apply(false, criteria.getMusicFolders()), Occur.MUST);

        return query.build();

    }

    /**
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomSongs(int, int, int, List)}.
     * 
     * @param musicFolders
     *            musicFolders
     * 
     * @return Query
     */
    public Query getRandomSongs(List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name())), Occur.MUST)
                .add(toFolderQuery.apply(false, musicFolders), Occur.MUST).build();
    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#searchByName( String, String, int, int, List, Class)}.
     * 
     * @param fieldName
     *            {@link FieldNamesConstants}
     * 
     * @return Query
     * 
     * @throws IOException
     *             When parsing of QueryParser fails
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term, WildcardQuery) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public Query searchByName(String fieldName, String name) throws IOException {

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        Analyzer analyzer = analyzerFactory.getQueryAnalyzer();

        // TODO #353
        // TODO Support for extended fields and boost
        try (TokenStream stream = analyzer.tokenStream(fieldName, name)) {
            stream.reset();
            stream.incrementToken();

            /*
             * XXX 3.x -> 8.x : In order to support wildcards, QueryParser has been replaced by the following process.
             */

            /* Wildcards apply only to tail tokens **/
            while (true) {
                String token = stream.getAttribute(CharTermAttribute.class).toString();
                if (stream.incrementToken()) {
                    mainQuery.add(new TermQuery(new Term(fieldName, token)), Occur.SHOULD);
                } else {
                    WildcardQuery wildcardQuery = new WildcardQuery(new Term(fieldName, token.concat(ASTERISK)));
                    mainQuery.add(wildcardQuery, Occur.SHOULD);
                    break;
                }
            }

        }

        return mainQuery.build();

    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomAlbums(int, List)}.
     * 
     * @param musicFolders
     *            musicFolders
     * 
     * @return Query
     */
    public Query getRandomAlbums(List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder().add(toFolderQuery.apply(false, musicFolders), Occur.SHOULD).build();
    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomAlbumsId3(int, List)}.
     * 
     * @param musicFolders
     *            musicFolders
     * 
     * @return Query
     */
    public Query getRandomAlbumsId3(List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder().add(toFolderQuery.apply(true, musicFolders), Occur.SHOULD).build();
    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getAlbumId3sByGenre(String, int, int, List)}
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public Query getAlbumId3sByGenres(String genres, List<MusicFolder> musicFolders) throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        if (!isEmpty(genres)) {
            BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
            try (TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(FieldNamesConstants.GENRE,
                    genres)) {
                stream.reset();
                while (stream.incrementToken()) {
                    genreQuery.add(new TermQuery(new Term(FieldNamesConstants.GENRE,
                            stream.getAttribute(CharTermAttribute.class).toString())), Occur.SHOULD);
                }
            }
            query.add(genreQuery.build(), Occur.MUST);
        }

        // sub - folder
        BooleanQuery.Builder folderQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> folderQuery.add(
                new TermQuery(new Term(FieldNamesConstants.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD));
        query.add(folderQuery.build(), Occur.MUST);

        return query.build();

    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getSongsByGenre(String, int, int, List)} Query generation
     * expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getAlbumsByGenre(int, int, String, List)}
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public Query getMediasByGenres(String genres, List<MusicFolder> musicFolders) throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        if (!isEmpty(genres)) {
            BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
            try (TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(FieldNamesConstants.GENRE,
                    genres)) {
                stream.reset();
                while (stream.incrementToken()) {
                    genreQuery.add(new TermQuery(new Term(FieldNamesConstants.GENRE,
                            stream.getAttribute(CharTermAttribute.class).toString())), Occur.SHOULD);
                }
            }
            query.add(genreQuery.build(), Occur.MUST);
        }

        // sub - folder
        BooleanQuery.Builder folderQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> folderQuery.add(
                new TermQuery(new Term(FieldNamesConstants.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD));
        query.add(folderQuery.build(), Occur.MUST);

        return query.build();

    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public Query toPreAnalyzedGenres(List<String> genres) throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();

        for (String genre : genres) {
            if (!isEmpty(genre)) {
                try (TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(FieldNamesConstants.GENRE,
                        genre)) {
                    stream.reset();
                    while (stream.incrementToken()) {
                        genreQuery.add(new TermQuery(new Term(FieldNamesConstants.GENRE,
                                stream.getAttribute(CharTermAttribute.class).toString())), Occur.SHOULD);
                    }
                }
            }
        }
        query.add(genreQuery.build(), Occur.MUST);

        return query.build();
    }

    public Query getGenre(String genre) throws IOException {
        return new BooleanQuery.Builder().add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.SHOULD)
                .build();
    }

}
