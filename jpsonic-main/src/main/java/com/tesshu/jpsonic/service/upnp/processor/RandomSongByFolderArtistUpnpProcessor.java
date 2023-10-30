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

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.stereotype.Service;

@Service
public class RandomSongByFolderArtistUpnpProcessor
        extends DirectChildrenContentProcessor<FolderArtistWrapper, FolderArtistWrapper> {

    private static final String TYPE_PREFIX_MUSIC_FOLDER = "MusicFolder:";
    private static final String TYPE_PREFIX_ARTIST = "artist:";

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;
    private final MusicFolderDao musicFolderDao;
    private final MediaFileService mediaFileService;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomSongByFolderArtistUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService, MusicFolderDao musicFolderDao, ArtistDao artistDao,
            SearchService searchService, SettingsService settingsService) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.artistDao = artistDao;
        this.musicFolderDao = musicFolderDao;
        this.searchService = searchService;
        this.settingsService = settingsService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG_BY_FOLDER_ARTIST;
    }

    @Override
    public void addChild(DIDLContent didl, FolderArtistWrapper item) {
        if (item.isArtist()) {
            didl.addContainer(createContainer(item));
        } else {
            didl.addItem(factory.toMusicTrack(item.getSong()));
        }
    }

    protected final String createArtistId(String id) {
        if (isArtistId(id)) {
            return id;
        }
        return TYPE_PREFIX_ARTIST.concat(id);
    }

    @Override
    public Container createContainer(FolderArtistWrapper item) {
        if (item.isArtist()) {
            MusicArtist artist = factory.toArtist(item.getArtist());
            artist.setId(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST.getValue() + ProcId.CID_SEPA + item.getId());
            artist.setParentID(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST.getValue());
            return artist;
        } else {
            StorageFolder container = new StorageFolder();
            container.setId(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST.getValue() + ProcId.CID_SEPA + item.getId());
            container.setParentID(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST.getValue());
            container.setTitle(item.getName());
            container.setChildCount(getChildSizeOf(item));
            container.setParentID(ProcId.FOLDER.getValue());
            return container;
        }
    }

    protected final String createMusicFolderId(String id) {
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
                    .getRandomSongsByArtist(item.getArtist(), count, offset, randomMax, util.getGuestFolders()).stream()
                    .map(FolderArtist::new).collect(toList());
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
    public FolderArtistWrapper getDirectChild(String ids) {
        int id = toRawId(ids);
        if (isArtistId(ids)) {
            Artist artist = artistDao.getArtist(id);
            if (artist == null) {
                throw new IllegalArgumentException("The specified Artist cannot be found.");
            }
            return new FolderArtist(artist);
        } else if (isMusicFolderId(ids)) {
            return new FolderArtist(
                    musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
        }
        return new FolderArtist(mediaFileService.getMediaFileStrict(id));
    }

    @Override
    public int getDirectChildrenCount() {
        return util.getGuestFolders().size();
    }

    @Override
    public List<FolderArtistWrapper> getDirectChildren(long offset, long maxResults) {
        List<MusicFolder> folders = util.getGuestFolders();
        return folders.subList((int) offset, Math.min(folders.size(), (int) (offset + maxResults))).stream()
                .map(FolderArtist::new).collect(toList());
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
