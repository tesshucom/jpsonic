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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.type.GenreMasterScope;
import com.tesshu.jpsonic.domain.type.GenreMasterSort;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FGenreOrFGAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenre;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenreAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFGenre;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrGenreLogic;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumId3ByFolderGenreProc extends DirectChildrenContentProc<FolderOrFGenre, FGenreOrFGAlbum> {

    private static final GenreMasterScope SCOPE = GenreMasterScope.ALBUM;
    private static final MediaFile.Type[] TYPES = { MediaFile.Type.MUSIC };

    private final MusicFolderProvider musicFolderProvider;
    private final MediaSearchProvider mediaSearchProvider;
    private final AlbumProvider albumProvider;
    private final UPnPDIDLFactory factory;
    private final SettingsFacade settingsFacade;
    private final FolderOrGenreLogic deligate;

    AlbumId3ByFolderGenreProc(MusicFolderProvider musicFolderProvider, UPnPDIDLFactory factory,
            SettingsFacade settingsFacade, MediaSearchProvider mediaSearchProvider,
            AlbumProvider albumProvider, FolderOrGenreLogic folderOrGenreLogic) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.factory = factory;
        this.settingsFacade = settingsFacade;
        this.mediaSearchProvider = mediaSearchProvider;
        this.albumProvider = albumProvider;
        this.deligate = folderOrGenreLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3_BY_FOLDER_GENRE;
    }

    private GenreMasterSort getSort() {
        return GenreMasterSort.of(settingsFacade.get(UPnPSKeys.options.upnpAlbumGenreSort));
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
    public List<FGenreOrFGAlbum> getChildren(FolderOrFGenre folderOrGenre, long offset,
            long count) {
        if (folderOrGenre.isFolderGenre()) {
            MusicFolder folder = folderOrGenre.getFolderGenre().folder();
            Genre genre = folderOrGenre.getFolderGenre().genre();
            return mediaSearchProvider
                .findAlbumId3sByGenres(List.of(folder), genre.name(), offset, count)
                .stream()
                .map(album -> new FolderGenreAlbum(folder, genre, album))
                .map(FGenreOrFGAlbum::new)
                .toList();
        }
        MusicFolder folder = folderOrGenre.getFolder();
        GenreMasterCriteria criteria = new GenreMasterCriteria(asList(folder), SCOPE, getSort(),
                TYPES);
        return mediaSearchProvider
            .getGenres(criteria, offset, count)
            .stream()
            .map(genre -> new FolderGenre(folder, genre))
            .map(FGenreOrFGAlbum::new)
            .toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFGenre folderOrGenre) {
        return deligate.getChildSizeOf(folderOrGenre, SCOPE, getSort(), TYPES);
    }

    private void addChild(DIDLContent parent, FolderGenreAlbum compositeAlbum,
            MediaFile.Type... types) {
        String genre = compositeAlbum.genre().name();
        Album album = compositeAlbum.album();
        MusicFolder folder = compositeAlbum.folder();
        int childCount = mediaSearchProvider.countChldren(List.of(folder), genre, album, types);
        parent.addContainer(factory.toAlbum(compositeAlbum, childCount));
    }

    @Override
    public void addChild(DIDLContent parent, FGenreOrFGAlbum genreOrAlbum) {
        if (genreOrAlbum.isAlbum()) {
            addChild(parent, genreOrAlbum.getAlbum(), TYPES);
        } else {
            deligate
                .addChild(parent, getProcId(), genreOrAlbum.getGenre(),
                        genreOrAlbum.getGenre().genre().albumCount());
        }
    }

    private BrowseResult browseFilteredAlbum(String fgaId, long offset, long count)
            throws ExecutionException {
        int folderId = FolderGenreAlbum.parseFolderId(fgaId);
        MusicFolder folder = musicFolderProvider
            .getGuestFolders()
            .stream()
            .filter(f -> f.id() == folderId)
            .findFirst()
            .orElseGet(null);
        Album album = albumProvider.requireAlbum(FolderGenreAlbum.parseAlbumId(fgaId));
        String genre = FolderGenreAlbum.parseGenreName(fgaId);
        List<MediaFile> songs = mediaSearchProvider
            .findChildren(List.of(folder), genre, album, offset, count, TYPES);
        int childSize = mediaSearchProvider.countChldren(List.of(folder), genre, album, TYPES);
        DIDLContent parent = new DIDLContent();
        songs.stream().forEach(song -> parent.addItem(factory.toMusicTrack(song)));
        return createBrowseResult(parent, songs.size(), childSize);
    }

    @Override
    public BrowseResult browseLeaf(String id, String filter, long offset, long maxLength)
            throws ExecutionException {
        if (FolderGenreAlbum.isCompositeId(id)) {
            return browseFilteredAlbum(id, offset, maxLength);
        }
        return super.browseLeaf(id, filter, offset, maxLength);
    }
}
