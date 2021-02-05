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

package org.airsonic.player.service.search;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.JMediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.EhcacheConfiguration.RandomCacheKey;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private QueryFactory queryFactory;

    @Autowired
    private IndexManager indexManager;

    @Autowired
    private SearchServiceUtilities util;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private JMediaFileDao mediaFileDao;

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
        if (isEmpty(searcher)) {
            return result;
        }

        try {

            TopDocs topDocs = searcher.search(criteria.getParsedQuery(), offset + count);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                util.addIfAnyMatch(result, criteria.getIndexType(), searcher.doc(topDocs.scoreDocs[i].doc));
            }

            if (settingsService.isOutputSearchQuery()) {
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
    public <T> ParamSearchResult<T> search(UPnPSearchCriteria criteria) {

        int offset = criteria.getOffset();
        int count = criteria.getCount();

        ParamSearchResult<T> result = new ParamSearchResult<>();
        result.setOffset(offset);

        if (count <= 0) {
            return result;
        }

        IndexType indexType = searchableIndex(criteria);
        if (isEmpty(indexType)) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(indexType);
        if (isEmpty(searcher)) {
            return result;
        }

        if (settingsService.isOutputSearchQuery()) {
            LOG.info("UpnP: UpnP-compliant field search : {} -> query:{}, offset:{}, count:{}", indexType,
                    criteria.getQuery(), criteria.getOffset(), criteria.getCount());
        }

        try {

            TopDocs topDocs = searcher.search(criteria.getParsedQuery(), offset + count);
            int totalHits = util.round.apply(topDocs.totalHits.value);
            result.setTotalHits(totalHits);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            if (IndexType.ARTIST_ID3 == indexType) {
                ParamSearchResult<Artist> artistResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                    util.addIgnoreNull(artistResult, indexType, util.getId.apply(doc), Artist.class);
                }
                artistResult.getItems().forEach(a -> result.getItems().add((T) a));
            } else if (IndexType.ALBUM_ID3 == indexType) {
                ParamSearchResult<Album> albumResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                    util.addIgnoreNull(albumResult, indexType, util.getId.apply(doc), Album.class);
                }
                albumResult.getItems().forEach(a -> result.getItems().add((T) a));
            } else if (IndexType.SONG == indexType) {
                ParamSearchResult<MediaFile> songResult = new ParamSearchResult<>();
                for (int i = start; i < end; i++) {
                    Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
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

    private IndexType searchableIndex(UPnPSearchCriteria criteria) {
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
            Document document = searcher.doc(docs.get(randomPos));
            id2ListCallBack.accept(result, util.getId.apply(document));
            docs.remove(randomPos);
        }

        return result;
    }

    @Override
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {

        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);
        if (isEmpty(searcher)) {
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
    public List<MediaFile> getRandomSongs(int count, int offset, int casheMax, List<MusicFolder> musicFolders) {

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<Integer>> addSubToResult = (ids) -> ids
                .subList((int) offset, Math.min(ids.size(), (int) (offset + count)))
                .forEach(id -> util.addIgnoreNull(result, IndexType.SONG, id));
        util.getCache(RandomCacheKey.SONG, casheMax, musicFolders).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.SONG);
        if (isEmpty(searcher)) {
            return result;
        }

        Query query = queryFactory.getRandomSongs(musicFolders);

        try {

            List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(sd -> sd.doc)
                    .collect(Collectors.toList());

            List<Integer> ids = new ArrayList<>();
            while (!docs.isEmpty() && ids.size() < casheMax) {
                int randomPos = util.nextInt.apply(docs.size());
                Document document = searcher.doc(docs.get(randomPos));
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

    private int min(Integer... integers) {
        int min = Integer.MAX_VALUE;
        for (int i : integers) {
            min = Integer.min(min, i);
        }
        return min;
    }

    @Override
    public List<MediaFile> getRandomSongsByArtist(Artist artist, int count, int offset, int casheMax,
            List<MusicFolder> musicFolders) {

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (files) -> {
            List<MediaFile> sub = files.subList((int) offset, min(files.size(), (int) (offset + count), casheMax));
            result.addAll(sub);
        };

        util.getCache(RandomCacheKey.SONG_BY_ARTIST, casheMax, musicFolders, artist.getName())
                .ifPresent(addSubToResult);
        if (0 < result.size()) {
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
        if (isEmpty(searcher)) {
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
        if (isEmpty(searcher)) {
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
        Consumer<List<Integer>> addSubToResult = (ids) -> ids
                .subList((int) offset, Math.min(ids.size(), (int) (offset + count)))
                .forEach(id -> util.addIgnoreNull(result, IndexType.ALBUM_ID3, id));
        util.getCache(RandomCacheKey.ALBUM, casheMax, musicFolders).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(searcher)) {
            return result;
        }

        Query query = queryFactory.getRandomAlbumsId3(musicFolders);

        try {

            List<Integer> docs = Arrays.stream(searcher.search(query, Integer.MAX_VALUE).scoreDocs).map(sd -> sd.doc)
                    .collect(Collectors.toList());

            List<Integer> ids = new ArrayList<>();
            while (!docs.isEmpty() && ids.size() < casheMax) {
                int randomPos = util.nextInt.apply(docs.size());
                Document document = searcher.doc(docs.get(randomPos));
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
        return genres.subList((int) offset, Math.min(genres.size(), (int) (offset + maxResults)));
    }

    @Override
    public int getGenresCount(boolean sortByAlbum) {
        return getGenres(sortByAlbum).size();
    }

    @Override
    public List<MediaFile> getAlbumsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (mediaFiles) -> result
                .addAll(mediaFiles.subList((int) offset, Math.min(mediaFiles.size(), (int) (offset + count))));
        util.getCache(genres, musicFolders, IndexType.ALBUM).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        List<String> preAnalyzedGenresList = indexManager.toPreAnalyzedGenres(Arrays.asList(genres));
        final List<MediaFile> cache = mediaFileDao.getAlbumsByGenre(0, Integer.MAX_VALUE, preAnalyzedGenresList,
                musicFolders);
        util.putCache(genres, musicFolders, IndexType.ALBUM, cache);
        addSubToResult.accept(cache);
        return result;
    }

    @Override
    public List<Album> getAlbumId3sByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {

        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        IndexSearcher searcher = indexManager.getSearcher(IndexType.ALBUM_ID3);
        if (isEmpty(searcher)) {
            return Collections.emptyList();
        }

        List<Album> result = new ArrayList<>();
        try {
            SortField[] sortFields = Arrays.stream(IndexType.ALBUM_ID3.getFields())
                    .map(n -> new SortField(n, SortField.Type.STRING)).toArray(i -> new SortField[i]);
            Query query = queryFactory.getAlbumId3sByGenres(genres, musicFolders);
            TopDocs topDocs = searcher.search(query, offset + count, new Sort(sortFields));

            int totalHits = util.round.apply(topDocs.totalHits.value);
            int start = Math.min(offset, totalHits);
            int end = Math.min(start + count, totalHits);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                util.addAlbumId3IfAnyMatch.accept(result, util.getId.apply(doc));
            }

        } catch (IOException e) {
            LOG.error("Failed to execute Lucene search.", e);
        } finally {
            indexManager.release(IndexType.ALBUM_ID3, searcher);
        }
        return result;

    }

    @Override
    public List<MediaFile> getSongsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders) {
        if (isEmpty(genres)) {
            return Collections.emptyList();
        }

        final List<MediaFile> result = new ArrayList<>();
        Consumer<List<MediaFile>> addSubToResult = (mediaFiles) -> result
                .addAll(mediaFiles.subList((int) offset, Math.min(mediaFiles.size(), (int) (offset + count))));
        util.getCache(genres, musicFolders, IndexType.SONG).ifPresent(addSubToResult);
        if (0 < result.size()) {
            return result;
        }

        List<String> preAnalyzedGenresList = indexManager.toPreAnalyzedGenres(Arrays.asList(genres));
        final List<MediaFile> cache = mediaFileDao.getSongsByGenre(preAnalyzedGenresList, 0, Integer.MAX_VALUE,
                musicFolders);
        util.putCache(genres, musicFolders, IndexType.SONG, cache);
        addSubToResult.accept(cache);
        return result;
    }

}
