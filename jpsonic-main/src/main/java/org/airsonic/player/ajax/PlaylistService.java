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

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.ajax;

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.i18n.AirsonicLocaleResolver;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.directwebremoting.WebContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.text.DateFormat;
import java.util.*;

/**
 * Provides AJAX-enabled services for manipulating playlists.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxPlaylistService")
public class PlaylistService {

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    @Qualifier("playlistService")
    private org.airsonic.player.service.PlaylistService deligate;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private AirsonicLocaleResolver airsonicLocaleResolver;

    public List<Playlist> getReadablePlaylists() {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        return deligate.getReadablePlaylistsForUser(username);
    }

    public List<Playlist> getWritablePlaylists() {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        return deligate.getWritablePlaylistsForUser(username);
    }

    public PlaylistInfo getPlaylist(int id) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();

        Playlist playlist = deligate.getPlaylist(id);
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);

        String username = securityService.getCurrentUsername(request);
        mediaFileService.populateStarredDate(files, username);
        populateAccess(files, username);
        return new PlaylistInfo(playlist, createEntries(files));
    }

    private void populateAccess(List<MediaFile> files, String username) {
        for (MediaFile file : files) {
            if (!securityService.isFolderAccessAllowed(file, username)) {
                file.setPresent(false);
            }
        }
    }

    public List<Playlist> createEmptyPlaylist() {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        Locale locale = airsonicLocaleResolver.resolveLocale(request);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale);

        Date now = new Date();
        Playlist playlist = new Playlist();
        playlist.setUsername(securityService.getCurrentUsername(request));
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setShared(false);
        playlist.setName(dateFormat.format(now));

        deligate.createPlaylist(playlist);
        return getReadablePlaylists();
    }

    public int createPlaylistForPlayQueue() throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        HttpServletResponse response = WebContextFactory.get().getHttpServletResponse();
        Player player = playerService.getPlayer(request, response);
        Locale locale = airsonicLocaleResolver.resolveLocale(request);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale);

        Date now = new Date();
        Playlist playlist = new Playlist();
        playlist.setUsername(securityService.getCurrentUsername(request));
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setShared(false);
        playlist.setName(dateFormat.format(now));

        deligate.createPlaylist(playlist);
        deligate.setFilesInPlaylist(playlist.getId(), player.getPlayQueue().getFiles());

        return playlist.getId();
    }

    public int createPlaylistForStarredSongs() {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        Locale locale = airsonicLocaleResolver.resolveLocale(request);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale);

        Date now = new Date();
        Playlist playlist = new Playlist();
        String username = securityService.getCurrentUsername(request);
        playlist.setUsername(username);
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setShared(false);

        ResourceBundle bundle = ResourceBundle.getBundle("org.airsonic.player.i18n.ResourceBundle", locale);
        playlist.setName(bundle.getString("top.starred") + " " + dateFormat.format(now));

        deligate.createPlaylist(playlist);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);
        List<MediaFile> songs = mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders);
        deligate.setFilesInPlaylist(playlist.getId(), songs);

        return playlist.getId();
    }

    public void appendToPlaylist(int playlistId, List<Integer> mediaFileIds) {
        List<MediaFile> files = deligate.getFilesInPlaylist(playlistId, true);
        for (Integer mediaFileId : mediaFileIds) {
            MediaFile file = mediaFileService.getMediaFile(mediaFileId);
            if (file != null) {
                files.add(file);
            }
        }
        deligate.setFilesInPlaylist(playlistId, files);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Entry) Not reusable
    private List<PlaylistInfo.Entry> createEntries(List<MediaFile> files) {
        List<PlaylistInfo.Entry> result = new ArrayList<>();
        for (MediaFile file : files) {
            result.add(new PlaylistInfo.Entry(file.getId(), file.getTitle(), file.getArtist(), file.getComposer(), file.getAlbumName(),
                    file.getGenre(), file.getDurationString(), file.getStarredDate() != null, file.isPresent()));
        }

        return result;
    }

    public PlaylistInfo toggleStar(int id, int index) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);
        MediaFile file = files.get(index);

        boolean starred = mediaFileDao.getMediaFileStarredDate(file.getId(), username) != null;
        if (starred) {
            mediaFileDao.unstarMediaFile(file.getId(), username);
        } else {
            mediaFileDao.starMediaFile(file.getId(), username);
        }
        return getPlaylist(id);
    }

    public PlaylistInfo remove(int id, int index) {
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);
        files.remove(index);
        deligate.setFilesInPlaylist(id, files);
        return getPlaylist(id);
    }

    public PlaylistInfo up(int id, int index) {
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);
        if (index > 0) {
            MediaFile file = files.remove(index);
            files.add(index - 1, file);
            deligate.setFilesInPlaylist(id, files);
        }
        return getPlaylist(id);
    }

    public PlaylistInfo rearrange(int id, int... indexes) {
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);
        MediaFile[] newFiles = new MediaFile[files.size()];
        for (int i = 0; i < indexes.length; i++) {
            newFiles[i] = files.get(indexes[i]);
        }
        deligate.setFilesInPlaylist(id, Arrays.asList(newFiles));
        return getPlaylist(id);
    }

    public PlaylistInfo down(int id, int index) {
        List<MediaFile> files = deligate.getFilesInPlaylist(id, true);
        if (index < files.size() - 1) {
            MediaFile file = files.remove(index);
            files.add(index + 1, file);
            deligate.setFilesInPlaylist(id, files);
        }
        return getPlaylist(id);
    }

    public void deletePlaylist(int id) {
        deligate.deletePlaylist(id);
    }

    public PlaylistInfo updatePlaylist(int id, String name, String comment, boolean shared) {
        Playlist playlist = deligate.getPlaylist(id);
        playlist.setName(name);
        playlist.setComment(comment);
        playlist.setShared(shared);
        deligate.updatePlaylist(playlist);
        return getPlaylist(id);
    }

    public void setPlaylistService(org.airsonic.player.service.PlaylistService playlistService) {
        this.deligate = playlistService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setAirsonicLocaleResolver(AirsonicLocaleResolver localeResolver) {
        this.airsonicLocaleResolver = localeResolver;
    }
}
