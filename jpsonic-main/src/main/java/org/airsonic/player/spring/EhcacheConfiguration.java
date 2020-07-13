package org.airsonic.player.spring;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.web.ShutdownListener;
import org.airsonic.player.cache.CacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContextListener;

@Configuration
public class EhcacheConfiguration {

    @Bean
    public ServletContextListener ehCacheShutdownListener() {
        return new ShutdownListener();
    }

    @Bean
    public Ehcache userCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("userCache");
    }

    @Bean
    public Ehcache mediaFileMemoryCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("mediaFileMemoryCache");
    }

    @Bean
    public Ehcache searchCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("searchCache");
    }

    public enum RandomCacheKey {
        ALBUM, SONG, SONG_BY_ARTIST
    }

    ;

    @Bean
    public Ehcache randomCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("randomCache");
    }

    public enum IndexCacheKey {
        FILE_STRUCTURE, ID3
    }

    ;

    @Bean
    public Ehcache indexCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("indexCache");
    }

    @Bean
    public Ehcache fontCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("fontCache");
    }

    @Bean
    public CacheFactory cacheFactory() {
        return new CacheFactory();
    }
}
