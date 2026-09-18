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
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.SearchResult;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.SearchResultProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.concurrent.ConcurrentUtils;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumId3Proc extends DirectChildrenContentProc<Album, MediaFile>
        implements SearchResultProcessor<Album> {

    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);

    private final MusicFolderProvider musicFolderProvider;
    private final AlbumProvider albumProvider;
    private final MediaFileProvider mediaFileProvider;
    private final UPnPDIDLFactory factory;

    AlbumId3Proc(MusicFolderProvider musicFolderProvider, AlbumProvider albumProvider,
            MediaFileProvider mediaFileProvider, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.albumProvider = albumProvider;
        this.mediaFileProvider = mediaFileProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3;
    }

    @Override
    public Container createContainer(Album album) {
        return factory.toAlbum(album);
    }

    @Override
    public List<Album> getDirectChildren(long offset, long count) {
        return albumProvider
            .findAlbums(musicFolderProvider.getGuestFolders(),
                    RuntimeOrderPolicy.AlbumSortOrder.DEFAULT, offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return albumProvider.countAlbums(musicFolderProvider.getGuestFolders());
    }

    @Override
    public Album getDirectChild(String id) {
        return albumProvider.requireAlbum(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(Album album, long offset, long count) {
        return mediaFileProvider
            .findChildren(musicFolderProvider.getGuestFolders(), album, offset, count,
                    EXCLUDED_TYPES);
    }

    @Override
    public int getChildSizeOf(Album album) {
        return album.songCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile song) {
        parent.addItem(factory.toMusicTrack(song));
    }

    @Override
    public final BrowseResult toBrowseResult(SearchResult<Album> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.items().forEach(album -> addDirectChild(parent, album));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.totalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
