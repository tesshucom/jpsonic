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
package org.airsonic.player.controller;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.IndexManager;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Controller for the creating a random play queue.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/randomPlayQueue.view")
public class RandomPlayQueueController {

    private static final Logger LOG = LoggerFactory.getLogger(RandomPlayQueueController.class);
    
    private static final String REQUEST_VALUE_ANY = "any";

    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private IndexManager indexManager;

    @PostMapping
    protected String handleRandomPlayQueue(
            ModelMap model,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("size") final Integer sizeParam,
            @RequestParam(value = "genre", required = false) final String genreParam,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "songRating", required = false) String songRating,
            @RequestParam(value = "lastPlayedValue", required = false) String lastPlayedValue,
            @RequestParam(value = "lastPlayedComp", required = false) String lastPlayedComp,
            @RequestParam(value = "albumRatingValue", required = false) Integer albumRatingValue,
            @RequestParam(value = "albumRatingComp", required = false) String albumRatingComp,
            @RequestParam(value = "playCountValue", required = false) Integer playCountValue,
            @RequestParam(value = "playCountComp", required = false) String playCountComp,
            @RequestParam(value = "format", required = false) final String formatParam,
            @RequestParam(value = "autoRandom", required = false) String autoRandom
    ) throws Exception {

        Integer fromYear = null;
        Integer toYear = null;
        Integer minAlbumRating = null;
        Integer maxAlbumRating = null;
        Integer minPlayCount = null;
        Integer maxPlayCount = null;
        Date minLastPlayedDate = null;
        Date maxLastPlayedDate = null;
        boolean doesShowStarredSongs = false;
        boolean doesShowUnstarredSongs = false;

        Integer size = sizeParam;
        String genre = genreParam;
        String format = formatParam;
        
        if (size == null) {
            size = 24;
        }

        // Handle the genre filter
        if (StringUtils.equalsIgnoreCase(REQUEST_VALUE_ANY, genre)) {
            genre = null;
        }

        // Handle the release year filter
        if (!StringUtils.equalsIgnoreCase(REQUEST_VALUE_ANY, year)) {
            String[] tmp = StringUtils.split(year);
            fromYear = Integer.parseInt(tmp[0]);
            toYear = Integer.parseInt(tmp[1]);
        }

        // Handle the song rating filter
        if (StringUtils.equalsIgnoreCase(REQUEST_VALUE_ANY, songRating)) {
            doesShowStarredSongs = true;
            doesShowUnstarredSongs = true;
        } else if (StringUtils.equalsIgnoreCase("starred", songRating)) {
            doesShowStarredSongs = true;
            doesShowUnstarredSongs = false;
        } else if (StringUtils.equalsIgnoreCase("unstarred", songRating)) {
            doesShowStarredSongs = false;
            doesShowUnstarredSongs = true;
        }

        // Handle the last played date filter
        Calendar lastPlayed = Calendar.getInstance();
        lastPlayed.setTime(new Date());
        switch (lastPlayedValue) { // nullable
            case REQUEST_VALUE_ANY:
                lastPlayed = null;
                break;
            case "1day":
                lastPlayed.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case "1week":
                lastPlayed.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case "1month":
                lastPlayed.add(Calendar.MONTH, -1);
                break;
            case "3months":
                lastPlayed.add(Calendar.MONTH, -3);
                break;
            case "6months":
                lastPlayed.add(Calendar.MONTH, -6);
                break;
            case "1year":
                lastPlayed.add(Calendar.YEAR, -1);
                break;
            default:
                // none
                break;
        }
        if (lastPlayed != null) {
            switch (lastPlayedComp) { // nullable
                case "lt":
                    minLastPlayedDate = null;
                    maxLastPlayedDate = lastPlayed.getTime();
                    break;
                case "gt":
                    minLastPlayedDate = lastPlayed.getTime();
                    maxLastPlayedDate = null;
                    break;
                default:
                    // none
                    break;
            }
        }

        // Handle the album rating filter
        if (albumRatingValue != null) {
            switch (albumRatingComp) { // nullable
                case "lt":
                    minAlbumRating = null;
                    maxAlbumRating = albumRatingValue - 1;
                    break;
                case "gt":
                    minAlbumRating = albumRatingValue + 1;
                    maxAlbumRating = null;
                    break;
                case "le":
                    minAlbumRating = null;
                    maxAlbumRating = albumRatingValue;
                    break;
                case "ge":
                    minAlbumRating = albumRatingValue;
                    maxAlbumRating = null;
                    break;
                case "eq":
                    minAlbumRating = albumRatingValue;
                    maxAlbumRating = albumRatingValue;
                    break;
                default:
                    // none
                    break;
            }
        }

        // Handle the play count filter
        if (playCountValue != null) {
            switch (playCountComp) { // nullable
                case "lt":
                    minPlayCount = null;
                    maxPlayCount = playCountValue - 1;
                    break;
                case "gt":
                    minPlayCount = playCountValue + 1;
                    maxPlayCount = null;
                    break;
                case "le":
                    minPlayCount = null;
                    maxPlayCount = playCountValue;
                    break;
                case "ge":
                    minPlayCount = playCountValue;
                    maxPlayCount = null;
                    break;
                case "eq":
                    minPlayCount = playCountValue;
                    maxPlayCount = playCountValue;
                    break;
                default:
                    // none
                    break;
            }
        }

        // Handle the format filter
        if (StringUtils.equalsIgnoreCase(format, REQUEST_VALUE_ANY)) format = null;

        // Handle the music folder filter
        List<MusicFolder> musicFolders = getMusicFolders(request);

        // Do we add to the current playlist or do we replace it?
        boolean shouldAddToPlayList = request.getParameter("addToPlaylist") != null;

        List<String> genres = null;
        if (null != genre) {
            List<String> preAnalyzeds = indexManager.toPreAnalyzedGenres(Arrays.asList(genre));
            if (0 != preAnalyzeds.size()) {
                genres = preAnalyzeds;
            } else {
                // #267 Invalid search for genre containing specific string
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Could not find the specified genre. A forbidden string such as \"double quotes\" may be used.");
                }
            }
        }

        // Search the database using these criteria
        RandomSearchCriteria criteria = new RandomSearchCriteria(
                size,
                genres,
                fromYear,
                toYear,
                musicFolders,
                minLastPlayedDate,
                maxLastPlayedDate,
                minAlbumRating,
                maxAlbumRating,
                minPlayCount,
                maxPlayCount,
                doesShowStarredSongs,
                doesShowUnstarredSongs,
                format
        );
        User user = securityService.getCurrentUser(request);
        Player player = playerService.getPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        playQueue.addFiles(shouldAddToPlayList, mediaFileService.getRandomSongs(criteria, user.getUsername()));

        if (autoRandom != null) {
            playQueue.setRandomSearchCriteria(criteria);
            playQueue.setInternetRadio(null);
        }

        // Render the 'reload' view to reload the play queue and the main page
        List<ReloadFrame> reloadFrames = new ArrayList<>();
        reloadFrames.add(new ReloadFrame("playQueue", "playQueue.view?"));
        reloadFrames.add(new ReloadFrame("upper", "top.view?"));
        Map<String, Object> map = LegacyMap.of("reloadFrames", reloadFrames);
        model.addAttribute("model", map);
        return "reload";
    }

    private List<MusicFolder> getMusicFolders(HttpServletRequest request) throws ServletRequestBindingException {
        String username = securityService.getCurrentUsername(request);
        Integer selectedMusicFolderId = ServletRequestUtils.getRequiredIntParameter(request, "musicFolderId");
        if (selectedMusicFolderId == -1) {
            selectedMusicFolderId = null;
        }
        return settingsService.getMusicFoldersForUser(username, selectedMusicFolderId);
    }
}
