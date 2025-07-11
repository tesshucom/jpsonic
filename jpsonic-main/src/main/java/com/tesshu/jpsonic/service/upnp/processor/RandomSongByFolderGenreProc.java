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
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FGenreOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderGenre;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFGenre;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class RandomSongByFolderGenreProc extends SongByFolderGenreProc implements CountLimitProc {

    private final SettingsService settingsService;
    private final SearchService searchService;
    private final UpnpDIDLFactory factory;
    private final FolderOrGenreLogic deligate;

    public RandomSongByFolderGenreProc(SettingsService settingsService, SearchService searchService,
            UpnpDIDLFactory factory, FolderOrGenreLogic folderOrGenreLogic) {
        super(settingsService, searchService, factory, folderOrGenreLogic);
        this.settingsService = settingsService;
        this.searchService = searchService;
        this.factory = factory;
        this.deligate = folderOrGenreLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_FOLDER_GENRE;
    }

    @Override
    public Container createContainer(FolderOrFGenre folderOrGenre) {
        return deligate.createContainer(getProcId(), folderOrGenre, SCOPE, getSort(), TYPES);
    }

    @Override
    public List<FGenreOrSong> getChildren(FolderOrFGenre folderOrGenre, long firstResult,
            long maxResults) {
        int offset = (int) firstResult;
        int max = getChildSizeOf(folderOrGenre);
        int count = toCount(firstResult, maxResults, max);
        if (folderOrGenre.isFolderGenre()) {
            MusicFolder folder = folderOrGenre.getFolderGenre().folder();
            Genre genre = folderOrGenre.getFolderGenre().genre();
            return searchService
                .getRandomSongs(count, offset, max, List.of(folder), genre.getName())
                .stream()
                .map(FGenreOrSong::new)
                .toList();
        }
        MusicFolder folder = folderOrGenre.getFolder();
        GenreMasterCriteria criteria = new GenreMasterCriteria(asList(folder), SCOPE, getSort(),
                TYPES);
        return searchService
            .getGenres(criteria, offset, count)
            .stream()
            .map(genre -> new FolderGenre(folder, genre))
            .map(FGenreOrSong::new)
            .toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFGenre folderOrGenre) {
        int childSize = super.getChildSizeOf(folderOrGenre);
        if (folderOrGenre.isFolderGenre()) {
            return Math.min(childSize, settingsService.getDlnaRandomMax());
        }
        return childSize;
    }

    @Override
    public void addChild(DIDLContent parent, FGenreOrSong genreOrSong) {
        if (genreOrSong.isSong()) {
            parent.addItem(factory.toMusicTrack(genreOrSong.getSong()));
        } else {
            deligate
                .addChild(parent, getProcId(), genreOrSong.getGenre(),
                        genreOrSong.getGenre().genre().getSongCount());
        }
    }
}
