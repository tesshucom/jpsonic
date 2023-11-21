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

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.DIDLContent;
import org.springframework.stereotype.Service;

@Service
public class AlbumByGenreUpnpProcessor extends SongByGenreUpnpProcessor {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SearchService searchService;
    private final MediaFileService mediaFileService;

    public AlbumByGenreUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            SearchService searchService) {
        super(util, factory, searchService);
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_GENRE;
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long count) {
        return searchService.getAlbumsByGenres(item.getName(), (int) offset, (int) count, util.getGuestFolders());
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.getAlbumCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile album) {
        parent.addContainer(factory.toAlbum(album,
                mediaFileService.getChildSizeOf(album, MediaType.PODCAST, MediaType.AUDIOBOOK, MediaType.VIDEO)));
    }
}
