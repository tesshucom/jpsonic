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

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AudiobookByGenreProc extends DirectChildrenContentProc<Genre, MediaFile> {

    private final SettingsService settingsService;
    private final SearchService searchService;
    private final UpnpDIDLFactory factory;
    private final UpnpProcessorUtil util;

    public AudiobookByGenreProc(SettingsService settingsService, UpnpProcessorUtil util, UpnpDIDLFactory factory,
            SearchService searchService) {
        super();
        this.settingsService = settingsService;
        this.util = util;
        this.factory = factory;
        this.searchService = searchService;
    }

    private GenreMasterCriteria createGenreMasterCriteria() {
        return new GenreMasterCriteria(util.getGuestFolders(), Scope.SONG,
                Sort.of(settingsService.getUPnPSongGenreSort()), MediaType.AUDIOBOOK);
    }

    @Override
    public ProcId getProcId() {
        return ProcId.AUDIOBOOK_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(genre, getProcId(), genre.getSongCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return searchService.getGenres(createGenreMasterCriteria(), offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return searchService.getGenresCount(createGenreMasterCriteria());
    }

    @Override
    public @Nullable Genre getDirectChild(String id) {
        return searchService.getGenres(createGenreMasterCriteria(), 0, Integer.MAX_VALUE).stream()
                .filter(genre -> genre.getName().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long maxResults) {
        return searchService.getSongsByGenres(item.getName(), (int) offset, (int) maxResults, util.getGuestFolders(),
                MediaType.AUDIOBOOK);
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.getSongCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile child) {
        parent.addItem(factory.toMusicTrack(child));
    }
}
