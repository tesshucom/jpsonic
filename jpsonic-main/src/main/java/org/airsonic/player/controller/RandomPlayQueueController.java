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

package org.airsonic.player.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the creating a random play queue.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/" + ViewName.ViewNameConstants.RANDOM_PLAYQUEUE)
public class RandomPlayQueueController {

    private static final Logger LOG = LoggerFactory.getLogger(RandomPlayQueueController.class);
    private static final String REQUEST_VALUE_ANY = "any";

    private final PlayerService playerService;
    private final MediaFileService mediaFileService;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final IndexManager indexManager;

    public RandomPlayQueueController(PlayerService playerService, MediaFileService mediaFileService,
            SecurityService securityService, SettingsService settingsService, IndexManager indexManager) {
        super();
        this.playerService = playerService;
        this.mediaFileService = mediaFileService;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.indexManager = indexManager;
    }

    @SuppressWarnings("PMD.NullAssignment")
    /*
     * (genre, lastPlayed, format) Intentional assignment in the case of receiving a param indicating no condition
     * value.
     */
    @PostMapping
    protected String handleRandomPlayQueue(ModelMap model, HttpServletRequest request, HttpServletResponse response,
            @RequestParam(Attributes.Request.NameConstants.SIZE) final Integer sizeParam,
            @RequestParam(value = Attributes.Request.NameConstants.GENRE, required = false) final String genreParam,
            @RequestParam(value = Attributes.Request.NameConstants.YEAR, required = false) String yearParam,
            @RequestParam(value = Attributes.Request.NameConstants.SONG_RATING, required = false) String songRating,
            @RequestParam(value = Attributes.Request.NameConstants.LAST_PLAYED_VALUE, required = false) String lastPlayedValue,
            @RequestParam(value = Attributes.Request.NameConstants.LAST_PLAYED_COMP, required = false) String lastPlayedComp,
            @RequestParam(value = Attributes.Request.NameConstants.ALBUM_RATING_VALUE, required = false) Integer albumRatingValue,
            @RequestParam(value = Attributes.Request.NameConstants.ALBUM_RATING_COMP, required = false) String albumRatingComp,
            @RequestParam(value = Attributes.Request.NameConstants.PLAY_COUNT_VALUE, required = false) Integer playCountValue,
            @RequestParam(value = Attributes.Request.NameConstants.PLAY_COUNT_COMP, required = false) String playCountComp,
            @RequestParam(value = Attributes.Request.NameConstants.FORMAT, required = false) final String formatParam,
            @RequestParam(value = Attributes.Request.NameConstants.AUTO_RANDOM, required = false) String autoRandom)
            throws ServletRequestBindingException {

        // Parse request parameters. All of these results are params to criteria.
        Integer size = sizeParam == null ? Integer.valueOf(24) : sizeParam;
        List<MusicFolder> musicFolders = getMusicFolders(request);
        LastPlayed lastPlayed = getLastPlayed(lastPlayedValue, lastPlayedComp);
        String genre = StringUtils.equalsIgnoreCase(REQUEST_VALUE_ANY, genreParam) ? null : genreParam;
        List<String> genres = parseGenre(genre);
        InceptionYear year = getInceptionYear(yearParam);
        AlbumRating albumRating = getAlbumRating(albumRatingValue, albumRatingComp);
        PlayCount playCount = getPlayCount(playCountValue, playCountComp);
        SongRating rating = getSongRating(songRating);
        String format = StringUtils.equalsIgnoreCase(formatParam, REQUEST_VALUE_ANY) ? null : formatParam;

        // Create instance of Criteria from parsed request parameters
        RandomSearchCriteria criteria = new RandomSearchCriteria(size, genres, year.getFromYear(), year.getToYear(),
                musicFolders, lastPlayed.getMinLastPlayedDate(), lastPlayed.getMaxLastPlayedDate(),
                albumRating.getMinAlbumRating(), albumRating.getMaxAlbumRating(), playCount.getMinPlayCount(),
                playCount.getMaxPlayCount(), rating.isDoesShowStarredSongs(), rating.isDoesShowUnstarredSongs(),
                format);

        User user = securityService.getCurrentUser(request);
        Player player = playerService.getPlayer(request, response);
        PlayQueue playQueue = player.getPlayQueue();
        // Do we add to the current playlist or do we replace it?
        boolean shouldAddToPlayList = request.getParameter(Attributes.Request.ADD_TO_PLAYLIST.value()) != null;

        playQueue.addFiles(shouldAddToPlayList, mediaFileService.getRandomSongs(criteria, user.getUsername()));
        if (autoRandom != null) {
            playQueue.setRandomSearchCriteria(criteria);
            playQueue.setInternetRadio(null);
        }

        // Render the 'reload' view to reload the play queue and the main page
        List<ReloadFrame> reloadFrames = new ArrayList<>();
        reloadFrames.add(new ReloadFrame("playQueue", ViewName.PLAY_QUEUE.value() + "?"));
        reloadFrames.add(new ReloadFrame("upper", ViewName.TOP.value() + "?"));
        Map<String, Object> map = LegacyMap.of("reloadFrames", reloadFrames);
        model.addAttribute("model", map);
        return "reload";
    }

    private InceptionYear getInceptionYear(String year) {
        Integer fromYear = null;
        Integer toYear = null;
        if (!StringUtils.equalsIgnoreCase(REQUEST_VALUE_ANY, year)) {
            String[] tmp = StringUtils.split(year);
            fromYear = Integer.parseInt(tmp[0]);
            toYear = Integer.parseInt(tmp[1]);
        }
        return new InceptionYear(fromYear, toYear);
    }

