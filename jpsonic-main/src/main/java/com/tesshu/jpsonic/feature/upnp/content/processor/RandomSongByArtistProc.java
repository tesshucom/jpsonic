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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.springframework.stereotype.Controller;

@Controller
class RandomSongByArtistProc extends DirectChildrenContentProc<Artist, MediaFile>
        implements CountLimitProc {

    private final MusicFolderProvider musicFolderProvider;
    private final ArtistProvider artistProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    RandomSongByArtistProc(MusicFolderProvider musicFolderProvider, ArtistProvider artistProvider,
            MediaSearchProvider mediaSearchProvider, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.artistProvider = artistProvider;
        this.mediaSearchProvider = mediaSearchProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_ARTIST;
    }

    @Override
    public Container createContainer(Artist artist) {
        MusicArtist container = factory.toArtist(artist);
        container.setId(getProcId().getValue() + ProcId.CID_SEPA + artist.id());
        container.setParentID(getProcId().getValue());
        return container;
    }

    @Override
    public List<Artist> getDirectChildren(long offset, long count) {
        return artistProvider.findArtists(musicFolderProvider.getGuestFolders(), offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return artistProvider.countArtists(musicFolderProvider.getGuestFolders());
    }

    @Override
    public Artist getDirectChild(String id) {
        return artistProvider.requireArtist(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(Artist artist, long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int randomMax = settingsFacade.get(UPnPSKeys.options.randomMax);
        int count = toCount(firstResult, maxResults, randomMax);
        return mediaSearchProvider
            .getRandomSongsByArtist(musicFolderProvider.getGuestFolders(), artist, offset, count,
                    randomMax);
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        return settingsFacade.get(UPnPSKeys.options.randomMax);
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile song) {
        parent.addItem(factory.toMusicTrack(song));
    }
}
