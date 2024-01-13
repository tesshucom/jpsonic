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

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.service.upnp.processor.composite.ArtistOrAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrArtist;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class ArtistByFolderProc extends DirectChildrenContentProc<FolderOrArtist, ArtistOrAlbum> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final FolderOrArtistLogic deligate;

    public ArtistByFolderProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, ArtistDao artistDao, AlbumDao albumDao,
            FolderOrArtistLogic folderOrArtistLogic) {
        super();
        this.util = util;
        this.factory = factory;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.deligate = folderOrArtistLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ARTIST_BY_FOLDER;
    }

    @Override
    public Container createContainer(FolderOrArtist folderArtist) {
        return deligate.createContainer(getProcId(), folderArtist);
    }

    @Override
    public List<FolderOrArtist> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount();
    }

    @Override
    public FolderOrArtist getDirectChild(String compositeId) {
        return deligate.getDirectChild(compositeId);
    }

    @Override
    public List<ArtistOrAlbum> getChildren(FolderOrArtist folderOrArtist, long first, long count) {
        int offset = (int) first;
        if (folderOrArtist.isArtist()) {
            Artist artist = folderOrArtist.getArtist();
            return albumDao.getAlbumsForArtist(offset, count, artist.getName(),
                    util.isSortAlbumsByYear(artist.getName()), util.getGuestFolders()).stream().map(ArtistOrAlbum::new)
                    .toList();
        }
        return artistDao.getAlphabetialArtists(offset, (int) count, Arrays.asList(folderOrArtist.getFolder())).stream()
                .map(ArtistOrAlbum::new).toList();
    }

    @Override
    public int getChildSizeOf(FolderOrArtist folderOrArtist) {
        return deligate.getChildSizeOf(folderOrArtist);
    }

    @Override
    public void addChild(DIDLContent parent, ArtistOrAlbum artistOrAlbum) {
        Container container = artistOrAlbum.isArtist() ? factory.toArtist(artistOrAlbum.getArtist())
                : factory.toAlbum(artistOrAlbum.getAlbum());
        parent.addContainer(container);
    }
}
