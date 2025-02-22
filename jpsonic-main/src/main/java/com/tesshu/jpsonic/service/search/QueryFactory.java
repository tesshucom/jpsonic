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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.SettingsService;
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

    private final SettingsService settingsService;
    private final AnalyzerFactory analyzerFactory;

    private Query toFolderIdQuery(MusicFolder folder) {
        // Unanalyzed field
        return new TermQuery(new Term(FieldNamesConstants.FOLDER_ID, folder.getId().toString()));
    }

    private Query toFolderPathQuery(MusicFolder folder) {
        // Unanalyzed field
        return new TermQuery(new Term(FieldNamesConstants.FOLDER, folder.getPathString()));
    }

    public @NonNull Query createFolderQuery(@NonNull Boolean isId3, @NonNull List<MusicFolder> folders) {
        BooleanQuery.Builder mfQuery = new BooleanQuery.Builder();
        folders.stream().map(isId3 ? this::toFolderIdQuery : this::toFolderPathQuery)
                .forEach(t -> mfQuery.add(t, Occur.SHOULD));
        return mfQuery.build();
    }

    private @NonNull Query createYearRangeQuery(@Nullable Integer from, @Nullable Integer to) {
        return IntPoint.newRangeQuery(FieldNamesConstants.YEAR, isEmpty(from) ? Integer.MIN_VALUE : from,
                isEmpty(to) ? Integer.MAX_VALUE : to);
    }

    public QueryFactory(SettingsService settingsService, AnalyzerFactory analyzerFactory) {
        super();
        this.settingsService = settingsService;
        this.analyzerFactory = analyzerFactory;
    }

    /**
     * Exclude the search field from the specified field depending on the condition. If necessary, exclude fields
     * related to Composer from the fields to be searched. Also, unnecessary fields are excluded according to the value
     * of Index Scheme.
     *
     * @param fields
     *            Field to search
     * @param includeComposer
     *            Whether to include fields related to Composer in the search. The judgment method may differ depending
     *            on the protocol. In the case of HTTP, personal settings are also considered. For UPnP, follow server
     *            settings.
     *
     * @return Final search target field
     */
    List<String> filterFields(List<String> fields, boolean includeComposer) {
        IndexScheme scheme = IndexScheme.of(settingsService.getIndexSchemeName());
        return fields.stream() //
                .filter(field -> includeComposer || !(FieldNamesConstants.COMPOSER.equals(field) //
                        || FieldNamesConstants.COMPOSER_READING.equals(field) //
                        || FieldNamesConstants.COMPOSER_READING_ROMANIZED.equals(field)))
                .filter(field -> scheme != IndexScheme.ROMANIZED_JAPANESE
                        && !(FieldNamesConstants.ARTIST_READING_ROMANIZED.equals(field) //
                                || FieldNamesConstants.COMPOSER_READING_ROMANIZED.equals(field))
                        || scheme == IndexScheme.ROMANIZED_JAPANESE)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (PhraseQuery, Term) Not reusable
    private Optional<Query> createFieldQuery(@NonNull String fieldName, @NonNull String queryString)
            throws IOException {
        PhraseQuery.Builder phrase = new PhraseQuery.Builder();
        BooleanQuery.Builder ngram = new BooleanQuery.Builder();
        boolean isRomanizedField = FieldNamesConstants.ARTIST_READING_ROMANIZED.equals(fieldName)
                || FieldNamesConstants.COMPOSER_READING_ROMANIZED.equals(fieldName);

        try (TokenStream stream = analyzerFactory.getAnalyzer()
                .tokenStream(isRomanizedField ? FieldNamesConstants.ARTIST_READING : fieldName, queryString)) {
            stream.reset();
            while (stream.incrementToken()) {
                String token = stream.getAttribute(CharTermAttribute.class).toString();
                if (isRomanizedField) {
                    ngram.add(new TermQuery(new Term(fieldName, token)), Occur.SHOULD);
                } else {
                    phrase.add(new Term(fieldName, token));
                    phrase.setSlop(1);
                }
            }
        }

        Query query = isRomanizedField ? ngram.build() : phrase.build();
        boolean isEmptyQuery = "\"\"".equals(query.toString());
        if (isEmptyQuery) {
            return Optional.empty();
        }
        return Optional.of(query);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (BoostQuery) Not reusable
    private Optional<Query> createPhraseQuery(List<String> fieldNames, boolean includeComposer,
            @NonNull String queryString, @NonNull IndexType indexType) throws IOException {

        List<String> targetFields = filterFields(fieldNames, includeComposer);
        if (targetFields.isEmpty()) {
            return Optional.empty();
        }

        BooleanQuery.Builder fieldQuerys = new BooleanQuery.Builder();
        for (String fieldName : targetFields) {
            Optional<Query> query = createFieldQuery(fieldName, queryString);
            query.ifPresent(q -> {
                fieldQuerys.add(indexType.getBoosts().containsKey(fieldName)
                        ? new BoostQuery(q, indexType.getBoosts().get(fieldName) * 2)
                        : q, Occur.SHOULD);
            });
        }

        return Optional.of(fieldQuerys.build());
    }

    // Called by UPnP
    public Optional<Query> createPhraseQuery(List<String> targetFields, @NonNull String queryString,
            @NonNull IndexType indexType) throws IOException {
        return createPhraseQuery(targetFields, settingsService.isSearchComposer(), queryString, indexType);
    }

    // Called by HTTP
    public Query searchByPhrase(@NonNull String searchInput, boolean includeComposer,
            @NonNull List<MusicFolder> musicFolders, @NonNull IndexType indexType) throws IOException {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        Optional<Query> multiFieldQuery = createPhraseQuery(indexType.getFields(), searchInput, indexType);
        multiFieldQuery.ifPresent(q -> mainQuery.add(q, Occur.MUST));

        boolean isId3 = indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3;
        Query folderQuery = createFolderQuery(isId3, musicFolders);
        mainQuery.add(folderQuery, Occur.MUST);

        return mainQuery.build();
    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomSongs(RandomSearchCriteria)}.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    public Query getRandomSongs(@NonNull RandomSearchCriteria criteria) throws IOException {

        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // Unanalyzed field
        query.add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name())), Occur.MUST);

        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
        if (!isEmpty(criteria.getGenres())) {
            for (String genre : criteria.getGenres()) {
                try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(FieldNamesConstants.GENRE, genre)) {
                    stream.reset();
                    while (stream.incrementToken()) {
                        String token = stream.getAttribute(CharTermAttribute.class).toString();
                        genreQuery.add(new TermQuery(new Term(FieldNamesConstants.GENRE, token)), Occur.SHOULD);
                    }
                }
            }
            query.add(genreQuery.build(), Occur.MUST);
        }

        if (!(isEmpty(criteria.getFromYear()) && isEmpty(criteria.getToYear()))) {
            query.add(createYearRangeQuery(criteria.getFromYear(), criteria.getToYear()), Occur.MUST);
        }

        query.add(createFolderQuery(false, criteria.getMusicFolders()), Occur.MUST);

        return query.build();

    }

    /**
     * {@link com.tesshu.jpsonic.service.SearchService#getRandomSongs(int, int, int, List, String...)}.
     *
     * @param musicFolders
     *            musicFolders
     *
     * @return Query
     */
    public Query getRandomSongs(@NonNull List<MusicFolder> musicFolders, String... genres) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder()
                .add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name())), Occur.MUST)
                .add(createFolderQuery(false, musicFolders), Occur.MUST);
        if (genres.length > 0) {
            BooleanQuery.Builder genreQueryBuilder = new BooleanQuery.Builder();
            Stream.of(genres).forEach(genre -> genreQueryBuilder
                    .add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.SHOULD));
            builder.add(genreQueryBuilder.build(), Occur.MUST);
        }
        return builder.build();
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
    public Query getRandomAlbums(@NonNull List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder().add(createFolderQuery(false, musicFolders), Occur.SHOULD).build();
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
    public Query getRandomAlbumsId3(@NonNull List<MusicFolder> musicFolders) {
        return new BooleanQuery.Builder().add(createFolderQuery(true, musicFolders), Occur.SHOULD).build();
    }

    /**
     * Query generation expression extracted from
     * {@link com.tesshu.jpsonic.service.SearchService#getAlbumId3sByGenre(String, int, int, List)}
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    public Query getAlbumId3sByGenres(@Nullable String genres, @NonNull List<MusicFolder> musicFolders)
            throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        if (!isEmpty(genres)) {
            BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
            try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(FieldNamesConstants.GENRE, genres)) {
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
    public Query getMediasByGenres(@Nullable String genres, @NonNull List<MusicFolder> musicFolders)
            throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        if (!isEmpty(genres)) {
            BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
            try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(FieldNamesConstants.GENRE, genres)) {
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
        musicFolders.forEach(musicFolder -> folderQuery
                .add(new TermQuery(new Term(FieldNamesConstants.FOLDER, musicFolder.getPathString())), Occur.SHOULD));
        query.add(folderQuery.build(), Occur.MUST);

        return query.build();

    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TermQuery, Term) Not reusable
    public Query toPreAnalyzedGenres(@NonNull List<String> genres) throws IOException {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();

        for (String genre : genres) {
            if (!isEmpty(genre)) {
                try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(FieldNamesConstants.GENRE, genre)) {
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

    public Query getGenre(@NonNull String genre) {
        return new BooleanQuery.Builder().add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.SHOULD)
                .build();
    }

    public Query getAlbumId3GenreCount(@NonNull String genre, List<MusicFolder> folders) {
        return new BooleanQuery.Builder().add(createFolderQuery(true, folders), Occur.MUST)
                .add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.MUST).build();
    }

    private Query getTypesQuery(MediaType... types) {
        assert types != null && types.length > 0;
        BooleanQuery.Builder typeQuery = new BooleanQuery.Builder();
        Stream.of(types).forEach(type -> typeQuery
                .add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, type.name())), Occur.SHOULD));
        return typeQuery.build();
    }

    public Query getSongGenreCount(@NonNull String genre, List<MusicFolder> folders, MediaType... types) {
        return new BooleanQuery.Builder().add(createFolderQuery(false, folders), Occur.MUST)
                .add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.MUST)
                .add(getTypesQuery(types), Occur.MUST).build();
    }

    public Query getAlbumChildren(Album album, String genre, List<MusicFolder> folders, MediaType... types) {
        assert types != null && types.length > 0;
        BooleanQuery.Builder typeQuery = new BooleanQuery.Builder();
        Stream.of(types).forEach(type -> typeQuery
                .add(new TermQuery(new Term(FieldNamesConstants.MEDIA_TYPE, type.name())), Occur.SHOULD));
        return new BooleanQuery.Builder()

                .add(new TermQuery(new Term(FieldNamesConstants.GENRE, genre)), Occur.MUST)
                .add(createFolderQuery(false, folders), Occur.MUST).add(typeQuery.build(), Occur.MUST).build();

    }
}
