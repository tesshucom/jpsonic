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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
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
    private static final int INDEX_VERSION = 24;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index-JP";

    private static final Object GENRE_LOCK = new Object();

    /**
     * File supplier for index directory.
     */
    private static final Supplier<File> ROOT_INDEX_DIRECTORY = () -> new File(SettingsService.getJpsonicHome(),
            INDEX_ROOT_DIR_NAME.concat(Integer.toString(INDEX_VERSION)));

    /**
     * Returns the directory of the specified index
     */
    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    /*
     * The locale doesn't matter because just converting the literal.
     */
    private static final Function<IndexType, File> GET_INDEX_DIRECTORY = (indexType) -> new File(
            ROOT_INDEX_DIRECTORY.get(), indexType.toString().toLowerCase());

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
        Term primarykey = documentFactory.createPrimarykey(album);
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
        Term primarykey = documentFactory.createPrimarykey(artist);
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
        Term primarykey = documentFactory.createPrimarykey(mediaFile);
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
            if (!isEmpty(mediaFile.getGenre())) {
                primarykey = documentFactory.createPrimarykey(mediaFile.getGenre().hashCode());
                Document document = documentFactory.createGenreDocument(mediaFile);
                writers.get(IndexType.GENRE).updateDocument(primarykey, document);
            }
        } catch (IOException x) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to create search index for " + mediaFile, x);
            }
        }
    }

    public final void startIndexing() {
        try {
            for (IndexType indexType : IndexType.values()) {
                writers.put(indexType, createIndexWriter(indexType));
            }
            clearGenreMaster();
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        }
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File indexDirectory = GET_INDEX_DIRECTORY.apply(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzerFactory.getAnalyzer());
        return new IndexWriter(FSDirectory.open(indexDirectory.toPath()), config);
    }

    private void clearMultiGenreMaster() {
        synchronized (GENRE_LOCK) {
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

        Term[] primarykeys = mediaFileDao.getArtistExpungeCandidates().stream().map(documentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artist doc.", e);
        }

        primarykeys = mediaFileDao.getAlbumExpungeCandidates().stream().map(documentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete album doc.", e);
        }

        primarykeys = mediaFileDao.getSongExpungeCandidates().stream().map(documentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.SONG).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete song doc.", e);
        }

        primarykeys = artistDao.getExpungeCandidates().stream().map(documentFactory::createPrimarykey)
                .toArray(Term[]::new);
        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artistId3 doc.", e);
        }

        primarykeys = albumDao.getExpungeCandidates().stream().map(documentFactory::createPrimarykey)
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
    public void stopIndexing(MediaLibraryStatistics statistics) {
        Arrays.asList(IndexType.values()).forEach(indexType -> stopIndexing(indexType, statistics));
        clearMultiGenreMaster();
    }

    /**
     * Close Writer of specified index and refresh SearcherManager.
     */
    private void stopIndexing(IndexType type, MediaLibraryStatistics statistics) {

        boolean isUpdate = false;
        // close
        try (IndexWriter writer = writers.get(type)) {
            Map<String, String> userData = PlayerUtils.objectToStringMap(statistics);
            writer.setLiveCommitData(userData.entrySet());
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
     * Return the MediaLibraryStatistics saved on commit in the index. Ensures that each index reports the same data. On
     * invalid indices, returns null.
     */
    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. SearcherManager inherits Closeable but ensures each searcher is closed only once all threads have
     * finished using it. No explicit close is done here.
     */
    public @Nullable MediaLibraryStatistics getStatistics() {
        MediaLibraryStatistics stats = null;
        for (IndexType indexType : IndexType.values()) {
            IndexSearcher searcher = getSearcher(indexType);
            if (searcher == null) {
                return null;
            }
            IndexReader indexReader = searcher.getIndexReader();
            if (!(indexReader instanceof DirectoryReader)) {
                return null;
            }
            try {
                Map<String, String> userData = ((DirectoryReader) indexReader).getIndexCommit().getUserData();
                MediaLibraryStatistics currentStats = PlayerUtils.stringMapToValidObject(MediaLibraryStatistics.class,
                        userData);
                if (stats == null) {
                    stats = currentStats;
                } else if (!Objects.equals(stats, currentStats)) {
                    // Index type had differing stats data
                    return null;
                }
            } catch (IOException | IllegalArgumentException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exception encountered while fetching index commit data", e);
                }
                return null;
            } finally {
                release(indexType, searcher);
            }
        }
        return stats;
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
        if (!searchers.containsKey(indexType)) {
            File indexDirectory = GET_INDEX_DIRECTORY.apply(indexType);
            try {
                if (indexDirectory.exists()) {
                    SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory.toPath()), null);
                    searchers.put(indexType, manager);
                } else if (LOG.isWarnEnabled()) {
                    LOG.warn("{} does not exist. Please run a scan.", indexDirectory.getAbsolutePath());
                }
            } catch (IndexNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Index {} does not exist in {}, likely not yet created.", indexType.toString(),
                            indexDirectory.getAbsolutePath());
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
        return null;
    }

    public void release(IndexType indexType, IndexSearcher indexSearcher) {
        if (searchers.containsKey(indexType)) {
            try {
                searchers.get(indexType).release(indexSearcher);
            } catch (IOException e) {
                LOG.error("Failed to release IndexSearcher.", e);
                searchers.remove(indexType);
            }
        } else {
            // irregular case
            try {
                indexSearcher.getIndexReader().close();
            } catch (IOException e) {
                LOG.warn("Failed to release. IndexSearcher has been closed.", e);
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
        deleteOldMethodFiles();
    }

    private void deleteFile(String label, File old) {
        if (FileUtil.exists(old)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Found " + label + ". Try to delete : {}", old.getAbsolutePath());
            }
            try {
                if (old.isFile()) {
                    FileUtils.deleteQuietly(old);
                } else {
                    FileUtils.deleteDirectory(old);
                }
            } catch (IOException e) {
                // Log only if failed
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to delete " + label + " : ".concat(old.getAbsolutePath()), e);
                }
            }
        }
    }

    void deleteLegacyFiles() {
        // Delete legacy files unconditionally
        Arrays.stream(SettingsService.getJpsonicHome().listFiles(
                (file, name) -> Pattern.compile("^lucene\\d+$").matcher(name).matches() || "index".contentEquals(name)))
                .forEach(old -> deleteFile("legacy index file", old));
    }

    void deleteOldFiles() {
        // Delete if not old index version
        Arrays.stream(SettingsService.getJpsonicHome().listFiles(
                (file, name) -> Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$").matcher(name).matches()))
                .filter(dir -> !ROOT_INDEX_DIRECTORY.get().getName().equals(dir.getName()))
                .forEach(old -> deleteFile("old index file", old));
    }

    void deleteOldMethodFiles() {
        if (settingsService.isSearchMethodChanged()) {
            Arrays.stream(SettingsService.getJpsonicHome().listFiles(
                    (file, name) -> Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$").matcher(name).matches()))
                    .filter(dir -> ROOT_INDEX_DIRECTORY.get().getName().equals(dir.getName()))
                    .forEach(old -> deleteFile("index with different method", old));
            settingsService.setSearchMethodChanged(false);
            settingsService.save();
        }
    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory() {
        // Check if Index is current version
        if (ROOT_INDEX_DIRECTORY.get().exists()) {
            // Index of current version already exists
            if (settingsService.isVerboseLogStart() && LOG.isInfoEnabled()) {
                LOG.info("Index was found (index version {}). ", INDEX_VERSION);
            }
        } else {
            if (ROOT_INDEX_DIRECTORY.get().mkdir()) {
                LOG.info("Index directory was created (index version {}). ", INDEX_VERSION);
            } else {
                LOG.warn("Failed to create index directory :  (index version {}). ", INDEX_VERSION);
            }
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
    public List<String> toPreAnalyzedGenres(List<String> genres) {

        if (isEmpty(genres) || genres.size() == 0) {
            return Collections.emptyList();
        }

        IndexSearcher searcher = getSearcher(IndexType.GENRE);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
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

        if (multiGenreMaster.isEmpty()) {
            refreshMultiGenreMaster();
        }

        if (settingsService.isSortGenresByAlphabet() && sortByAlbum) {
            if (multiGenreMaster.containsKey(GenreSort.ALBUM_ALPHABETICAL)) {
                return multiGenreMaster.get(GenreSort.ALBUM_ALPHABETICAL);
            }
            synchronized (GENRE_LOCK) {
                List<Genre> albumGenres = new ArrayList<>();
                if (!isEmpty(multiGenreMaster.get(GenreSort.ALBUM_COUNT))) {
                    albumGenres.addAll(multiGenreMaster.get(GenreSort.ALBUM_COUNT));
                    albumGenres.sort(comparators.genreOrderByAlpha());
                }
                multiGenreMaster.put(GenreSort.ALBUM_ALPHABETICAL, albumGenres);
                return albumGenres;
            }
        } else if (settingsService.isSortGenresByAlphabet()) {
            if (multiGenreMaster.containsKey(GenreSort.SONG_ALPHABETICAL)) {
                return multiGenreMaster.get(GenreSort.SONG_ALPHABETICAL);
            }
            synchronized (GENRE_LOCK) {
                List<Genre> albumGenres = new ArrayList<>();
                if (!isEmpty(multiGenreMaster.get(GenreSort.SONG_COUNT))) {
                    albumGenres.addAll(multiGenreMaster.get(GenreSort.SONG_COUNT));
                    albumGenres.sort(comparators.genreOrderByAlpha());
                }
                multiGenreMaster.put(GenreSort.SONG_ALPHABETICAL, albumGenres);
                return albumGenres;
            }
        }

        List<Genre> genres = sortByAlbum ? multiGenreMaster.get(GenreSort.ALBUM_COUNT)
                : multiGenreMaster.get(GenreSort.SONG_COUNT);
        return isEmpty(genres) ? Collections.emptyList() : genres;

    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidCatchingGenericException" })
    /*
     * [AvoidInstantiatingObjectsInLoops] (Genre) Not reusable [AvoidCatchingGenericException] LOG Exception due to
     * constraints of 'lucene' {@link HighFreqTerms#getHighFreqTerms(IndexReader, int, String, Comparator)}
     */
    private void refreshMultiGenreMaster() {

        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM);

        try {
            if (!isEmpty(genreSearcher) && !isEmpty(songSearcher) && !isEmpty(albumSearcher)) {

                mayBeInit: synchronized (GENRE_LOCK) {

                    multiGenreMaster.clear();

                    int numTerms = HighFreqTerms.DEFAULT_NUMTERMS;
                    Comparator<TermStats> c = new HighFreqTerms.DocFreqComparator();
                    TermStats[] stats;
                    try {
                        stats = HighFreqTerms.getHighFreqTerms(genreSearcher.getIndexReader(), numTerms,
                                FieldNamesConstants.GENRE, c);
                    } catch (Exception e) {
                        LOG.error(
                                "The genre field may not exist. This is an expected error before scan or using library without genre. : ",
                                e);
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
