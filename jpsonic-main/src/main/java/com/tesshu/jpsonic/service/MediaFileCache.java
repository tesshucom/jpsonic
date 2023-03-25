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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tesshu.jpsonic.domain.MediaFile;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MediaFileCache {

    private final Ehcache mediaFileMemoryCache;
    private final AtomicBoolean enabled;

    public MediaFileCache(Ehcache mediaFileMemoryCache) {
        super();
        this.mediaFileMemoryCache = mediaFileMemoryCache;
        enabled = new AtomicBoolean(true);
    }

    boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        if (!isEnabled()) {
            mediaFileMemoryCache.removeAll();
        }
    }

    public void put(Path path, MediaFile mediaFile) {
        if (isEnabled()) {
            mediaFileMemoryCache.put(new Element(path, mediaFile));
        }
    }

    @Nullable
    public MediaFile get(Path path) {
        if (!isEnabled()) {
            return null;
        }
        Element element = mediaFileMemoryCache.get(path);
        return element == null ? null : (MediaFile) element.getObjectValue();
    }

    public void removeAll() {
        mediaFileMemoryCache.removeAll();
    }

    public boolean remove(Path path) {
        return mediaFileMemoryCache.remove(path);
    }
}
