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

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.stereotype.Service;

@Service
public class ArtistByFolderUpnpProcessor
        extends DirectChildrenContentProcessor<FolderArtistAlbumWrapper, FolderArtistAlbumWrapper> {

    private static final String TYPE_PREFIX_MUSIC_FOLDER = "mf:";
    private static final String TYPE_PREFIX_ARTIST = "ar:";
    private static final String TYPE_PREFIX_ALBUM = "al:";

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final MusicFolderDao musicFolderDao;
    private final MediaFileService mediaFileService;

    public ArtistByFolderUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService, MusicFolderDao musicFolderDao, ArtistDao artistDao, AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.musicFolderDao = musicFolderDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ARTIST_BY_FOLDER;
    }

    @Override
    public void addChild(DIDLContent didl, FolderArtistAlbumWrapper item) {
        if (item.isArtist()) {
            didl.addContainer(createContainer(item));
        } else if (item.isAlbum()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(factory.toMusicTrack(item.getSong()));
        }
    }

    protected static final String createAlbumId(String id) {
        if (isAlbumId(id)) {
            return id;
        }
        return TYPE_PREFIX_ALBUM.concat(id);
    }

    protected static final String createArtistId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ARTIST.concat(id);
    }

    @Override
    public Container createContainer(FolderArtistAlbumWrapper item) {
        if (item.isArtist()) {
            return factory.toArtist(item.getArtist());
        } else if (item.isAlbum()) {
            return factory.toAlbum(item.getAlbum());
        } else {
            StorageFolder container = new StorageFolder();
            container.setId(getProcId().getValue() + ProcId.CID_SEPA + item.getId());
            container.setTitle(item.getName());
            container.setChildCount(getChildSizeOf(item));
            container.setParentID(ProcId.FOLDER.getValue());
            return container;
        }
    }

    protected static final String createMusicFolderId(String id) {
        if (isMusicFolderId(id)) {
            return id;
        }
        return TYPE_PREFIX_MUSIC_FOLDER.concat(id);
    }

    @Override
    public List<FolderArtistAlbumWrapper> getChildren(FolderArtistAlbumWrapper item, long first, long maxResults) {
        if (item.isArtist()) {
            return albumDao.getAlbumsForArtist(first, maxResults, item.getName(),
                    util.isSortAlbumsByYear(item.getName()), util.getGuestFolders()).stream().map(FolderContent::new)
                    .collect(toList());
        } else if (item.isAlbum()) {
            return mediaFileService
                    .getSongsForAlbum(first, maxResults, item.getAlbum().getArtist(), item.getAlbum().getName())
                    .stream().map(FolderContent::new).collect(toList());
        } else {
            return artistDao.getAlphabetialArtists((int) first, (int) maxResults, Arrays.asList(item.getFolder()))
                    .stream().map(FolderContent::new).collect(toList());
        }
    }

    @Override
    public int getChildSizeOf(FolderArtistAlbumWrapper item) {
        return 1;
    }

    @Override
    public FolderArtistAlbumWrapper getDirectChild(String ids) {
        int id = toRawId(ids);
        if (isArtistId(ids)) {
            Artist artist = artistDao.getArtist(id);
            if (artist == null) {
                throw new IllegalArgumentException("The specified Artist cannot be found.");
            }
            return new FolderContent(artist);
        } else if (isAlbumId(ids)) {
            Album album = albumDao.getAlbum(id);
            if (album == null) {
                throw new IllegalArgumentException("The specified Album cannot be found.");
            }
            return new FolderContent(album);
        } else if (isMusicFolderId(ids)) {
            return new FolderContent(
                    musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
        }
        return new FolderContent(mediaFileService.getMediaFileStrict(id));
    }

    @Override
    public int getDirectChildrenCount() {
        return util.getGuestFolders().size();
    }

    @Override
    public List<FolderArtistAlbumWrapper> getDirectChildren(long offset, long maxResults) {
        List<MusicFolder> folders = util.getGuestFolders();
        return folders.subList((int) offset, Math.min(folders.size(), (int) (offset + maxResults))).stream()
                .map(FolderContent::new).collect(toList());
    }

    private static boolean isAlbumId(String id) {
        return id.startsWith(TYPE_PREFIX_ALBUM);
    }

    private static boolean isArtistId(String id) {
        return id.startsWith(TYPE_PREFIX_ARTIST);
    }

    private static boolean isMusicFolderId(String id) {
        return id.startsWith(TYPE_PREFIX_MUSIC_FOLDER);
    }

    private int toRawId(String prefixed) {
        return Integer.parseInt(prefixed.replaceAll("^.*:", ""));
    }

    static class FolderContent implements FolderArtistAlbumWrapper {

        private Artist artist;
        private Album album;
        private MusicFolder folder;
        private MediaFile song;

        private final String id;

        private final String name;

        public FolderContent(Album album) {
            super();
            this.album = album;
            this.id = createAlbumId(Integer.toString(album.getId()));
            this.name = album.getName();
        }

        public FolderContent(Artist artist) {
            super();
            this.artist = artist;
            this.id = createArtistId(Integer.toString(artist.getId()));
            this.name = artist.getName();
        }

        public FolderContent(MediaFile song) {
            super();
            this.song = song;
            this.id = Integer.toString(song.getId());
            this.name = song.getName();
        }

        public FolderContent(MusicFolder folder) {
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
