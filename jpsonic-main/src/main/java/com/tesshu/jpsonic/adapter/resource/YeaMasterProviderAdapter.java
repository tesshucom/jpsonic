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

package com.tesshu.jpsonic.adapter.resource;

import java.util.List;

import com.tesshu.jpsonic.domain.provider.master.YearMasterCriteria;
import com.tesshu.jpsonic.domain.provider.master.YearMasterProvider;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import org.springframework.stereotype.Component;

@Component
public class YeaMasterProviderAdapter implements YearMasterProvider {

    private final MediaFileDao mediaFileDao;

    public YeaMasterProviderAdapter(MediaFileDao mediaFileDao) {
        super();
        this.mediaFileDao = mediaFileDao;
    }

    @Override
    public List<YearsEntry> findYearsEntries(YearMasterCriteria criteria, long offset, long count) {
        if (criteria.scope() == YearMasterCriteria.YearMasterScope.SONG) {
            return mediaFileDao
                .finedSongYearsEntries(criteria.folders(), criteria.interval(), criteria.sort(),
                        offset, count, criteria.types());
        }
        return mediaFileDao
            .finedAlbumYearsEntries(criteria.folders(), criteria.interval(), criteria.sort(),
                    offset, count, criteria.types());
    }

    @Override
    public int countYearsEntries(YearMasterCriteria criteria) {
        if (criteria.scope() == YearMasterCriteria.YearMasterScope.SONG) {
            return mediaFileDao
                .countSongYearsEntries(criteria.folders(), criteria.interval(), criteria.types());
        }
        return mediaFileDao
            .countAlbumYearsEntries(criteria.folders(), criteria.interval(), criteria.types());
    }
}
