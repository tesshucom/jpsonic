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
 * (C) 2009 Sindre Mehus
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.Playlist;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlaylistProvider;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class PlaylistProc extends DirectChildrenContentProc<Playlist, MediaFile> {

    private final SettingsFacade settingsFacade;
    private final MusicFolderProvider musicFolderProvider;
    private final PlaylistProvider playlistProvider;
    private final UPnPDIDLFactory factory;

    PlaylistProc(SettingsFacade settingsFacade, MusicFolderProvider musicFolderProvider,
            PlaylistProvider playlistProvider, UPnPDIDLFactory factory) {
        super();
        this.settingsFacade = settingsFacade;
        this.musicFolderProvider = musicFolderProvider;
        this.playlistProvider = playlistProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PLAYLIST;
    }

    @Override
    public Container createContainer(Playlist playlist) {
        return factory.toPlaylist(playlist);
    }

    @Override
    public List<Playlist> getDirectChildren(long offset, long count) {
        if (settingsFacade.get(UPnPSKeys.options.guestPublish)) {
            return playlistProvider.findPublishedPlaylists(offset, count);
        }
        return playlistProvider.findPlaylists(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        if (settingsFacade.get(UPnPSKeys.options.guestPublish)) {
            return playlistProvider.countPublishedPlaylists();
        }
        return playlistProvider.countPlaylists();
    }

    @Override
    public Playlist getDirectChild(String id) {
        return playlistProvider.requirePlaylist(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(Playlist playlist, long offset, long count) {
        return playlistProvider
            .findChildren(musicFolderProvider.getGuestFolders(), playlist, offset, count);
    }

    @Override
    public int getChildSizeOf(Playlist playlist) {
        return playlist.fileCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile song) {
        parent.addItem(factory.toMusicTrack(song));
    }
}
