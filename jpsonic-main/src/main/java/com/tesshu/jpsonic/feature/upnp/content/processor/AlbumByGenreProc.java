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

import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumByGenreProc extends DirectChildrenContentProc<Genre, MediaFile> {

    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);

    private final MusicFolderProvider musicFolderProvider;
    private final MediaFileProvider mediaFileProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final UPnPDIDLFactory factory;

    AlbumByGenreProc(MusicFolderProvider musicFolderProvider, MediaFileProvider mediaFileProvider,
            MediaSearchProvider mediaSearchProvider, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.mediaFileProvider = mediaFileProvider;
        this.mediaSearchProvider = mediaSearchProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.songCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return mediaSearchProvider.findLegacyGenres(false, offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaSearchProvider.getGenresCount(false);
    }

    @Override
    public @Nullable Genre getDirectChild(String id) {
        return mediaSearchProvider
            .findLegacyGenres(false, 0, Integer.MAX_VALUE)
            .stream()
            .filter(genre -> genre.name().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<MediaFile> getChildren(Genre genre, long offset, long count) {
        return mediaSearchProvider
            .findAlbumsByGenres(musicFolderProvider.getGuestFolders(), genre.name(), (int) offset,
                    (int) count);
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.albumCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile album) {
        parent
            .addContainer(
                    factory.toAlbum(album, mediaFileProvider.countChildren(album, EXCLUDED_TYPES)));
    }
}
