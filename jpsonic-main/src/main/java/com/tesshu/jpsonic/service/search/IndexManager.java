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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.ThreadSafe;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.ScannerStateServiceImpl;
import com.tesshu.jpsonic.service.search.SearchServiceUtilities.LegacyGenreCriteria;
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
import org.apache.lucene.store.FSDirectory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * IndexManager is the core Lucene index management component for Jpsonic.
 * <p>
 * This class is tightly coupled with Lucene's low-level index operations and
 * provides full control over CRUD operations on various index types (e.g.,
 * songs, albums, artists, genres). Unlike {@code SearchService}, which retains
 * compatibility with legacy code and acts as an abstracted interface, this
 * class directly implements all index management logic.
 * </p>
 *
 * <p>
 * Due to potential dependency conflicts between search and indexing
 * responsibilities, the design encourages delegating all non-search-related
 * index operations to this class, isolating Lucene-specific concerns from the
 * legacy service layer.
 * </p>
 *
 * <p>
 * This class handles:
 * <ul>
 * <li>Creation and removal of Lucene documents for songs, albums, artists, and
 * genres</li>
 * <li>Genre master construction and genre field consistency</li>
 * <li>Index lifecycle operations such as start, stop, delete, and
 * initialization</li>
 * <li>Locking and cache coordination to support concurrent scan/search
 * processes</li>
 * </ul>
 * </p>
 *
 * <p>
 * It is thread-safe under most operations and supports concurrent read/write
 * access via a configurable
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock}. Internal methods
 * use Lucene’s {@link SearcherManager} and {@link IndexWriter} components to
 * manage real-time index visibility and data integrity.
 * </p>
 *
 * <p>
 * Note: Initialization, cleanup, and searcher refresh mechanisms rely on Spring
 * lifecycle management (e.g., {@link jakarta.annotation.PostConstruct}) and
 * explicit coordination via {@code @DependsOn("shortExecutor")}.
 * </p>
 *
 */
