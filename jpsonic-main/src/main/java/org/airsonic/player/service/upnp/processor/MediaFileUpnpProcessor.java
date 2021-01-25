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

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.Player;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.airsonic.player.util.StringUtil;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.seamless.util.MimeType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

@Service
public class MediaFileUpnpProcessor extends UpnpContentProcessor<MediaFile, MediaFile> {

    private final UpnpProcessorUtil util;

    private final JMediaFileService mediaFileService;

    private final PlayerService playerService;

    public static final int SINGLE_MUSIC_FOLDER = 1;

    public MediaFileUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            PlayerService p) {
        super(d, u);
        this.util = u;
        this.mediaFileService = m;
        this.playerService = p;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX);
    }

    @PostConstruct
    @Override
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

    private void applyId(MediaFile item, Container container) {
        container.setId(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR
                + item.getId());
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
    }

    @Override
    public Container createContainer(MediaFile item) {
        if (item.isAlbum()) {
            MusicAlbum container = new MusicAlbum();
            container.setAlbumArtURIs(new URI[] { createAlbumArtURI(item) });
            if (item.getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
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
            return container;
        }
        return null;
    }

    @Override
    public int getItemCount() {
        int count;
        List<MusicFolder> allFolders = util.getAllMusicFolders();
        if (allFolders.size() == SINGLE_MUSIC_FOLDER) {
            count = mediaFileService.getChildSizeOf(allFolders.get(0));
        } else {
            count = allFolders.size();
        }
        return count;
    }

    @Override
    public List<MediaFile> getItems(long offset, long maxResults) {
        List<MusicFolder> allFolders = util.getAllMusicFolders();
        List<MediaFile> returnValue = new ArrayList<>();
        if (allFolders.size() == SINGLE_MUSIC_FOLDER) {
            returnValue = getChildren(mediaFileService.getMediaFile(allFolders.get(0).getPath()), offset, maxResults);
        } else {
            for (int i = (int) offset; i < Math.min(allFolders.size(), offset + maxResults); i++) {
                returnValue.add(mediaFileService.getMediaFile(allFolders.get(i).getPath()));
            }
        }
        return returnValue;
    }

    @Override
    public MediaFile getItemById(String id) {
        return mediaFileService.getMediaFile(Integer.parseInt(id));
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
            didl.addItem(createItem(item));
        } else {
            didl.addContainer(createContainer(item));
        }
    }

    @Override
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
        item.setResources(Arrays.asList(createResourceForSong(song)));
        item.setDescription(song.getComment());

        MediaFile parent = mediaFileService.getParentOf(song);
        if (!ObjectUtils.isEmpty(parent)) {
            item.setParentID(String.valueOf(parent.getId()));
            item.addProperty(new ALBUM_ART_URI(createAlbumArtURI(parent)));
        }

        return item;
    }

    public final BrowseResult toBrowseResult(ParamSearchResult<MediaFile> result) {
        DIDLContent didl = new DIDLContent();
        try {
            result.getItems().forEach(i -> addItem(didl, i));
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (Exception e) {
            return null;
        }
    }

    public final Res createResourceForSong(MediaFile song) {
        Player player = playerService.getGuestPlayer(null);
        MimeType mimeType = util.getMimeType(song, player);
        Res res = new Res(mimeType, null, createStreamURI(song, player));
        res.setDuration(formatDuration(song.getDurationSeconds()));
        return res;
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        return StringUtil.formatDurationHMMSS((int) seconds) + ".0";
    }

    public final URI createArtistArtURI(MediaFile artist) {
        return util.createURIWithToken(
                UriComponentsBuilder.fromUriString(util.getBaseUrl() + "/ext/" + ViewName.COVER_ART.value())
                        .queryParam("id", artist.getId()).queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

    public final URI createAlbumArtURI(MediaFile album) {
        return util.createURIWithToken(
                UriComponentsBuilder.fromUriString(util.getBaseUrl() + "/ext/" + ViewName.COVER_ART.value())
                        .queryParam("id", album.getId()).queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

    private String createStreamURI(MediaFile song, Player player) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(util.getBaseUrl() + "/ext/stream")
                .queryParam("id", song.getId()).queryParam("player", player.getId());
        if (song.isVideo()) {
            builder.queryParam("format", TranscodingService.FORMAT_RAW);
        }
        return util.createURIStringWithToken(builder);
    }

}
