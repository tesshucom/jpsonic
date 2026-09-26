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

import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.provider.master.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.provider.master.GenreMasterProvider;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import org.springframework.stereotype.Component;

@Component
public class GenreMasterProviderAdapter implements GenreMasterProvider {

    private final MediaSearchProvider deligate;

    public GenreMasterProviderAdapter(MediaSearchProvider deligate) {
        this.deligate = deligate;
    }

    @Override
    public int getLegacyGenresCount(boolean sortByAlbum) {
        return deligate.getGenresCount(sortByAlbum);
    }

    @Override
    public int getGenresCount(GenreMasterCriteria criteria) {
        return deligate.getGenresCount(criteria);
    }

    @Override
    public List<Genre> getGenres(GenreMasterCriteria criteria, long offset, long maxResults) {
        return deligate.getGenres(criteria, offset, maxResults);
    }

    @Override
    public List<Genre> findLegacyGenres(boolean sortByAlbum, long offset, long count) {
        return deligate.findLegacyGenres(sortByAlbum, offset, count);
    }
}
