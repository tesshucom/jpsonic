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
import java.util.stream.Stream;

import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumByGenreProc extends DirectChildrenContentProc<Genre, MediaFile> {

    private static final MediaType[] EXCLUDED_TYPES = Stream
        .of(MediaType.PODCAST, MediaType.VIDEO)
        .toArray(size -> new MediaType[size]);

    private final UPnPProcessorUtil util;
    private final UPnPDIDLFactory factory;
    private final MediaSearchProvider mediaSearchProvider;
    private final MediaFileService mediaFileService;

    AlbumByGenreProc(UPnPProcessorUtil util, UPnPDIDLFactory factory,
            MediaFileService mediaFileService, MediaSearchProvider mediaSearchProvider) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.mediaSearchProvider = mediaSearchProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.getSongCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return mediaSearchProvider.getGenres(false, offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaSearchProvider.getGenresCount(false);
    }

    @Override
    public @Nullable Genre getDirectChild(String id) {
        return mediaSearchProvider
            .getGenres(false)
            .stream()
            .filter(genre -> genre.getName().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long count) {
        return mediaSearchProvider
            .getAlbumsByGenres(item.getName(), (int) offset, (int) count, util.getGuestFolders());
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.getAlbumCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile album) {
        parent
            .addContainer(
                    factory.toAlbum(album, mediaFileService.getChildSizeOf(album, EXCLUDED_TYPES)));
    }
}
