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

import java.util.List;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class IndexId3Proc extends DirectChildrenContentProc<MusicIndex, Artist> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MusicIndexService musicIndexService;
    private final ArtistDao artistDao;

    public IndexId3Proc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MusicIndexService musicIndexService,
            ArtistDao artistDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.musicIndexService = musicIndexService;
        this.artistDao = artistDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.INDEX_ID3;
    }

    @Override
    public Container createContainer(MusicIndex musicIndex) {
        return factory.toMusicIndex(musicIndex, getProcId(), getChildSizeOf(musicIndex));
    }

    @Override
    public List<MusicIndex> getDirectChildren(long offset, long count) {
        return musicIndexService.getIndexedId3ArtistCounts(util.getGuestFolders()).keySet().stream().skip(offset)
                .limit(count).toList();
    }

    @Override
    public int getDirectChildrenCount() {
        return artistDao.getMudicIndexCount(util.getGuestFolders());
    }

    @Override
    public MusicIndex getDirectChild(String id) {
        return musicIndexService.getIndexedId3ArtistCounts(util.getGuestFolders()).keySet().stream()
                .filter(i -> i.getIndex().equals(id)).findFirst().get();
    }

    @Override
    public List<Artist> getChildren(MusicIndex musicIndex, long offset, long maxLength) {
        return artistDao.getArtists(musicIndex, util.getGuestFolders(), offset, maxLength);
    }

    @Override
    public int getChildSizeOf(MusicIndex musicIndex) {
        return musicIndexService.getIndexedId3ArtistCounts(util.getGuestFolders()).get(musicIndex);
    }

    @Override
    public void addChild(DIDLContent parent, Artist artist) {
        parent.addContainer(factory.toArtist(artist));
    }
}
