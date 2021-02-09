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

package org.airsonic.player.service.upnp.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AlbumUpnpProcessor extends UpnpContentProcessor<Album, MediaFile> {

    public static final String ALL_BY_ARTIST = "allByArtist";
    public static final String ALL_RECENT_ID3 = "allRecentId3";

    private final UpnpProcessorUtil util;
    private final JMediaFileService mediaFileService;
    private final JAlbumDao albumDao;
    private final CoverArtLogic coverArtLogic;

    public AlbumUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m, JAlbumDao a,
            CoverArtLogic c) {
        super(d, u);
        this.util = u;
        this.mediaFileService = m;
        this.albumDao = a;
        this.coverArtLogic = c;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ALBUM_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.albums");
    }

    /**
     * Browses the top-level content of a type.
     */
    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion... orderBy)
            throws Exception {
        DIDLContent didl = new DIDLContent();
        List<Album> selectedItems = albumDao.getAlphabeticalAlbums((int) firstResult, (int) maxResults, false, true,
                util.getAllMusicFolders());
        for (Album item : selectedItems) {
            addItem(didl, item);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
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
        return albumDao.getAlphabeticalAlbums((int) offset, (int) maxResults, false, true, util.getAllMusicFolders());
    }

    @Override
    public Album getItemById(String id) {
        Album returnValue;
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
        return mediaFileService.getSongsCountForAlbum(album.getArtist(), album.getName());
    }

    @Override
    public List<MediaFile> getChildren(Album album, long offset, long maxResults) {
        List<MediaFile> children = mediaFileService.getSongsForAlbum(offset, maxResults, album.getArtist(),
                album.getName());
        if (album.getId() == -1) {
            List<Album> albums;
            if (album.getComment().startsWith(ALL_BY_ARTIST)) {
                ArtistUpnpProcessor ap = getDispatcher().getArtistProcessor();
                albums = ap.getChildren(ap.getItemById(album.getComment().replaceAll(ALL_BY_ARTIST + "_", "")), offset,
                        maxResults);
            } else if (album.getComment().equalsIgnoreCase(ALL_RECENT_ID3)) {
                albums = getDispatcher().getRecentAlbumId3Processor().getItems(offset, maxResults);
            } else {
                albums = new ArrayList<>();
            }
            for (Album a : albums) {
                if (a.getId() != -1) {
                    children.addAll(
                            mediaFileService.getSongsForAlbum(offset, maxResults, album.getArtist(), album.getName()));
                }
            }
        } else {
            children = mediaFileService.getSongsForAlbum(offset, maxResults, album.getArtist(), album.getName());
        }
        return children;
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsCountForArtist(artist, musicFolders);
    }

    public List<Album> getAlbumsForArtist(final String artist, long offset, long maxResults, boolean byYear,
            final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsForArtist(offset, maxResults, artist, byYear, musicFolders);
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

    public final PersonWithRole[] getAlbumArtists(String artist) {
        return new PersonWithRole[] { new PersonWithRole(artist) };
    }

    public URI createAlbumArtURI(Album album) {
        return util.createURIWithToken(UriComponentsBuilder
                .fromUriString(util.getBaseUrl() + "/ext/" + ViewName.COVER_ART.value())
                .queryParam("id", coverArtLogic.createKey(album)).queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

    public final BrowseResult toBrowseResult(ParamSearchResult<Album> result) {
        DIDLContent didl = new DIDLContent();
        try {
            for (Album item : result.getItems()) {
                addItem(didl, item);
            }
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (Exception e) {
            return null;
        }
    }

}
