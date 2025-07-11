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

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.processor.composite.GenreAlbum;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumId3ByGenreProc extends DirectChildrenContentProc<Genre, GenreAlbum> {

    private static final MediaType[] TYPES = { MediaType.MUSIC };

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SettingsService settingsService;
    private final SearchService searchService;
    private final AlbumDao albumDao;

    public AlbumId3ByGenreProc(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            SettingsService settingsService, SearchService searchService, AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.settingsService = settingsService;
        this.searchService = searchService;
        this.albumDao = albumDao;
    }

    private GenreMasterCriteria createGenreMasterCriteria() {
        return new GenreMasterCriteria(util.getGuestFolders(), Scope.ALBUM,
                Sort.of(settingsService.getUPnPAlbumGenreSort()), TYPES);
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3_BY_GENRE;
    }

    @Override
    public Container createContainer(Genre genre) {
        return factory.toGenre(getProcId(), genre, genre.getAlbumCount());
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return searchService.getGenres(createGenreMasterCriteria(), offset, maxResults);
    }

    @Override
    public int getDirectChildrenCount() {
        return searchService.getGenresCount(createGenreMasterCriteria());
    }

    @Override
    public Genre getDirectChild(String genreName) {
        return getDirectChildren(0, Integer.MAX_VALUE)
            .stream()
            .filter(g -> g.getName().equals(genreName))
            .findFirst()
            .orElseGet(null);
    }

    @Override
    public List<GenreAlbum> getChildren(Genre genre, long offset, long maxLength) {
        return searchService
            .getAlbumId3sByGenres(genre.getName(), (int) offset, (int) maxLength,
                    util.getGuestFolders())
            .stream()
            .map(album -> new GenreAlbum(genre, album))
            .toList();
    }

    @Override
    public int getChildSizeOf(Genre genre) {
        return genre.getAlbumCount();
    }

    private int getChildSizeOf(String genre, Album album) {
        return searchService.getChildSizeOf(genre, album, util.getGuestFolders(), TYPES);
    }

    @Override
    public void addChild(DIDLContent parent, GenreAlbum composite) {
        parent
            .addContainer(factory
                .toAlbumWithGenre(composite,
                        getChildSizeOf(composite.genre().getName(), composite.album())));
    }

    @Override
    public BrowseResult browseLeaf(String id, String filter, long offset, long maxLength)
            throws ExecutionException {
        final DIDLContent content = new DIDLContent();
        if (GenreAlbum.isCompositeId(id)) {
            String genre = GenreAlbum.parseGenreName(id);
            Album album = albumDao.getAlbum(GenreAlbum.parseAlbumId(id));
            List<MediaFile> songs = searchService
                .getChildrenOf(genre, album, (int) offset, (int) maxLength, util.getGuestFolders(),
                        TYPES);
            songs.stream().forEach(song -> content.addItem(factory.toMusicTrack(song)));
            return createBrowseResult(content, songs.size(), getChildSizeOf(genre, album));
        }

        // If it's not the CompositeId, it's the Genre name
        Genre genre = getDirectChild(id);
        List<GenreAlbum> albums = getChildren(genre, offset, maxLength);
        albums.stream().forEach(album -> addChild(content, album));
        return createBrowseResult(content, albums.size(), getChildSizeOf(genre));
    }
}