    private static class InceptionYear {
        private final Integer fromYear;
        private final Integer toYear;

        public InceptionYear(Integer fromYear, Integer toYear) {
            super();
            this.fromYear = fromYear;
            this.toYear = toYear;
        }

        public Integer getFromYear() {
            return fromYear;
        }

        public Integer getToYear() {
            return toYear;
        }
    }

    @SuppressWarnings("PMD.NullAssignment")
    /*
     * (lastPlayed) Intentional assignment in the case of receiving a param indicating no condition value.
     */
    private LastPlayed getLastPlayed(String lastPlayedValue, String lastPlayedComp) {
        Date minLastPlayedDate = null;
        Date maxLastPlayedDate = null;
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
                maxLastPlayedDate = lastPlayed.getTime();
                break;
            case "gt":
                minLastPlayedDate = lastPlayed.getTime();
                break;
            default:
                // none
                break;
            }
        }
        return new LastPlayed(minLastPlayedDate, maxLastPlayedDate);
    }

    private static class LastPlayed {
        private final Date minLastPlayedDate;
        private final Date maxLastPlayedDate;

        public LastPlayed(Date minLastPlayedDate, Date maxLastPlayedDate) {
            super();
            this.minLastPlayedDate = minLastPlayedDate;
            this.maxLastPlayedDate = maxLastPlayedDate;
        }

        public Date getMinLastPlayedDate() {
            return minLastPlayedDate;
        }

        public Date getMaxLastPlayedDate() {
            return maxLastPlayedDate;
        }
    }

    private AlbumRating getAlbumRating(Integer albumRatingValue, String albumRatingComp) {
        Integer minAlbumRating = null;
        Integer maxAlbumRating = null;
        if (albumRatingValue != null) {
            switch (albumRatingComp) { // nullable
            case "lt":
                maxAlbumRating = albumRatingValue - 1;
                break;
            case "gt":
                minAlbumRating = albumRatingValue + 1;
                break;
            case "le":
                maxAlbumRating = albumRatingValue;
                break;
            case "ge":
                minAlbumRating = albumRatingValue;
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
        return new AlbumRating(minAlbumRating, maxAlbumRating);
    }

    private static class AlbumRating {
        private final Integer minAlbumRating;
        private final Integer maxAlbumRating;

        public AlbumRating(Integer minAlbumRating, Integer maxAlbumRating) {
            super();
            this.minAlbumRating = minAlbumRating;
            this.maxAlbumRating = maxAlbumRating;
        }

        public Integer getMinAlbumRating() {
            return minAlbumRating;
        }

        public Integer getMaxAlbumRating() {
            return maxAlbumRating;
        }
    }

    private PlayCount getPlayCount(Integer playCountValue, String playCountComp) {
        Integer minPlayCount = null;
        Integer maxPlayCount = null;
        if (playCountValue != null) {
            switch (playCountComp) { // nullable
            case "lt":
                maxPlayCount = playCountValue - 1;
                break;
            case "gt":
                minPlayCount = playCountValue + 1;
                break;
            case "le":
                maxPlayCount = playCountValue;
                break;
            case "ge":
                minPlayCount = playCountValue;
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
        return new PlayCount(minPlayCount, maxPlayCount);
    }

    private static class PlayCount {
        private final Integer minPlayCount;
        private final Integer maxPlayCount;

        public PlayCount(Integer minPlayCount, Integer maxPlayCount) {
            super();
            this.minPlayCount = minPlayCount;
            this.maxPlayCount = maxPlayCount;
        }

        public Integer getMinPlayCount() {
            return minPlayCount;
        }

        public Integer getMaxPlayCount() {
            return maxPlayCount;
        }

    }

    private SongRating getSongRating(String songRating) {
        boolean doesShowStarredSongs = false;
        boolean doesShowUnstarredSongs = false;
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
        return new SongRating(doesShowStarredSongs, doesShowUnstarredSongs);
    }

    private static class SongRating {
        private final boolean doesShowStarredSongs;
        private final boolean doesShowUnstarredSongs;

        public SongRating(boolean doesShowStarredSongs, boolean doesShowUnstarredSongs) {
            super();
            this.doesShowStarredSongs = doesShowStarredSongs;
            this.doesShowUnstarredSongs = doesShowUnstarredSongs;
        }

        public boolean isDoesShowStarredSongs() {
            return doesShowStarredSongs;
        }

        public boolean isDoesShowUnstarredSongs() {
            return doesShowUnstarredSongs;
        }
    }

    private List<String> parseGenre(String genre) {
        List<String> genres = null;
        if (null != genre) {
            List<String> preAnalyzeds = indexManager.toPreAnalyzedGenres(Arrays.asList(genre));
            if (0 == preAnalyzeds.size()) {
                // #267 Invalid search for genre containing specific string
                if (LOG.isWarnEnabled()) {
                    LOG.warn(
                            "Could not find the specified genre. A forbidden string such as \"double quotes\" may be used.");
                }
            } else {
                genres = preAnalyzeds;
            }
        }
        return genres;
    }

    @SuppressWarnings("PMD.NullAssignment") // (selectedMusicFolderId) Intentional assignment in the case of receiving a
                                            // param indicating no condition value.
    private List<MusicFolder> getMusicFolders(HttpServletRequest request) throws ServletRequestBindingException {
        String username = securityService.getCurrentUsername(request);
        Integer selectedMusicFolderId = ServletRequestUtils.getRequiredIntParameter(request,
                Attributes.Request.MUSIC_FOLDER_ID.value());
        if (selectedMusicFolderId == -1) {
            selectedMusicFolderId = null;
        }
        return settingsService.getMusicFoldersForUser(username, selectedMusicFolderId);
    }
}
