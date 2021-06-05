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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistFactory;
import chameleon.playlist.SpecificPlaylistProvider;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.dao.JPlaylistDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.playlist.PlaylistExportHandler;
import com.tesshu.jpsonic.service.playlist.PlaylistImportHandler;
import com.tesshu.jpsonic.util.StringUtil;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * 
 * @see PlayQueue
 */
@Service
public class PlaylistService {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class);

    private final JMediaFileDao mediaFileDao;
    private final JPlaylistDao playlistDao;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final List<PlaylistExportHandler> exportHandlers;
    private final List<PlaylistImportHandler> importHandlers;
    private final JpsonicComparators comparators;

    public PlaylistService(JMediaFileDao mediaFileDao, JPlaylistDao playlistDao, SecurityService securityService,
            SettingsService settingsService, List<PlaylistExportHandler> exportHandlers,
            List<PlaylistImportHandler> importHandlers, JpsonicComparators comparators) {
        this.mediaFileDao = mediaFileDao;
        this.playlistDao = playlistDao;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.exportHandlers = exportHandlers;
        this.importHandlers = importHandlers;
        this.comparators = comparators;
    }

    public int getCountAll() {
        return playlistDao.getCountAll();
    }

    public List<Playlist> getAllPlaylists() {
        if (settingsService.isDlnaGuestPublish()) {
            return sort(playlistDao.getReadablePlaylistsForUser(User.USERNAME_GUEST));
        }
        return sort(playlistDao.getAllPlaylists());
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {
        return sort(playlistDao.getReadablePlaylistsForUser(username));
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {

        // Admin users are allowed to modify all playlists that are visible to them.
        if (securityService.isAdmin(username)) {
            return getReadablePlaylistsForUser(username);
        }

        return sort(playlistDao.getWritablePlaylistsForUser(username));
    }

    private List<Playlist> sort(List<Playlist> playlists) {
        playlists.sort(comparators.playlistOrder());
        return playlists;
    }

    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return playlistDao.getPlaylistUsers(playlistId);
    }

    public int getCountInPlaylist(int id) {
        return mediaFileDao.getCountInPlaylist(id);
    }

    public List<MediaFile> getFilesInPlaylist(int id) {
        return getFilesInPlaylist(id, false);
    }

    public List<MediaFile> getFilesInPlaylist(int id, long offset, long count) {
        return mediaFileDao.getFilesInPlaylist(id, offset, count);
    }

    public List<MediaFile> getFilesInPlaylist(int id, boolean includeNotPresent) {
        List<MediaFile> files = mediaFileDao.getFilesInPlaylist(id);
        if (includeNotPresent) {
            return files;
        }
        List<MediaFile> presentFiles = new ArrayList<>(files.size());
        for (MediaFile file : files) {
            if (file.isPresent()) {
                presentFiles.add(file);
            }
        }
        return presentFiles;
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        playlistDao.setFilesInPlaylist(id, files);
    }

    public void createPlaylist(Playlist playlist) {
        playlistDao.createPlaylist(playlist);
    }

    public void addPlaylistUser(int playlistId, String username) {
        playlistDao.addPlaylistUser(playlistId, username);
    }

    public void deletePlaylistUser(int playlistId, String username) {
        playlistDao.deletePlaylistUser(playlistId, username);
    }

    public boolean isReadAllowed(Playlist playlist, String username) {
        if (username == null) {
            return false;
        }
        if (username.equals(playlist.getUsername()) || playlist.isShared()) {
            return true;
        }
        return playlistDao.getPlaylistUsers(playlist.getId()).contains(username);
    }

    public boolean isWriteAllowed(Playlist playlist, String username) {
        return username != null && username.equals(playlist.getUsername());
    }

    public void deletePlaylist(int id) {
        playlistDao.deletePlaylist(id);
    }

    public void updatePlaylist(Playlist playlist) {
        playlistDao.updatePlaylist(playlist);
    }

    public Playlist importPlaylist(String username, String playlistName, String fileName, InputStream inputStream,
            Playlist existingPlaylist) throws ExecutionException {

        // TODO: handle other encodings
        SpecificPlaylist inputSpecificPlaylist;
        try {
            inputSpecificPlaylist = SpecificPlaylistFactory.getInstance().readFrom(inputStream, "UTF-8");
        } catch (IOException e) {
            throw new ExecutionException("Unsupported playlist " + fileName, e);
        }
        PlaylistImportHandler importHandler = getImportHandler(inputSpecificPlaylist);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using " + importHandler.getClass().getSimpleName() + " playlist import handler");
        }

        Pair<List<MediaFile>, List<String>> result = importHandler.handle(inputSpecificPlaylist);

        if (result.getLeft().isEmpty() && !result.getRight().isEmpty()) {
            for (String error : result.getRight()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("File in playlist '" + fileName + "' not found: " + error);
                }
            }
            throw new ExecutionException(new IOException("No songs in the playlist were found."));
        }

        Date now = new Date();
        Playlist playlist;
        if (existingPlaylist == null) {
            playlist = new Playlist();
            playlist.setUsername(username);
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(true);
            playlist.setName(playlistName);
            playlist.setComment("Auto-imported from " + fileName);
            playlist.setImportedFrom(fileName);
            createPlaylist(playlist);
        } else {
            playlist = existingPlaylist;
        }

        setFilesInPlaylist(playlist.getId(), result.getLeft());

        return playlist;
    }

    public String getExportPlaylistExtension() {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        return provider.getContentTypes()[0].getExtensions()[0];
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap and rethrow due to constraints of 'chameleon' {@link SpecificPlaylist#writeTo(OutputStream, String)}
     */
    public void exportPlaylist(int id, OutputStream out) throws ExecutionException {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        PlaylistExportHandler handler = getExportHandler(provider);
        try {
            SpecificPlaylist specificPlaylist = handler.handle(id, provider);
            specificPlaylist.writeTo(out, StringUtil.ENCODING_UTF8);
        } catch (Exception e) {
            throw new ExecutionException("Unable to write playlist to stream.", e);
        }
    }

    private PlaylistImportHandler getImportHandler(SpecificPlaylist playlist) {
        return importHandlers.stream().filter(handler -> handler.canHandle(playlist.getClass())).findFirst()
                .orElseThrow(() -> new RuntimeException("No import handler for " + playlist.getClass().getName()));

    }

    private PlaylistExportHandler getExportHandler(SpecificPlaylistProvider provider) {
        return exportHandlers.stream().filter(handler -> handler.canHandle(provider.getClass())).findFirst()
                .orElseThrow(() -> new RuntimeException("No export handler for " + provider.getClass().getName()));
    }

    public void importPlaylists() {
        if (settingsService.isVerboseLogScanning() && LOG.isInfoEnabled()) {
            LOG.info("Starting playlist import.");
        }
        doImportPlaylists();
        if (settingsService.isVerboseLogScanning() && LOG.isInfoEnabled()) {
            LOG.info("Completed playlist import.");
        }
    }

    private void doImportPlaylists() {
        String playlistFolderPath = settingsService.getPlaylistFolder();
        if (playlistFolderPath == null) {
            return;
        }
        File playlistFolder = new File(playlistFolderPath);
        if (!playlistFolder.exists()) {
            return;
        }

        List<Playlist> allPlaylists = playlistDao.getAllPlaylists();
        File[] listFiles = playlistFolder.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                try {
                    importPlaylistIfUpdated(file, allPlaylists);
                } catch (ExecutionException e) {
                    ConcurrentUtils.handleCauseUnchecked(e);
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to auto-import playlist " + file + ". ", e);
                    }
                }
            }
        }
    }

    private void importPlaylistIfUpdated(File file, List<Playlist> allPlaylists) throws ExecutionException {

        String fileName = file.getName();
        Playlist existingPlaylist = null;
        for (Playlist playlist : allPlaylists) {
            if (fileName.equals(playlist.getImportedFrom())) {
                existingPlaylist = playlist;
                if (file.lastModified() <= playlist.getChanged().getTime()) {
                    // Already imported and not changed since.
                    return;
                }
            }
        }
        try (InputStream in = Files.newInputStream(Paths.get(file.toURI()))) {
            // With the transition away from a hardcoded admin account to Admin Roles, there is no longer
            // a specific account to use for auto-imported playlists, so use the first admin account
            importPlaylist(securityService.getAdminUsername(), FilenameUtils.getBaseName(fileName), fileName, in,
                    existingPlaylist);
            if (LOG.isInfoEnabled()) {
                LOG.info("Auto-imported playlist " + file);
            }
        } catch (IOException e) {
            throw new ExecutionException("Unable to read the file: " + file.getPath(), e);
        }
    }

}
