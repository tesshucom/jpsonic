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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.spring.EhcacheConfiguration.RandomCacheKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Termination used by SearchService.
 *
 * Since SearchService operates as a proxy for storage (DB) using lucene, there are many redundant descriptions
 * different from essential data processing. This class is a transfer class for saving those redundant descriptions.
 *
 * Exception handling is not termination, so do not include exception handling in this class.
 */
@Component
public class SearchServiceUtilities {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceUtilities.class);

    /* Search by id only. */
    @Autowired
    private ArtistDao artistDao;

    /* Search by id only. */
    @Autowired
    private AlbumDao albumDao;

    @Autowired
    @Qualifier("searchCache")
    private Ehcache searchCache;

    @Autowired
    @Qualifier("randomCache")
    private Ehcache randomCache;

    /*
     * Search by id only. Although there is no influence at present, mediaFileService has a caching mechanism. Service
     * is used instead of Dao until you are sure you need to use mediaFileDao.
     */
    @Autowired
    private MediaFileService mediaFileService;

    private static Random random;

    static {
        try {
            random = SecureRandom.getInstance("NativePRNG");
            LOG.info("NativePRNG is used to create a random list of songs.");
        } catch (NoSuchAlgorithmException e) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
                LOG.info("SHA1PRNG is used to create a random list of songs.");
            } catch (NoSuchAlgorithmException e1) {
                random = new Random(System.currentTimeMillis());
                LOG.info("NativePRNG and SHA1PRNG cannot be used on this platform.");
            }
        }
    }

    public Function<Integer, Integer> nextInt = (range) -> random.nextInt(range);

    public final Function<Long, Integer> round = (i) -> {
        // return
        // NumericUtils.floatToSortableInt(i);
        return i.intValue();
    };

    public final Function<Document, Integer> getId = d -> {
        return Integer.valueOf(d.get(FieldNamesConstants.ID));
    };

    public final BiConsumer<List<MediaFile>, Integer> addMediaFileIfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(m -> id == m.getId())) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (!isEmpty(mediaFile)) {
                dist.add(mediaFile);
            }
        }
    };

    public final BiConsumer<List<Artist>, Integer> addArtistId3IfAnyMatch = (dist, id) -> {
        if (!dist.stream().anyMatch(a -> id == a.getId())) {
            Artist artist = artistDao.getArtist(id);
            if (!isEmpty(artist)) {
                dist.add(artist);
            }
        }
    };

    public final Function<Class<?>, @Nullable IndexType> getIndexType = (assignableClass) -> {
        IndexType indexType = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            indexType = IndexType.ALBUM_ID3;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            indexType = IndexType.ARTIST_ID3;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            indexType = IndexType.SONG;
        }
        return indexType;
    };

    public final Function<Class<?>, @Nullable String> getFieldName = (assignableClass) -> {
        String fieldName = null;
        if (assignableClass.isAssignableFrom(Album.class)) {
            fieldName = FieldNamesConstants.ALBUM;
        } else if (assignableClass.isAssignableFrom(Artist.class)) {
            fieldName = FieldNamesConstants.ARTIST;
        } else if (assignableClass.isAssignableFrom(MediaFile.class)) {
            fieldName = FieldNamesConstants.TITLE;
        }
        return fieldName;
    };

    public final BiConsumer<List<Album>, Integer> addAlbumId3IfAnyMatch = (dist, subjectId) -> {
        if (!dist.stream().anyMatch(a -> subjectId == a.getId())) {
            Album album = albumDao.getAlbum(subjectId);
            if (!isEmpty(album)) {
                dist.add(album);
            }
        }
    };

    @SuppressWarnings("unchecked")
    public final boolean addIgnoreNull(@SuppressWarnings("rawtypes") Collection collection, Object object) {
        return CollectionUtils.addIgnoreNull(collection, object);
    }

    public final boolean addIgnoreNull(Collection<?> collection, IndexType indexType, int subjectId) {
        if (indexType == IndexType.ALBUM || indexType == IndexType.SONG) {
            return addIgnoreNull(collection, mediaFileService.getMediaFile(subjectId));
        } else if (indexType == IndexType.ALBUM_ID3) {
            return addIgnoreNull(collection, albumDao.getAlbum(subjectId));
        }
        return false;
    }

    public final <T> void addIgnoreNull(ParamSearchResult<T> dist, IndexType indexType, int subjectId,
            Class<T> subjectClass) {
        if (indexType == IndexType.SONG || indexType == IndexType.ALBUM || indexType == IndexType.ARTIST) {
            MediaFile mediaFile = mediaFileService.getMediaFile(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(mediaFile));
        } else if (indexType == IndexType.ARTIST_ID3) {
            Artist artist = artistDao.getArtist(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(artist));
        } else if (indexType == IndexType.ALBUM_ID3) {
            Album album = albumDao.getAlbum(subjectId);
            addIgnoreNull(dist.getItems(), subjectClass.cast(album));
        }
    }

    public final void addIfAnyMatch(SearchResult dist, IndexType subjectIndexType, Document subject) {
        int documentId = getId.apply(subject);
        if (subjectIndexType == IndexType.ARTIST || subjectIndexType == IndexType.ALBUM
                || subjectIndexType == IndexType.SONG) {
            addMediaFileIfAnyMatch.accept(dist.getMediaFiles(), documentId);
        } else if (subjectIndexType == IndexType.ARTIST_ID3) {
            addArtistId3IfAnyMatch.accept(dist.getArtists(), documentId);
        } else if (subjectIndexType == IndexType.ALBUM_ID3) {
            addAlbumId3IfAnyMatch.accept(dist.getAlbums(), documentId);
        }
    }

    public final String[] filterComposer(String[] fields, boolean includeComposer) {
        return Arrays.asList(fields).stream()
                .filter(f -> includeComposer ? true
                        : !(FieldNamesConstants.COMPOSER.equals(f) || FieldNamesConstants.COMPOSER_READING.equals(f)))
                .collect(Collectors.toList()).toArray(new String[0]);
    }

    private String createCacheKey(String genres, List<MusicFolder> musicFolders, IndexType indexType) {
        StringBuilder b = new StringBuilder();
        b.append(genres).append('[');
        musicFolders.forEach(m -> b.append(m.getId()).append(','));
        b.append(']').append(indexType.name());
        return b.toString();
    }

    private String createCacheKey(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders,
            String... additional) {
        StringBuilder b = new StringBuilder();
        b.append(key).append(',').append(casheMax).append('[');
        musicFolders.forEach(m -> b.append(m.getId()).append(','));
        if (!isEmpty(additional)) {
            Arrays.asList(additional).stream().forEach(s -> b.append(s).append(','));
        }
        b.append(']');
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<MediaFile>> getCache(String genres, List<MusicFolder> musicFolders, IndexType indexType) {
        List<MediaFile> mediaFiles = null;
        Element element;
        synchronized (searchCache) {
            element = searchCache.get(createCacheKey(genres, musicFolders, indexType));
        }
        if (!isEmpty(element)) {
            mediaFiles = (List<MediaFile>) element.getObjectValue();
        }
        return Optional.ofNullable(mediaFiles);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<MediaFile>> getCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders,
            String... additional) {
        List<MediaFile> mediaFiles = null;
        Element element;
        synchronized (randomCache) {
            element = randomCache.get(createCacheKey(key, casheMax, musicFolders, additional));
        }
        if (!isEmpty(element)) {
            mediaFiles = (List<MediaFile>) element.getObjectValue();
        }
        return Optional.ofNullable(mediaFiles);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<Integer>> getCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders) {
        List<Integer> ids = null;
        Element element;
        synchronized (randomCache) {
            element = randomCache.get(createCacheKey(key, casheMax, musicFolders));
        }
        if (!isEmpty(element)) {
            ids = (List<Integer>) element.getObjectValue();
        }
        return Optional.ofNullable(ids);
    }

    public void putCache(String genres, List<MusicFolder> musicFolders, IndexType indexType, List<MediaFile> value) {
        synchronized (searchCache) {
            searchCache.put(new Element(createCacheKey(genres, musicFolders, indexType), value));
        }
    }

    public void putCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders, List<Integer> value) {
        synchronized (randomCache) {
            randomCache.put(new Element(createCacheKey(key, casheMax, musicFolders), value));
        }
    }

    public void putCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders, List<MediaFile> value,
            String... additional) {
        synchronized (randomCache) {
            randomCache.put(new Element(createCacheKey(key, casheMax, musicFolders, additional), value));
        }
    }

}