@Component
@DependsOn("shortExecutor")
public class IndexManager implements ReadWriteLockSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    private static final MediaType[] MUSIC_AND_AUDIOBOOK = { MediaType.MUSIC, MediaType.AUDIOBOOK };

    private final LuceneUtils luceneUtils;
    private final AnalyzerFactory analyzerFactory;
    private final DocumentFactory documentFactory;
    private final QueryFactory queryFactory;
    private final SearchServiceUtilities util;
    private final JpsonicComparators comparators;
    private final SettingsService settingsService;
    private final ScannerStateServiceImpl scannerState;
    private final ArtistDao artistDao;
    private final Executor shortExecutor;

    private final Map<IndexType, SearcherManager> searchers;
    private final Map<IndexType, IndexWriter> writers;
    private final ReentrantReadWriteLock genreLock = new ReentrantReadWriteLock();

    @NonNull
    Path getRootIndexDirectory() {
        return Path
            .of(SettingsService.getJpsonicHome().toString(),
                    luceneUtils.getIndexRootDirName() + luceneUtils.getIndexVersion());
    }

    private @NonNull Path getIndexDirectory(IndexType indexType) {
        return Path
            .of(getRootIndexDirectory().toString(),
                    indexType.toString().toLowerCase(Locale.ENGLISH));
    }

    public IndexManager(LuceneUtils luceneUtils, AnalyzerFactory analyzerFactory,
            DocumentFactory documentFactory, QueryFactory queryFactory, SearchServiceUtilities util,
            JpsonicComparators comparators, SettingsService settingsService,
            ScannerStateServiceImpl scannerState, ArtistDao artistDao,
            @Qualifier("shortExecutor") Executor shortExecutor) {
        super();
        this.luceneUtils = luceneUtils;
        this.analyzerFactory = analyzerFactory;
        this.documentFactory = documentFactory;
        this.queryFactory = queryFactory;
        this.util = util;
        this.comparators = comparators;
        this.settingsService = settingsService;
        this.scannerState = scannerState;
        this.artistDao = artistDao;
        this.shortExecutor = shortExecutor;
        searchers = new ConcurrentHashMap<>();
        writers = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        deleteOldIndexFiles();
        initializeIndexDirectory();
        scannerState.setReady();
    }

    @ThreadSafe(enableChecks = false)
    public void index(@NonNull Album album) {
        try {
            // Update index for album ID3
            writers
                .get(IndexType.ALBUM_ID3)
                .updateDocument(DocumentFactory.createPrimarykey(album),
                        documentFactory.createAlbumId3Document(album));

            // If genre exists, update index for album genre
            String genre = album.getGenre();
            if (genre != null && !genre.isEmpty()) {
                writers
                    .get(IndexType.ALBUM_ID3_GENRE)
                    .updateDocument(DocumentFactory.createPrimarykey(genre.hashCode()),
                            documentFactory.createGenreDocument(album));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            writers
                .get(IndexType.ARTIST_ID3)
                .updateDocument(DocumentFactory.createPrimarykey(artist),
                        documentFactory.createArtistId3Document(artist, musicFolder));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void index(@NonNull MediaFile mediaFile) {
        try {
            Term primaryKey = DocumentFactory.createPrimarykey(mediaFile);
            Document document;

            if (mediaFile.isFile()) {
                document = documentFactory.createSongDocument(mediaFile);
                writers.get(IndexType.SONG).updateDocument(primaryKey, document);
            } else if (mediaFile.isAlbum()) {
                document = documentFactory.createAlbumDocument(mediaFile);
                writers.get(IndexType.ALBUM).updateDocument(primaryKey, document);
            } else {
                document = documentFactory.createArtistDocument(mediaFile);
                writers.get(IndexType.ARTIST).updateDocument(primaryKey, document);
            }

            String genre = mediaFile.getGenre();
            if (!isEmpty(genre) && mediaFile.getMediaType() != MediaType.PODCAST) {
                writers
                    .get(IndexType.GENRE)
                    .updateDocument(DocumentFactory.createPrimarykey(genre.hashCode()),
                            documentFactory.createGenreDocument(mediaFile));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void startIndexing() {
        try {
            for (IndexType type : IndexType.values()) {
                writers.put(type, createIndexWriter(type));
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

    @ThreadSafe(enableChecks = false)
    public void expungeArtist(int id) {
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void expungeAlbum(int id) {
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void expungeSong(int id) {
        try {
            writers.get(IndexType.SONG).deleteDocuments(DocumentFactory.createPrimarykey(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void expungeArtistId3(@NonNull List<Integer> expungeCandidates) {
        Term[] primaryKeys = expungeCandidates
            .stream()
            .map(DocumentFactory::createPrimarykey)
            .toArray(Term[]::new);

        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primaryKeys);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ThreadSafe(enableChecks = false)
    public void expungeAlbumId3(List<Integer> candidates) {
        Term[] primaryKeys = candidates
            .stream()
            .map(DocumentFactory::createPrimarykey)
            .toArray(Term[]::new);
        try {
            writers.get(IndexType.ALBUM_ID3).deleteDocuments(primaryKeys);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void expungeGenreOtherThan(List<Genre> existing) {
        writeLock(genreLock);
        // This method runs during scanning.
        try {
            removeUnnecessaryGenres(existing, IndexType.GENRE);
            removeUnnecessaryGenres(existing, IndexType.ALBUM_ID3_GENRE);
            // Must not be called asynchronously.
            // Genre data is required for subsequent scan processing.
            refreshMultiGenreMaster();
        } finally {
            writeUnlock(genreLock);
        }
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException" }) // lucene/HighFreqTerms#getHighFreqTerms
    private void removeUnnecessaryGenres(List<Genre> existing, IndexType genreType) {
        try {
            if (existing.isEmpty()) {
                writers.get(genreType).deleteAll();
                writers.get(genreType).flush();
                util.removeCacheAll();
                return;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Stop indexing to safely obtain a Searcher
        stopIndexing(genreType);
        IndexSearcher genreSearcher = getSearcher(genreType);
        if (genreSearcher == null) {
            return;
        }

        try {
            Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
            if (fields.isEmpty()) {
                return;
            }

            // Get the most frequent genre terms currently indexed
            TermStats[] highFreqTerms = HighFreqTerms
                .getHighFreqTerms(genreSearcher.getIndexReader(), HighFreqTerms.DEFAULT_NUMTERMS,
                        FieldNamesConstants.GENRE, new HighFreqTerms.DocFreqComparator());

            List<String> indexedNames = Stream
                .of(highFreqTerms)
                .map(t -> t.termtext.utf8ToString())
                .toList();

            List<String> existingNames = existing
                .stream()
                .map(Genre::getName)
                .collect(Collectors.toList());

            Term[] obsoleteTerms = indexedNames
                .stream()
                .filter(name -> !existingNames.contains(name))
                .map(String::hashCode)
                .map(DocumentFactory::createPrimarykey)
                .toArray(Term[]::new);

            // Reopen writer for editing and delete obsolete genres
            writers.put(genreType, createIndexWriter(genreType));
            writers.get(genreType).deleteDocuments(obsoleteTerms);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            LOG.info("The genre field may not exist.");
        } finally {
            release(genreType, genreSearcher);
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
     * Close Writer of all indexes and update SearcherManager. Called at the end of
     * the Scan flow.
     */
    public void stopIndexing() {
        Arrays.asList(IndexType.values()).forEach(this::stopIndexing);
        util.removeCacheAll();
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    private void stopIndexing(IndexType type) {

        // Close the writer safely using try-with-resources
        try (IndexWriter writer = writers.get(type)) {
            if (writer == null) {
                return;
            }
            writers.get(type).commit();
            writer.close();
            writers.remove(type);
        } catch (IOException e) {
            // Remove the writer from the map after closing
            writers.remove(type);
            throw new UncheckedIOException(e);
        }

        // Refresh the reader as the index may have changed
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
     * Return the IndexSearcher of the specified index. At initial startup, it may
     * return null if the user performs any search before performing a scan.
     */
    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. SearcherManager inherits Closeable but ensures each searcher
     * is closed only once all threads have finished using it. No explicit close is
     * done here.
     */
    public @Nullable IndexSearcher getSearcher(@NonNull IndexType indexType) {
        readLock(genreLock);
        try {
            if (!searchers.containsKey(indexType)
                    && !initSearcherManagerWithLockUpgrade(indexType)) {
                return null;
            }

            SearcherManager manager = searchers.get(indexType);
            if (manager != null) {
                try {
                    return manager.acquire();
                } catch (ClassCastException | IOException e) {
                    LOG.warn("Failed to acquire IndexSearcher for {}.", indexType, e);
                }
            }
        } finally {
            readUnlock(genreLock);
        }
        return null;
    }

    /**
     * Upgrade read lock to write lock to initialize SearcherManager if index
     * exists. Returns true if initialization succeeded or already exists, false
     * otherwise.
     */
    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. SearcherManager inherits Closeable but ensures each searcher
     * is closed only once all threads have finished using it. No explicit close is
     * done here.
     */
    private boolean initSearcherManagerWithLockUpgrade(IndexType indexType) {
        readUnlock(genreLock);
        writeLock(genreLock);
        try {
            // Double-check to avoid race condition
            if (searchers.containsKey(indexType)) {
                return true;
            }

            Path indexDirectory = getIndexDirectory(indexType);
            if (!Files.exists(indexDirectory)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("{} does not exist. Please run a scan.", indexDirectory);
                }
                return false;
            }

            try {
                SearcherFactory searcherFactory = new CustomSearcherFactory(shortExecutor);
                SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory),
                        searcherFactory);
                searchers.put(indexType, manager);
                return true;
            } catch (IndexNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG
                        .debug("Index {} does not exist in {}, likely not yet created.", indexType,
                                indexDirectory);
                }
                return false;
            } catch (IOException e) {
                LOG.warn("Failed to initialize SearcherManager for {}.", indexType, e);
                return false;
            }
        } finally {
            writeUnlock(genreLock);
            readLock(genreLock);
        }
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
                // #1280 This method is called automatically from various finally clauses.
                // If you have never scanned, Searcher is null.
                if (indexSearcher != null) {
                    try {
                        indexSearcher.getIndexReader().close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        } finally {
            writeUnlock(genreLock);
        }
    }

    /**
     * Check the version of the index and clean it up if necessary. Legacy type
     * indexes (files or directories starting with lucene) are deleted. If there is
     * no index directory, initialize the directory. If the index directory exists
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
        Pattern legacyPattern = Pattern.compile("^lucene\\d+$");
        String legacyName = "index";
        Path homeDir = SettingsService.getJpsonicHome();

        try (Stream<Path> files = Files.list(homeDir)) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return legacyPattern.matcher(name).matches() || legacyName.equals(name);
            }).forEach(path -> deleteFile("legacy index file", path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void deleteOldFiles() {
        // Delete old index files except the current root index directory
        String currentRootDirName = getRootIndexDirectory().getFileName().toString();
        Pattern indexPattern = Pattern.compile("^" + luceneUtils.getIndexRootDirName() + "\\d+$");
        Path homeDir = SettingsService.getJpsonicHome();

        try (Stream<Path> files = Files.list(homeDir)) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return indexPattern.matcher(name).matches() && !currentRootDirName.equals(name);
            }).forEach(path -> deleteFile("old index file", path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a directory corresponding to the current index version.
     */
    void initializeIndexDirectory() {
        Path rootIndexDir = getRootIndexDirectory();

        // Check if the current version of the index exists
        if (Files.exists(rootIndexDir)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Index was found (index version {}).", luceneUtils.getIndexVersion());
            }
            return;
        }

        // Delete all artist data if index doesn't exist
        artistDao.deleteAll();

        // Attempt to create the index directory
        if (FileUtil.createDirectories(rootIndexDir) == null && LOG.isWarnEnabled()) {
            LOG
                .warn("Failed to create index directory (index version {}).",
                        luceneUtils.getIndexVersion());
        }
    }

    /**
     * Get pre-parsed genre string from parsed genre string. The pre-analysis genre
     * text refers to the non-normalized genre text stored in the database layer.
     * The genre analysis includes tokenize processing. Therefore, the parsed genre
     * string and the cardinal of the unedited genre string are n: n.
     *
     * @param genres          list of analyzed genres
     * @param isFileStructure If true, returns the genre of FileStructure (Genres
     *                        associated with media types other than PODCAST).
     *                        Otherwise, returns the genre of Album (Multiple Genres
     *                        associated with ID3 Album).
     *
     * @return The entire pre-analysis genre text that contains the specified
     *         genres.
     *
     * @version 114.2.0
     *
     * @since 101.2.0
     */
    public List<String> toPreAnalyzedGenres(@NonNull List<String> genres, boolean isFileStructure) {
        if (genres.isEmpty()) {
            return Collections.emptyList();
        }

        IndexType genreType = isFileStructure ? IndexType.GENRE : IndexType.ALBUM_ID3_GENRE;
        readLock(genreLock);
        IndexSearcher searcher;
        try {
            searcher = getSearcher(genreType);
            if (searcher == null) {
                return Collections.emptyList();
            }
        } finally {
            readUnlock(genreLock);
        }

        Set<String> resultSet = new LinkedHashSet<>();
        try {
            Query query = queryFactory.toPreAnalyzedGenres(genres);
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            int totalHits = util.round(luceneUtils.getTotalHits(topDocs));

            for (int i = 0; i < totalHits; i++) {
                Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                IndexableField[] fields = doc.getFields(FieldNamesConstants.GENRE_KEY);

                if (fields != null) {
                    for (IndexableField field : fields) {
                        String value = field.stringValue();
                        if (value != null) {
                            resultSet.add(value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            release(genreType, searcher);
        }

        return new ArrayList<>(resultSet);
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        readLock(genreLock);
        try {
            // Refresh genre master cache if song count cache is empty
            if (util.getCache(LegacyGenreCriteria.SONG_COUNT).isEmpty()) {
                refreshMultiGenreMaster();
            }

            if (settingsService.isSortGenresByAlphabet()) {
                return getSortedGenres(sortByAlbum);
            }

            // If sorting alphabetically is not enabled, return cached list
            List<Genre> genres = sortByAlbum ? util.getCache(LegacyGenreCriteria.ALBUM_COUNT)
                    : util.getCache(LegacyGenreCriteria.SONG_COUNT);

            return isEmpty(genres) ? Collections.emptyList() : genres;

        } finally {
            readUnlock(genreLock);
        }
    }

    /** Get sorted genres, either by album or song, with caching */
    private List<Genre> getSortedGenres(boolean sortByAlbum) {
        LegacyGenreCriteria alphaCriteria = sortByAlbum ? LegacyGenreCriteria.ALBUM_ALPHABETICAL
                : LegacyGenreCriteria.SONG_ALPHABETICAL;

        if (util.containsCache(alphaCriteria)) {
            return util.getCache(alphaCriteria);
        }

        LegacyGenreCriteria countCriteria = sortByAlbum ? LegacyGenreCriteria.ALBUM_COUNT
                : LegacyGenreCriteria.SONG_COUNT;

        List<Genre> genres = new ArrayList<>();
        if (!isEmpty(util.getCache(countCriteria))) {
            genres.addAll(util.getCache(countCriteria));
            genres.sort(comparators.genreOrderByAlpha());
        }

        util.putCache(alphaCriteria, genres);
        return genres;
    }

    private void refreshMultiGenreMaster() {
        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM);

        try {
            if (isEmpty(genreSearcher) || isEmpty(songSearcher) || isEmpty(albumSearcher)) {
                return;
            }

            utilClearCaches();

            Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
            if (fields.isEmpty()) {
                LOG.info("The multi-genre master has been updated(no record).");
                return;
            }

            List<String> genreNames = getHighFrequencyGenreNames(genreSearcher);
            if (genreNames.isEmpty()) {
                LOG.info("The genre field may not exist.");
                return;
            }

            List<Genre> genres = collectGenres(genreNames, songSearcher, albumSearcher);
            cacheGenres(genres);

            LOG.info("The multi-genre master has been updated.");

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            release(IndexType.GENRE, genreSearcher);
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }
    }

    private void utilClearCaches() {
        Stream.of(LegacyGenreCriteria.values()).forEach(util::removeCache);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // lucene/HighFreqTerms#getHighFreqTerms
    private List<String> getHighFrequencyGenreNames(IndexSearcher genreSearcher)
            throws IOException {
        int numTerms = HighFreqTerms.DEFAULT_NUMTERMS;
        Comparator<TermStats> comparator = new HighFreqTerms.DocFreqComparator();
        TermStats[] stats;

        try {
            stats = HighFreqTerms
                .getHighFreqTerms(genreSearcher.getIndexReader(), numTerms,
                        FieldNamesConstants.GENRE, comparator);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        return Arrays
            .stream(stats)
            .map(t -> t.termtext.utf8ToString())
            .collect(Collectors.toList());
    }

    private List<Genre> collectGenres(List<String> genreNames, IndexSearcher songSearcher,
            IndexSearcher albumSearcher) throws IOException {

        List<Genre> genres = new ArrayList<>();
        for (String genreName : genreNames) {
            Query query = queryFactory.getGenre(genreName);
            TopDocs songDocs = songSearcher.search(query, Integer.MAX_VALUE);
            int songCount = util.round(luceneUtils.getTotalHits(songDocs));
            TopDocs albumDocs = albumSearcher.search(query, Integer.MAX_VALUE);
            int albumCount = util.round(luceneUtils.getTotalHits(albumDocs));
            genres.add(new Genre(genreName, songCount, albumCount));
        }
        return genres;
    }

    private void cacheGenres(List<Genre> genres) {
        genres.sort(comparators.genreOrder(false));
        util.putCache(LegacyGenreCriteria.SONG_COUNT, genres);

        List<Genre> genresByAlbum = genres
            .stream()
            .filter(g -> g.getAlbumCount() != 0)
            .sorted(comparators.genreOrder(true))
            .collect(Collectors.toList());

        util.putCache(LegacyGenreCriteria.ALBUM_COUNT, genresByAlbum);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // lucene/HighFreqTerms#getHighFreqTerms
    private List<GenreFreq> getGenreFreqs() {
        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        if (isEmpty(genreSearcher)) {
            return Collections.emptyList();
        }

        try {
            Collection<String> fields = FieldInfos.getIndexedFields(genreSearcher.getIndexReader());
            if (fields.isEmpty()) {
                LOG.info("The multi-genre master has been updated(no record).");
                return Collections.emptyList();
            }

            TermStats[] stats;
            try {
                stats = HighFreqTerms
                    .getHighFreqTerms(genreSearcher.getIndexReader(),
                            HighFreqTerms.DEFAULT_NUMTERMS, FieldNamesConstants.GENRE,
                            new HighFreqTerms.DocFreqComparator());
            } catch (Exception e) {
                LOG.info("The genre field may not exist.");
                return Collections.emptyList();
            }

            return Arrays
                .stream(stats)
                .map(stat -> new GenreFreq(stat.termtext.utf8ToString(), stat.docFreq))
                .toList();

        } finally {
            release(IndexType.GENRE, genreSearcher);
        }
    }

    private int getAlbumGenreCount(IndexSearcher searcher, String genreName,
            GenreMasterCriteria criteria) {
        try {
            Query query = queryFactory.getAlbumId3GenreCount(genreName, criteria.folders());
            return luceneUtils.getCount(searcher, query);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int getSongGenreCount(IndexSearcher searcher, String genreName,
            GenreMasterCriteria criteria) {
        try {
            MediaType[] types = criteria.types().length == 0 ? MUSIC_AND_AUDIOBOOK
                    : criteria.types();
            Query query = queryFactory.getSongGenreCount(genreName, criteria.folders(), types);
            return luceneUtils.getCount(searcher, query);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Genre> createGenreMaster(GenreMasterCriteria criteria) {
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(songSearcher) || isEmpty(albumSearcher)) {
            return Collections.emptyList();
        }

        try {
            List<Genre> result = new ArrayList<>();
            for (GenreFreq genreFreq : getGenreFreqs()) {
                String name = genreFreq.genre;
                int albumCount = getAlbumGenreCount(albumSearcher, name, criteria);
                int songCount = getSongGenreCount(songSearcher, name, criteria);

                boolean addGenre = switch (criteria.scope()) {
                case ALBUM -> albumCount > 0 && songCount > 0;
                case SONG -> songCount > 0;
                };

                if (addGenre) {
                    result.add(new Genre(name, songCount, albumCount));
                }
            }

            switch (criteria.sort()) {
            case NAME -> result.sort(comparators.genreOrderByAlpha());
            case ALBUM_COUNT -> {
                result.sort(comparators.genreOrderByAlpha());
                result.sort(comparators.genreOrder(true));
            }
            case SONG_COUNT -> {
                result.sort(comparators.genreOrderByAlpha());
                result.sort(comparators.genreOrder(false));
            }
            case FREQUENCY -> {
                // no sorting needed
            }
            }
            return result;

        } finally {
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }
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
        public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader)
                throws IOException {

            return new IndexSearcher(reader, executor);
        }
    }
}
