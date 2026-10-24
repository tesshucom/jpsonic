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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.adapter.master;

import java.util.List;

import com.tesshu.jpsonic.domain.provider.master.YearMasterCriteria;
import com.tesshu.jpsonic.domain.provider.master.YearMasterProvider;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import org.springframework.stereotype.Component;

@Component
public class YeaMasterProviderAdapter implements YearMasterProvider {

    private final MediaFileDao mediaFileDao;
    private final MasterCacheImpl masterCache;

    public YeaMasterProviderAdapter(MediaFileDao mediaFileDao, MasterCacheImpl masterCache) {
        this.mediaFileDao = mediaFileDao;
        this.masterCache = masterCache;
    }

    @Override
    public List<YearsEntry> findYearsEntries(YearMasterCriteria criteria, long offset, long count) {
        List<YearsEntry> cached = masterCache.getCache(criteria);
        if (!cached.isEmpty()) {
            return cached.stream().skip(offset).limit(count).toList();
        }

        List<YearsEntry> result;
        if (criteria.scope() == YearMasterCriteria.YearMasterScope.SONG) {
            result = mediaFileDao
                .finedSongYearsEntries(criteria.folders(), criteria.interval(), criteria.sort(), 0,
                        Integer.MAX_VALUE, criteria.types());
        } else {
            result = mediaFileDao
                .finedAlbumYearsEntries(criteria.folders(), criteria.interval(), criteria.sort(), 0,
                        Integer.MAX_VALUE, criteria.types());
        }

        masterCache.putCache(criteria, result);
        return result.stream().skip(offset).limit(count).toList();
    }

    @Override
    public int countYearsEntries(YearMasterCriteria criteria) {
        return findYearsEntries(criteria, 0, Integer.MAX_VALUE).size();
    }
}
