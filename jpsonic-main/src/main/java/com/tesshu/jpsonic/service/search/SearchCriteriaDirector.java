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

package com.tesshu.jpsonic.service.search;

import java.io.IOException;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn({ "queryFactory", "settingsService" })
public class SearchCriteriaDirector {

    private final QueryFactory queryFactory;

    public SearchCriteriaDirector(QueryFactory queryFactory) {
        super();
        this.queryFactory = queryFactory;
    }

    public SearchCriteria construct(String searchInput, int offset, int count, boolean includeComposer,
            List<MusicFolder> musicFolders, IndexType indexType) throws IOException {
        SearchCriteria criteria = new SearchCriteria(searchInput, offset, count, includeComposer, musicFolders,
                indexType);
        criteria.setParsedQuery(queryFactory.searchByPhrase(searchInput, includeComposer, musicFolders, indexType));
        return criteria;
    }
}
