package com.tesshu.jpsonic.infrastructure.cache;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.infrastructure.cache.CacheKeys.CacheKey;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ModelCache {

    private final Ehcache genreCache;
    private final Ehcache randomCache;
    private final ReentrantLock genreCacheLock = new ReentrantLock();
    private final ReentrantLock randomCacheLock = new ReentrantLock();

    public ModelCache(@Qualifier("genreCache") Ehcache genreCache,
            @Qualifier("randomCache") Ehcache randomCache) {
        super();
        this.genreCache = genreCache;
        this.randomCache = randomCache;
    }

    /**
     * Returns whether the cache contains an entry for the given legacy criteria.
     */
    public boolean containsCache(CacheKey key) {
        genreCacheLock.lock();
        try {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) genreCache.getKeys();
            return keys.stream().anyMatch(k -> k.equals(key.name()));
        } finally {
            genreCacheLock.unlock();
        }
    }

    String createCacheKey(CacheKeys.random key, int cacheMax, List<MusicFolder> musicFolders,
            String... additional) {
        StringBuilder b = new StringBuilder();
        b.append(key.name()).append(',').append(cacheMax).append('[');
        String musicFolderIds = musicFolders
            .stream()
            .map(m -> String.valueOf(m.id()))
            .collect(Collectors.joining(","));
        b.append(musicFolderIds);
        if (!isEmpty(additional)) {
            if (!musicFolderIds.isEmpty()) {
                b.append(',');
            }
            b.append(Arrays.stream(additional).collect(Collectors.joining(",")));
        }
        b.append(']');
        return b.toString();
    }

    /**
     * Builds a string cache key from genre criteria.
     */
    String createCacheKey(GenreMasterCriteria criteria) {
        String folderIds = criteria
            .folders()
            .stream()
            .map(folder -> String.valueOf(folder.id()))
            .collect(Collectors.joining(","));
        String mediaTypes = Stream
            .of(criteria.types())
            .map(Enum::name)
            .collect(Collectors.joining(","));
        return String
            .format("genre-%s,%s,[%s],[%s]", criteria.scope().name(), criteria.sort().name(),
                    folderIds, mediaTypes);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<Integer>> getCache(CacheKeys.random key, int cacheMax,
            List<MusicFolder> musicFolders) {
        List<Integer> ids = null;
        Element element;
        randomCacheLock.lock();
        try {
            element = randomCache.get(createCacheKey(key, cacheMax, musicFolders));
        } finally {
            randomCacheLock.unlock();
        }
        if (!isEmpty(element)) {
            ids = (List<Integer>) element.getObjectValue();
        }
        return Optional.ofNullable(ids);
    }

    /**
     * Retrieves cached list of MediaFile based on key and music folders.
     */
    @SuppressWarnings("unchecked")
    public Optional<List<MediaFile>> getCache(CacheKeys.random key, int cacheMax,
            List<MusicFolder> musicFolders, String... additional) {
        List<MediaFile> mediaFiles = null;
        Element element;
        randomCacheLock.lock();
        try {
            element = randomCache.get(createCacheKey(key, cacheMax, musicFolders, additional));
        } finally {
            randomCacheLock.unlock();
        }
        if (!isEmpty(element)) {
            mediaFiles = (List<MediaFile>) element.getObjectValue();
        }
        return Optional.ofNullable(mediaFiles);
    }

    /**
     * Retrieves cached list of Genre based on GenreMasterCriteria.
     */
    @SuppressWarnings("unchecked")
    public List<Genre> getCache(GenreMasterCriteria criteria) {
        genreCacheLock.lock();
        try {
            Element element = genreCache.get(createCacheKey(criteria));
            return isEmpty(element) ? Collections.emptyList()
                    : (List<Genre>) element.getObjectValue();
        } finally {
            genreCacheLock.unlock();
        }
    }

    /**
     * Retrieves cached list of Genre based on legacy criteria.
     */
    @SuppressWarnings("unchecked")
    public List<Genre> getCache(CacheKeys.genre genreKey) {
        genreCacheLock.lock();
        try {
            Element element = genreCache.get(genreKey.name());
            return isEmpty(element) ? Collections.emptyList()
                    : (List<Genre>) element.getObjectValue();
        } finally {
            genreCacheLock.unlock();
        }
    }

    /**
     * Stores a list of Integer IDs in the cache using the provided key and music
     * folders.
     */
    public void putCache(CacheKeys.random key, int cacheMax, List<MusicFolder> musicFolders,
            List<Integer> value) {
        randomCacheLock.lock();
        try {
            randomCache.put(new Element(createCacheKey(key, cacheMax, musicFolders), value));
        } finally {
            randomCacheLock.unlock();
        }
    }

    /**
     * Stores a list of MediaFile objects in the cache using the provided key and
     * music folders.
     */
    public void putCache(CacheKeys.random key, int cacheMax, List<MusicFolder> folders,
            List<MediaFile> value, String... additional) {
        randomCacheLock.lock();
        try {
            randomCache.put(new Element(createCacheKey(key, cacheMax, folders, additional), value));
        } finally {
            randomCacheLock.unlock();
        }
    }

    /**
     * Stores a list of Genre based on GenreMasterCriteria in the genre cache.
     */
    public void putCache(GenreMasterCriteria criteria, List<Genre> value) {
        genreCacheLock.lock();
        try {
            genreCache.put(new Element(createCacheKey(criteria), value));
        } finally {
            genreCacheLock.unlock();
        }
    }

    /**
     * Stores a list of Genre based on legacy criteria in the genre cache.
     */
    public void putCache(CacheKeys.CacheKey key, List<Genre> value) {
        genreCacheLock.lock();
        try {
            genreCache.put(new Element(key.name(), value));
        } finally {
            genreCacheLock.unlock();
        }
    }

}
