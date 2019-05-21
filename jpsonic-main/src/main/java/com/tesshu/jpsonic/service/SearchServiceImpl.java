/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) Jpsonic Based upon Airsonic, 
 Copyright 2016 (C) Airsonic Authors Based upon Subsonic, 
 Copyright 2009 (C) Sindre Mehus
 */
package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.service.search.*;
import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.domain.*;
import org.airsonic.player.service.SearchService;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.tesshu.jpsonic.service.search.IndexType.*;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Performs Lucene-based searching and indexing.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private QueryFactory queryFactory;
    
    @Autowired
    private IndexManager deligate;

    @Autowired
    private SearchServiceTermination termination;

    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public String getVersion() {
        return deligate.getVersion();
    }

    @Override
    public final void startIndexing() {
        deligate.startIndexing();
    }

    @Override
    public void index(MediaFile mediaFile) {
        deligate.index(mediaFile);
    }

    @Override
    public void index(Artist artist, MusicFolder musicFolder) {
        deligate.index(artist, musicFolder);
    }

    @Override
    public void index(Album album) {
        deligate.index(album);
    }

    @Override
    public void stopIndexing() {
        deligate.stopIndexing();
    }

    @Override
    public List<Genre> getGenres(boolean sortByAlbum) {
        return deligate.getGenres(sortByAlbum);
    }

    @Override
    public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {

        final int offset = criteria.getOffset();
        final int count = criteria.getCount();
        final SearchResult result = new SearchResult();
        result.setOffset(offset);

        if (count <= 0) {
            return result;
        }

        final Query query = queryFactory.search(criteria, musicFolders, indexType);
        IndexSearcher searcher = deligate.getSearcher(indexType);
        if(isEmpty(searcher)) {
            return result;
        }

        try {

            TopDocs topDocs = searcher.search(query, offset + count);

            int totalHits = termination.round.apply(topDocs.totalHits);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                termination.addIfAnyMatch(result, indexType, searcher.doc(topDocs.scoreDocs[i].doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            deligate.release(indexType, searcher);
        }
        return result;
    }

    @Override
    public <T> ParamSearchResult<T> searchByName(String name, int offset, int count, List<MusicFolder> folderList, Class<T> assignableClass) {

        // we only support album, artist, and song for now
        @Nullable
        IndexType indexType = termination.getIndexType.apply(assignableClass);
        @Nullable
        String fieldName = termination.getFieldName.apply(assignableClass);

        ParamSearchResult<T> result = new ParamSearchResult<T>();
        result.setOffset(offset);

        if (isEmpty(indexType) || isEmpty(fieldName) || count <= 0) {
            return result;
        }

        Query query = queryFactory.searchByName(name, folderList, indexType);
        IndexSearcher searcher = deligate.getSearcher(indexType);
        if(isEmpty(searcher)) {
            return result;
        }

        try {
            SortField[] sortFields = Arrays.stream(indexType.getFields())
                    .map(n -> new SortField(n, SortField.Type.STRING))
                    .filter(n -> !FieldNames.FOLDER.equals(n.toString()) && !FieldNames.FOLDER_ID.equals(n.toString()))
                    .toArray(i -> new SortField[i]);
            Sort sort = new Sort(sortFields);
            TopDocs topDocs = searcher.search(query, offset + count, sort);

            int totalHits = termination.round.apply(topDocs.totalHits);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                termination.addIgnoreNull(result, indexType, termination.getId.apply(doc), assignableClass);
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            deligate.release(indexType, searcher);
        }
        return result;
    }

    /**
     * 
     * @param count Number of albums to return.
     * @param searcher
     * @param query
     * @param id2ListCallBack Callback to get D from id and store it in List
     * @return result
     * @throws IOException
     */
    private final <D> List<D> createRandomFiles(
            int count,
            IndexSearcher searcher,
            Query query,
            BiConsumer<List<D>, Integer> id2ListCallBack) throws IOException{

        List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs)
                .map(sd -> sd.doc)
                .collect(Collectors.toList());

        List<D> result = new ArrayList<>();
        while (!docs.isEmpty() && result.size() < count) {
            int randomPos = random.nextInt(docs.size());
            Document document = searcher.doc(docs.get(randomPos));
            id2ListCallBack.accept(result, termination.getId.apply(document));
            docs.remove(randomPos);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

        final Query query = queryFactory.getRandomSongs(criteria);
        IndexSearcher searcher = deligate.getSearcher(SONG);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(criteria.getCount(), searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM, id));
        } catch (IOException e) {
            LOG.error("Failed to search or random songs.", e);
        } finally {
            deligate.release(SONG, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbums(musicFolders);
        IndexSearcher searcher = deligate.getSearcher(ALBUM);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM, id));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            deligate.release(ALBUM, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);
        IndexSearcher searcher = deligate.getSearcher(ALBUM_ID3);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM_ID3, id));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            deligate.release(ALBUM_ID3, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<Album> getAlbumId3sByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {

        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getAlbumId3sByGenres(genres, musicFolders);
        IndexSearcher searcher = deligate.getSearcher(ALBUM_ID3);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<Album> result = new ArrayList<>();
        try {
            SortField[] sortFields = Arrays.stream(ALBUM_ID3.getFields())
                    .filter(n -> n.equals(FieldNames.FOLDER_ID))
                    .map(n -> new SortField(n, SortField.Type.STRING))
                    .toArray(i -> new SortField[i]);
            Sort sort = new Sort(sortFields);
            TopDocs topDocs = searcher.search(query, offset + count, sort);

            int totalHits = termination.round.apply(topDocs.totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                termination.addAlbumId3IfAnyMatch.accept(result, termination.getId.apply(doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            deligate.release(ALBUM_ID3, searcher);
        }
        return result;

    }

    private List<MediaFile> getMediasByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders,
            IndexType indexType, SortField[] sortFields) {

        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getMediasByGenres(genres, musicFolders);
        IndexSearcher searcher = deligate.getSearcher(indexType);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<MediaFile> result = new ArrayList<>();
        try {
            Sort sort = new Sort(sortFields);
            TopDocs topDocs = searcher.search(query, offset + count, sort);

            int totalHits = termination.round.apply(topDocs.totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                termination.addMediaFileIfAnyMatch.accept(result, termination.getId.apply(doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            deligate.release(indexType, searcher);
        }
        return result;

    }

    @Override
    public List<MediaFile> getAlbumsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        SortField[] sortFields = Arrays.stream(ALBUM.getFields())
                .filter(n -> n.equals(FieldNames.FOLDER))
                .map(n -> new SortField(n, SortField.Type.STRING))
                .toArray(i -> new SortField[i]);
        return getMediasByGenres(genres, offset, count, musicFolders, ALBUM, sortFields);
    }

    @Override
    public List<MediaFile> getSongsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        SortField[] sortFields = Arrays.stream(SONG.getFields())
                .map(n -> new SortField(n, SortField.Type.STRING))
                .toArray(i -> new SortField[i]);
        return getMediasByGenres(genres, offset, count, musicFolders, SONG, sortFields);
    }

}