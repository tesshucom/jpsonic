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

package org.airsonic.player.service.search;

import java.util.List;

import org.airsonic.player.domain.MusicFolder;

public class SearchCriteria extends LuceneSearchCriteria {

    private final List<MusicFolder> musicFolders;
    private final IndexType indexType;

    SearchCriteria(String searchInput, int offset, int count, boolean includeComposer, List<MusicFolder> musicFolders,
            IndexType indexType) {
        super(searchInput, offset, count, includeComposer);
        this.musicFolders = musicFolders;
        this.indexType = indexType;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

}
