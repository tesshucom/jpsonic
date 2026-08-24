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
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.SearchResult;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.SearchResultProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.concurrent.ConcurrentUtils;
import com.tesshu.jpsonic.infrastructure.policy.RuntimeOrderResolver;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class ArtistProc extends DirectChildrenContentProc<Artist, Album>
        implements SearchResultProcessor<Artist>, RuntimeOrderResolver {

    private final MusicFolderProvider musicFolderProvider;
    private final ArtistProvider artistProvider;
    private final AlbumProvider albumProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    ArtistProc(MusicFolderProvider musicFolderProvider, ArtistProvider artistProvider,
            AlbumProvider albumProvider, SettingsFacade settingsFacade, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.artistProvider = artistProvider;
        this.albumProvider = albumProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ARTIST;
    }

    @Override
    public Container createContainer(Artist artist) {
        return factory.toArtist(artist);
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
    public List<Album> getChildren(Artist artist, long offset, long count) {
        AlbumSortOrder order = resolveChildOrder(artist, settingsFacade);
        return albumProvider
            .findChildren(musicFolderProvider.getGuestFolders(), artist, order, offset, count);
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        return artistProvider.countChildren(musicFolderProvider.getGuestFolders(), artist.name());
    }

    @Override
    public void addChild(DIDLContent parent, Album album) {
        parent.addContainer(factory.toAlbum(album));
    }

    @Override
    public final BrowseResult toBrowseResult(SearchResult<Artist> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.items().forEach(artist -> addDirectChild(parent, artist));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.totalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
