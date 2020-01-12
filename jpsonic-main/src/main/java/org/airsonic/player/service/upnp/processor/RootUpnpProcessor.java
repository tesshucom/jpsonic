/*
  This file is part of Airsonic.

  Airsonic is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Airsonic is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RootUpnpProcessor extends UpnpContentProcessor<Container, Container> {

    private ArrayList<Container> containers = new ArrayList<>();

    private SettingsService settingsService;

    public RootUpnpProcessor(@Lazy UpnpProcessDispatcher dispatcher, UpnpProcessorUtil util, SettingsService settingsService) {
        super(dispatcher, util);
        this.settingsService = settingsService;
    }

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

        if (settingsService.isDlnaIndexVisible()) {
            containers.add(getDispatcher().getIndexProcessor().createRootContainer());
        }
        if (settingsService.isDlnaIndexId3Visible()) {
            containers.add(getDispatcher().getIndexId3Processor().createRootContainer());
        }
        if (settingsService.isDlnaFolderVisible()) {
            containers.add(getDispatcher().getMediaFileProcessor().createRootContainer());
        }
        if (settingsService.isDlnaArtistVisible()) {
            containers.add(getDispatcher().getArtistProcessor().createRootContainer());
        }
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
        if (settingsService.isDlnaRecentAlbumVisible()) {
            containers.add(getDispatcher().getRecentAlbumProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRecentAlbumId3Visible()) {
            containers.add(getDispatcher().getRecentAlbumId3Processor().createRootContainer());
        }
        if (settingsService.isDlnaRandomAlbumVisible()) {
            containers.add(getDispatcher().getRandomAlbumProcessor().createRootContainer());
        }
        if (settingsService.isDlnaRandomSongVisible()) {
            containers.add(getDispatcher().getRandomSongProcessor().createRootContainer());
        }
        if (settingsService.isDlnaPodcastVisible()) {
            containers.add(getDispatcher().getPodcastProcessor().createRootContainer());
        }

        return org.airsonic.player.util.Util.subList(containers, offset, maxResults);
    }

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
        return org.airsonic.player.util.Util.subList(getChildren(item), offset, maxResults);
    }

    public void addChild(DIDLContent didl, Container child) {
        // special case; root doesn't have object instances
    }

}
