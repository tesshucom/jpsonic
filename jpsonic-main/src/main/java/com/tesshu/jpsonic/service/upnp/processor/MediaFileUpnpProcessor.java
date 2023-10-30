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

package com.tesshu.jpsonic.service.upnp.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.stereotype.Service;

@Service
public class MediaFileUpnpProcessor extends DirectChildrenContentProcessor<MediaFile, MediaFile> {

    public static final int SINGLE_MUSIC_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;

    public MediaFileUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.FOLDER;
    }

    private void applyId(MediaFile item, Container container) {
        container.setId(ProcId.FOLDER.getValue() + ProcId.CID_SEPA + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
        if (mediaFileService.isRoot(item)) {
            container.setParentID(ProcId.FOLDER.getValue());
        } else {
            MediaFile parent = mediaFileService.getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        }
    }

    @Override
    public Container createContainer(MediaFile item) {
        if (item.isAlbum()) {
            MusicAlbum container = new MusicAlbum();
            container.addProperty(factory.toAlbumArt(item));
            if (item.getArtist() != null) {
                container.addProperty(factory.toPerson(item.getArtist()));
            }
            container.setDescription(item.getComment());
            applyId(item, container);
            return container;
        } else if (item.isDirectory()) {
            if (isEmpty(item.getArtist())) {
                StorageFolder container = new StorageFolder();
                applyId(item, container);
                return container;
            }
            MusicArtist container = new MusicArtist();
            applyId(item, container);
            item.getCoverArtPath().ifPresent(path -> container.addProperty(factory.toAlbumArt(item)));
            return container;
        }
        return null;
    }

    @Override
    public int getDirectChildrenCount() {
        int count;
        List<MusicFolder> allFolders = util.getGuestFolders();
        if (allFolders.size() == SINGLE_MUSIC_FOLDER) {
            count = mediaFileService.getChildSizeOf(allFolders.get(0));
        } else {
            count = allFolders.size();
        }
        return count;
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long maxResults) {
        List<MusicFolder> allFolders = util.getGuestFolders();
        List<MediaFile> returnValue = new ArrayList<>();
        if (allFolders.size() == SINGLE_MUSIC_FOLDER) {
            MediaFile folder = mediaFileService.getMediaFile(allFolders.get(0).toPath());
            if (folder != null) {
                returnValue = getChildren(folder, offset, maxResults);
            }
        } else {
            for (int i = (int) offset; i < Math.min(allFolders.size(), offset + maxResults); i++) {
                returnValue.add(mediaFileService.getMediaFile(allFolders.get(i).toPath()));
            }
        }
        return returnValue;
    }

    @Override
    public MediaFile getDirectChild(String id) {
        return mediaFileService.getMediaFileStrict(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(MediaFile item) {
        return mediaFileService.getChildSizeOf(item);
    }

    @Override
    public List<MediaFile> getChildren(MediaFile item, long offset, long maxResults) {
        if (item.isAlbum()) {
            return mediaFileService.getSongsForAlbum(offset, maxResults, item);
        }
        if (isEmpty(item.getArtist())) {
            return mediaFileService.getChildrenOf(item, offset, maxResults, false);
        }
        return mediaFileService.getChildrenOf(item, offset, maxResults, util.isSortAlbumsByYear(item.getName()));
    }

    @Override
    public void addItem(DIDLContent didl, MediaFile item) {
        if (item.isFile()) {
            didl.addItem(factory.toMusicTrack(item));
        } else {
            didl.addContainer(createContainer(item));
        }
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        if (child.isFile()) {
            didl.addItem(factory.toMusicTrack(child));
        } else {
            didl.addContainer(createContainer(child));
        }
    }

    public BrowseResult toBrowseResult(ParamSearchResult<MediaFile> result) {
        DIDLContent didl = new DIDLContent();
        try {
            result.getItems().forEach(i -> addItem(didl, i));
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
