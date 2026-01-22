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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.SuppressLint;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import com.tesshu.jpsonic.persistence.param.ShuffleSelectionParam;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.spring.EhcacheConfiguration.RandomCacheKey;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of the SearchService interface providing various search
 * capabilities over music data indexed with Lucene.
 * 
 * <p>
 * This service handles executing Lucene queries, converting search results into
 * domain entities, caching random search results, and managing pagination and
 * filtering based on music folders, genres, artists, and media types.
 * </p>
 * 
 * <p>
 * Dependencies include:
 * <ul>
 * <li>Lucene utilities for search execution and query building</li>
 * <li>Data access objects (DAOs) for Albums and MediaFiles</li>
 * <li>Application-specific utilities and settings services</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The service supports both standard web search criteria and UPnP search
 * criteria, offering random selection capabilities with caching to optimize
 * performance.
 * </p>
 * 
 * <p>
 * Exception handling ensures that IO and Lucene-related errors are logged
 * without crashing the service, and resources such as IndexSearcher are
 * properly released.
 * </p>
 * 
 * @see SearchService
 * @see QueryFactory
 * @see IndexManager
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final LuceneUtils luceneUtils;
    private final QueryFactory queryFactory;
    private final IndexManager indexManager;
    private final SearchServiceUtilities util;
    private final SettingsService settingsService;
    private final MediaFileDao mediaFileDao;
    private final AlbumDao albumDao;

    public SearchServiceImpl(LuceneUtils luceneUtils, QueryFactory queryFactory,
            IndexManager indexManager, SearchServiceUtilities util, SettingsService settingsService,
            MediaFileDao mediaFileDao, AlbumDao albumDao) {
        super();
        this.luceneUtils = luceneUtils;
        this.queryFactory = queryFactory;
        this.indexManager = indexManager;
        this.util = util;
        this.settingsService = settingsService;
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
    }

    // Logs the search query if settings and log level allow it
    private void logSearchQueryIfNeeded(HttpSearchCriteria criteria) {
        if (settingsService.isOutputSearchQuery() && LOG.isInfoEnabled()) {
            LOG
                .info("Web: Multi-field search : {} -> query:{}, offset:{}, count:{}",
                        criteria.targetType(), criteria.input(), criteria.offset(),
                        criteria.count());
        }
    }

    @Override
    public SearchResult search(HttpSearchCriteria criteria) {
        SearchResult result = new SearchResult();
        int offset = criteria.offset();
        int count = criteria.count();
        result.setOffset(offset);

        // Return early if count is less than or equal to zero
        if (count <= 0) {
            return result;
        }

        // Get the IndexSearcher for the given target type
        IndexSearcher searcher = indexManager.getSearcher(criteria.targetType());
        if (searcher == null) {
            return result;
        }

        try {
            // Execute search with offset + count to allow paging
            TopDocs topDocs = searcher.search(criteria.parsedQuery(), offset + count);

            // Get the rounded total number of hits
            int totalHits = util.round(luceneUtils.getTotalHits(topDocs));
            result.setTotalHits(totalHits);

            // Calculate the result range (start to end) safely within bounds
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            // Add documents in the specified range to the result if they match
            for (int i = start; i < end; i++) {
                util
                    .addIfAnyMatch(result, criteria.targetType(),
                            searcher.storedFields().document(topDocs.scoreDocs[i].doc));
            }

            // Log the search query info if logging is enabled
            logSearchQueryIfNeeded(criteria);

        } catch (IOException e) {
            // Handle search failure
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            // Always release the searcher to avoid resource leaks
            indexManager.release(criteria.targetType(), searcher);
        }

        return result;
    }

    @Override
    @SuppressLint(value = "NULL_DEREFERENCE", justification = "False positive. #1585")
    public <T> ParamSearchResult<T> search(UPnPSearchCriteria criteria) {
        int offset = criteria.offset();
        int count = criteria.count();

        ParamSearchResult<T> result = new ParamSearchResult<>();
        result.setOffset(offset);

        IndexType indexType = criteria.targetType();

        // Early return if count is invalid or index type is null
        if (count <= 0 || indexType == null) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (searcher == null) {
            return result;
        }

        // Optionally log search info
        writeUPnPSerchLog(indexType, criteria);

        try {
            // Execute Lucene search with enough results for paging
            TopDocs topDocs = searcher.search(criteria.parsedQuery(), offset + count);

            // Set total number of hits
            int totalHits = util.round(luceneUtils.getTotalHits(topDocs));
            result.setTotalHits(totalHits);

            // Calculate result bounds
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            // Dispatch by index type
            if (IndexType.ARTIST_ID3 == indexType) {
                processDocuments(searcher, topDocs, start, end, result, indexType, Artist.class);
            } else if (IndexType.ALBUM_ID3 == indexType) {
                processDocuments(searcher, topDocs, start, end, result, indexType, Album.class);
            } else {
                processDocuments(searcher, topDocs, start, end, result, indexType, MediaFile.class);
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            // Always release resources
            indexManager.release(indexType, searcher);
        }

        return result;
    }

    /**
     * Generic method to extract documents, convert them to desired type, and add to
     * result.
     */
    @SuppressWarnings("unchecked")
    private <T, E> void processDocuments(IndexSearcher searcher, TopDocs topDocs, int start,
            int end, ParamSearchResult<T> result, IndexType indexType, Class<E> clazz)
            throws IOException {

        ParamSearchResult<E> tempResult = new ParamSearchResult<>();

        for (int i = start; i < end; i++) {
            Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
            util.addEntityIfPresent(tempResult, indexType, util.getId(doc), clazz);
        }

        // Add items to the original result (with casting)
        tempResult.getItems().forEach(item -> result.getItems().add((T) item));
    }

    private void writeUPnPSerchLog(IndexType indexType, UPnPSearchCriteria criteria) {
        if (settingsService.isOutputSearchQuery() && LOG.isInfoEnabled()) {
            LOG
                .info("UpnP: UpnP-compliant field search : {} -> query:{}, offset:{}, count:{}",
                        indexType, criteria.input(), criteria.offset(), criteria.count());
        }
    }

    /**
     * Common processing of random method.
     *
     * @param count            Number of albums to return.
     * @param idToListCallback Callback to get D from id and store it in List
     */
    private <D> List<D> createRandomDocsList(int count, IndexSearcher searcher, Query query,
            BiConsumer<List<D>, Integer> idToListCallback) throws IOException {

        // Get all matching document IDs from Lucene
        List<Integer> docIds = Arrays
            .stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs)
            .map(sd -> sd.doc)
            .collect(Collectors.toList());

        List<D> result = new ArrayList<>();

        // Randomly pick documents until the desired count is reached or list is
        // exhausted
        while (!docIds.isEmpty() && result.size() < count) {
            int randomIndex = util.nextInt(docIds.size());

            // Fetch the document at the random position
            Document document = searcher.storedFields().document(docIds.get(randomIndex));

            // Convert the document ID and add to result via the callback
            idToListCallback.accept(result, util.getId(document));

            // Remove selected doc to avoid duplicates
            docIds.remove(randomIndex);
        }

        return result;
    }

    @SuppressWarnings("PMD.LambdaCanBeMethodReference") // false positive
    @Override
    public List<MediaFile> getRandomSongs(ShuffleSelectionParam criteria) {
        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);

        // Return empty list if no searcher is available (e.g., on first startup)
        if (searcher == null) {
            return Collections.emptyList();
        }

        try {
            // Build query to fetch candidate songs for random selection
            Query query = queryFactory.getRandomSongs(criteria);

            // Create a random list of MediaFile objects using the callback
            return createRandomDocsList(criteria.getCount(), searcher, query,
                    (resultList, id) -> util.addMediaFileIfAnyMatch(resultList, id));

        } catch (IOException e) {
            // Log any Lucene or IO-related failures
            LOG.error("Failed to search for random songs.", e);
        } finally {
            // Always release the searcher to prevent resource leaks
            indexManager.release(IndexType.SONG, searcher);
        }

        // Return empty list if something went wrong
        return Collections.emptyList();
    }

    @Override
    public List<MediaFile> getRandomSongs(int count, int offset, int cacheMax,
            List<MusicFolder> musicFolders, String... genres) {

        final List<MediaFile> result = new ArrayList<>();

        // Callback to add a sublist of IDs (after offset & limit) to the result list
        Consumer<List<Integer>> addSubsetToResult = ids -> ids
            .stream()
            .skip(offset)
            .limit(count)
            .forEach(id -> util.addMediaFileIfAnyMatch(result, id));

        // Try to get cached IDs first
        util.getCache(RandomCacheKey.SONG, cacheMax, musicFolders).ifPresent(addSubsetToResult);

        if (!result.isEmpty()) {
            return result; // Return if cache hit
        }

        // Get Lucene IndexSearcher for songs
        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);
        if (searcher == null) {
            return result;
        }

        try {
            // Build query based on folders and genres
            Query query = queryFactory.getRandomSongs(musicFolders, genres);

            // Fetch all matching Lucene documents
            List<Integer> docIds = Arrays
                .stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs)
                .map(sd -> sd.doc)
                .collect(Collectors.toList());

            // Select up to `cacheMax` unique random IDs
            List<Integer> ids = new ArrayList<>();
            while (!docIds.isEmpty() && ids.size() < cacheMax) {
                int randomIndex = util.nextInt(docIds.size());
                Document doc = searcher.storedFields().document(docIds.get(randomIndex));
                ids.add(util.getId(doc));
                docIds.remove(randomIndex);
            }

            // Store the randomly selected IDs in cache
            util.putCache(RandomCacheKey.SONG, cacheMax, musicFolders, ids);

            // Apply offset & limit, and add to result
            addSubsetToResult.accept(ids);

        } catch (IOException e) {
            LOG.error("Failed to search for random songs.", e);
        } finally {
            // Always release searcher to avoid resource leak
            indexManager.release(IndexType.SONG, searcher);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongsByArtist(Artist artist, int count, int offset,
            int cacheMax, List<MusicFolder> musicFolders) {

        final List<MediaFile> result = new ArrayList<>();

        // Define logic to extract a sublist (offset + count) from the source list
        Consumer<List<MediaFile>> addSubsetToResult = files -> files
            .stream()
            .skip(offset)
            .limit(count)
            .forEach(result::add);

        // Try retrieving from cache first
        util
            .getCache(RandomCacheKey.SONG_BY_ARTIST, cacheMax, musicFolders, artist.getName())
            .ifPresent(addSubsetToResult);

        if (!result.isEmpty()) {
            return result; // Return if cached result exists
        }

        // Get random songs for the artist from the database
        List<MediaFile> songs = mediaFileDao
            .getRandomSongsForAlbumArtist(cacheMax, artist.getName(), musicFolders,
                    (range, limit) -> {
                        // Generate a list of unique random integers within [0, range)
                        List<Integer> randomIndices = new ArrayList<>();
                        while (randomIndices.size() < Math.min(limit, range)) {
                            int random = util.nextInt(range);
                            if (!randomIndices.contains(random)) {
                                randomIndices.add(random);
                            }
                        }
                        return randomIndices;
                    });

        // Cache the retrieved songs for future access
        util
            .putCache(RandomCacheKey.SONG_BY_ARTIST, cacheMax, musicFolders, songs,
                    artist.getName());

        // Add the subset (offset + count) to the result
        addSubsetToResult.accept(songs);

        return result;
    }

    @SuppressWarnings("PMD.LambdaCanBeMethodReference") // false positive
    @Override
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {
        // Get Lucene IndexSearcher for albums
        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM);

        // Return empty list if searcher is unavailable (e.g., index not ready)
        if (searcher == null) {
            return Collections.emptyList();
        }

        try {
            // Create query to retrieve albums from the given folders
            Query query = queryFactory.getRandomAlbums(musicFolders);

            // Select random documents and convert them to MediaFile objects
            return createRandomDocsList(count, searcher, query,
                    (resultList, id) -> util.addMediaFileIfAnyMatch(resultList, id));

        } catch (IOException e) {
            // Log error if Lucene search fails
            LOG.error("Failed to search for random albums.", e);

        } finally {
            // Ensure resources are properly released
            indexManager.release(IndexType.ALBUM, searcher);
        }

        // Fallback: return empty list if exception occurs
        return Collections.emptyList();
    }

    @SuppressWarnings("PMD.LambdaCanBeMethodReference") // false positive
    @Override
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {
        // Obtain IndexSearcher for ID3-based albums
        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);

        // Return empty list if index is not available
        if (searcher == null) {
            return Collections.emptyList();
        }

        try {
            // Build query to search for random albums (ID3-based) within the given folders
            Query query = queryFactory.getRandomAlbumsId3(musicFolders);

            // Fetch random album documents and convert them to Album entities
            return createRandomDocsList(count, searcher, query,
                    (resultList, id) -> util.addAlbumId3IfAnyMatch(resultList, id));

        } catch (IOException e) {
            // Log any IO or Lucene search error
            LOG.error("Failed to search for random albums (ID3).", e);

        } finally {
            // Ensure the IndexSearcher is always released
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }

        // Fallback in case of errors
        return Collections.emptyList();
    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, int offset, int cacheMax,
            List<MusicFolder> musicFolders) {
        final List<Album> result = new ArrayList<>();

        // Callback to apply offset & limit and add to result
        Consumer<List<Integer>> addSubsetToResult = ids -> ids
            .stream()
            .skip(offset)
            .limit(count)
            .forEach(id -> util.addAlbumId3IfAnyMatch(result, id));

        // Try loading from cache first
        util.getCache(RandomCacheKey.ALBUM, cacheMax, musicFolders).ifPresent(addSubsetToResult);

        if (!result.isEmpty()) {
            return result; // Return if cached data is available
        }

        // Retrieve IndexSearcher for ALBUM_ID3 index
        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (searcher == null) {
            return result;
        }

        try {
            // Build Lucene query for albums
            Query query = queryFactory.getRandomAlbumsId3(musicFolders);

            // Fetch all matching document IDs
            List<Integer> docIds = Arrays
                .stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs)
                .map(sd -> sd.doc)
                .collect(Collectors.toList());

            // Select up to `cacheMax` random IDs without duplicates
            List<Integer> selectedIds = new ArrayList<>();
            while (!docIds.isEmpty() && selectedIds.size() < cacheMax) {
                int randomIndex = util.nextInt(docIds.size());
                Document document = searcher.storedFields().document(docIds.get(randomIndex));
                selectedIds.add(util.getId(document));
                docIds.remove(randomIndex);
            }

            // Cache the selected album IDs
            util.putCache(RandomCacheKey.ALBUM, cacheMax, musicFolders, selectedIds);

            // Apply offset and count to final result
            addSubsetToResult.accept(selectedIds);

        } catch (IOException e) {
            LOG.error("Failed to search for random albums (ID3).", e);
        } finally {
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }

        return result;
    }

    @Override
    public List<Genre> getGenres(boolean sortByAlbum) {
        return indexManager.getGenres(sortByAlbum);
    }

    @Override
    public List<Genre> getGenres(boolean sortByAlbum, long offset, long maxResults) {
        List<Genre> genres = getGenres(sortByAlbum);
        return genres
            .stream()
            .skip(offset)
            .limit(Math.min(genres.size() - offset, (int) maxResults))
            .toList();
    }

    @Override
    public List<Genre> getGenres(GenreMasterCriteria criteria, long offset, long maxResults) {
        // Return empty list if maxResults is zero or negative
        if (maxResults <= 0) {
            return Collections.emptyList();
        }

        // Try to get cached genres for the given criteria
        List<Genre> genres = util.getCache(criteria);

        // If cache is empty, create genre master list and cache it
        if (genres.isEmpty()) {
            genres = indexManager.createGenreMaster(criteria);
            util.putCache(criteria, genres);
        }

        // Safely skip offset and limit results, making sure not to exceed list size
        int start = (int) Math.min(offset, genres.size());
        int limit = (int) Math.min(maxResults, genres.size() - start);

        return genres.stream().skip(start).limit(limit).toList();
    }

    @Override
    public int getGenresCount(boolean sortByAlbum) {
        return getGenres(sortByAlbum).size();
    }

    @Override
    public int getGenresCount(GenreMasterCriteria criteria) {
        return getGenres(criteria, 0, Integer.MAX_VALUE).size();
    }

    @Override
    public List<MediaFile> getAlbumsByGenres(String genres, int offset, int count,
            List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }
        List<String> preAnalyzedGenres = indexManager
            .toPreAnalyzedGenres(Arrays.asList(genres), true);
        return mediaFileDao.getAlbumsByGenre(offset, count, preAnalyzedGenres, musicFolders);
    }

    @Override
    public List<Album> getAlbumId3sByGenres(String genres, int offset, int count,
            List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }
        List<String> preAnalyzedGenres = indexManager
            .toPreAnalyzedGenres(Arrays.asList(genres), false);
        return albumDao.getAlbumsByGenre(offset, count, preAnalyzedGenres, musicFolders);
    }

    @Override
    public List<MediaFile> getSongsByGenres(String genres, int offset, int count,
            List<MusicFolder> musicFolders, MediaType... types) {
        // Return empty list if genres string is null or empty
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        // Convert input genres string (already processed as a list) into pre-analyzed
        // genre tokens
        List<String> preAnalyzedGenres = indexManager
            .toPreAnalyzedGenres(Arrays.asList(genres), true);

        // Use provided media types or default to MUSIC and AUDIOBOOK
        List<MediaType> targetTypes = (types.length == 0)
                ? Arrays.asList(MediaType.MUSIC, MediaType.AUDIOBOOK)
                : Arrays.asList(types);

        // Delegate search to DAO
        return mediaFileDao
            .getSongsByGenre(preAnalyzedGenres, offset, count, musicFolders, targetTypes);
    }

    @Override
    public int getChildSizeOf(String genre, Album album, List<MusicFolder> folders,
            MediaType... types) {
        return mediaFileDao
            .getChildSizeOf(folders, indexManager.toPreAnalyzedGenres(Arrays.asList(genre), true),
                    album.getArtist(), album.getName(), types);
    }

    @Override
    public List<MediaFile> getChildrenOf(String genre, Album album, int offset, int count,
            List<MusicFolder> folders, MediaType... types) {
        return mediaFileDao
            .getChildrenOf(folders, indexManager.toPreAnalyzedGenres(Arrays.asList(genre), true),
                    album.getArtist(), album.getName(), offset, count, types);
    }
}
