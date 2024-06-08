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
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.ThreadSafe;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.ScannerStateServiceImpl;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.concurrent.ReadWriteLockSupport;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollectorManager;
import org.apache.lucene.store.FSDirectory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("shortExecutor")
public class IndexManager implements ReadWriteLockSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    /**
     * Schema version of Airsonic index. It may be incremented in the following cases:
     *
     * - Incompatible update case in Lucene index implementation - When schema definition is changed due to modification
     * of AnalyzerFactory, DocumentFactory or the class that they use.
     *
     */
    private static final int INDEX_VERSION = 27;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index-JP";

    private final ReentrantReadWriteLock genreLock = new ReentrantReadWriteLock();

    private enum GenreSort {
        ALBUM_COUNT, SONG_COUNT, ALBUM_ALPHABETICAL, SONG_ALPHABETICAL
    }

    private static final MediaType[] MUSIC_AND_AUDIOBOOK = { MediaType.MUSIC, MediaType.AUDIOBOOK };

    private final AnalyzerFactory analyzerFactory;
    private final DocumentFactory documentFactory;
    private final QueryFactory queryFactory;
    private final SearchServiceUtilities util;
    private final JpsonicComparators comparators;
    private final SettingsService settingsService;
    private final ScannerStateServiceImpl scannerState;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final Executor shortExecutor;

    private final Map<IndexType, SearcherManager> searchers;
    private final Map<IndexType, IndexWriter> writers;

    /*
     *
     */
    private final Map<GenreSort, List<Genre>> multiGenreMaster;

    @NonNull
    Path getRootIndexDirectory() {
        return Path.of(SettingsService.getJpsonicHome().toString(), INDEX_ROOT_DIR_NAME + INDEX_VERSION);
    }

    private @NonNull Path getIndexDirectory(IndexType indexType) {
        return Path.of(getRootIndexDirectory().toString(), indexType.toString().toLowerCase(Locale.ENGLISH));
    }

    public IndexManager(AnalyzerFactory analyzerFactory, DocumentFactory documentFactory, QueryFactory queryFactory,
            SearchServiceUtilities util, JpsonicComparators comparators, SettingsService settingsService,
            ScannerStateServiceImpl scannerState, ArtistDao artistDao, AlbumDao albumDao,
            @Qualifier("shortExecutor") Executor shortExecutor) {
        super();
        this.analyzerFactory = analyzerFactory;
        this.documentFactory = documentFactory;
        this.queryFactory = queryFactory;
        this.util = util;
        this.comparators = comparators;
        this.settingsService = settingsService;
        this.scannerState = scannerState;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.shortExecutor = shortExecutor;
        searchers = new ConcurrentHashMap<>();
        writers = new ConcurrentHashMap<>();
        multiGenreMaster = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        deleteOldIndexFiles();
        initializeIndexDirectory();
        scannerState.setReady();
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#updateDocument is atomic.
    public void index(Album album) {
        Term primarykey = DocumentFactory.createPrimarykey(album);
        Document document = documentFactory.createAlbumId3Document(album);
        try {
            writers.get(IndexType.ALBUM_ID3).updateDocument(primarykey, document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#updateDocument is atomic.
    public void index(Artist artist, MusicFolder musicFolder) {
        Term primarykey = DocumentFactory.createPrimarykey(artist);
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        try {
            writers.get(IndexType.ARTIST_ID3).updateDocument(primarykey, document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#updateDocument is atomic.
    public void index(@NonNull MediaFile mediaFile) {
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
            if (!isEmpty(genre) && mediaFile.getMediaType() != MediaType.PODCAST) {
                Term genrekey = DocumentFactory.createPrimarykey(genre.hashCode());
                Document document = documentFactory.createGenreDocument(mediaFile);
                writers.get(IndexType.GENRE).updateDocument(genrekey, document);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. Protected by a thread lock. No concurrent access.
    public void startIndexing() {
        try {
            for (IndexType indexType : IndexType.values()) {
                writers.put(indexType, createIndexWriter(indexType));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private @NonNull IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        Path indexDirectory = getIndexDirectory(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzerFactory.getAnalyzer());
        return new IndexWriter(FSDirectory.open(indexDirectory), config);
    }

    private void clearMultiGenreMaster() {
        writeLock(genreLock);
        try {
            multiGenreMaster.clear();
        } finally {
            writeUnlock(genreLock);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#deleteDocuments is atomic.
    public void expungeArtist(int id) {
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#deleteDocuments is atomic.
    public void expungeAlbum(int id) {
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#deleteDocuments is atomic.
    public void expungeSong(int id) {
        try {
            writers.get(IndexType.SONG).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#deleteDocuments is atomic.
    public void expungeArtistId3(@NonNull List<Integer> expungeCandidates) {
        Term[] primarykeys = expungeCandidates.stream().map(DocumentFactory::createPrimarykey).toArray(Term[]::new);
        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. writers#get#deleteDocuments is atomic.
    public void expungeAlbumId3(List<Integer> candidates) {
        Term[] primarykeys = candidates.stream().map(DocumentFactory::createPrimarykey).toArray(Term[]::new);
        try {
            writers.get(IndexType.ALBUM_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException" }) // lucene/HighFreqTerms#getHighFreqTerms
    public void expungeGenreOtherThan(List<Genre> existing) {
        writeLock(genreLock);
        try {

            // This method is executed during scanning.

            try {
                if (existing.isEmpty()) {
                    writers.get(IndexType.GENRE).deleteAll();
                    writers.get(IndexType.GENRE).flush();
                    clearMultiGenreMaster();
                    return;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            // Stop indexing to get Searcher.
            stopIndexing(IndexType.GENRE);
            IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
            if (genreSearcher == null) {
                return;
            }

            try {
                Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
                if (fields.isEmpty()) {
                    return;
                }

                TermStats[] stats = HighFreqTerms.getHighFreqTerms(genreSearcher.getIndexReader(),
                        HighFreqTerms.DEFAULT_NUMTERMS, FieldNamesConstants.GENRE,
                        new HighFreqTerms.DocFreqComparator());
                List<String> indexedNames = Arrays.stream(stats).map(t -> t.termtext.utf8ToString())
                        .collect(Collectors.toList());
                List<String> existingNames = existing.stream().map(Genre::getName).collect(Collectors.toList());
                Term[] primarykeys = indexedNames.stream().filter(name -> !existingNames.contains(name))
                        .map(String::hashCode).map(DocumentFactory::createPrimarykey).toArray(Term[]::new);

                // Reopen Writer for editing.
                writers.put(IndexType.GENRE, createIndexWriter(IndexType.GENRE));
                writers.get(IndexType.GENRE).deleteDocuments(primarykeys);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                LOG.info("The genre field may not exist.");
            } finally {
                release(IndexType.GENRE, genreSearcher);
            }

            // Don't call it asynchronously. Genre is required for subsequent processing of the scan.
            refreshMultiGenreMaster();
        } finally {
            writeUnlock(genreLock);
        }
    }

    @ThreadSafe(enableChecks = false) // False positive. Absolutely not concurrent.
    @SuppressWarnings("PMD.CloseResource") // False positive. Do not close.
    public void deleteAll() {
        for (IndexWriter writer : writers.values()) {
            try {
                writer.deleteAll();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Close Writer of all indexes and update SearcherManager. Called at the end of the Scan flow.
     */
    public void stopIndexing() {
        Arrays.asList(IndexType.values()).forEach(this::stopIndexing);
        clearMultiGenreMaster();
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    @SuppressFBWarnings(value = { "NP_LOAD_OF_KNOWN_NULL_VALUE",
            "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALU" }, justification = "spotbugs/spotbugs#1338")
    private void stopIndexing(IndexType type) {

        // close
        try (IndexWriter writer = writers.get(type)) {
            if (writer == null) {
                return;
            }
            writers.get(type).commit();
            writer.close();
            writers.remove(type);
        } catch (IOException e) {
            writers.remove(type);
            throw new UncheckedIOException(e);
        }

        // refresh reader as index may have been written
        if (searchers.containsKey(type)) {
            try {
                searchers.get(type).maybeRefresh();
            } catch (IOException e) {
                searchers.remove(type);
                throw new UncheckedIOException(e);
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
        readLock(genreLock);
        try {
            if (!searchers.containsKey(indexType)) {
                Path indexDirectory = getIndexDirectory(indexType);
                writeLock(genreLock);
                try {
                    if (Files.exists(indexDirectory)) {
                        SearcherFactory searcherFactory = new CustomSearcherFactory(shortExecutor);
                        SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory),
                                searcherFactory);
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
                } finally {
                    writeUnlock(genreLock);
                    readLock(genreLock);
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
        } finally {
            readUnlock(genreLock);
        }
        return null;
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        writeLock(genreLock);
        try {
            if (searchers.containsKey(indexType)) {
                try {
                    searchers.get(indexType).release(indexSearcher);
                } catch (IOException e) {
                    searchers.remove(indexType);
                    throw new UncheckedIOException(e);
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
                    throw new UncheckedIOException(e);
                }
            }
        } finally {
            writeUnlock(genreLock);
        }
    }

    /**
     * Check the version of the index and clean it up if necessary. Legacy type indexes (files or directories starting
     * with lucene) are deleted. If there is no index directory, initialize the directory. If the index directory exists
     * and is not the current version, initialize the directory.
     */
    void deleteOldIndexFiles() {
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
    void initializeIndexDirectory() {
        // Check if Index is current version
        if (Files.exists(getRootIndexDirectory())) {
            // Index of current version already exists
            if (LOG.isInfoEnabled()) {
                LOG.info("Index was found (index version {}). ", INDEX_VERSION);
            }
            return;
        }

        artistDao.deleteAll();
        albumDao.deleteAll();
        if (FileUtil.createDirectories(getRootIndexDirectory()) == null) {
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
        readLock(genreLock);
        try {
            searcher = getSearcher(IndexType.GENRE);
            if (isEmpty(searcher)) {
                return result;
            }
        } finally {
            readUnlock(genreLock);
        }

        try {
            final Query query = queryFactory.toPreAnalyzedGenres(genres);
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            for (int i = 0; i < totalHits; i++) {
                IndexableField[] fields = searcher.storedFields().document(topDocs.scoreDocs[i].doc)
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
            throw new UncheckedIOException(e);
        } finally {
            release(IndexType.GENRE, searcher);
        }
        return result;
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        readLock(genreLock);
        try {
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
        } finally {
            readUnlock(genreLock);
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
            throw new UncheckedIOException(e);
        } finally {
            release(IndexType.GENRE, genreSearcher);
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }

    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // lucene/HighFreqTerms#getHighFreqTerms
    private List<GenreFreq> getGenreFreqs() {
        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        if (isEmpty(genreSearcher)) {
            return Collections.emptyList();
        }

        Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
        if (fields.isEmpty()) {
            LOG.info("The multi-genre master has been updated(no record).");
            return Collections.emptyList();
        }

        try {
            TermStats[] stats = HighFreqTerms.getHighFreqTerms(genreSearcher.getIndexReader(),
                    HighFreqTerms.DEFAULT_NUMTERMS, FieldNamesConstants.GENRE, new HighFreqTerms.DocFreqComparator());
            return Arrays.stream(stats).map(stat -> new GenreFreq(stat.termtext.utf8ToString(), stat.docFreq)).toList();
        } catch (Exception e) {
            LOG.info("The genre field may not exist.");
            return Collections.emptyList();
        } finally {
            release(IndexType.GENRE, genreSearcher);
        }
    }

    private int getAlbumGenreCount(IndexSearcher searcher, String genreName, GenreMasterCriteria criteria)
            throws IOException {
        if (criteria.scope() != Scope.ALBUM) {
            return Genre.COUNT_UNACQUIRED;
        }
        Query query = queryFactory.getAlbumId3GenreCount(genreName, criteria.folders());
        return searcher.search(query, new TotalHitCountCollectorManager());
    }

    private int getSongGenreCount(IndexSearcher searcher, String genreName, GenreMasterCriteria criteria)
            throws IOException {
        if (criteria.scope() == Scope.SONG || criteria.sort() == Sort.SONG_COUNT) {
            MediaType[] types = criteria.scope() == Scope.SONG ? criteria.types() : MUSIC_AND_AUDIOBOOK;
            Query query = queryFactory.getSongGenreCount(genreName, criteria.folders(), types);
            return searcher.search(query, new TotalHitCountCollectorManager());
        }
        return Genre.COUNT_UNACQUIRED;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // [AvoidInstantiatingObjectsInLoops] (Genre) Not reusable
    public List<Genre> createGenreMaster(GenreMasterCriteria criteria) {
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(songSearcher) || isEmpty(albumSearcher)) {
            return Collections.emptyList();
        }

        List<Genre> result = new ArrayList<>();
        try {
            for (GenreFreq genreFreq : getGenreFreqs()) {
                String name = genreFreq.genre;
                int albumCount = getAlbumGenreCount(albumSearcher, name, criteria);
                int songCount = getSongGenreCount(songSearcher, name, criteria);
                if (criteria.scope() == Scope.ALBUM && albumCount > 0) {
                    result.add(new Genre(name, songCount, albumCount));
                } else if (criteria.scope() == Scope.SONG && songCount > 0) {
                    result.add(new Genre(name, songCount, albumCount));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }

        switch (criteria.sort()) {
        case FREQUENCY -> {
        }
        case NAME -> {
            result.sort(comparators.genreOrderByAlpha());
        }
        case ALBUM_COUNT -> {
            result.sort(comparators.genreOrderByAlpha());
            result.sort(comparators.genreOrder(true));
        }
        case SONG_COUNT -> {
            result.sort(comparators.genreOrderByAlpha());
            result.sort(comparators.genreOrder(false));
        }
        }
        return result;
    }

    private record GenreFreq(String genre, int songCount) {
    }

    private static class CustomSearcherFactory extends SearcherFactory {

        private final Executor executor;

        public CustomSearcherFactory(Executor executor) {
            super();
            this.executor = executor;
        }

        @Override
        public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {

            return new IndexSearcher(reader, executor);
        }
    }
}
