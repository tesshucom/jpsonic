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

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class RootUpnpProcessor extends UpnpContentProcessor<Container, Container> {

    private final List<Container> containers = new ArrayList<>();
    private final SettingsService settingsService;

    public RootUpnpProcessor(@Lazy UpnpProcessDispatcher dispatcher, UpnpProcessorUtil util,
            SettingsService settingsService) {
        super(dispatcher, util);
        this.settingsService = settingsService;
    }

    @Override
    protected final Container createRootContainer() {
        StorageFolder root = new StorageFolder();
        root.setId(UpnpProcessDispatcher.CONTAINER_ID_ROOT);
        root.setParentID("-1");

        // MediaLibraryStatistics statistics = indexManager.getStatistics();
        // returning large storageUsed values doesn't play nicely with
        // some upnp clients
        // root.setStorageUsed(statistics == null ? 0 :
        // statistics.getTotalLengthInBytes());
        root.setStorageUsed(-1L);
        root.setTitle("Jpsonic Media");
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);

        root.setChildCount(6);
        return root;
    }

    @Override
    public void initTitle() {
        // to be none
    }

    @Override
    public Container createContainer(Container item) {
        // the items are the containers in this case.
        return item;
    }

    @Override
    public int getItemCount() {
        return containers.size();
    }

    @Override
    public List<Container> getItems(long offset, long maxResults) {
        containers.clear();
        addIndexContainer(containers);
        if (settingsService.isDlnaFolderVisible()) {
            containers.add(getDispatcher().getMediaFileProcessor().createRootContainer());
        }
        addArtistContainer(containers);
        if (settingsService.isDlnaAlbumVisible()) {
            containers.add(getDispatcher().getAlbumProcessor().createRootContainer());
        }
        if (settingsService.isDlnaPlaylistVisible()) {
            containers.add(getDispatcher().getPlaylistProcessor().createRootContainer());
        }
        if (settingsService.isDlnaAlbumByGenreVisible()) {
            containers.add(getDispatcher().getAlbumByGenreProcessor().createRootContainer());
        }
        if (settingsService.isDlnaSongByGenreVisible()) {
            containers.add(getDispatcher().getSongByGenreProcessor().createRootContainer());
        }
        addRecentAlbumContainer(containers);
        addRandomContainer(containers);
        if (settingsService.isDlnaPodcastVisible()) {
            containers.add(getDispatcher().getPodcastProcessor().createRootContainer());
        }

        return com.tesshu.jpsonic.util.PlayerUtils.subList(containers, offset, maxResults);
    }

    private void addIndexContainer(List<Container> containers) {
        if (settingsService.isDlnaIndexVisible()) {
            containers.add(getDispatcher().getIndexProcessor().createRootContainer());
        }
        if (settingsService.isDlnaIndexId3Visible()) {
            containers.add(getDispatcher().getIndexId3Processor().createRootContainer());
        }
    }

    private void addArtistContainer(List<Container> containers) {
        if (settingsService.isDlnaArtistVisible()) {
            containers.add(getDispatcher().getArtistProcessor().createRootContainer());
        }
        if (settingsService.isDlnaArtistByFolderVisible()) {
            containers.add(getDispatcher().getArtistByFolderProcessor().createRootContainer());
        }
    }

    private void addRecentAlbumContainer(List<Container> containers) {
        if (settingsService.isDlnaRecentAlbumVisible()) {
            containers.add(getDispatcher().getRecentAlbumProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRecentAlbumId3Visible()) {
            containers.add(getDispatcher().getRecentAlbumId3Processor().createRootContainer());
        }
    }

    private void addRandomContainer(List<Container> containers) {
        if (settingsService.isDlnaRandomSongVisible()) {
            containers.add(getDispatcher().getRandomSongProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRandomAlbumVisible()) {
            containers.add(getDispatcher().getRandomAlbumProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRandomSongByArtistVisible()) {
            containers.add(getDispatcher().getRandomSongByArtistProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRandomSongByFolderArtistVisible()) {
            containers.add(getDispatcher().getRandomSongByFolderArtistProcessor().createRootContainer());
        }
    }

    @Override
    public Container getItemById(String id) {
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
        return com.tesshu.jpsonic.util.PlayerUtils.subList(getChildren(item), offset, maxResults);
    }

    @Override
    public void addChild(DIDLContent didl, Container child) {
        // special case; root doesn't have object instances
    }

}
