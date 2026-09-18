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
import com.tesshu.jpsonic.domain.model.MusicIndex;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicIndexProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class IndexId3Proc extends DirectChildrenContentProc<MusicIndex, Artist> {

    private final MusicFolderProvider musicFolderProvider;
    private final MusicIndexProvider musicIndexProvider;
    private final ArtistProvider artistProvider;
    private final UPnPDIDLFactory factory;

    IndexId3Proc(MusicFolderProvider musicFolderProvider, MusicIndexProvider musicIndexProvider,
            ArtistProvider artistProvider, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.musicIndexProvider = musicIndexProvider;
        this.artistProvider = artistProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.INDEX_ID3;
    }

    @Override
    public Container createContainer(MusicIndex musicIndex) {
        return factory.toMusicIndex(getProcId(), musicIndex, getChildSizeOf(musicIndex));
    }

    @Override
    public List<MusicIndex> getDirectChildren(long offset, long count) {
        return musicIndexProvider
            .findIndexedId3Artists(musicFolderProvider.getGuestFolders())
            .keySet()
            .stream()
            .skip(offset)
            .limit(count)
            .toList();
    }

    @Override
    public int getDirectChildrenCount() {
        return artistProvider.countMudicIndexes(musicFolderProvider.getGuestFolders());
    }

    @Override
    public MusicIndex getDirectChild(String id) {
        return musicIndexProvider
            .findIndexedId3Artists(musicFolderProvider.getGuestFolders())
            .keySet()
            .stream()
            .filter(i -> i.index().equals(id))
            .findFirst()
            .get();
    }

    @Override
    public List<Artist> getChildren(MusicIndex musicIndex, long offset, long count) {
        return artistProvider
            .findArtists(musicFolderProvider.getGuestFolders(), musicIndex.index(), offset, count);
    }

    @Override
    public int getChildSizeOf(MusicIndex musicIndex) {
        return musicIndexProvider
            .countIndexedId3Artists(musicFolderProvider.getGuestFolders())
            .get(musicIndex);
    }

    @Override
    public void addChild(DIDLContent parent, Artist artist) {
        parent.addContainer(factory.toArtist(artist));
    }
}
