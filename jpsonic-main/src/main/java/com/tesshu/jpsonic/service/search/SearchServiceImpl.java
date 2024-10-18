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
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.spring.EhcacheConfiguration.RandomCacheKey;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public SearchServiceImpl(LuceneUtils luceneUtils, QueryFactory queryFactory, IndexManager indexManager,
            SearchServiceUtilities util, SettingsService settingsService, MediaFileDao mediaFileDao,
            AlbumDao albumDao) {
        super();
        this.luceneUtils = luceneUtils;
        this.queryFactory = queryFactory;
        this.indexManager = indexManager;
        this.util = util;
        this.settingsService = settingsService;
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
    }

    @Override
    public SearchResult search(SearchCriteria criteria) {

        SearchResult result = new SearchResult();
        int offset = criteria.getOffset();
        int count = criteria.getCount();
        result.setOffset(offset);

        if (count <= 0) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(criteria.getIndexType());
        if (searcher == null) {
            return result;
        }

        try {

            TopDocs topDocs = searcher.search(criteria.getParsedQuery(), offset + count);
            int totalHits = util.round.apply(luceneUtils.getTotalHits(topDocs));
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                util.addIfAnyMatch(result, criteria.getIndexType(),
                        searcher.storedFields().document(topDocs.scoreDocs[i].doc));
            }

            if (settingsService.isOutputSearchQuery() && LOG.isInfoEnabled()) {
                LOG.info("Web: Multi-field search : {} -> query:{}, offset:{}, count:{}", criteria.getIndexType(),
                        criteria.getQuery(), criteria.getOffset(), criteria.getCount());
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(criteria.getIndexType(), searcher);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    @SuppressLint(value = "NULL_DEREFERENCE", justification = "False positive. #1585")
    public <T> ParamSearchResult<T> search(UPnPSearchCriteria criteria) {

        int offset = criteria.getOffset();
        int count = criteria.getCount();

        ParamSearchResult<T> result = new ParamSearchResult<>();
        result.setOffset(offset);

        IndexType indexType = searchableIndex(criteria);
        if (count <= 0 || indexType == null) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (searcher == null) {
            return result;
        }

        writeUPnPSerchLog(indexType, criteria);

        try {

            TopDocs topDocs = searcher.search(criteria.getParsedQuery(), offset + count);
            int totalHits = util.round.apply(luceneUtils.getTotalHits(topDocs));
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            if (IndexType.ARTIST_ID3 == indexType) {
                ParamSearchResult<Artist> artistResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                    util.addIgnoreNull(artistResult, indexType, util.getId.apply(doc), Artist.class);
                }
                artistResult.getItems().forEach(a -> result.getItems().add((T) a));
            } else if (IndexType.ALBUM_ID3 == indexType) {
                ParamSearchResult<Album> albumResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                    util.addIgnoreNull(albumResult, indexType, util.getId.apply(doc), Album.class);
                }
                albumResult.getItems().forEach(a -> result.getItems().add((T) a));
            } else if (IndexType.SONG == indexType) {
                ParamSearchResult<MediaFile> songResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.storedFields().document(topDocs.scoreDocs[i].doc);
                    util.addIgnoreNull(songResult, indexType, util.getId.apply(doc), MediaFile.class);
                }
                songResult.getItems().forEach(a -> result.getItems().add((T) a));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(indexType, searcher);
        }
        return result;

    }

    private void writeUPnPSerchLog(IndexType indexType, UPnPSearchCriteria criteria) {
        if (settingsService.isOutputSearchQuery() && LOG.isInfoEnabled()) {
            LOG.info("UpnP: UpnP-compliant field search : {} -> query:{}, offset:{}, count:{}", indexType,
                    criteria.getQuery(), criteria.getOffset(), criteria.getCount());
        }
    }

    private @Nullable IndexType searchableIndex(UPnPSearchCriteria criteria) {
        IndexType indexType = null;
        if (Artist.class == criteria.getAssignableClass()) {
            indexType = IndexType.ARTIST_ID3;
        } else if (Album.class == criteria.getAssignableClass()) {
            indexType = IndexType.ALBUM_ID3;
        } else if (MediaFile.class == criteria.getAssignableClass()) {
            indexType = IndexType.SONG;
        }
        return indexType;
    }

    /**
     * Common processing of random method.
     *
     * @param count
     *            Number of albums to return.
     * @param id2ListCallBack
     *            Callback to get D from id and store it in List
     */
    private <D> List<D> createRandomDocsList(int count, IndexSearcher searcher, Query query,
            BiConsumer<List<D>, Integer> id2ListCallBack) throws IOException {

        List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(sd -> sd.doc)
                .collect(Collectors.toList());

        List<D> result = new ArrayList<>();
        while (!docs.isEmpty() && result.size() < count) {
            int randomPos = util.nextInt.apply(docs.size());
            Document document = searcher.storedFields().document(docs.get(randomPos));
            id2ListCallBack.accept(result, util.getId.apply(document));
            docs.remove(randomPos);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);
        if (searcher == null) {
            // At first start
            return Collections.emptyList();
        }

        try {

            Query query = queryFactory.getRandomSongs(criteria);
            return createRandomDocsList(criteria.getCount(), searcher, query,
                    (dist, id) -> util.addIgnoreNull(dist, IndexType.SONG, id));

        } catch (IOException e) {
            LOG.error("Failed to search or random songs.", e);
        } finally {
            indexManager.release(IndexType.SONG, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public List<MediaFile> getRandomSongs(int count, int offset, int casheMax, List<MusicFolder> musicFolders,
            String... genres) {

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<Integer>> addSubToResult = (ids) -> ids.stream().skip(offset).limit(count)
                .forEach(id -> util.addIgnoreNull(result, IndexType.SONG, id));
        util.getCache(RandomCacheKey.SONG, casheMax, musicFolders).ifPresent(addSubToResult);
        if (!result.isEmpty()) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);
        if (searcher == null) {
            return result;
        }

        Query query = queryFactory.getRandomSongs(musicFolders, genres);

        try {

            List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(sd -> sd.doc)
                    .collect(Collectors.toList());

            List<Integer> ids = new ArrayList<>();
            while (!docs.isEmpty() && ids.size() < casheMax) {
                int randomPos = util.nextInt.apply(docs.size());
                Document document = searcher.storedFields().document(docs.get(randomPos));
                ids.add(util.getId.apply(document));
                docs.remove(randomPos);
            }

            util.putCache(RandomCacheKey.SONG, casheMax, musicFolders, ids);

            addSubToResult.accept(ids);

        } catch (IOException e) {
            LOG.error("Failed to search for random songs.", e);
        } finally {
            indexManager.release(IndexType.SONG, searcher);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongsByArtist(Artist artist, int count, int offset, int casheMax,
            List<MusicFolder> musicFolders) {

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (files) -> files.stream().skip(offset).limit(count)
                .forEach(result::add);

        util.getCache(RandomCacheKey.SONG_BY_ARTIST, casheMax, musicFolders, artist.getName())
                .ifPresent(addSubToResult);
        if (!result.isEmpty()) {
            return result;
        }

        List<MediaFile> songs = mediaFileDao.getRandomSongsForAlbumArtist(casheMax, artist.getName(), musicFolders,
                (range, limit) -> {
                    List<Integer> randoms = new ArrayList<>();
                    while (randoms.size() < Math.min(limit, range)) {
                        Integer random = util.nextInt.apply(range);
                        if (!randoms.contains(random)) {
                            randoms.add(random);
                        }
                    }
                    return randoms;
                });

        util.putCache(RandomCacheKey.SONG_BY_ARTIST, casheMax, musicFolders, songs, artist.getName());

        addSubToResult.accept(songs);

        return result;

    }

    @Override
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM);
        if (searcher == null) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getRandomAlbums(musicFolders);

        try {

            return createRandomDocsList(count, searcher, query,
                    (dist, id) -> util.addIgnoreNull(dist, IndexType.ALBUM, id));

        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            indexManager.release(IndexType.ALBUM, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (searcher == null) {
            return Collections.emptyList();
        }

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);

        try {

            return createRandomDocsList(count, searcher, query,
                    (dist, id) -> util.addIgnoreNull(dist, IndexType.ALBUM_ID3, id));

        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
        } finally {
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Album> getRandomAlbumsId3(int count, int offset, int casheMax, List<MusicFolder> musicFolders) {

        final List<Album> result = new ArrayList<>();
        Consumer<List<Integer>> addSubToResult = (ids) -> ids.stream().skip(offset).limit(count)
                .forEach(id -> util.addIgnoreNull(result, IndexType.ALBUM_ID3, id));
        util.getCache(RandomCacheKey.ALBUM, casheMax, musicFolders).ifPresent(addSubToResult);
        if (!result.isEmpty()) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (searcher == null) {
            return result;
        }

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);

        try {

            List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(sd -> sd.doc)
                    .collect(Collectors.toList());

            List<Integer> ids = new ArrayList<>();
            while (!docs.isEmpty() && ids.size() < casheMax) {
                int randomPos = util.nextInt.apply(docs.size());
                Document document = searcher.storedFields().document(docs.get(randomPos));
                ids.add(util.getId.apply(document));
                docs.remove(randomPos);
            }

            util.putCache(RandomCacheKey.ALBUM, casheMax, musicFolders, ids);

            addSubToResult.accept(ids);

        } catch (IOException e) {
            LOG.error("Failed to search for random albums.", e);
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
        return genres.stream().skip(offset).limit(Math.min(genres.size() - offset, (int) maxResults)).toList();
    }

    @Override
    public List<Genre> getGenres(GenreMasterCriteria criteria, long offset, long maxResults) {
        if (maxResults <= 0) {
            return Collections.emptyList();
        }
        List<Genre> genres = util.getCache(criteria);
        if (genres.isEmpty()) {
            genres = indexManager.createGenreMaster(criteria);
            util.putCache(criteria, genres);
        }
        return genres.stream().skip(offset).limit(Math.min(genres.size() - offset, (int) maxResults)).toList();
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
    public List<MediaFile> getAlbumsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }
        List<String> preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList(genres), true);
        return mediaFileDao.getAlbumsByGenre(offset, count, preAnalyzedGenres, musicFolders);
    }

    @Override
    public List<Album> getAlbumId3sByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }
        List<String> preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList(genres), false);
        return albumDao.getAlbumsByGenre(offset, count, preAnalyzedGenres, musicFolders);
    }

    @Override
    public List<MediaFile> getSongsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders,
            MediaType... types) {
        final List<MediaFile> result = new ArrayList<>();
        if (isEmpty(genres)) {
            return result;
        }
        List<String> preAnalyzedGenres = indexManager.toPreAnalyzedGenres(Arrays.asList(genres), true);
        List<MediaType> targetTypes = types.length == 0 ? Arrays.asList(MediaType.MUSIC, MediaType.AUDIOBOOK)
                : Arrays.asList(types);
        return mediaFileDao.getSongsByGenre(preAnalyzedGenres, offset, count, musicFolders, targetTypes);
    }

    @Override
    public int getChildSizeOf(String genre, Album album, List<MusicFolder> folders, MediaType... types) {
        return mediaFileDao.getChildSizeOf(folders, indexManager.toPreAnalyzedGenres(Arrays.asList(genre), true),
                album.getArtist(), album.getName(), types);
    }

    @Override
    public List<MediaFile> getChildrenOf(String genre, Album album, int offset, int count, List<MusicFolder> folders,
            MediaType... types) {
        return mediaFileDao.getChildrenOf(folders, indexManager.toPreAnalyzedGenres(Arrays.asList(genre), true),
                album.getArtist(), album.getName(), offset, count, types);
    }
}
