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

package org.airsonic.player.spring;

import javax.servlet.ServletContextListener;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.web.ShutdownListener;
import org.airsonic.player.cache.CacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
