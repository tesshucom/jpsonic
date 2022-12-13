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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Function class that is strongly linked to the lucene index implementation. Legacy has an implementation in
 * SearchService.
 *
 * If the index CRUD and search functionality are in the same class, there is often a dependency conflict on the class
 * used. Although the interface of SearchService is left to maintain the legacy implementation, it is desirable that
 * methods of index operations other than search essentially use this class directly.
 */
@Component
public class IndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    /**
     * Schema version of Airsonic index. It may be incremented in the following cases:
     *
     * - Incompatible update case in Lucene index implementation - When schema definition is changed due to modification
     * of AnalyzerFactory, DocumentFactory or the class that they use.
     *
     */
    private static final int INDEX_VERSION = 26;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index-JP";

    private final Object genreLock = new Object();

    private enum GenreSort {
        ALBUM_COUNT, SONG_COUNT, ALBUM_ALPHABETICAL, SONG_ALPHABETICAL
    }

    private final AnalyzerFactory analyzerFactory;
    private final DocumentFactory documentFactory;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final QueryFactory queryFactory;
    private final SearchServiceUtilities util;
    private final JpsonicComparators comparators;
    private final SettingsService settingsService;
    private final Map<IndexType, SearcherManager> searchers;
    private final Map<IndexType, IndexWriter> writers;
    private final Map<GenreSort, List<Genre>> multiGenreMaster;

    private @NonNull Path getRootIndexDirectory() {
        return Path.of(SettingsService.getJpsonicHome().toString(), INDEX_ROOT_DIR_NAME + INDEX_VERSION);
    }

    private @NonNull Path getIndexDirectory(IndexType indexType) {
        return Path.of(getRootIndexDirectory().toString(), indexType.toString().toLowerCase(Locale.ENGLISH));
    }

    public IndexManager(AnalyzerFactory analyzerFactory, DocumentFactory documentFactory, MediaFileDao mediaFileDao,
            ArtistDao artistDao, AlbumDao albumDao, QueryFactory queryFactory, SearchServiceUtilities util,
            JpsonicComparators comparators, SettingsService settingsService) {
        super();
        this.analyzerFactory = analyzerFactory;
        this.documentFactory = documentFactory;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.queryFactory = queryFactory;
        this.util = util;
        this.comparators = comparators;
        this.settingsService = settingsService;
        searchers = new ConcurrentHashMap<>();
        writers = new ConcurrentHashMap<>();
        multiGenreMaster = new ConcurrentHashMap<>();
    }

    public void index(Album album) {
        Term primarykey = DocumentFactory.createPrimarykey(album);
        Document document = documentFactory.createAlbumId3Document(album);
        try {
            writers.get(IndexType.ALBUM_ID3).updateDocument(primarykey, document);
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create search index for " + album, e);
            }
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        Term primarykey = DocumentFactory.createPrimarykey(artist);
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        try {
            writers.get(IndexType.ARTIST_ID3).updateDocument(primarykey, document);
        } catch (IOException x) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create search index for " + artist, x);
            }
        }
    }

    public void index(MediaFile mediaFile) {
        Term primarykey = DocumentFactory.createPrimarykey(mediaFile);
        try {
            if (mediaFile.isFile()) {
                Document document = documentFactory.createSongDocument(mediaFile);
                writers.get(IndexType.SONG).updateDocument(primarykey, document);
            } else if (mediaFile.isAlbum()) {
                Document document = documentFactory.createAlbumDocument(mediaFile);
                writers.get(IndexType.ALBUM).updateDocument(primarykey, document);
            } else {
                Document document = documentFactory.createArtistDocument(mediaFile);
                writers.get(IndexType.ARTIST).updateDocument(primarykey, document);
            }
            String genre = mediaFile.getGenre();
            if (!isEmpty(genre)) {
                primarykey = DocumentFactory.createPrimarykey(genre.hashCode());
                Document document = documentFactory.createGenreDocument(mediaFile);
                writers.get(IndexType.GENRE).updateDocument(primarykey, document);
            }
        } catch (IOException x) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create search index for " + mediaFile, x);
            }
        }
    }

    public void startIndexing() {
        try {
            for (IndexType indexType : IndexType.values()) {
                writers.put(indexType, createIndexWriter(indexType));
            }
            clearGenreMaster();
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        }
    }

    private @NonNull IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        Path indexDirectory = getIndexDirectory(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzerFactory.getAnalyzer());
        return new IndexWriter(FSDirectory.open(indexDirectory), config);
    }

    private void clearMultiGenreMaster() {
        synchronized (genreLock) {
            multiGenreMaster.clear();
        }
    }

    private void clearGenreMaster() {
        try {
            writers.get(IndexType.GENRE).deleteAll();
            writers.get(IndexType.GENRE).flush();
            clearMultiGenreMaster();
        } catch (IOException e) {
            LOG.error("Failed to clear genre index.", e);
        }
    }

    public void expunge() {

        Term[] primarykeys = mediaFileDao.getArtistExpungeCandidates().stream().map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artist doc.", e);
        }

        primarykeys = mediaFileDao.getAlbumExpungeCandidates().stream().map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete album doc.", e);
        }

        primarykeys = mediaFileDao.getSongExpungeCandidates().stream().map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.SONG).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete song doc.", e);
        }

        primarykeys = artistDao.getExpungeCandidates().stream().map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artistId3 doc.", e);
        }

        primarykeys = albumDao.getExpungeCandidates().stream().map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ALBUM_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete albumId3 doc.", e);
        }

    }

    /**
     * Close Writer of all indexes and update SearcherManager. Called at the end of the Scan flow.
     */
    public void stopIndexing() {
        Arrays.asList(IndexType.values()).forEach(indexType -> stopIndexing(indexType));
        clearMultiGenreMaster();
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    private void stopIndexing(IndexType type) {

        boolean isUpdate = false;
        // close
        try (IndexWriter writer = writers.get(type)) {
            isUpdate = -1 != writers.get(type).commit();
            writer.close();
            writers.remove(type);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Success to create or update search index : [" + type + "]");
            }
        } catch (IOException e) {
            writers.remove(type);
            LOG.error("Failed to create search index.", e);
        }

        // refresh reader as index may have been written
        if (isUpdate && searchers.containsKey(type)) {
            try {
                searchers.get(type).maybeRefresh();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("SearcherManager has been refreshed : [" + type + "]");
                }
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to refresh SearcherManager : [" + type + "]", e);
                }
                searchers.remove(type);
            }
        }

    }

    /**
     * Return the IndexSearcher of the specified index. At initial startup, it may return null if the user performs any
     * search before performing a scan.
     */
    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. SearcherManager inherits Closeable but ensures each searcher is closed only once all threads have
     * finished using it. No explicit close is done here.
     */
    public @Nullable IndexSearcher getSearcher(@NonNull IndexType indexType) {
        synchronized (genreLock) {
            if (!searchers.containsKey(indexType)) {
                Path indexDirectory = getIndexDirectory(indexType);
                try {
                    if (Files.exists(indexDirectory)) {
                        SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory), null);
                        searchers.put(indexType, manager);
                    } else if (LOG.isWarnEnabled()) {
                        LOG.warn("{} does not exist. Please run a scan.", indexDirectory);
                    }
                } catch (IndexNotFoundException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Index {} does not exist in {}, likely not yet created.", indexType.toString(),
                                indexDirectory);
                    }
                    return null;
                } catch (IOException e) {
                    LOG.warn("Failed to initialize SearcherManager.", e);
                    return null;
                }
            }
            try {
                SearcherManager manager = searchers.get(indexType);
                if (!isEmpty(manager)) {
                    return searchers.get(indexType).acquire();
                }
            } catch (ClassCastException | IOException e) {
                LOG.warn("Failed to acquire IndexSearcher.", e);
            }
        }
        return null;
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        synchronized (genreLock) {
            if (searchers.containsKey(indexType)) {
                try {
                    searchers.get(indexType).release(indexSearcher);
                } catch (IOException e) {
                    LOG.error("Failed to release IndexSearcher.", e);
                    searchers.remove(indexType);
                }
            } else {
                try {
                    /*
                     * #1280 This method is called automatically from various finally clauses. If you have never
                     * scanned, Searcher is null.
                     */
                    if (indexSearcher != null) {
                        indexSearcher.getIndexReader().close();
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to release. IndexSearcher has been closed.", e);
                }
            }
        }
    }

    /**
     * Check the version of the index and clean it up if necessary. Legacy type indexes (files or directories starting
     * with lucene) are deleted. If there is no index directory, initialize the directory. If the index directory exists
     * and is not the current version, initialize the directory.
     */
    public void deleteOldIndexFiles() {
        deleteLegacyFiles();
        deleteOldFiles();
    }

    private void deleteFile(String label, Path old) {
        if (Files.exists(old)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Found " + label + ". Try to delete : {}", old);
            }
            if (Files.isRegularFile(old)) {
                FileUtil.deleteIfExists(old);
            } else {
                FileUtil.deleteDirectory(old);
            }
        }
    }

    void deleteLegacyFiles() {
        // Delete legacy files unconditionally
        Pattern legacyPattern1 = Pattern.compile("^lucene\\d+$");
        String legacyName2 = "index";

        try (Stream<Path> children = Files.list(SettingsService.getJpsonicHome())) {
            children.filter(childPath -> {
                String name = childPath.getFileName().toString();
                return legacyPattern1.matcher(name).matches() || legacyName2.contentEquals(name);
            }).forEach(childPath -> deleteFile("legacy index file", childPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void deleteOldFiles() {
        // Delete if not old index version
        Pattern indexPattern = Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$");

        try (Stream<Path> children = Files.list(SettingsService.getJpsonicHome())) {
            children.filter(childPath -> {
                String name = childPath.getFileName().toString();
                return indexPattern.matcher(name).matches()
                        && !getRootIndexDirectory().getFileName().toString().equals(name);
            }).forEach(childPath -> deleteFile("old index file", childPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory() {
        // Check if Index is current version
        if (Files.exists(getRootIndexDirectory())) {
            // Index of current version already exists
            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Index was found (index version {}). ", INDEX_VERSION);
            }
        } else if (FileUtil.createDirectories(getRootIndexDirectory()) == null) {
            LOG.warn("Failed to create index directory :  (index version {}). ", INDEX_VERSION);
        }
    }

    /**
     * Get pre-parsed genre string from parsed genre string.
     *
     * The genre analysis includes tokenize processing. Therefore, the parsed genre string and the cardinal of the
     * unedited genre string are n: n.
     *
     * @param genres
     *            list of analyzed genres
     *
     * @return pre-analyzed genres
     *
     * @since 101.2.0
     */
    public List<String> toPreAnalyzedGenres(@NonNull List<String> genres) {

        List<String> result = new ArrayList<>();
        if (genres.isEmpty()) {
            return result;
        }

        IndexSearcher searcher;
        synchronized (genreLock) {
            searcher = getSearcher(IndexType.GENRE);
            if (isEmpty(searcher)) {
                return result;
            }
        }

        try {
            final Query query = queryFactory.toPreAnalyzedGenres(genres);
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            for (int i = 0; i < totalHits; i++) {
                IndexableField[] fields = searcher.doc(topDocs.scoreDocs[i].doc)
                        .getFields(FieldNamesConstants.GENRE_KEY);
                if (!isEmpty(fields)) {
                    List<String> fieldValues = Arrays.stream(fields).map(IndexableField::stringValue)
                            .collect(Collectors.toList());
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
            release(IndexType.GENRE, searcher);
        }
        return result;
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        synchronized (genreLock) {
            if (multiGenreMaster.isEmpty()) {
                refreshMultiGenreMaster();
            }
            if (settingsService.isSortGenresByAlphabet() && sortByAlbum) {
                if (multiGenreMaster.containsKey(GenreSort.ALBUM_ALPHABETICAL)) {
                    return multiGenreMaster.get(GenreSort.ALBUM_ALPHABETICAL);
                }
                List<Genre> albumGenres = new ArrayList<>();
                if (!isEmpty(multiGenreMaster.get(GenreSort.ALBUM_COUNT))) {
                    albumGenres.addAll(multiGenreMaster.get(GenreSort.ALBUM_COUNT));
                    albumGenres.sort(comparators.genreOrderByAlpha());
                }
                multiGenreMaster.put(GenreSort.ALBUM_ALPHABETICAL, albumGenres);
                return albumGenres;
            } else if (settingsService.isSortGenresByAlphabet()) {
                if (multiGenreMaster.containsKey(GenreSort.SONG_ALPHABETICAL)) {
                    return multiGenreMaster.get(GenreSort.SONG_ALPHABETICAL);
                }
                List<Genre> albumGenres = new ArrayList<>();
                if (!isEmpty(multiGenreMaster.get(GenreSort.SONG_COUNT))) {
                    albumGenres.addAll(multiGenreMaster.get(GenreSort.SONG_COUNT));
                    albumGenres.sort(comparators.genreOrderByAlpha());
                }
                multiGenreMaster.put(GenreSort.SONG_ALPHABETICAL, albumGenres);
                return albumGenres;
            }

            List<Genre> genres = sortByAlbum ? multiGenreMaster.get(GenreSort.ALBUM_COUNT)
                    : multiGenreMaster.get(GenreSort.SONG_COUNT);
            return isEmpty(genres) ? Collections.emptyList() : genres;
        }
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidCatchingGenericException" }) // lucene/HighFreqTerms#getHighFreqTerms
    // [AvoidInstantiatingObjectsInLoops] (Genre) Not reusable
    private void refreshMultiGenreMaster() {

        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM);

        try {
            if (!isEmpty(genreSearcher) && !isEmpty(songSearcher) && !isEmpty(albumSearcher)) {

                mayBeInit: {

                    multiGenreMaster.clear();

                    Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
                    if (fields.isEmpty()) {
                        LOG.info("The multi-genre master has been updated(no record).");
                        return;
                    }

                    int numTerms = HighFreqTerms.DEFAULT_NUMTERMS;
                    Comparator<TermStats> c = new HighFreqTerms.DocFreqComparator();
                    TermStats[] stats;
                    try {
                        stats = HighFreqTerms.getHighFreqTerms(genreSearcher.getIndexReader(), numTerms,
                                FieldNamesConstants.GENRE, c);
                    } catch (Exception e) {
                        LOG.info("The genre field may not exist.");
                        break mayBeInit;
                    }
                    List<String> genreNames = Arrays.stream(stats).map(t -> t.termtext.utf8ToString())
                            .collect(Collectors.toList());

                    List<Genre> genres = new ArrayList<>();
                    for (String genreName : genreNames) {
                        Query query = queryFactory.getGenre(genreName);
                        TopDocs topDocs = songSearcher.search(query, Integer.MAX_VALUE);
                        int songCount = util.round.apply(topDocs.totalHits.value);
                        topDocs = albumSearcher.search(query, Integer.MAX_VALUE);
                        int albumCount = util.round.apply(topDocs.totalHits.value);
                        genres.add(new Genre(genreName, songCount, albumCount));
                    }

                    genres.sort(comparators.genreOrder(false));
                    multiGenreMaster.put(GenreSort.SONG_COUNT, genres);

                    List<Genre> genresByAlbum = new ArrayList<>();
                    genres.stream().filter(g -> 0 != g.getAlbumCount()).forEach(genresByAlbum::add);
                    genresByAlbum.sort(comparators.genreOrder(true));
                    multiGenreMaster.put(GenreSort.ALBUM_COUNT, genresByAlbum);

                    LOG.info("The multi-genre master has been updated.");
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            release(IndexType.GENRE, genreSearcher);
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }

    }
}
