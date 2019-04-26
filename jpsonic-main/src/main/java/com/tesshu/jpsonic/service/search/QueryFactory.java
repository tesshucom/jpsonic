/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Factory class of Lucene Query.
 * This class is an extract of the functionality that was once part of SearchService.
 * It is for maintainability and verification.
 * 
 * Each corresponds to the SearchService method.
 * Therefore, when the version of lucene changes greatly,
 * verification with query grammar is possible.
 **/
@Component
public class QueryFactory {

    private static Analyzer analyzer =  AnalyzerFactory.getInstance().getQueryAnalyzer();

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#search(SearchCriteria, List, IndexType)}
     * 
     * @param criteria
     * @param musicFolders
     * @param indexType
     * @return Query
     */
    public Query search(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {

        /* FOLDER is not included in all searches. */
        String[] targetFields = Arrays.stream(indexType.getFields())
            .filter(field -> !field.equals(FieldNames.FOLDER))
            .filter(field -> !field.equals(FieldNames.FOLDER_ID))
            .filter(field -> !field.equals(FieldNames.GENRE))
            .toArray(i -> new String[i]);

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        BooleanQuery.Builder subFieldsQuery = new BooleanQuery.Builder();
        Arrays.stream(targetFields).forEach(fieldName -> {
            TokenStream stream = analyzer.tokenStream(fieldName, criteria.getQuery());
            try {
                stream.reset();
                while (stream.incrementToken()) {
                    String txt = stream.getAttribute(CharTermAttribute.class).toString();
                    WildcardQuery wildcardQuery = new WildcardQuery(new Term(fieldName, txt));
                    if(indexType.getBoosts().containsKey(fieldName)) {
                        subFieldsQuery.add(new BoostQuery(wildcardQuery, indexType.getBoosts().get(fieldName)), Occur.SHOULD);
                    }else {
                        subFieldsQuery.add(wildcardQuery, Occur.SHOULD);
                    }
                }
                stream.close();
            } catch (IOException e) {
                // error case difficult to predict..
                LoggerFactory.getLogger(QueryFactory.class).warn("Error during query analysis.", e);
            }
        });
        mainQuery.add(subFieldsQuery.build(), Occur.MUST );

        BooleanQuery.Builder subMusicFoldersQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> {
            if (indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3) {
                subMusicFoldersQuery.add(new TermQuery(new Term(FieldNames.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD);
            } else {
                subMusicFoldersQuery.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD);
            }
        });
        mainQuery.add(subMusicFoldersQuery.build(), Occur.MUST);

        return mainQuery.build();

    }

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getRandomSongs(RandomSearchCriteria)}
     * @param criteria
     * @return
     */
    public Query getRandomSongs(RandomSearchCriteria criteria) {

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        booleanQuery.add(new TermQuery(new Term(FieldNames.MEDIA_TYPE, MediaType.MUSIC.name())), Occur.MUST);

        String genre = criteria.getGenre();
        if (!isEmpty(criteria.getGenre())) {
            try {
                if (!isEmpty(genre)) {
                    TokenStream stream = AnalyzerFactory.getInstance().getAnalyzer().tokenStream(FieldNames.GENRE, genre);
                    stream.reset();
                    while (stream.incrementToken()) {
                        genre = stream.getAttribute(CharTermAttribute.class).toString();
                    }
                    stream.close();
                }
            } catch (IOException e) {
                // error case difficult to predict..
                LoggerFactory.getLogger(QueryFactory.class).warn("Error during query analysis.", e);
            }
            booleanQuery.add(new TermQuery(new Term(FieldNames.GENRE, genre)), Occur.MUST);
        }

        if (!(isEmpty(criteria.getFromYear()) && isEmpty(criteria.getToYear()))) {
            booleanQuery.add(IntPoint.newRangeQuery(FieldNames.YEAR, 
                isEmpty(criteria.getFromYear())
                    ? Integer.MIN_VALUE
                    : criteria.getFromYear(),
                isEmpty(criteria.getToYear())
                    ? Integer.MAX_VALUE :
                    criteria.getToYear()),
                Occur.MUST);
        }

        BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
        criteria.getMusicFolders().forEach(musicFolder ->
            subQuery.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD));
        booleanQuery.add(subQuery.build(), Occur.MUST);

