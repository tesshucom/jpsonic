package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.util.FileUtil;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.tesshu.jpsonic.service.search.IndexType.ALBUM;
import static com.tesshu.jpsonic.service.search.IndexType.ALBUM_ID3;
import static com.tesshu.jpsonic.service.search.IndexType.ARTIST;
import static com.tesshu.jpsonic.service.search.IndexType.ARTIST_ID3;
import static com.tesshu.jpsonic.service.search.IndexType.GENRE;
import static com.tesshu.jpsonic.service.search.IndexType.SONG;
import static org.springframework.util.ObjectUtils.isEmpty;

/*
 * Function class that is strongly linked to the lucene index implementation.
 * Legacy has an implementation in SearchService.
 * 
 * If the index CRUD and search functionality are in the same class,
 * there is often a dependency conflict on the class used.
 * 
 * Although the interface of SearchService is left to maintain the legacy implementation,
 * it is desirable that methods of index operations other than search essentially use this class directly.
 */
@Component
public class IndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    private static final Map<IndexType, SearcherManager> searcherManagerMap = new ConcurrentHashMap<>();

    private static final Map<IndexType, IndexWriter> indexWriterMap = new ConcurrentHashMap<>();

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private SearchServiceTermination termination;

    /**
     * @since 101.1.0
     */
    public String INDEX_FILE_SUFFIX = "jp";

    /**
     * @since 101.1.0
     */
    public String INDEX_PRODUCT_VERSION = "7.7.1";

    private final IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File dir = termination.getDirectory.apply(getVersion(), indexType);
        IndexWriterConfig config = new IndexWriterConfig(AnalyzerFactory.getInstance().getAnalyzer());
        return new IndexWriter(FSDirectory.open(dir.toPath()), config);
    }

    public /* @Nullable */ IndexSearcher getSearcher(IndexType indexType) {
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
            SearcherManager manager = searcherManagerMap.get(indexType);
            if (!isEmpty(manager)) {
                return searcherManagerMap.get(indexType).acquire();
            }
        } catch (Exception e) {
            LOG.warn("Failed to acquire IndexSearcher.", e);
        }
        return null;
    }

    public String getVersion() {
        return SearchService.INDEX_FILE_PREFIX + INDEX_PRODUCT_VERSION + INDEX_FILE_SUFFIX
                + "-" + IndexType.getVersion()
                + "-" + DocumentFactory.getVersion();
    }

    public void index(Album album) {
        try {
            Term primarykey = termination.createPrimarykey(album);
            indexWriterMap.get(ALBUM_ID3).updateDocument(primarykey, documentFactory.createDocument(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            Term primarykey = termination.createPrimarykey(artist);
            indexWriterMap.get(ARTIST_ID3).updateDocument(primarykey, documentFactory.createDocument(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    public void index(MediaFile mediaFile) {
        
        try {
            Term primarykey = termination.createPrimarykey(mediaFile);
            if (mediaFile.isFile()) {
                indexWriterMap.get(SONG).updateDocument(primarykey, documentFactory.createSongDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                indexWriterMap.get(ALBUM).updateDocument(primarykey, documentFactory.createAlbumDocument(mediaFile));
            } else {
                indexWriterMap.get(ARTIST).updateDocument(primarykey, documentFactory.createArtistDocument(mediaFile));
            }
            if (!isEmpty(mediaFile.getGenre())) {
                primarykey = termination.createPrimarykey(mediaFile.getGenre());
                indexWriterMap.get(GENRE).updateDocument(primarykey, documentFactory.createGenreDocument(mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        if (searcherManagerMap.containsKey(indexType)) {
            try {
                searcherManagerMap.get(indexType).release(indexSearcher);
            } catch (IOException e) {
                LOG.error("Failed to release IndexSearcher.", e);
                searcherManagerMap.remove(indexType);
            }
        }
    }

    public void expunge() {

        List<MediaFile> mediaFiles = mediaFileDao.getExpungementCandidate();

        Term[] primarykeys = mediaFiles.stream()
            .filter(m -> MediaType.MUSIC == m.getMediaType()
                || MediaType.PODCAST == m.getMediaType()
                || MediaType.AUDIOBOOK == m.getMediaType()
                || MediaType.VIDEO == m.getMediaType())
            .map(m -> termination.createPrimarykey(m))
            .toArray(i -> new Term[i]);
        try {
            indexWriterMap.get(SONG).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete song doc.", e);
        }

        primarykeys = mediaFiles.stream()
            .filter(m -> MediaType.ALBUM == m.getMediaType())
            .map(m -> termination.createPrimarykey(m))
            .toArray(i -> new Term[i]);
        try {
            indexWriterMap.get(ALBUM).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete album doc.", e);
        }

        List<Artist> artists = artistDao.getExpungementCandidate();
        primarykeys = artists.stream()
            .map(m -> termination.createPrimarykey(m))
            .toArray(i -> new Term[i]);
        try {
            indexWriterMap.get(ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artistId3 doc.", e);
        }

        List<Album> albums = albumDao.getExpungementCandidate();

        primarykeys = albums.stream()
            .map(m -> termination.createPrimarykey(m))
            .toArray(i -> new Term[i]);
        try {
            indexWriterMap.get(ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete albumId3 doc.", e);
        }

    }

    public final void startIndexing() {
        try {

            indexWriterMap.put(ARTIST, createIndexWriter(ARTIST));
            indexWriterMap.put(ARTIST_ID3, createIndexWriter(ARTIST_ID3));
            indexWriterMap.put(ALBUM, createIndexWriter(ALBUM));
            indexWriterMap.put(ALBUM_ID3, createIndexWriter(ALBUM_ID3));
            indexWriterMap.put(SONG, createIndexWriter(SONG));
            indexWriterMap.put(GENRE, createIndexWriter(GENRE));
            indexWriterMap.get(GENRE).deleteAll();

        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        }
    }

    public void stopIndexing() {

        expunge();

        stopIndexing(ARTIST);
        stopIndexing(ARTIST_ID3);
        stopIndexing(ALBUM);
        stopIndexing(ALBUM_ID3);
        stopIndexing(SONG);
        stopIndexing(GENRE);

    }

    private void stopIndexing(IndexType type) {

        long count = -1;
        // close
        try {
            count = indexWriterMap.get(type).commit();
            indexWriterMap.get(type).close();
            indexWriterMap.remove(type);
            LOG.info("Success to create or update search index : [" + type + "]");
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        } finally {
            FileUtil.closeQuietly(indexWriterMap.get(type));
        }

        // refresh
        if (-1 != count && searcherManagerMap.containsKey(type)) {
            try {
                searcherManagerMap.get(type).maybeRefresh();
                LOG.info("SearcherManager has been refreshed : [" + type + "]");
            } catch (IOException e) {
                LOG.error("Failed to refresh SearcherManager : [" + type + "]", e);
                searcherManagerMap.remove(type);
            }
        }

    }

    /**
     * Get pre-parsed genre string from parsed genre string.
     * 
     * The genre analysis includes tokenize processing.
     * Therefore, the parsed genre string and the cardinal of the unedited genre string are n: n.
     * 
     * @param list of analyzed genres
     * @return　list of pre-analyzed genres
     * @since 101.2.0
     */
    public List<String> toPreAnalyzedGenres(List<String> genres) {

        if (isEmpty(genres) || genres.size() == 0) {
            return Collections.emptyList();
        }

        final Query query = queryFactory.toPreAnalyzedGenres(genres);
        IndexSearcher searcher = getSearcher(GENRE);
        if(isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        try {
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            int totalHits = termination.round.apply(topDocs.totalHits);
            for (int i = 0; i < totalHits; i++) {
                IndexableField[] fields = searcher.doc(topDocs.scoreDocs[i].doc).getFields(FieldNames.GENRE_KEY);
                if (!isEmpty(fields)) {
                    List<String> fieldValues = Arrays.stream(fields).map(f -> f.stringValue()).collect(Collectors.toList());
                    fieldValues.forEach(v -> {
                        if (!result.contains(v)) {
                            result.add(v);
                        }
                    });
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            release(GENRE, searcher);
        }
        return result;
    }

    public void updateArtistSort(Album album) {
        try {
            if (isEmpty(album.getArtistSort())) {
                Term term = new Term(FieldNames.ID, Integer.toString(album.getId()));
                Field field = new TextField(FieldNames.ARTIST_READING, album.getArtistSort(), Store.YES);
                Field field2 = new SortedDocValuesField(FieldNames.ARTIST_READING, new BytesRef(album.getArtistSort()));
                indexWriterMap.get(ALBUM_ID3).updateDocValues(term, field, field2);
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    /**
     * Update Genres.
     * 
     * @param album contents of update.
     * @since 101.2.0
     */
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

    /*
     * All genre master acquisitions are aggregated into this method.
     * The current situation uses legacy database tables.
     * By design, it is possible to process entirely with the index on the lucene side,
     * so it is possible to hide the implementation without using a DB in this class.
     */
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

}
