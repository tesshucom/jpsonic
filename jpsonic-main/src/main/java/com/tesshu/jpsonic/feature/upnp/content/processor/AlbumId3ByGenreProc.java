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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.type.GenreMasterScope;
import com.tesshu.jpsonic.domain.type.GenreMasterSort;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.GenreAlbum;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumId3ByGenreProc extends DirectChildrenContentProc<Genre, GenreAlbum> {

    private static final MediaFile.Type[] TYPES = { MediaFile.Type.MUSIC };

    private final MusicFolderProvider musicFolderProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final AlbumProvider albumProvider;
    private final SettingsFacade settingsFacade;
    private final UPnPDIDLFactory factory;

    AlbumId3ByGenreProc(MusicFolderProvider musicFolderProvider,
            MediaSearchProvider mediaSearchProvider, AlbumProvider albumProvider,
            SettingsFacade settingsFacade, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.mediaSearchProvider = mediaSearchProvider;
        this.albumProvider = albumProvider;
        this.settingsFacade = settingsFacade;
        this.factory = factory;
    }

    private GenreMasterCriteria createGenreMasterCriteria() {
        return new GenreMasterCriteria(musicFolderProvider.getGuestFolders(),
                GenreMasterScope.ALBUM,
                GenreMasterSort.of(settingsFacade.get(UPnPSKeys.options.upnpAlbumGenreSort)),
                TYPES);
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.albumCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return mediaSearchProvider.getGenres(createGenreMasterCriteria(), offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return mediaSearchProvider.getGenresCount(createGenreMasterCriteria());
    }

    @Override
    public Genre getDirectChild(String genreName) {
        return getDirectChildren(0, Integer.MAX_VALUE)
            .stream()
            .filter(g -> g.name().equals(genreName))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<GenreAlbum> getChildren(Genre genre, long offset, long count) {
        return mediaSearchProvider
            .findAlbumId3sByGenres(musicFolderProvider.getGuestFolders(), genre.name(), offset,
                    count)
            .stream()
            .map(album -> new GenreAlbum(genre, album))
            .toList();
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.albumCount();
    }

    private int countChldren(String genre, Album album) {
        return mediaSearchProvider
            .countChldren(musicFolderProvider.getGuestFolders(), genre, album, TYPES);
    }

    @Override
    public void addChild(DIDLContent parent, GenreAlbum composite) {
        parent
            .addContainer(factory
                .toAlbumWithGenre(composite,
                        countChldren(composite.genre().name(), composite.album())));
    }

    @Override
    public BrowseResult browseLeaf(String id, String filter, long offset, long count)
            throws ExecutionException {
        final DIDLContent content = new DIDLContent();
        if (GenreAlbum.isCompositeId(id)) {
            String genre = GenreAlbum.parseGenreName(id);
            Album album = albumProvider.requireAlbum(GenreAlbum.parseAlbumId(id));
            List<MediaFile> songs = mediaSearchProvider
                .findChildren(musicFolderProvider.getGuestFolders(), genre, album, offset, count,
                        TYPES);
            songs.stream().forEach(song -> content.addItem(factory.toMusicTrack(song)));
            return createBrowseResult(content, songs.size(), countChldren(genre, album));
        }

        // If it's not the CompositeId, it's the Genre name
        Genre genre = getDirectChild(id);
        List<GenreAlbum> albums = getChildren(genre, offset, count);
        albums.stream().forEach(album -> addChild(content, album));
        return createBrowseResult(content, albums.size(), getChildSizeOf(genre));
    }
}
