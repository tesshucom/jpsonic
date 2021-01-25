/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */

package org.airsonic.player.service.upnp.processor;

import static java.util.stream.Collectors.toList;
import static org.airsonic.player.service.upnp.UpnpProcessDispatcher.CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ArtistByFolderUpnpProcessor
        extends UpnpContentProcessor<FolderArtistAlbumWrapper, FolderArtistAlbumWrapper> {

    private static final String TYPE_PREFIX_MUSIC_FOLDER = "mf:";
    private static final String TYPE_PREFIX_ARTIST = "ar:";
    private static final String TYPE_PREFIX_ALBUM = "al:";
    private final UpnpProcessorUtil util;
    private final JArtistDao artistDao;
    private final JAlbumDao albumDao;
    private final MusicFolderDao musicFolderDao;
    private final JMediaFileService mediaFileService;

    public ArtistByFolderUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            MusicFolderDao md, JArtistDao a, JAlbumDao al) {
        super(d, u);
        util = u;
        mediaFileService = m;
        artistDao = a;
        albumDao = al;
        musicFolderDao = md;
        setRootId(CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX);
    }

    @Override
    public void addChild(DIDLContent didl, FolderArtistAlbumWrapper item) {
        if (item.isArtist()) {
            didl.addContainer(createContainer(item));
        } else if (item.isAlbum()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(item.getSong()));
        }
    }

    static final String createAlbumId(String id) {
        if (isAlbumId(id)) {
            return id;
        }
        return TYPE_PREFIX_ALBUM.concat(id);
    }

    static final String createArtistId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ARTIST.concat(id);
    }

    @Override
    public Container createContainer(FolderArtistAlbumWrapper item) {
        if (item.isArtist()) {
            MusicArtist container = new MusicArtist();
            container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
            container.setParentID(getRootId());
            container.setTitle(item.getName());
            container.setChildCount(item.getArtist().getAlbumCount());
            if (item.getArtist().getCoverArtPath() != null) {
                container.setProperties(Arrays.asList(
                        new ALBUM_ART_URI(getDispatcher().getArtistProcessor().createArtistArtURI(item.getArtist()))));
            }
            return container;
        } else if (item.isAlbum()) {
            MusicAlbum container = new MusicAlbum();
            container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
            if (item.getAlbum().getCoverArtPath() != null) {
                container.setAlbumArtURIs(
                        new URI[] { getDispatcher().getAlbumProcessor().createAlbumArtURI(item.getAlbum()) });
            }
            container.setDescription(item.getAlbum().getComment());
            container.setParentID(getRootId());
            container.setTitle(item.getName());
            if (item.getAlbum().getArtist() != null) {
                container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getAlbum().getArtist()));
            }
            return container;
        } else {
            StorageFolder container = new StorageFolder();
            container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
            container.setParentID(getRootId());
            container.setTitle(item.getName());
            container.setChildCount(getChildSizeOf(item));
            container.setParentID(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX);
            return container;
        }
    }

    static final String createMusicFolderId(String id) {
        if (isMusicFolderId(id)) {
            return id;
        }
        return TYPE_PREFIX_MUSIC_FOLDER.concat(id);
    }

    @Override
    public List<FolderArtistAlbumWrapper> getChildren(FolderArtistAlbumWrapper item, long first, long maxResults) {
        if (item.isArtist()) {
            return getDispatcher()
                    .getAlbumProcessor().getAlbumsForArtist(item.getName(), first, maxResults,
                            util.isSortAlbumsByYear(item.getName()), util.getAllMusicFolders())
                    .stream().map(Leaf::new).collect(toList());
        } else if (item.isAlbum()) {
            return mediaFileService
                    .getSongsForAlbum(first, maxResults, item.getAlbum().getArtist(), item.getAlbum().getName())
                    .stream().map(Leaf::new).collect(toList());
        } else {
            return artistDao.getAlphabetialArtists((int) first, (int) maxResults, Arrays.asList(item.getFolder()))
                    .stream().map(Leaf::new).collect(toList());
        }
    }

    @Override
    public int getChildSizeOf(FolderArtistAlbumWrapper item) {
        return 1;
    }

    @Override
    public FolderArtistAlbumWrapper getItemById(String ids) {
        int id = toRawId(ids);
        if (isArtistId(ids)) {
            return new Leaf(artistDao.getArtist(id));
        } else if (isAlbumId(ids)) {
            return new Leaf(albumDao.getAlbum(id));
        } else if (isMusicFolderId(ids)) {
            return new Leaf(
                    musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
        }
        return new Leaf(mediaFileService.getMediaFile(id));
    }

    @Override
    public int getItemCount() {
        return util.getAllMusicFolders().size();
    }

    @Override
    public List<FolderArtistAlbumWrapper> getItems(long offset, long maxResults) {
        List<MusicFolder> folders = util.getAllMusicFolders();
        return folders.subList((int) offset, Math.min(folders.size(), (int) (offset + maxResults))).stream()
                .map(Leaf::new).collect(toList());
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.artists");
    }

    private static final boolean isAlbumId(String id) {
        return id.startsWith(TYPE_PREFIX_ALBUM);
    }

    private static final boolean isArtistId(String id) {
        return id.startsWith(TYPE_PREFIX_ARTIST);
    }

    private static final boolean isMusicFolderId(String id) {
        return id.startsWith(TYPE_PREFIX_MUSIC_FOLDER);
    }

    private final int toRawId(String prefixed) {
        return Integer.parseInt(prefixed.replaceAll("^.*:", ""));
    }

    static class Leaf implements FolderArtistAlbumWrapper {

        private Artist artist;
        private Album album;
        private MusicFolder folder;
        private MediaFile song;

        private final String id;

        private final String name;

        public Leaf(Album album) {
            super();
            this.album = album;
            this.id = createAlbumId(Integer.toString(album.getId()));
            this.name = album.getName();
        }

        public Leaf(Artist artist) {
            super();
            this.artist = artist;
            this.id = createArtistId(Integer.toString(artist.getId()));
            this.name = artist.getName();
        }

        public Leaf(MediaFile song) {
            super();
            this.song = song;
            this.id = Integer.toString(song.getId());
            this.name = song.getName();
        }

        public Leaf(MusicFolder folder) {
            super();
            this.folder = folder;
            this.id = createMusicFolderId(Integer.toString(folder.getId()));
            this.name = folder.getName();
        }

        @Override
        public Album getAlbum() {
            return album;
        }

        @Override
        public Artist getArtist() {
            return artist;
        }

        @Override
        public MusicFolder getFolder() {
            return folder;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MediaFile getSong() {
            return song;
        }

        @Override
        public boolean isAlbum() {
            return null != album;
        }

        @Override
        public boolean isArtist() {
            return null != artist;
        }
    }
}
