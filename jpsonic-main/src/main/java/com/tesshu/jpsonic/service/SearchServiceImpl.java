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

import org.airsonic.player.dao.*;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.util.FileUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private MediaFileDao mediaFileDao;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private SearchServiceTermination termination;

    /**
     * @since 101.1.0
     */
    public String INDEX_PRODUCT_VERSION = "7.7.1";

    /**
     * @since 101.1.0
     */
    public String INDEX_FILE_SUFFIX = "jp";

    private static final Map<IndexType, SearcherManager> searcherManagerMap = new ConcurrentHashMap<>();

    private final Random random = new Random(System.currentTimeMillis());
    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;

    @Override
    public String getVersion() {
        return INDEX_FILE_PREFIX + INDEX_PRODUCT_VERSION + INDEX_FILE_SUFFIX
                + "-" + IndexType.getVersion()
                + "-" + DocumentFactory.getVersion();
    }

    private final IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File dir = termination.getDirectory.apply(getVersion(), indexType);
        IndexWriterConfig config = new IndexWriterConfig(AnalyzerFactory.getInstance().getAnalyzer());
        return new IndexWriter(FSDirectory.open(dir.toPath()), config);
    }

    @Override
    public final void startIndexing() {
        try {
            artistWriter = createIndexWriter(ARTIST);
            artistId3Writer = createIndexWriter(ARTIST_ID3);
            albumWriter = createIndexWriter(ALBUM);
            albumId3Writer = createIndexWriter(ALBUM_ID3);
            songWriter = createIndexWriter(SONG);
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        }
    }

    @Override
    public void index(MediaFile mediaFile) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(mediaFile.getId()));
            if (mediaFile.isFile()) {
                songWriter.updateDocument(term, documentFactory.createDocument(SONG, mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.updateDocument(term, documentFactory.createDocument(ALBUM, mediaFile));
            } else {
                artistWriter.updateDocument(term, documentFactory.createDocument(ARTIST, mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    @Override
    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(artist.getId()));
            artistId3Writer.updateDocument(term, documentFactory.createDocument(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    @Override
    public void index(Album album) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(album.getId()));
            albumId3Writer.updateDocument(term, documentFactory.createDocument(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    @Override
    public void updateArtistSort(Album album) {
        try {
            if (isEmpty(album.getArtistSort())) {
                Term term = new Term(FieldNames.ID, Integer.toString(album.getId()));
                Field field = new TextField(FieldNames.ARTIST_READING, album.getArtistSort(), Store.YES);
                Field field2 = new SortedDocValuesField(FieldNames.ARTIST_READING, new BytesRef(album.getArtistSort()));
                albumId3Writer.updateDocValues(term, field, field2);
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    private void stopIndexing(IndexWriter writer) {
        try {
            writer.flush();
            writer.close();
            LOG.info("Success to create or update search index : [" + writer + "]");
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            FileUtil.closeQuietly(writer);
        }
    }

    @Override
    public void stopIndexing() {

        stopIndexing(artistWriter);
        stopIndexing(artistId3Writer);
        stopIndexing(albumWriter);
        stopIndexing(albumId3Writer);
        stopIndexing(songWriter);

        if (0 < searcherManagerMap.size()) {
            searcherManagerMap.keySet().stream().forEach(key -> {
                try {
                    searcherManagerMap.get(key).maybeRefresh();
                } catch (IOException e) {
                    LOG.error("Failed to refresh IndexSearcher.", e);
                    searcherManagerMap.remove(key);
                }
            });
            LOG.info("SearcherManager has been refreshed.");
        }

    }

    private /* @Nullable */ IndexSearcher getSearcher(IndexType indexType) {
        if (!searcherManagerMap.containsKey(indexType)) {
            synchronized (searcherManagerMap) {
                File indexDirectory = termination.getDirectory.apply(getVersion(), indexType);
                try {
                    if (indexDirectory.exists()) {
                        SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory.toPath()), null);
                        searcherManagerMap.put(indexType, manager);
                    } else {
                        LOG.warn("{} does not exist. Please run a scan.", indexDirectory.getAbsolutePath());
                    }
                } catch (IOException e) {
                    LOG.error("Failed to initialize SearcherManager.", e);
                }
            }
        }
        try {
            return searcherManagerMap.get(indexType).acquire();
        } catch (Exception e) {
            LOG.warn("Failed to acquire IndexSearcher.", e);
        }
        return null;
    }

    private void release(IndexType indexType, IndexSearcher indexSearcher) {
        if (searcherManagerMap.containsKey(indexType)) {
            try {
                searcherManagerMap.get(indexType).release(indexSearcher);
            } catch (IOException e) {
                LOG.error("Failed to release IndexSearcher.", e);
                searcherManagerMap.remove(indexType);
            }
        }
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
        IndexSearcher searcher = getSearcher(indexType);
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
            release(indexType, searcher);
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
        IndexSearcher searcher = getSearcher(indexType);
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
            release(indexType, searcher);
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
        IndexSearcher searcher = getSearcher(SONG);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(criteria.getCount(), searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM, id));
        } catch (IOException e) {
            LOG.error("Failed to search or random songs.", e);
        } finally {
            release(SONG, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbums(musicFolders);
        IndexSearcher searcher = getSearcher(ALBUM);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM, id));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            release(ALBUM, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);
        IndexSearcher searcher = getSearcher(ALBUM_ID3);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query, (dist, id) -> termination.addIgnoreNull(dist, ALBUM_ID3, id));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            release(ALBUM_ID3, searcher);
        }

        return Collections.emptyList();

    }

    @Override
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

    @Override
    public void updateGenres() {

        Map<String, Genre> genres = new HashMap<String, Genre>();

        // song count
        IndexSearcher searcher = getSearcher(SONG);
        try {
            if (0 != searcher.getIndexReader().getDocCount(FieldNames.GENRE)) {
                TermsEnum termEnum = MultiFields.getTerms(searcher.getIndexReader(), FieldNames.GENRE).iterator();
                BytesRef bytesRef = termEnum.next();
                while (!isEmpty(bytesRef)) {
                    String name = bytesRef.utf8ToString();
                    genres.put(name, new Genre(name));
                    bytesRef = termEnum.next();
                }
                Iterator<Genre> values = genres.values().iterator();
                while (values.hasNext()) {
                    String name = values.next().getName();
                    Query query = queryFactory.getMediasForGenreCount(name, true);
                    int count = searcher.count(query);
                    genres.get(name).setSongCount(count);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to update song count for genres.", e);
        } finally {
            release(SONG, searcher);
        }

        // album count
        searcher = getSearcher(ALBUM);
        try {
            if (0 != searcher.getIndexReader().getDocCount(FieldNames.GENRE)) {
                TermsEnum termEnum = MultiFields.getTerms(searcher.getIndexReader(), FieldNames.GENRE).iterator();
                BytesRef bytesRef = termEnum.next();
                while (!isEmpty(bytesRef)) {
                    String name = bytesRef.utf8ToString();
                    if (!genres.containsKey(name)) {
                        genres.put(name, new Genre(name));
                    }
                    bytesRef = termEnum.next();
                }
                Iterator<Genre> values = genres.values().iterator();
                while (values.hasNext()) {
                    String name = values.next().getName();
                    Query query = queryFactory.getMediasForGenreCount(name, false);
                    int count = searcher.count(query);
                    genres.get(name).setAlbumCount(count);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to update song count for genres.", e);
        } finally {
            release(ALBUM, searcher);
        }

        // update db
        mediaFileDao.updateGenres(genres.values().stream()
                .collect(Collectors.toList()));
    }

    @Override
    public List<Album> getAlbumId3sByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {

        if (isEmpty(genre)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getAlbumId3sByGenre(genre, musicFolders);
        IndexSearcher searcher = getSearcher(ALBUM_ID3);
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
            release(ALBUM_ID3, searcher);
        }
        return result;

    }

    private List<MediaFile> getMediasByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders,
            IndexType indexType, SortField[] sortFields) {

        if (isEmpty(genre)) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getMediasByGenre(genre, musicFolders);
        IndexSearcher searcher = getSearcher(indexType);
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
            release(indexType, searcher);
        }
        return result;

    }

    @Override
    public List<MediaFile> getAlbumsByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {
        SortField[] sortFields = Arrays.stream(ALBUM.getFields())
                .filter(n -> n.equals(FieldNames.FOLDER))
                .map(n -> new SortField(n, SortField.Type.STRING))
                .toArray(i -> new SortField[i]);
        return getMediasByGenre(genre, offset, count, musicFolders, ALBUM, sortFields);
    }

    @Override
    public List<MediaFile> getSongsByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {
        SortField[] sortFields = Arrays.stream(SONG.getFields())
                .map(n -> new SortField(n, SortField.Type.STRING))
                .toArray(i -> new SortField[i]);
        return getMediasByGenre(genre, offset, count, musicFolders, SONG, sortFields);
    }

}