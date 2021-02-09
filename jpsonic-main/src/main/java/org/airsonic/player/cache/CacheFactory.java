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

package org.airsonic.player.cache;

import java.io.File;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.InitializingBean;

/**
 * Initializes Ehcache and creates caches.
 *
 * @author Sindre Mehus
 */
public class CacheFactory implements InitializingBean {

    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() {
        Configuration configuration = ConfigurationFactory.parseConfiguration();

        // Override configuration to make sure cache is stored in Airsonic home dir.
        File cacheDir = new File(SettingsService.getJpsonicHome(), "cache");
        configuration.getDiskStoreConfiguration().setPath(cacheDir.getPath());

        cacheManager = CacheManager.create(configuration);
    }

    public Ehcache getCache(String name) {
        return cacheManager.getCache(name);
    }
}
