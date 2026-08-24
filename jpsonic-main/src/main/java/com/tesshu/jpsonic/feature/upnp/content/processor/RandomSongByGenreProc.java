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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class RandomSongByGenreProc extends SongByGenreProc implements CountLimitProc {

    private final MusicFolderProvider musicFolderProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    RandomSongByGenreProc(MusicFolderProvider musicFolderProvider,
            MediaSearchProvider mediaSearchProvider, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super(musicFolderProvider, mediaSearchProvider, settingsFacade, factory);
        this.musicFolderProvider = musicFolderProvider;
        this.mediaSearchProvider = mediaSearchProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.songCount());
    }

    @Override
    public List<MediaFile> getChildren(Genre genre, long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int max = getChildSizeOf(genre);
        int count = toCount(firstResult, maxResults, max);
        return mediaSearchProvider
            .getRandomSongs(musicFolderProvider.getGuestFolders(), offset, count, max,
                    genre.name());
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return Math.min(genre.songCount(), settingsFacade.get(UPnPSKeys.options.randomMax));
    }
}
