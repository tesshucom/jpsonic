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

package com.tesshu.jpsonic.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for showing a user's starred items.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/starred", "/starred.view" })
public class StarredController {

    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final PlayerService playerService;
    private final MediaFileDao mediaFileDao;
    private final MediaFileService mediaFileService;
    private final ViewAsListSelector viewSelector;

    public StarredController(MusicFolderService musicFolderService, SecurityService securityService,
            PlayerService playerService, MediaFileDao mediaFileDao, MediaFileService mediaFileService,
            ViewAsListSelector viewSelector) {
        super();
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.mediaFileDao = mediaFileDao;
        this.mediaFileService = mediaFileService;
        this.viewSelector = viewSelector;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        List<MusicFolder> musicFolders = musicFolderService.getMusicFoldersForUser(username);

        List<MediaFile> artists = mediaFileDao.getStarredDirectories(0, Integer.MAX_VALUE, username, musicFolders);
        List<MediaFile> albums = mediaFileDao.getStarredAlbums(0, Integer.MAX_VALUE, username, musicFolders);
        List<MediaFile> files = mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, username, musicFolders);
        mediaFileService.populateStarredDate(artists, username);
        mediaFileService.populateStarredDate(albums, username);
        mediaFileService.populateStarredDate(files, username);

        List<MediaFile> songs = new ArrayList<>();
        List<MediaFile> videos = new ArrayList<>();
        for (MediaFile file : files) {
            (file.isVideo() ? videos : songs).add(file);
        }

        UserSettings userSettings = securityService.getUserSettings(username);
        return new ModelAndView("starred", "model",
                LegacyMap.of("user", user, "partyModeEnabled", userSettings.isPartyModeEnabled(), "visibility",
                        userSettings.getMainVisibility(), "player", playerService.getPlayer(request, response),
                        "coverArtSize", CoverArtScheme.MEDIUM.getSize(), "artists", artists, "albums", albums, "songs",
                        songs, "videos", videos, "viewAsList", viewSelector.isViewAsList(request, user.getUsername()),
                        "openDetailStar", userSettings.isOpenDetailStar(), "simpleDisplay",
                        userSettings.isSimpleDisplay()));
    }

}
