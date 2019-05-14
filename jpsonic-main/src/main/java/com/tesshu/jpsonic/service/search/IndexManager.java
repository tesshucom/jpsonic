package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
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

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private SearchServiceTermination termination;
    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;

    private IndexWriter genreWriter;

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
            Term term = new Term(FieldNames.ID, Integer.toString(album.getId()));
            albumId3Writer.updateDocument(term, documentFactory.createDocument(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
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

    public void index(MediaFile mediaFile) {
        try {
            Term term = new Term(FieldNames.ID, Integer.toString(mediaFile.getId()));
            if (mediaFile.isFile()) {
                songWriter.updateDocument(term, documentFactory.createSongDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.updateDocument(term, documentFactory.createAlbumDocument(mediaFile));
            } else {
                artistWriter.updateDocument(term, documentFactory.createArtistDocument(mediaFile));
            }
            if (!isEmpty(mediaFile.getGenre())) {
                term = new Term(FieldNames.GENRE_KEY, mediaFile.getGenre());
                genreWriter.updateDocument(term, documentFactory.createGenreDocument(mediaFile));
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

    public final void startIndexing() {
        try {
            artistWriter = createIndexWriter(ARTIST);
            artistId3Writer = createIndexWriter(ARTIST_ID3);
            albumWriter = createIndexWriter(ALBUM);
            albumId3Writer = createIndexWriter(ALBUM_ID3);
            songWriter = createIndexWriter(SONG);
            genreWriter = createIndexWriter(GENRE);
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        }
    }

    public void stopIndexing() {

        stopIndexing(artistWriter);
        stopIndexing(artistId3Writer);
        stopIndexing(albumWriter);
        stopIndexing(albumId3Writer);
        stopIndexing(songWriter);
        stopIndexing(genreWriter);

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
                albumId3Writer.updateDocValues(term, field, field2);
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
}
