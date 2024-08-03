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

import static java.util.Arrays.asList;

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
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FGenreOrFGAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderGenre;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderGenreAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFGenre;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumId3ByFolderGenreProc extends DirectChildrenContentProc<FolderOrFGenre, FGenreOrFGAlbum> {

    private static final Scope SCOPE = GenreMasterCriteria.Scope.ALBUM;
    private static final MediaType[] TYPES = { MediaType.MUSIC, MediaType.AUDIOBOOK };

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SettingsService settingsService;
    private final SearchService searchService;
    private final AlbumDao albumDao;
    private final FolderOrGenreLogic deligate;

    public AlbumId3ByFolderGenreProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, SettingsService settingsService,
            SearchService searchService, AlbumDao albumDao, FolderOrGenreLogic folderOrGenreLogic) {
        super();
        this.util = util;
        this.factory = factory;
        this.settingsService = settingsService;
        this.searchService = searchService;
        this.albumDao = albumDao;
        this.deligate = folderOrGenreLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3_BY_FOLDER_GENRE;
    }

    private Sort getSort() {
        return Sort.of(settingsService.getUPnPAlbumGenreSort());
    }

    @Override
    public Container createContainer(FolderOrFGenre folderOrGenre) {
        return deligate.createContainer(getProcId(), folderOrGenre, SCOPE, getSort(), TYPES);
    }

    @Override
    public List<FolderOrFGenre> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count, SCOPE, getSort(), TYPES);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount(SCOPE, getSort(), TYPES);
    }

    @Override
    public FolderOrFGenre getDirectChild(String id) {
        return deligate.getDirectChild(id, SCOPE, getSort(), TYPES);
    }

    @Override
    public List<FGenreOrFGAlbum> getChildren(FolderOrFGenre folderOrGenre, long offset, long count) {
        if (folderOrGenre.isFolderGenre()) {
            MusicFolder folder = folderOrGenre.getFolderGenre().folder();
            Genre genre = folderOrGenre.getFolderGenre().genre();
            return searchService.getAlbumId3sByGenres(genre.getName(), (int) offset, (int) count, asList(folder))
                    .stream().map(album -> new FolderGenreAlbum(folder, genre, album)).map(FGenreOrFGAlbum::new)
                    .toList();
        }
        MusicFolder folder = folderOrGenre.getFolder();
        GenreMasterCriteria criteria = new GenreMasterCriteria(asList(folder), SCOPE, getSort(), TYPES);
        return searchService.getGenres(criteria, offset, count).stream().map(genre -> new FolderGenre(folder, genre))
                .map(FGenreOrFGAlbum::new).toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFGenre folderOrGenre) {
        return deligate.getChildSizeOf(folderOrGenre, SCOPE, getSort(), TYPES);
    }

    private void addChild(DIDLContent parent, FolderGenreAlbum compositeAlbum, MediaType... types) {
        String genre = compositeAlbum.genre().getName();
        Album album = compositeAlbum.album();
        MusicFolder folder = compositeAlbum.folder();
        int childCount = searchService.getChildSizeOf(genre, album, asList(folder), types);
        parent.addContainer(factory.toAlbum(compositeAlbum, childCount));
    }

    @Override
    public void addChild(DIDLContent parent, FGenreOrFGAlbum genreOrAlbum) {
        if (genreOrAlbum.isAlbum()) {
            addChild(parent, genreOrAlbum.getAlbum(), TYPES);
        } else {
            deligate.addChild(parent, getProcId(), genreOrAlbum.getGenre(),
                    genreOrAlbum.getGenre().genre().getAlbumCount());
        }
    }

    private BrowseResult browseFilteredAlbum(String fgaId, long offset, long maxLength) throws ExecutionException {
        int folderId = FolderGenreAlbum.parseFolderId(fgaId);
        MusicFolder folder = util.getGuestFolders().stream().filter(f -> f.getId() == folderId).findFirst()
                .orElseGet(null);
        Album album = albumDao.getAlbum(FolderGenreAlbum.parseAlbumId(fgaId));
        String genre = FolderGenreAlbum.parseGenreName(fgaId);
        List<MediaFile> songs = searchService.getChildrenOf(genre, album, (int) offset, (int) maxLength, asList(folder),
                TYPES);
        int childSize = searchService.getChildSizeOf(genre, album, asList(folder), TYPES);
        DIDLContent parent = new DIDLContent();
        songs.stream().forEach(song -> parent.addItem(factory.toMusicTrack(song)));
        return createBrowseResult(parent, songs.size(), childSize);
    }

    @Override
    public BrowseResult browseLeaf(String id, String filter, long offset, long maxLength) throws ExecutionException {
        if (FolderGenreAlbum.isCompositeId(id)) {
            return browseFilteredAlbum(id, offset, maxLength);
        }
        return super.browseLeaf(id, filter, offset, maxLength);
    }
}
