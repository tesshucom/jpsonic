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

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.service.upnp.UPnPContentProcessor;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RootUpnpProcessor implements UPnPContentProcessor<Container, Container> {

    private final List<Container> containers = new ArrayList<>();
    private final UpnpProcessDispatcher dispatcher;
    private final SettingsService settingsService;
    private final ResourceBundle resourceBundle;

    public RootUpnpProcessor(@Lazy UpnpProcessDispatcher dispatcher, SettingsService settingsService) {
        this.dispatcher = dispatcher;
        this.settingsService = settingsService;
        this.resourceBundle = ResourceBundle.getBundle("com.tesshu.jpsonic.i18n.ResourceBundle",
                settingsService.getLocale());
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ROOT;
    }

    @Override
    public String getProcTitle() {
        return "Jpsonic Media";
    }

    @Override
    public void setProcTitle(String title) {
        // to be none
    }

    @Override
    public final Container createRootContainer() {
        StorageFolder root = new StorageFolder();
        root.setId(ProcId.ROOT.getValue());
        root.setParentID("-1");
        root.setStorageUsed(-1L);
        root.setTitle(getProcTitle());
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);
        root.setChildCount(6);
        return root;
    }

    @Override
    public Container createContainer(Container item) {
        return item;
    }

    @Override
    public int getDirectChildrenCount() {
        return containers.size();
    }

    private void addContainer(ProcId id) {
        UPnPContentProcessor<?, ?> proc = dispatcher.findProcessor(id);
        if (id != ProcId.ROOT) {
            String key = switch (id) {
            case ROOT -> "none";
            case FOLDER -> "dlna.title.folders";
            case ARTIST, ARTIST_BY_FOLDER -> "dlna.title.artists";
            case ALBUM -> "dlna.title.albums";
            case ALBUM_BY_GENRE -> "dlna.title.albumbygenres";
            case INDEX, INDEX_ID3 -> "dlna.title.index";
            case PLAYLIST -> "dlna.title.playlists";
            case PODCAST -> "dlna.title.podcast";
            case RANDOM_ALBUM -> "dlna.title.randomAlbum";
            case RANDOM_SONG -> "dlna.title.randomSong";
            case RANDOM_SONG_BY_ARTIST, RANDOM_SONG_BY_FOLDER_ARTIST -> "dlna.title.randomSongByArtist";
            case RECENT -> "dlna.title.recentAlbums";
            case RECENT_ID3 -> "dlna.title.recentAlbumsId3";
            case SONG_BY_GENRE -> "dlna.title.songbygenres";
            default -> throw new IllegalArgumentException("Unexpected value: " + getProcId());
            };
            proc.setProcTitle(resourceBundle.getString(key));
        }
        containers.add(proc.createRootContainer());
    }

    @Override
    public List<Container> getDirectChildren(long offset, long maxResults) {
        containers.clear();
        applyIndexContainer();
        if (settingsService.isDlnaFolderVisible()) {
            addContainer(ProcId.FOLDER);
        }
        applyArtistContainer();
        if (settingsService.isDlnaAlbumVisible()) {
            addContainer(ProcId.ALBUM);
        }
        if (settingsService.isDlnaPlaylistVisible()) {
            addContainer(ProcId.PLAYLIST);
        }
        if (settingsService.isDlnaAlbumByGenreVisible()) {
            addContainer(ProcId.ALBUM_BY_GENRE);
        }
        if (settingsService.isDlnaSongByGenreVisible()) {
            addContainer(ProcId.SONG_BY_GENRE);
        }
        applyRecentAlbumContainer();
        applyRandomContainer();
        if (settingsService.isDlnaPodcastVisible()) {
            addContainer(ProcId.PODCAST);
        }

        return PlayerUtils.subList(containers, offset, maxResults);
    }

    private void applyIndexContainer() {
        if (settingsService.isDlnaIndexVisible()) {
            addContainer(ProcId.INDEX);
        }
        if (settingsService.isDlnaIndexId3Visible()) {
            addContainer(ProcId.INDEX_ID3);
        }
    }

    private void applyArtistContainer() {
        if (settingsService.isDlnaArtistVisible()) {
            addContainer(ProcId.ARTIST);
        }
        if (settingsService.isDlnaArtistByFolderVisible()) {
            addContainer(ProcId.ARTIST_BY_FOLDER);
        }
    }

    private void applyRecentAlbumContainer() {
        if (settingsService.isDlnaRecentAlbumVisible()) {
            addContainer(ProcId.RECENT);
        }
        if (settingsService.isDlnaRecentAlbumId3Visible()) {
            addContainer(ProcId.RECENT_ID3);
        }
    }

    private void applyRandomContainer() {
        if (settingsService.isDlnaRandomSongVisible()) {
            addContainer(ProcId.RANDOM_SONG);
        }
        if (settingsService.isDlnaRandomAlbumVisible()) {
            addContainer(ProcId.RANDOM_ALBUM);
        }
        if (settingsService.isDlnaRandomSongByArtistVisible()) {
            addContainer(ProcId.RANDOM_SONG_BY_ARTIST);
        }
        if (settingsService.isDlnaRandomSongByFolderArtistVisible()) {
            addContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST);
        }
    }

    @Override
    public Container getDirectChild(String id) {
        return createRootContainer();
    }

    @Override
    public int getChildSizeOf(Container item) {
        return getChildren(item).size();
    }

    public final List<Container> getChildren(Container item) {
        return containers;
    }

    @Override
    public List<Container> getChildren(Container item, long offset, long maxResults) {
        return PlayerUtils.subList(getChildren(item), offset, maxResults);
    }

    @Override
    public void addChild(DIDLContent didl, Container child) {
        // to be none
    }
}
