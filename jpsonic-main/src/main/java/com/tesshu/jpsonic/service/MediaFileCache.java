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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.io.File;

import com.tesshu.jpsonic.domain.MediaFile;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.stereotype.Component;

@Component
public class MediaFileCache {

    private final Ehcache mediaFileMemoryCache;
    private boolean enabled;

    public MediaFileCache(Ehcache mediaFileMemoryCache) {
        super();
        this.mediaFileMemoryCache = mediaFileMemoryCache;
        enabled = true;
    }

    void put(File file, MediaFile mediaFile) {
        if (enabled) {
            mediaFileMemoryCache.put(new Element(file, mediaFile));
        }
    }

    MediaFile get(File file) {
        if (!enabled) {
            return null;
        }
        Element element = mediaFileMemoryCache.get(file);
        return element == null ? null : (MediaFile) element.getObjectValue();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            mediaFileMemoryCache.removeAll();
        }
    }

    public void removeAll() {
        mediaFileMemoryCache.removeAll();
    }

    boolean remove(File file) {
        return mediaFileMemoryCache.remove(file);
    }
}
