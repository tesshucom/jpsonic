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

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.IndexType;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class MediaFileUpnpProcessor extends UpnpContentProcessor <MediaFile, MediaFile> {

    private final Pattern isVarious = Pattern.compile("^various.*$");

    protected final MediaFileDao mediaFileDao;

    protected final MediaFileService mediaFileService;

    private final SearchService searchService;

    public MediaFileUpnpProcessor(MediaFileDao mediaFileDao, MediaFileService mediaFileService, SearchService searchService) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.folders");
    }

    @Override
    // overriding for the case of browsing a file
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        MediaFile item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addChild(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    public Container createContainer(MediaFile item) {
        MusicAlbum container = new MusicAlbum();
        if (item.isAlbum()) {
            container.setAlbumArtURIs(new URI[] { getDispatcher().getAlbumProcessor().getAlbumArtURI(item.getId()) });
            if (item.getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
            }
            container.setDescription(item.getComment());
        }
        container.setId(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
        if (!mediaFileService.isRoot(item)) {
            MediaFile parent = mediaFileService.getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        } else {
            container.setParentID(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX);
        }
        return container;
    }

    @Override
    public int getItemCount() {
        int count = 0;
        List<MusicFolder> allFolders = getAllMusicFolders();
        if (allFolders.size() == 1) {
            count = mediaFileDao.getChildSizeOf(allFolders.get(0).getPath().getPath());
        } else {
            count = allFolders.size();
        }
        return count;
    }

    @Override
    public List<MediaFile> getItems(long offset, long maxResults) {
        List<MusicFolder> allFolders = getAllMusicFolders();
        List<MediaFile> returnValue = new ArrayList<MediaFile>();
        if (1 == allFolders.size()) {
            returnValue = getChildren(mediaFileService.getMediaFile(allFolders.get(0).getPath()), offset, maxResults);
        } else {
            for (int i = (int) offset; i < Math.min(allFolders.size(), offset + maxResults); i++) {
                returnValue.add(mediaFileService.getMediaFile(allFolders.get(i).getPath()));
            }
        }
        return returnValue;
    }

    public MediaFile getItemById(String id) {
        return mediaFileService.getMediaFile(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(MediaFile item) {
        return mediaFileDao.getChildSizeOf(item.getPath());
    }

    @Override
    public List<MediaFile> getChildren(MediaFile item, long offset, long maxResults) {
        if (item.isAlbum()) {
            return mediaFileDao.getSongsForAlbum(item, offset, maxResults);
        }
        if (isEmpty(item.getArtist())) {
            return mediaFileService.getChildrenOf(item, offset, maxResults, false);
        }
        // TODO #340
        boolean isSortAlbumsByYear = isSortAlbumsByYear() && !(isProhibitSortVarious() && isVarious.matcher(item.getName().toLowerCase()).matches());
        return mediaFileService.getChildrenOf(item, offset, maxResults, isSortAlbumsByYear);
    }

    public void addItem(DIDLContent didl, MediaFile item) {
        if (item.isFile()) {
            didl.addItem(createItem(item));
        } else {
            didl.addContainer(createContainer(item));
        }
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        if (child.isFile()) {
            didl.addItem(createItem(child));
        } else {
            didl.addContainer(createContainer(child));
        }
    }

    public final Item createItem(MediaFile song) {

        MusicTrack item = new MusicTrack();

        item.setId(String.valueOf(song.getId()));
        item.setTitle(song.getTitle());
        item.setAlbum(song.getAlbumName());
        if (song.getArtist() != null) {
            item.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(song.getArtist()));
        }
        Integer year = song.getYear();
        if (year != null) {
            item.setDate(year + "-01-01");
        }
        item.setOriginalTrackNumber(song.getTrackNumber());
        if (song.getGenre() != null) {
            item.setGenres(new String[] { song.getGenre() });
        }
        item.setResources(Arrays.asList(getDispatcher().createResourceForSong(song)));
        item.setDescription(song.getComment());

        MediaFile parent = mediaFileService.getParentOf(song);
        if (!ObjectUtils.isEmpty(parent)) {
            item.setParentID(String.valueOf(parent.getId()));
            item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(getDispatcher().getAlbumProcessor().getAlbumArtURI(parent.getId())));
        }

        return item;
    }

    public BrowseResult search(SearchCriteria criteria, IndexType indexType) {
        DIDLContent didl = new DIDLContent();
        try {
            ParamSearchResult<MediaFile> result = searchService.search(criteria, indexType);
            result.getItems().forEach(i -> addItem(didl, i));
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (Exception e) {
            return null;
        }
    }

}
