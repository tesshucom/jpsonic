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

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FArtistOrSong;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrArtistLogic;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class RandomSongByFolderArtistProc extends DirectChildrenContentProc<FolderOrFArtist, FArtistOrSong>
        implements CountLimitProc {

    private final UPnPProcessorUtil util;
    private final UPnPDIDLFactory factory;
    private final ArtistDao artistDao;
    private final MediaSearchProvider mediaSearchProvider;
    private final SettingsFacade settingsFacade;
    private final FolderOrArtistLogic deligate;

    RandomSongByFolderArtistProc(UPnPProcessorUtil util, UPnPDIDLFactory factory,
            ArtistDao artistDao, MediaSearchProvider mediaSearchProvider,
            SettingsFacade settingsFacade, FolderOrArtistLogic folderOrArtistLogic) {
        super();
        this.util = util;
        this.factory = factory;
        this.artistDao = artistDao;
        this.mediaSearchProvider = mediaSearchProvider;
        this.settingsFacade = settingsFacade;
        this.deligate = folderOrArtistLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_FOLDER_ARTIST;
    }

    @Override
    public Container createContainer(FolderOrFArtist folderOrArtist) {
        return deligate.createContainer(getProcId(), folderOrArtist);
    }

    @Override
    public List<FolderOrFArtist> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount();
    }

    @Override
    public FolderOrFArtist getDirectChild(String compositeId) {
        return deligate.getDirectChild(compositeId);
    }

    @Override
    public List<FArtistOrSong> getChildren(FolderOrFArtist folderOrArtist, long firstResult,
            long maxResults) {
        int offset = (int) firstResult;
        if (folderOrArtist.isFolderArtist()) {
            int randomMax = settingsFacade.get(UPnPSKeys.options.randomMax);
            int count = toCount(firstResult, maxResults, randomMax);
            return mediaSearchProvider
                .getRandomSongsByArtist(folderOrArtist.getFolderArtist().artist(), count, offset,
                        randomMax, util.getGuestFolders())
                .stream()
                .map(FArtistOrSong::new)
                .toList();
        }
        MusicFolder folder = folderOrArtist.getFolder();
        return artistDao
            .getAlphabetialArtists(offset, (int) maxResults,
                    Arrays.asList(folderOrArtist.getFolder()))
            .stream()
            .map(artist -> new FolderArtist(folder, artist))
            .map(FArtistOrSong::new)
            .toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFArtist folderOrArtist) {
        return deligate.getChildSizeOf(folderOrArtist);
    }

    @Override
    public void addChild(DIDLContent parent, FArtistOrSong artistOrSong) {
        if (artistOrSong.isFolderArtist()) {
            parent
                .addContainer(
                        deligate.createContainer(getProcId(), artistOrSong.getFolderArtist()));
        } else {
            parent.addItem(factory.toMusicTrack(artistOrSong.getSong()));
        }
    }
}
