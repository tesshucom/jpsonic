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

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.GenreMasterCriteria;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderGenre;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFGenre;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.springframework.stereotype.Service;

@Service
public class FolderOrGenreLogic {

    private static final int SINGLE_FOLDER = 1;

    private final SearchService searchService;
    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;

    public FolderOrGenreLogic(SearchService searchService, UpnpProcessorUtil util, UpnpDIDLFactory factory) {
        super();
        this.searchService = searchService;
        this.util = util;
        this.factory = factory;
    }

    private Container createContainer(ProcId procId, FolderGenre folderGenre, Scope scope) {
        int childCount = getChildSizeOf(folderGenre.genre(), scope);
        return factory.toGenre(procId, folderGenre, childCount);
    }

    private Container createContainer(ProcId procId, MusicFolder folder, Scope scope, Sort sort, MediaType... types) {
        int childCount = getChildSizeOf(folder, scope, sort, types);
        return factory.toMusicFolder(procId, folder, childCount);
    }

    Container createContainer(ProcId procId, FolderOrFGenre folderOrGenre, Scope scope, Sort sort, MediaType... types) {
        if (folderOrGenre.isFolderGenre()) {
            return createContainer(procId, folderOrGenre.getFolderGenre(), scope);
        }
        return createContainer(procId, folderOrGenre.getFolder(), scope, sort, types);
    }

    List<FolderOrFGenre> getDirectChildren(long offset, long count, Scope scope, Sort sort, MediaType... types) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            MusicFolder folder = folders.get(0);
            return searchService.getGenres(new GenreMasterCriteria(asList(folder), scope, sort, types), offset, count)
                    .stream().map(genre -> new FolderOrFGenre(new FolderGenre(folder, genre))).toList();
        }
        return folders.stream().skip(offset).limit(count).map(FolderOrFGenre::new).toList();
    }

    int getDirectChildrenCount(Scope scope, Sort sort, MediaType... types) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            MusicFolder folder = folders.get(0);
            return searchService.getGenresCount(new GenreMasterCriteria(asList(folder), scope, sort, types));
        }
        return folders.size();
    }

    private FolderOrFGenre getDirectChildGenres(String folderGenreId, Scope scope, Sort sort, MediaType... types) {
        int folderId = FolderGenre.parseFolderId(folderGenreId);
        MusicFolder folder = util.getGuestFolders().stream().filter(musicFolder -> musicFolder.getId() == folderId)
                .findFirst().orElseGet(null);
        String genreName = FolderGenre.parseGenreName(folderGenreId);
        Genre genre = searchService
                .getGenres(new GenreMasterCriteria(asList(folder), scope, sort, types), 0, Integer.MAX_VALUE).stream()
                .filter(g -> genreName.equals(g.getName())).findFirst().orElseGet(null);
        if (genre == null) {
            throw new IllegalArgumentException("The specified Genre cannot be found.");
        }
        return new FolderOrFGenre(new FolderGenre(folder, genre));
    }

    private FolderOrFGenre getDirectChildFolder(int folderId) {
        MusicFolder folder = util.getGuestFolders().stream().filter(musicFolder -> musicFolder.getId() == folderId)
                .findFirst().orElseGet(null);
        return new FolderOrFGenre(folder);
    }

    FolderOrFGenre getDirectChild(String id, Scope scope, Sort sort, MediaType... types) {
        if (FolderGenre.isCompositeId(id)) {
            return getDirectChildGenres(id, scope, sort, types);
        }
        int folderId = Integer.parseInt(id);
        return getDirectChildFolder(folderId);
    }

    private int getChildSizeOf(Genre genre, Scope scope) {
        return Scope.ALBUM == scope ? genre.getAlbumCount() : genre.getSongCount();
    }

    private int getChildSizeOf(MusicFolder musicFolder, Scope scope, Sort sort, MediaType... types) {
        return searchService.getGenresCount(new GenreMasterCriteria(asList(musicFolder), scope, sort, types));
    }

    public int getChildSizeOf(FolderOrFGenre folderOrGenre, Scope scope, Sort sort, MediaType... types) {
        if (folderOrGenre.isFolderGenre()) {
            return getChildSizeOf(folderOrGenre.getFolderGenre().genre(), scope);
        }
        return getChildSizeOf(folderOrGenre.getFolder(), scope, sort, types);
    }

    void addChild(DIDLContent parent, ProcId procId, FolderGenre folderGenre, int childCount) {
        GenreContainer genreContainer = factory.toGenre(procId, folderGenre, childCount);
        parent.addContainer(genreContainer);
    }
}
