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
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.ArtistOrAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrArtistLogic;
import com.tesshu.jpsonic.infrastructure.policy.RuntimeOrderResolver;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class ArtistByFolderProc extends DirectChildrenContentProc<FolderOrFArtist, ArtistOrAlbum>
        implements RuntimeOrderResolver {

    private final ArtistProvider artistProvider;
    private final AlbumProvider albumProvider;
    private final FolderOrArtistLogic deligate;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    ArtistByFolderProc(ArtistProvider artistProvider, AlbumProvider albumProvider,
            FolderOrArtistLogic folderOrArtistLogic, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super();
        this.artistProvider = artistProvider;
        this.albumProvider = albumProvider;
        this.deligate = folderOrArtistLogic;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ARTIST_BY_FOLDER;
    }

    @Override
    public Container createContainer(FolderOrFArtist folderOrArtist) {
        return deligate.createContainer(getProcId(), folderOrArtist);
    }

    @Override
    public List<FolderOrFArtist> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount();
    }

    @Override
    public FolderOrFArtist getDirectChild(String compositeId) {
        return deligate.getDirectChild(compositeId);
    }

    @Override
    public List<ArtistOrAlbum> getChildren(FolderOrFArtist folderOrArtist, long offset,
            long count) {
        if (folderOrArtist.isFolderArtist()) {
            Artist artist = folderOrArtist.getFolderArtist().artist();
            RuntimeOrderPolicy.AlbumSortOrder order = resolveChildOrder(artist, settingsFacade);
            List<MusicFolder> folders = List.of(folderOrArtist.getFolderArtist().folder());
            return albumProvider
                .findChildren(folders, artist, order, offset, count)
                .stream()
                .map(ArtistOrAlbum::new)
                .toList();
        }
        List<MusicFolder> folders = List.of(folderOrArtist.getFolder());
        return artistProvider
            .findArtists(folders, offset, count)
            .stream()
            .map(ArtistOrAlbum::new)
            .toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFArtist folderOrArtist) {
        return deligate.getChildSizeOf(folderOrArtist);
    }

    @Override
    public void addChild(DIDLContent parent, ArtistOrAlbum artistOrAlbum) {
        Container container = artistOrAlbum.isArtist() ? factory.toArtist(artistOrAlbum.getArtist())
                : factory.toAlbum(artistOrAlbum.getAlbum());
        parent.addContainer(container);
    }
}
