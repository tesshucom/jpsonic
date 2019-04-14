/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import com.tesshu.jpsonic.service.search.*;
import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.dao.*;
import org.airsonic.player.domain.*;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tesshu.jpsonic.service.search.IndexType.*;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
@Service
public class SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchService.class);

    private static final String INDEX_FILE_PREFIX = "lucene";

    private static final String INDEX_PRODUCT_VERSION = "7.7.1";

    private static final String INDEX_FILE_SUFFIX = "jp";

    public static final Pattern INDEX_FILE_NAME_PATTERN = Pattern.compile("^" + INDEX_FILE_PREFIX + ".*$");

    private static final Map<IndexType, SearcherManager> searcherManagerMap = new ConcurrentHashMap<>();

    private final Analyzer analyzer = AnalyzerFactory.getInstance().getAnalyzer();

    private final Random random = new Random(System.currentTimeMillis());

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private QueryFactory queryFactory;

    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;


    private final Function<Long, Integer> round = (i) -> {
        // return
        // NumericUtils.floatToSortableInt(i);
        return i.intValue();
    };

    private final Function<Document, Integer> getId = d -> {
        return Integer.valueOf(d.get(FieldNames.ID));
    };

    private final BiConsumer<List<MediaFile>, Integer> addMediaFileIfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(m -> id == m.getId())) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (!isEmpty(mediaFile)) {
                dist.add(mediaFile);
            }
        }
    };

    private final BiConsumer<List<Artist>, Integer> addArtistId3IfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(a -> id == a.getId())) {
            Artist artist = artistDao.getArtist(id);
            if (!isEmpty(artist)) {
                dist.add(artist);
            }
        }
    };

    private final Function<Class<?>, @Nullable IndexType> getIndexType = (assignableClass) -> {
        IndexType indexType = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            indexType = IndexType.ALBUM_ID3;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            indexType = IndexType.ARTIST_ID3;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            indexType = IndexType.SONG;
        }
        return indexType;
    };

    private final Function<Class<?>, @Nullable String> getFieldName = (assignableClass) -> {
        String fieldName = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            fieldName = FieldNames.ALBUM;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            fieldName = FieldNames.ARTIST;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            fieldName = FieldNames.TITLE;
        }
        return fieldName;
    };

    private final BiConsumer<List<Album>, Integer> addAlbumId3IfAnyMatch = (dist, subjectId) -> {
        if (!dist.stream().anyMatch(a -> subjectId == a.getId())) {
            Album album = albumDao.getAlbum(subjectId);
            if (!isEmpty(album)) {
                dist.add(album);
            }
        }
    };

    private final boolean addIgnoreNull(Collection<?> collection, Object object) {
        return CollectionUtils.addIgnoreNull(collection, object);
    }

    private final <T> void addIgnoreNull(ParamSearchResult<T> dist, IndexType indexType, int subjectId, Class<T> subjectClass) {
        if (indexType == SONG) {
            MediaFile mediaFile = mediaFileService.getMediaFile(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(mediaFile));
        } else if (indexType == ARTIST_ID3) {
            Artist artist = artistDao.getArtist(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(artist));
        } else if (indexType == ALBUM_ID3) {
            Album album = albumDao.getAlbum(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(album));
        }
    }

    public String getVersion() {
        return INDEX_FILE_PREFIX + INDEX_PRODUCT_VERSION + INDEX_FILE_SUFFIX
                + "-" + IndexType.getVersion()
                + "-" + DocumentFactory.getVersion();
    }

    private final Supplier<File> getRootDirectory = () -> {
        return new File(SettingsService.getJpsonicHome(), getVersion());
    };

    private final Function<IndexType, File> getDirectory = (indexType) -> {
        return new File(getRootDirectory.get(),
            indexType.toString().toLowerCase());
    };

    private final void addIfAnyMatch(SearchResult dist, IndexType subjectIndexType, Document subject) {
        int documentId = getId.apply(subject);
        if (subjectIndexType == ARTIST | subjectIndexType == ALBUM | subjectIndexType == SONG) {
            addMediaFileIfAnyMatch.accept(dist.getMediaFiles(), documentId);
        } else if (subjectIndexType == ARTIST_ID3) {
            addArtistId3IfAnyMatch.accept(dist.getArtists(), documentId);
        } else if (subjectIndexType == ALBUM_ID3) {
            addAlbumId3IfAnyMatch.accept(dist.getAlbums(), documentId);
        }
    }

    private final IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File dir = getDirectory.apply(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        return new IndexWriter(FSDirectory.open(dir.toPath()), config);
    }

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

    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(artist.getId()));
            artistId3Writer.updateDocument(term, documentFactory.createDocument(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    public void index(Album album) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(album.getId()));
            albumId3Writer.updateDocument(term, documentFactory.createDocument(album));
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
                File indexDirectory = getDirectory.apply(indexType);
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

            int totalHits = round.apply(topDocs.totalHits);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                addIfAnyMatch(result, indexType, searcher.doc(topDocs.scoreDocs[i].doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            release(indexType, searcher);
        }
        return result;
    }

    public <T> ParamSearchResult<T> searchByName(String name, int offset, int count, List<MusicFolder> folderList, Class<T> assignableClass) {

        // we only support album, artist, and song for now
        @Nullable
        IndexType indexType = getIndexType.apply(assignableClass);
        @Nullable
        String fieldName = getFieldName.apply(assignableClass);

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

            int totalHits = round.apply(topDocs.totalHits);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                this.addIgnoreNull(result, indexType, getId.apply(doc), assignableClass);
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
            id2ListCallBack.accept(result, getId.apply(document));
            docs.remove(randomPos);
        }

        return result;
    }

    /**
     * Returns a number of random songs.
     *
     * @param criteria Search criteria.
     * @return List of random songs.
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

        final Query query = queryFactory.getRandomSongs(criteria);
        IndexSearcher searcher = getSearcher(SONG);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(criteria.getCount(), searcher, query, (dist, id)
                    -> addIgnoreNull(dist, mediaFileService.getMediaFile(id)));
        } catch (IOException e) {
            LOG.error("Failed to search or random songs.", e);
        } finally {
            release(SONG, searcher);
        }

        return Collections.emptyList();

    }

    /**
     * Returns a number of random albums.
     *
     * @param count Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbums(musicFolders);
        IndexSearcher searcher = getSearcher(ALBUM);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query,  (dist, id)
                    -> addIgnoreNull(dist, mediaFileService.getMediaFile(id)));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            release(ALBUM, searcher);
        }

        return Collections.emptyList();

    }

    /**
     * Returns a number of random albums, using ID3 tag.
     *
     * @param count        Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);
        IndexSearcher searcher = getSearcher(ALBUM_ID3);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        try {
            return createRandomFiles(count, searcher, query,  (dist, id)
                    -> addIgnoreNull(dist, albumDao.getAlbum(id)));
        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            release(ALBUM_ID3, searcher);
        }

        return Collections.emptyList();

    }

}
