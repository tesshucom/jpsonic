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

package org.airsonic.player.service.search;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Util;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Function class that is strongly linked to the lucene index implementation.
 * Legacy has an implementation in SearchService.
 *
 * If the index CRUD and search functionality are in the same class,
 * there is often a dependency conflict on the class used.
 * Although the interface of SearchService is left to maintain the legacy implementation,
 * it is desirable that methods of index operations other than search essentially use this class directly.
 */
@Component
public class IndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    /**
     * Schema version of Airsonic index.
     * It may be incremented in the following cases:
     *
     *  - Incompatible update case in Lucene index implementation
     *  - When schema definition is changed due to modification of AnalyzerFactory,
     *    DocumentFactory or the class that they use.
     *
     */
    private static final int INDEX_VERSION = 20;

    /**
     * Literal name of index top directory.
     */
    private static final String INDEX_ROOT_DIR_NAME = "index-JP";

    /**
     * File supplier for index directory.
     */
    private Supplier<File> rootIndexDirectory = () ->
        new File(SettingsService.getJpsonicHome(), INDEX_ROOT_DIR_NAME.concat(Integer.toString(INDEX_VERSION)));

    /**
     * Returns the directory of the specified index
     */
    private Function<IndexType, File> getIndexDirectory = (indexType) ->
        new File(rootIndexDirectory.get(), indexType.toString().toLowerCase());

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private SearchServiceUtilities util;

    @Autowired
    private JpsonicComparators comparators;

    @Autowired
    private SettingsService settingsService;

    private Map<IndexType, SearcherManager> searchers = new EnumMap<>(IndexType.class);

    private Map<IndexType, IndexWriter> writers = new EnumMap<>(IndexType.class);

    private enum GenreSort {
        ALBUM_COUNT, SONG_COUNT, ALBUM_ALPHABETICAL, SONG_ALPHABETICAL
    }

    ;

    private Map<GenreSort, List<Genre>> multiGenreMaster = new EnumMap<>(GenreSort.class);

    public void index(Album album) {
        Term primarykey = documentFactory.createPrimarykey(album);
        Document document = documentFactory.createAlbumId3Document(album);
        try {
            writers.get(IndexType.ALBUM_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        Term primarykey = documentFactory.createPrimarykey(artist);
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        try {
            writers.get(IndexType.ARTIST_ID3).updateDocument(primarykey, document);
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
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
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public final void startIndexing() {
        try {
            for (IndexType IndexType : IndexType.values()) {
                writers.put(IndexType, createIndexWriter(IndexType));
            }
            clearGenreMaster();
        } catch (IOException e) {
            LOG.error("Failed to create search index.", e);
        }
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File indexDirectory = getIndexDirectory.apply(indexType);
        IndexWriterConfig config = new IndexWriterConfig(analyzerFactory.getAnalyzer());
        return new IndexWriter(FSDirectory.open(indexDirectory.toPath()), config);
    }

    private void clearMultiGenreMaster() {
        multiGenreMaster.clear();
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

        Term[] primarykeys = mediaFileDao.getArtistExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ARTIST).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artist doc.", e);
        }

        primarykeys = mediaFileDao.getAlbumExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ALBUM).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete album doc.", e);
        }

        primarykeys = mediaFileDao.getSongExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.SONG).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete song doc.", e);
        }

        primarykeys = artistDao.getExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ARTIST_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete artistId3 doc.", e);
        }

        primarykeys = albumDao.getExpungeCandidates().stream()
                .map(m -> documentFactory.createPrimarykey(m))
                .toArray(i -> new Term[i]);
        try {
            writers.get(IndexType.ALBUM_ID3).deleteDocuments(primarykeys);
        } catch (IOException e) {
            LOG.error("Failed to delete albumId3 doc.", e);
        }

    }

    /**
     * Close Writer of all indexes and update SearcherManager.
     * Called at the end of the Scan flow.
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
            Map<String,String> userData = Util.objectToStringMap(statistics);
            writer.setLiveCommitData(userData.entrySet());
            isUpdate = -1 != writers.get(type).commit();
            writer.close();
            writers.remove(type);
            LOG.trace("Success to create or update search index : [" + type + "]");
        } catch (IOException e) {
            writers.remove(type);
            LOG.error("Failed to create search index.", e);
        }

        // refresh reader as index may have been written
        if (isUpdate && searchers.containsKey(type)) {
            try {
                searchers.get(type).maybeRefresh();
                LOG.trace("SearcherManager has been refreshed : [" + type + "]");
            } catch (IOException e) {
                LOG.error("Failed to refresh SearcherManager : [" + type + "]", e);
                searchers.remove(type);
            }
        }

    }

    /**
     * Return the MediaLibraryStatistics saved on commit in the index. Ensures that each index reports the same data.
     * On invalid indices, returns null.
     */
    public @Nullable MediaLibraryStatistics getStatistics() {
        MediaLibraryStatistics stats = null;
        for (IndexType indexType : IndexType.values()) {
            IndexSearcher searcher = getSearcher(indexType);
            if (searcher == null) {
                LOG.trace("No index for type " + indexType);
                return null;
            }
            IndexReader indexReader = searcher.getIndexReader();
            if (!(indexReader instanceof DirectoryReader)) {
                LOG.warn("Unexpected index type " + indexReader.getClass());
                return null;
            }
            try {
                Map<String, String> userData = ((DirectoryReader) indexReader).getIndexCommit().getUserData();
                MediaLibraryStatistics currentStats = Util.stringMapToValidObject(MediaLibraryStatistics.class,
                        userData);
                if (stats == null) {
                    stats = currentStats;
                } else {
                    if (!Objects.equals(stats, currentStats)) {
                        LOG.warn("Index type " + indexType + " had differing stats data");
                        return null;
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.debug("Exception encountered while fetching index commit data", e);
                return null;
            }
        }
        return stats;
    }

    /**
     * Return the IndexSearcher of the specified index.
     * At initial startup, it may return null
     * if the user performs any search before performing a scan.
     */
    public @Nullable IndexSearcher getSearcher(IndexType indexType) {
        if (!searchers.containsKey(indexType)) {
            File indexDirectory = getIndexDirectory.apply(indexType);
            try {
                if (indexDirectory.exists()) {
                    SearcherManager manager = new SearcherManager(FSDirectory.open(indexDirectory.toPath()), null);
                    searchers.put(indexType, manager);
                } else {
                    LOG.warn("{} does not exist. Please run a scan.", indexDirectory.getAbsolutePath());
                }
            } catch (IndexNotFoundException e) {
                LOG.debug("Index {} does not exist in {}, likely not yet created.", indexType.toString(), indexDirectory.getAbsolutePath());
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
        } catch (Exception e) {
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
            } catch (Exception e) {
                LOG.warn("Failed to release. IndexSearcher has been closed.", e);
            }
        }
    }

    /**
     * Check the version of the index and clean it up if necessary.
     * Legacy type indexes (files or directories starting with lucene) are deleted.
     * If there is no index directory, initialize the directory.
     * If the index directory exists and is not the current version,
     * initialize the directory.
     */
    public void deleteOldIndexFiles() {

        // Delete legacy files unconditionally
        Arrays.stream(SettingsService.getJpsonicHome().listFiles(
            (file, name) -> Pattern.compile("^lucene\\d+$").matcher(name).matches() || "index".contentEquals(name)))
                .forEach(old -> {
                    if (FileUtil.exists(old)) {
                        LOG.info("Found legacy index file. Try to delete : {}", old.getAbsolutePath());
                        try {
                            if (old.isFile()) {
                                FileUtils.deleteQuietly(old);
                            } else {
                                FileUtils.deleteDirectory(old);
                            }
                        } catch (IOException e) {
                            // Log only if failed
                            LOG.warn("Failed to delete the legacy Index : ".concat(old.getAbsolutePath()), e);
                        }
                    }
                });

        // Delete if not old index version
        Arrays.stream(SettingsService.getJpsonicHome()
                .listFiles(
                    (file, name) -> Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$").matcher(name).matches()))
                .filter(dir -> !dir.getName().equals(rootIndexDirectory.get().getName())).forEach(old -> {
                    if (FileUtil.exists(old)) {
                        LOG.info("Found old index file. Try to delete : {}", old.getAbsolutePath());
                        try {
                            if (old.isFile()) {
                                FileUtils.deleteQuietly(old);
                            } else {
                                FileUtils.deleteDirectory(old);
                            }
                        } catch (IOException e) {
                            // Log only if failed
                            LOG.warn("Failed to delete the old Index : ".concat(old.getAbsolutePath()), e);
                        }
                    }
                });

        if (settingsService.isSearchMethodChanged()) {
            Arrays.stream(SettingsService.getJpsonicHome()
                .listFiles((file, name) -> Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$").matcher(name).matches()))
                .filter(dir -> dir.getName().equals(rootIndexDirectory.get().getName()))
                .forEach(old -> {
                    if (FileUtil.exists(old)) {
                        LOG.info("The search method has changed. Try to delete : {}", old.getAbsolutePath());
                        try {
                            if (old.isFile()) {
                                FileUtils.deleteQuietly(old);
                            } else {
                                FileUtils.deleteDirectory(old);
                            }
                        } catch (IOException e) {
                            // Log only if failed
                            LOG.warn("Failed to delete the Index : ".concat(old.getAbsolutePath()), e);
                        }
                    }
                });
            settingsService.setSearchMethodChanged(false);
            settingsService.save();
        }

    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory() {
        // Check if Index is current version
        if (rootIndexDirectory.get().exists()) {
            // Index of current version already exists
            LOG.info("Index was found (index version {}). ", INDEX_VERSION);
        } else {
            if (rootIndexDirectory.get().mkdir()) {
                LOG.info("Index directory was created (index version {}). ", INDEX_VERSION);
            } else {
                LOG.warn("Failed to create index directory :  (index version {}). ", INDEX_VERSION);
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
     * @return�@list of pre-analyzed genres
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
            release(IndexType.GENRE, searcher);
        }
        return result;
    }

    List<Genre> getGenres(boolean sortByAlbum) {

        synchronized (multiGenreMaster) {
            if (multiGenreMaster.isEmpty()) {
                refreshMultiGenreMaster();
            }
        }

        if (settingsService.isSortGenresByAlphabet() && sortByAlbum) {
            if (multiGenreMaster.containsKey(GenreSort.ALBUM_ALPHABETICAL)) {
                return multiGenreMaster.get(GenreSort.ALBUM_ALPHABETICAL);
            }
            synchronized (multiGenreMaster) {
                List<Genre> albumGenres = new ArrayList<Genre>();
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
            synchronized (multiGenreMaster) {
                List<Genre> albumGenres = new ArrayList<Genre>();
                if (!isEmpty(multiGenreMaster.get(GenreSort.SONG_COUNT))) {
                    albumGenres.addAll(multiGenreMaster.get(GenreSort.SONG_COUNT));
                    albumGenres.sort(comparators.genreOrderByAlpha());
                }
                multiGenreMaster.put(GenreSort.SONG_ALPHABETICAL, albumGenres);
                return albumGenres;
            }
        }

        List<Genre> genres = sortByAlbum
                ? multiGenreMaster.get(GenreSort.ALBUM_COUNT)
                : multiGenreMaster.get(GenreSort.SONG_COUNT);
        return isEmpty(genres) ? Collections.emptyList() : genres;

    }

    private void refreshMultiGenreMaster() {

        IndexSearcher genreSearcher = getSearcher(IndexType.GENRE);
        IndexSearcher songSearcher = getSearcher(IndexType.SONG);
        IndexSearcher albumSearcher = getSearcher(IndexType.ALBUM);

        try {
            if (!isEmpty(genreSearcher) && !isEmpty(songSearcher) && !isEmpty(albumSearcher)) {

                mayBeInit: synchronized (multiGenreMaster) {

                    multiGenreMaster.clear();

                    int numTerms = HighFreqTerms.DEFAULT_NUMTERMS;
                    Comparator<TermStats> c = new HighFreqTerms.DocFreqComparator();
                    TermStats[] stats = null;
                    try {
                        stats = HighFreqTerms.getHighFreqTerms(genreSearcher.getIndexReader(), numTerms, FieldNames.GENRE, c);
                    } catch (Exception e) {
                        LOG.error("The genre field may not exist. This is an expected error before scan or using library without genre. : ", e.toString());
                        break mayBeInit;
                    }
                    List<String> genreNames = Arrays.asList(stats).stream().map(t -> t.termtext.utf8ToString()).collect(Collectors.toList());

                    List<Genre> genres = new ArrayList<Genre>();
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

                    List<Genre> genresByAlbum = new ArrayList<Genre>();
                    genres.stream().filter(g -> 0 != g.getAlbumCount()).forEach(g -> genresByAlbum.add(g));
                    genresByAlbum.sort(comparators.genreOrder(true));
                    multiGenreMaster.put(GenreSort.ALBUM_COUNT, genresByAlbum);

                    LOG.info("The multi-genre master has been updated.");

                }

            }
        } catch (Exception e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            release(IndexType.GENRE, genreSearcher);
            release(IndexType.SONG, songSearcher);
            release(IndexType.ALBUM, albumSearcher);
        }

    }
}
