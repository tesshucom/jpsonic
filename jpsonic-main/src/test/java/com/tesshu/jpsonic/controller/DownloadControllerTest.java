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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.entity.PlayQueue;
import com.tesshu.jpsonic.persistence.api.entity.Player;
import com.tesshu.jpsonic.persistence.api.entity.Playlist;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.ServletRequestBindingException;

class DownloadControllerTest extends AbstractNeedsScan {

    @Autowired
    private DownloadController downloadController;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private MediaFileDao mediaFileDao;

    private List<MusicFolder> musicFolders;

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 1,
                        false));
        }
        return musicFolders;
    }

    @Test
    @WithMockUser(username = "admin")
    void testHandleRequestWithMediaFile() throws ExecutionException {

        Player player = new Player();
        player.setId(1);
        playerService.createPlayer(player);

        MediaFile album = mediaFileDao.getNewestAlbums(0, 1, musicFolders).get(0);
        MediaFile song = mediaFileDao.getChildrenOf(album.getPathString()).get(0);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Range", "bytes=0-1023");
        req.setParameter("player", Integer.toString(player.getId()));
        req.setParameter("id", Integer.toString(song.getId()));
        MockHttpServletResponse res = new MockHttpServletResponse();

        try {
            downloadController.handleRequest(req, res);
            try (OutputStream out = res.getOutputStream()) {
                assertNotNull(out);
            }
        } catch (ServletRequestBindingException | IOException e) {
            throw new ExecutionException(e);
        }

        req.setParameter("id", Integer.toString(album.getId()));
        res = new MockHttpServletResponse();

        try {
            downloadController.handleRequest(req, res);
            try (OutputStream out = res.getOutputStream()) {
                assertNotNull(out);
            }
        } catch (ServletRequestBindingException | IOException e) {
            throw new ExecutionException(e);
        }

        Playlist playlist = new Playlist();
        playlist.setName("download test");
        playlist.setId(10);
        playlist.setCreated(now());
        playlist.setChanged(now());
        playlist.setShared(false);
        playlist.setUsername("admin");
        playlistService.createPlaylist(playlist);
        playlistService.setFilesInPlaylist(playlist.getId(), Arrays.asList(song));
        req = new MockHttpServletRequest();
        req.addHeader("Range", "bytes=0-1023");
        req.setParameter("player", Integer.toString(player.getId()));
        req.setParameter("id", Integer.toString(playlist.getId()));
        res = new MockHttpServletResponse();

        try {
            downloadController.handleRequest(req, res);
            try (OutputStream out = res.getOutputStream()) {
                assertNotNull(out);
            }
        } catch (ServletRequestBindingException | IOException e) {
            throw new ExecutionException(e);
        }

        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(false, playlistService.getFilesInPlaylist(playlist.getId()));
        player.setPlayQueue(playQueue);
        playerService.updatePlayer(player);
        req = new MockHttpServletRequest();
        req.addHeader("Range", "bytes=0-1023");
        req.setParameter("player", Integer.toString(player.getId()));
        res = new MockHttpServletResponse();

        try {
            downloadController.handleRequest(req, res);
            try (OutputStream out = res.getOutputStream()) {
                assertNotNull(out);
            }
        } catch (ServletRequestBindingException | IOException e) {
            throw new ExecutionException(e);
        }

        playlistService.deletePlaylist(playlist.getId());
        playerService.removePlayerById(player.getId());
    }
}
