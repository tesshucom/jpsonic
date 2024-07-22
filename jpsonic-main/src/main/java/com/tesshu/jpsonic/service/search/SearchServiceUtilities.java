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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.spring.EhcacheConfiguration.RandomCacheKey;
import jakarta.annotation.PostConstruct;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ArtistDao artistDao;

    /* Search by id only. */
    private final AlbumDao albumDao;
    private final Ehcache genreCache;
    private final Ehcache randomCache;
    private final ReentrantLock genreCacheLock = new ReentrantLock();
    private final ReentrantLock randomCacheLock = new ReentrantLock();

    /**
     * For backward compatibility
     */
    enum LegacyGenreCriteria {
        ALBUM_COUNT, SONG_COUNT, ALBUM_ALPHABETICAL, SONG_ALPHABETICAL
    }

    @SuppressWarnings("PMD.SingularField")
    private Random random;

    /*
     * Search by id only. Although there is no influence at present, mediaFileService has a caching mechanism. Service
     * is used instead of Dao until you are sure you need to use mediaFileDao.
     */
    private final MediaFileService mediaFileService;

    @SuppressWarnings("PMD.LambdaCanBeMethodReference")
    public Function<Integer, Integer> nextInt = (range) -> random.nextInt(range);

    // return
    // NumericUtils.floatToSortableInt(i);
    public final Function<Long, Integer> round = Long::intValue;

    public final Function<Document, Integer> getId = d -> Integer.valueOf(d.get(FieldNamesConstants.ID));

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

    public SearchServiceUtilities(ArtistDao artistDao, AlbumDao albumDao, @Qualifier("genreCache") Ehcache genreCache,
            @Qualifier("randomCache") Ehcache randomCache, MediaFileService mediaFileService) {
        super();
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.genreCache = genreCache;
        this.randomCache = randomCache;
        this.mediaFileService = mediaFileService;
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The Random class is only used if the native random number generator is not available")
    @SuppressWarnings("PMD.UnusedAssignment") // false positive
    @PostConstruct
    public void postConstruct() {
        try {
            random = SecureRandom.getInstance("NativePRNG");
            if (LOG.isInfoEnabled()) {
                LOG.info("NativePRNG is used to create a random list of songs.");
            }
        } catch (NoSuchAlgorithmException e) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
                if (LOG.isInfoEnabled()) {
                    LOG.info("SHA1PRNG is used to create a random list of songs.");
                }
            } catch (NoSuchAlgorithmException e1) {
                random = new Random(Instant.now().toEpochMilli());
                if (LOG.isInfoEnabled()) {
                    LOG.info("NativePRNG and SHA1PRNG cannot be used on this platform.");
                }
            }
        }
    }

    public final void addMediaFileIfAnyMatch(List<MediaFile> dist, Integer id) {
        if (dist.stream().noneMatch(m -> id == m.getId())) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (!isEmpty(mediaFile)) {
                dist.add(mediaFile);
            }
        }
    }

    public final void addArtistId3IfAnyMatch(List<Artist> dist, Integer id) {
        if (dist.stream().noneMatch(a -> id == a.getId())) {
            Artist artist = artistDao.getArtist(id);
            if (!isEmpty(artist)) {
                dist.add(artist);
            }
        }
    }

    public final void addAlbumId3IfAnyMatch(List<Album> dist, Integer subjectId) {
        if (dist.stream().noneMatch(a -> subjectId == a.getId())) {
            Album album = albumDao.getAlbum(subjectId);
            if (!isEmpty(album)) {
                dist.add(album);
            }
        }
    }

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
            addMediaFileIfAnyMatch(dist.getMediaFiles(), documentId);
        } else if (subjectIndexType == IndexType.ARTIST_ID3) {
            addArtistId3IfAnyMatch(dist.getArtists(), documentId);
        } else if (subjectIndexType == IndexType.ALBUM_ID3) {
            addAlbumId3IfAnyMatch(dist.getAlbums(), documentId);
        }
    }

    private String createCacheKey(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders,
            String... additional) {
        StringBuilder b = new StringBuilder();
        b.append(key).append(',').append(casheMax).append('[');
        musicFolders.forEach(m -> b.append(m.getId()).append(','));
        if (!isEmpty(additional)) {
            Arrays.asList(additional).forEach(s -> b.append(s).append(','));
        }
        b.append(']');
        return b.toString();
    }

    private String createCacheKey(GenreMasterCriteria criteria) {
        StringBuilder b = new StringBuilder();
        b.append(criteria.scope().name()).append(',').append(criteria.sort().name()).append(',');
        criteria.folders().forEach(folder -> b.append(folder.getId()).append(','));
        b.append("],[");
        Stream.of(criteria.types()).forEach(type -> b.append(type.name()).append(','));
        b.append(']');
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<MediaFile>> getCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders,
            String... additional) {
        List<MediaFile> mediaFiles = null;
        Element element;
        randomCacheLock.lock();
        try {
            element = randomCache.get(createCacheKey(key, casheMax, musicFolders, additional));
        } finally {
            randomCacheLock.unlock();
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
        randomCacheLock.lock();
        try {
            element = randomCache.get(createCacheKey(key, casheMax, musicFolders));
        } finally {
            randomCacheLock.unlock();
        }
        if (!isEmpty(element)) {
            ids = (List<Integer>) element.getObjectValue();
        }
        return Optional.ofNullable(ids);
    }

    @SuppressWarnings("unchecked")
    public List<Genre> getCache(GenreMasterCriteria criteria) {
        genreCacheLock.lock();
        try {
            Element element = genreCache.get(createCacheKey(criteria));
            return isEmpty(element) ? Collections.emptyList() : (List<Genre>) element.getObjectValue();
        } finally {
            genreCacheLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Genre> getCache(LegacyGenreCriteria criteria) {
        genreCacheLock.lock();
        try {
            Element element = genreCache.get(criteria.name());
            return isEmpty(element) ? Collections.emptyList() : (List<Genre>) element.getObjectValue();
        } finally {
            genreCacheLock.unlock();
        }
    }

    public boolean containsCache(LegacyGenreCriteria criteria) {
        genreCacheLock.lock();
        try {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) genreCache.getKeys();
            return keys.stream().anyMatch(k -> k.equals(criteria.name()));
        } finally {
            genreCacheLock.unlock();
        }
    }

    public void putCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders, List<Integer> value) {
        randomCacheLock.lock();
        try {
            randomCache.put(new Element(createCacheKey(key, casheMax, musicFolders), value));
        } finally {
            randomCacheLock.unlock();
        }
    }

    public void putCache(RandomCacheKey key, int casheMax, List<MusicFolder> musicFolders, List<MediaFile> value,
            String... additional) {
        randomCacheLock.lock();
        try {
            randomCache.put(new Element(createCacheKey(key, casheMax, musicFolders, additional), value));
        } finally {
            randomCacheLock.unlock();
        }
    }

    public void putCache(GenreMasterCriteria criteria, List<Genre> value) {
        genreCacheLock.lock();
        try {
            genreCache.put(new Element(createCacheKey(criteria), value));
        } finally {
            genreCacheLock.unlock();
        }
    }

    public void putCache(LegacyGenreCriteria criteria, List<Genre> value) {
        genreCacheLock.lock();
        try {
            genreCache.put(new Element(criteria.name(), value));
        } finally {
            genreCacheLock.unlock();
        }
    }

    public void removeCache(LegacyGenreCriteria criteria) {
        genreCacheLock.lock();
        try {
            genreCache.remove(criteria.name());
        } finally {
            genreCacheLock.unlock();
        }

    }

    public void removeCacheAll() {
        genreCacheLock.lock();
        try {
            genreCache.removeAll();
        } finally {
            genreCacheLock.unlock();
        }

        randomCacheLock.lock();
        try {
            randomCache.removeAll();
        } finally {
            randomCacheLock.unlock();
        }
    }
}
