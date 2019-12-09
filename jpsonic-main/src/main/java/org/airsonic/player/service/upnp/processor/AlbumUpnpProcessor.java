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

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlbumUpnpProcessor extends UpnpContentProcessor <Album, MediaFile> {

    private final UpnpProcessorUtil util;

    private final SearchService searchService;

    private final MediaFileDao mediaFileDao;

    private final AlbumDao albumDao;

    private final CoverArtLogic coverArtLogic;

    public static final String ALL_BY_ARTIST = "allByArtist";

    public static final String ALL_RECENT_ID3 = "allRecentId3";

    public AlbumUpnpProcessor(UpnpProcessDispatcher dispatcher, UpnpProcessorUtil util, SearchService searchService, MediaFileDao mediaFileDao, AlbumDao albumDao, CoverArtLogic coverArtLogic) {
        super(dispatcher, util);
        this.util = util;
        this.searchService = searchService;
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
        this.coverArtLogic = coverArtLogic;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ALBUM_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.albums");
    }

    /**
     * Browses the top-level content of a type.
     */
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<Album> selectedItems = albumDao.getAlphabeticalAlbums(firstResult, maxResults, false, true, util.getAllMusicFolders());
        for (Album item : selectedItems) {
            addItem(didl, item);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    public Container createContainer(Album album) {
        MusicAlbum container = new MusicAlbum();

        if (album.getId() == -1) {
            container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + album.getComment());
        } else {
            container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + album.getId());
            if (album.getCoverArtPath() != null) {
                container.setAlbumArtURIs(new URI[] { createAlbumArtURI(album) });
            }
            container.setDescription(album.getComment());
        }
        container.setParentID(getRootId());
        container.setTitle(album.getName());
        if (album.getArtist() != null) {
            container.setArtists(getAlbumArtists(album.getArtist()));
        }
        return container;
    }

    @Override
    public int getItemCount() {
        return albumDao.getAlbumCount(util.getAllMusicFolders());
    }

    @Override
    public List<Album> getItems(long offset, long maxResults) {
        return albumDao.getAlphabeticalAlbums(offset, maxResults, false, true, util.getAllMusicFolders());
    }

    public Album getItemById(String id) {
        Album returnValue = null;
        if (id.startsWith(ALL_BY_ARTIST) || id.equalsIgnoreCase(ALL_RECENT_ID3)) {
            returnValue = new Album();
            returnValue.setId(-1);
            returnValue.setComment(id);
        } else {
            returnValue = albumDao.getAlbum(Integer.parseInt(id));
        }
        return returnValue;
    }

    @Override
    public int getChildSizeOf(Album album) {
        return mediaFileDao.getSongsCountForAlbum(album.getArtist(), album.getName());
    }

    @Override
    public List<MediaFile> getChildren(Album album, long offset, long maxResults) {
        List<MediaFile> children = mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName(), offset, maxResults);
        if (album.getId() == -1) {
            List<Album> albums = null;
            if (album.getComment().startsWith(ALL_BY_ARTIST)) {
                ArtistUpnpProcessor ap = getDispatcher().getArtistProcessor();
                albums = ap.getChildren(ap.getItemById(album.getComment().replaceAll(ALL_BY_ARTIST + "_", "")), offset, maxResults);
            } else if (album.getComment().equalsIgnoreCase(ALL_RECENT_ID3)) {
                albums = getDispatcher().getRecentAlbumId3Processor().getItems(offset, maxResults);
            } else {
                albums = new ArrayList<>();
            }
            for (Album a : albums) {
                if (a.getId() != -1) {
                    children.addAll(mediaFileDao.getSongsForAlbum(a.getArtist(), a.getName(), offset, maxResults));
                }
            }
        } else {
            children = mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName(), offset, maxResults);
        }
        return children;
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsCountForArtist(artist, musicFolders);
    }

    public List<Album> getAlbumsForArtist(final String artist, long offset, long maxResults, boolean byYear, final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsForArtist(offset, maxResults, artist, byYear, musicFolders);
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

    public final PersonWithRole[] getAlbumArtists(String artist) {
        return new PersonWithRole[] { new PersonWithRole(artist) };
    }

    private URI createAlbumArtURI(Album album) {
        return util.createURIWithToken(UriComponentsBuilder.fromUriString(util.getBaseUrl() + "/ext/coverArt.view")
                .queryParam("id", coverArtLogic.createKey(album))
                .queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

    public BrowseResult searchByName(String name, long firstResult, long maxResults, SortCriterion[] orderBy) {
        DIDLContent didl = new DIDLContent();
        try {
            List<MusicFolder> folders = util.getAllMusicFolders();
            @SuppressWarnings("deprecation")
            ParamSearchResult<Album> result = searchService.searchByName(name, (int) firstResult, (int) maxResults, folders, Album.class);
            List<Album> selectedItems = result.getItems();
            for (Album item : selectedItems) {
                addItem(didl, item);
            }
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (Exception e) {
            return null;
        }
    }

}