        return booleanQuery.build();

    }

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#searchByName(String, int, int, List, Class)}
     * @param name
     * @param fieldName
     * @return
     */
    public Query searchByName(String name, List<MusicFolder> musicFolders, IndexType indexType) {

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        BooleanQuery.Builder subFieldsQuery = new BooleanQuery.Builder();
        
        /* FOLDER is not included in all searches. */
        String[] targetFields = Arrays.stream(indexType.getFields())
            .filter(field -> !field.equals(FieldNames.FOLDER))
            .filter(field -> !field.equals(FieldNames.FOLDER_ID))
            .toArray(i -> new String[i]);

        Arrays.stream(targetFields).forEach(fieldName -> {
            TokenStream stream = analyzer.tokenStream(fieldName, name);
            try {
                stream.reset();
                while (stream.incrementToken()) {
                    String txt = stream.getAttribute(CharTermAttribute.class).toString();
                    WildcardQuery wildcardQuery = new WildcardQuery(new Term(fieldName, txt));
                    if (indexType.getBoosts().containsKey(fieldName)) {
                        subFieldsQuery.add(new BoostQuery(wildcardQuery, indexType.getBoosts().get(fieldName)), Occur.SHOULD);
                    } else {
                        subFieldsQuery.add(wildcardQuery, Occur.SHOULD);
                    }
                }
                stream.close();
            } catch (IOException e) {
                // error case difficult to predict..
                LoggerFactory.getLogger(QueryFactory.class).warn("Error during query analysis.", e);
            }
        });

        mainQuery.add(subFieldsQuery.build(), Occur.MUST);
        BooleanQuery.Builder subMusicFoldersQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> {
            if (indexType == IndexType.SONG) {
                subMusicFoldersQuery.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD);
            } else {
                subMusicFoldersQuery.add(new TermQuery(new Term(FieldNames.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD);
            }
        });
        mainQuery.add(subMusicFoldersQuery.build(), Occur.MUST);
        return mainQuery.build();
    }

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getRandomAlbums(int, List)}
     * @param musicFolders
     * @return
     */
    public Query getRandomAlbums(List<MusicFolder> musicFolders) {

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder ->
            subQuery.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD));
        booleanQuery.add(subQuery.build(), Occur.MUST);

        return booleanQuery.build();

    }

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getRandomAlbumsId3(int, List)}
     * @param musicFolders
     * @return
     */
    public Query getRandomAlbumsId3(List<MusicFolder> musicFolders) {

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder ->
            subQuery.add(new TermQuery(new Term(FieldNames.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD));
        booleanQuery.add(subQuery.build(), Occur.MUST);

        return booleanQuery.build();

    }
    
    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getAlbumId3sByGenre(String, int, int, List)}
     * @param musicFolders
     */
    public Query getAlbumId3sByGenre(String genre, List<MusicFolder> musicFolders) {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
        try {
            if (!isEmpty(genre)) {
                TokenStream stream = analyzer.tokenStream(FieldNames.GENRE, genre);
                stream.reset();
                while (stream.incrementToken()) {
                    genreQuery.add(new TermQuery(new Term(FieldNames.GENRE, stream.getAttribute(CharTermAttribute.class).toString())), Occur.SHOULD);
                }
                stream.close();
            }
        } catch (IOException e) {
            // error case difficult to predict..
            LoggerFactory.getLogger(QueryFactory.class).warn("Error during query analysis.", e);
        }
        query.add(genreQuery.build(), Occur.MUST);

        // sub - folder
        BooleanQuery.Builder folderQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> folderQuery.add(new TermQuery(new Term(FieldNames.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD));
        query.add(folderQuery.build(), Occur.MUST);

        return query.build();

    }

    /**
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getSongsByGenre(String, int, int, List)}
     * Query generation expression extracted from {@link org.airsonic.player.service.SearchService#getAlbumsByGenre(int, int, String, List)}
     * @param musicFolders
     * @return
     */
    public Query getMediasByGenre(String genre, List<MusicFolder> musicFolders) {

        // main
        BooleanQuery.Builder query = new BooleanQuery.Builder();

        // sub - genre
        BooleanQuery.Builder genreQuery = new BooleanQuery.Builder();
        try {
            if (!isEmpty(genre)) {
                TokenStream stream = analyzer.tokenStream(FieldNames.GENRE, genre);
                stream.reset();
                while (stream.incrementToken()) {
                    genreQuery.add(new TermQuery(new Term(FieldNames.GENRE, stream.getAttribute(CharTermAttribute.class).toString())), Occur.SHOULD);
                }
                stream.close();
            }
        } catch (IOException e) {
            // error case difficult to predict..
            LoggerFactory.getLogger(QueryFactory.class).warn("Error during query analysis.", e);
        }
        query.add(genreQuery.build(), Occur.MUST);

        // sub - folder
        BooleanQuery.Builder folderQuery = new BooleanQuery.Builder();
        musicFolders.forEach(musicFolder -> folderQuery.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD));
        query.add(folderQuery.build(), Occur.MUST);

        return query.build();

    }
}
