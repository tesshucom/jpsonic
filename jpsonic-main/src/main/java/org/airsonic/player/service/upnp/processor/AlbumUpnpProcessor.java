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
import org.airsonic.player.service.JWTSecurityService;
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

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class AlbumUpnpProcessor extends UpnpContentProcessor <Album, MediaFile> {

    public static final String ALL_BY_ARTIST = "allByArtist";

    public static final String ALL_RECENT = "allRecent";

    private final MediaFileDao mediaFileDao;

    protected final AlbumDao albumDao;
    
    private JWTSecurityService jwtSecurityService;

    public AlbumUpnpProcessor(MediaFileDao mediaFileDao, AlbumDao albumDao, JWTSecurityService jwtSecurityService) {
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
        this.jwtSecurityService = jwtSecurityService;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ALBUM_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dnla.title.albums");
    }

    /**
     * Browses the top-level content of a type.
     */
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<Album> selectedItems = albumDao.getAlphabeticalAlbums(firstResult, maxResults, false, true, getAllMusicFolders());
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
            container.setAlbumArtURIs(new URI[] { getAlbumArtURI(album.getId()) });
            container.setDescription(album.getComment());
        }
        container.setParentID(getRootId());
        container.setTitle(album.getName());
        // TODO: correct artist?
        if (album.getArtist() != null) {
            container.setArtists(getAlbumArtists(album.getArtist()));
        }
        return container;
    }

    @Override
    public int getItemCount() {
        return albumDao.getAlbumCount(getAllMusicFolders());
    }

    @Override
    public List<Album> getItems(long offset, long maxResults) {
        return albumDao.getAlphabeticalAlbums(offset, maxResults, false, true, getAllMusicFolders());
    }

    public Album getItemById(String id) {
        Album returnValue = null;
        if (id.startsWith(ALL_BY_ARTIST) || id.equalsIgnoreCase(ALL_RECENT)) {
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
            } else if (album.getComment().equalsIgnoreCase(ALL_RECENT)) {
                albums = getDispatcher().getRecentAlbumProcessor().getItems(offset, maxResults);
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

    public List<Album> getAlbumsForArtist(final String artist, long offset, long maxResults, final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsForArtist(offset, maxResults, artist, musicFolders);
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

    public URI getAlbumArtURI(int albumId) {
        return jwtSecurityService.addJWTToken(UriComponentsBuilder.fromUriString(getDispatcher().getBaseUrl() + "/ext/coverArt.view").queryParam("id", albumId).queryParam("size", CoverArtScheme.LARGE.getSize())).build().encode().toUri();
    }

    public PersonWithRole[] getAlbumArtists(String artist) {
        return new PersonWithRole[] { new PersonWithRole(artist) };
    }

}
