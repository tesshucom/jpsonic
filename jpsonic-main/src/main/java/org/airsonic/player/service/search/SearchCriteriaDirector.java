/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MusicFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SearchCriteriaDirector {

    @Autowired
    private QueryFactory queryFactory;

    public SearchCriteria construct(String searchInput, int offset, int count, boolean includeComposer, List<MusicFolder> musicFolders, IndexType indexType) throws IOException {
        SearchCriteria criteria = new SearchCriteria(searchInput, offset, count, includeComposer, musicFolders, indexType);
        // TODO #407 Support for exact match/prefix/ phrase search
        criteria.setParsedQuery(queryFactory.search(criteria, musicFolders, indexType));
        return criteria;
    }

}
