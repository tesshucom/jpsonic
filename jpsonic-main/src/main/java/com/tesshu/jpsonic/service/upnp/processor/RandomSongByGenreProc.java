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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class RandomSongByGenreProc extends SongByGenreProc implements CountLimitProc {

    private final SettingsService settingsService;
    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SearchService searchService;

    public RandomSongByGenreProc(SettingsService settingsService, UpnpProcessorUtil util,
            UpnpDIDLFactory factory, SearchService searchService) {
        super(settingsService, util, factory, searchService);
        this.settingsService = settingsService;
        this.util = util;
        this.factory = factory;
        this.searchService = searchService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.getSongCount());
    }

    @Override
    public List<MediaFile> getChildren(Genre genre, long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int max = getChildSizeOf(genre);
        int count = toCount(firstResult, maxResults, max);
        return searchService
            .getRandomSongs(count, offset, max, util.getGuestFolders(), genre.getName());
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return Math.min(genre.getSongCount(), settingsService.getDlnaRandomMax());
    }
}
