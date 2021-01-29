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

import static java.util.stream.Collectors.toList;
import static org.airsonic.player.service.upnp.UpnpProcessDispatcher.CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RandomSongByFolderArtistUpnpProcessor
        extends UpnpContentProcessor<FolderArtistWrapper, FolderArtistWrapper> {

    private static final String TYPE_PREFIX_MUSIC_FOLDER = "MusicFolder:";
    private static final String TYPE_PREFIX_ARTIST = "artist:";
    private final UpnpProcessorUtil util;
    private final JArtistDao artistDao;
    private final MusicFolderDao musicFolderDao;
    private final JMediaFileService mediaFileService;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomSongByFolderArtistUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u,
            JMediaFileService m, MusicFolderDao md, JArtistDao a, SearchService s, SettingsService ss) {
        super(d, u);
        util = u;
        mediaFileService = m;
        artistDao = a;
        musicFolderDao = md;
        searchService = s;
        settingsService = ss;
        setRootId(CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST);
    }

    @Override
    public void addChild(DIDLContent didl, FolderArtistWrapper item) {
        if (item.isArtist()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(item.getSong()));
        }
    }

    final String createArtistId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ARTIST.concat(id);
    }

    @Override
    public Container createContainer(FolderArtistWrapper item) {
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

    final String createMusicFolderId(String id) {
        if (isMusicFolderId(id)) {
            return id;
        }
        return TYPE_PREFIX_MUSIC_FOLDER.concat(id);
    }

    @Override
    public List<FolderArtistWrapper> getChildren(FolderArtistWrapper item, long first, long maxResults) {
        int offset = (int) first;
        if (item.isArtist()) {
            int randomMax = settingsService.getDlnaRandomMax();
            int count = (offset + (int) maxResults) > randomMax ? randomMax - offset : (int) maxResults;
            return searchService
                    .getRandomSongsByArtist(item.getArtist(), count, offset, randomMax, util.getAllMusicFolders())
                    .stream().map(FolderArtist::new).collect(toList());
        } else {
            return artistDao.getAlphabetialArtists(offset, (int) maxResults, Arrays.asList(item.getFolder())).stream()
                    .map(FolderArtist::new).collect(toList());
        }
    }

    @Override
    public int getChildSizeOf(FolderArtistWrapper item) {
        return 1;
    }

    @Override
    public FolderArtistWrapper getItemById(String ids) {
        int id = toRawId(ids);
        if (isArtistId(ids)) {
            return new FolderArtist(artistDao.getArtist(id));
        } else if (isMusicFolderId(ids)) {
            return new FolderArtist(
                    musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
        }
        return new FolderArtist(mediaFileService.getMediaFile(id));
    }

    @Override
    public int getItemCount() {
        return util.getAllMusicFolders().size();
    }

    @Override
    public List<FolderArtistWrapper> getItems(long offset, long maxResults) {
        List<MusicFolder> folders = util.getAllMusicFolders();
        return folders.subList((int) offset, Math.min(folders.size(), (int) (offset + maxResults))).stream()
                .map(FolderArtist::new).collect(toList());
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomSongByArtist");
    }

    private boolean isArtistId(String id) {
        return id.startsWith(TYPE_PREFIX_ARTIST);
    }

    private boolean isMusicFolderId(String id) {
        return id.startsWith(TYPE_PREFIX_MUSIC_FOLDER);
    }

    private int toRawId(String prefixed) {
        return Integer.parseInt(prefixed.replaceAll("^.*:", ""));
    }

    class FolderArtist implements FolderArtistWrapper {

        private Artist artist;
        private MusicFolder folder;
        private MediaFile song;

        private final String id;
        private final String name;

        public FolderArtist(Artist artist) {
            super();
            this.artist = artist;
            this.id = createArtistId(Integer.toString(artist.getId()));
            this.name = artist.getName();
        }

        public FolderArtist(MediaFile song) {
            super();
            this.song = song;
            this.id = Integer.toString(song.getId());
            this.name = song.getName();
        }

        public FolderArtist(MusicFolder folder) {
            super();
            this.folder = folder;
            this.id = createMusicFolderId(Integer.toString(folder.getId()));
            this.name = folder.getName();
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
        public boolean isArtist() {
            return null != artist;
        }
    }
}
