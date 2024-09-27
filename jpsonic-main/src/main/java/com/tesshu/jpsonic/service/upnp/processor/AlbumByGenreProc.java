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
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumByGenreProc extends DirectChildrenContentProc<Genre, MediaFile> {

    private static final MediaType[] EXCLUDED_TYPES = Stream.of(MediaType.PODCAST, MediaType.VIDEO)
            .toArray(size -> new MediaType[size]);

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SearchService searchService;
    private final MediaFileService mediaFileService;

    public AlbumByGenreProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            SearchService searchService) {
        super();
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
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.getSongCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return searchService.getGenres(false, offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return searchService.getGenresCount(false);
    }

    @Override
    public @Nullable Genre getDirectChild(String id) {
        return searchService.getGenres(false).stream().filter(genre -> genre.getName().equals(id)).findFirst()
                .orElse(null);
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
        parent.addContainer(factory.toAlbum(album, mediaFileService.getChildSizeOf(album, EXCLUDED_TYPES)));
    }
}
