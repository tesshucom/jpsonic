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

import java.util.List;

import javax.annotation.PreDestroy;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.airsonic.player.cache.CacheFactory;
import org.airsonic.player.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class EhcacheConfiguration {

    private final SettingsService settingsService;

    public EhcacheConfiguration(@Lazy SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
    }

    @Bean
    public CacheDisposer cacheDisposer() {
        return new CacheDisposer(settingsService);
    }

    /*
     * Shutdown priority is equal to database.
     * 
     * @see net.sf.ehcache.constructs.web.ShutdownListener
     */
    public static class CacheDisposer {

        private static final Logger LOG = LoggerFactory.getLogger(CacheDisposer.class);

        private final SettingsService settingsService;

        public CacheDisposer(SettingsService settingsService) {
            super();
            this.settingsService = settingsService;
        }

        @PreDestroy
        public void preDestroy() {
            List<CacheManager> knownCacheManagers = CacheManager.ALL_CACHE_MANAGERS;
            if (settingsService.isVerboseLogShutdown() && LOG.isInfoEnabled()) {
                LOG.info("Shutting down " + knownCacheManagers.size() + " CacheManagers.");
            }
            while (!knownCacheManagers.isEmpty()) {
                ((CacheManager) CacheManager.ALL_CACHE_MANAGERS.get(0)).shutdown();
            }
        }
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

    @Bean
    public Ehcache randomCache(CacheFactory cacheFactory) {
        return cacheFactory.getCache("randomCache");
    }

    public enum IndexCacheKey {
        FILE_STRUCTURE, ID3
    }

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
