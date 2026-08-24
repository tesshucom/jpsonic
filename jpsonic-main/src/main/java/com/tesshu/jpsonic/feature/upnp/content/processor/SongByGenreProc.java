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
 * (C) 2009 Sindre Mehus
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.type.GenreMasterScope;
import com.tesshu.jpsonic.domain.type.GenreMasterSort;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class SongByGenreProc extends DirectChildrenContentProc<Genre, MediaFile> {

    private static final MediaFile.Type[] TYPES = { MediaFile.Type.MUSIC };

    private final MusicFolderProvider musicFolderProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    SongByGenreProc(MusicFolderProvider musicFolderProvider,
            MediaSearchProvider mediaSearchProvider, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.mediaSearchProvider = mediaSearchProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    private GenreMasterCriteria createGenreMasterCriteria() {
        return new GenreMasterCriteria(musicFolderProvider.getGuestFolders(), GenreMasterScope.SONG,
                GenreMasterSort.of(settingsFacade.get(UPnPSKeys.options.upnpSongGenreSort)), TYPES);
    }

    @Override
    public ProcId getProcId() {
        return ProcId.SONG_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.songCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return mediaSearchProvider.getGenres(createGenreMasterCriteria(), offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaSearchProvider.getGenresCount(createGenreMasterCriteria());
    }

    @Override
    public @Nullable Genre getDirectChild(String id) {
        return mediaSearchProvider
            .getGenres(createGenreMasterCriteria(), 0, Integer.MAX_VALUE)
            .stream()
            .filter(genre -> genre.name().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<MediaFile> getChildren(Genre genre, long offset, long count) {
        return mediaSearchProvider
            .getSongsByGenres(musicFolderProvider.getGuestFolders(), List.of(genre.name()), offset,
                    count, TYPES);
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.songCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile child) {
        parent.addItem(factory.toMusicTrack(child));
    }
}
