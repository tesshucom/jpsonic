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

import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the home page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/home")
public class HomeController {

    private static final int LIST_SIZE = 40;

    private final SettingsService settingsService;
    private final MediaScannerService mediaScannerService;
    private final RatingService ratingService;
    private final SecurityService securityService;
    private final MediaFileService mediaFileService;
    private final SearchService searchService;
    private final MusicIndexService musicIndexService;

    public HomeController(SettingsService settingsService, MediaScannerService mediaScannerService,
            RatingService ratingService, SecurityService securityService, MediaFileService mediaFileService,
            SearchService searchService, MusicIndexService musicIndexService) {
        super();
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
        this.ratingService = ratingService;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
        this.musicIndexService = musicIndexService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws ServletRequestBindingException {

        User user = securityService.getCurrentUser(request);
        if (user.isAdminRole() && settingsService.isGettingStartedEnabled()) {
            return new ModelAndView(new RedirectView(ViewName.GETTING_STARTED.value()));
        }
        int listOffset = getIntParameter(request, Attributes.Request.LIST_OFFSET.value(), 0);
        AlbumListType listType = AlbumListType
                .fromId(getStringParameter(request, Attributes.Request.LIST_TYPE.value()));
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        if (listType == null) {
            listType = userSettings.getDefaultAlbumList();
        }

        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(user.getUsername());
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername(),
                selectedMusicFolder == null ? null : selectedMusicFolder.getId());

        Map<String, Object> map = LegacyMap.of();
        List<Album> albums = Collections.emptyList();
        switch (listType) {
        case HIGHEST:
            albums = getHighestRated(listOffset, LIST_SIZE, musicFolders);
            break;
        case FREQUENT:
            albums = getMostFrequent(listOffset, LIST_SIZE, musicFolders);
            break;
        case RECENT:
            albums = getMostRecent(listOffset, LIST_SIZE, musicFolders);
            break;
        case NEWEST:
            albums = getNewest(listOffset, LIST_SIZE, musicFolders);
            break;
        case STARRED:
            albums = getStarred(listOffset, LIST_SIZE, user.getUsername(), musicFolders);
            break;
        case RANDOM:
            albums = getRandom(LIST_SIZE, musicFolders);
            break;
        case ALPHABETICAL:
            albums = getAlphabetical(listOffset, LIST_SIZE, true, musicFolders);
            break;
        case DECADE:
            List<Integer> decades = createDecades();
            map.put("decades", decades);
            int decade = getIntParameter(request, Attributes.Request.DECADE.value(), decades.get(0));
            map.put("decade", decade);
            albums = getByYear(listOffset, LIST_SIZE, decade, decade + 9, musicFolders);
            break;
        case GENRE:
            List<Genre> genres = searchService.getGenres(true);
            map.put("genres", genres);
            if (!genres.isEmpty()) {
                String genre = getStringParameter(request, Attributes.Request.GENRE.value(), genres.get(0).getName());
                map.put("genre", genre);
                albums = getByGenre(listOffset, LIST_SIZE, genre, musicFolders);
            }
            break;
        case INDEX:
            MusicFolderContent musicFolderContent = musicIndexService.getMusicFolderContent(musicFolders, false);
            map.put("indexedArtists", musicFolderContent.getIndexedArtists());
            map.put("singleSongs", musicFolderContent.getSingleSongs());
            map.put("indexes", musicFolderContent.getIndexedArtists().keySet());
            map.put("isOpenDetailIndex", userSettings.isOpenDetailIndex());
            map.put("assignAccesskeyToNumber", userSettings.isAssignAccesskeyToNumber());
            break;
        default:
            break;
        }

        map.put("user", user);
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("downloadRole", user.isDownloadRole());
        map.put("showDownload", userSettings.isShowDownload());

        map.put("albums", albums);
        map.put("welcomeTitle", settingsService.getWelcomeTitle());
        map.put("welcomeSubtitle", settingsService.getWelcomeSubtitle());
        map.put("welcomeMessage", settingsService.getWelcomeMessage());
        map.put("isIndexBeingCreated", mediaScannerService.isScanning());
        map.put("musicFoldersExist", !settingsService.getAllMusicFolders().isEmpty());
        map.put("listType", listType.getId());
        map.put("listSize", LIST_SIZE);
        map.put("coverArtSize", CoverArtScheme.MEDIUM.getSize());
        map.put("listOffset", listOffset);
        map.put("musicFolder", selectedMusicFolder);
        map.put("showRate", userSettings.isShowRate());
        return new ModelAndView("home", "model", map);
    }

    private List<Album> getHighestRated(int offset, int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile mediaFile : ratingService.getHighestRatedAlbums(offset, count, musicFolders)) {
            Album album = createAlbum(mediaFile);
            album.setRating((int) Math.round(ratingService.getAverageRating(mediaFile) * 10.0D));
            result.add(album);
        }
        return result;
    }

    private List<Album> getMostFrequent(int offset, int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile mediaFile : mediaFileService.getMostFrequentlyPlayedAlbums(offset, count, musicFolders)) {
            Album album = createAlbum(mediaFile);
            album.setPlayCount(mediaFile.getPlayCount());
            result.add(album);
        }
        return result;
    }

    private List<Album> getMostRecent(int offset, int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile mediaFile : mediaFileService.getMostRecentlyPlayedAlbums(offset, count, musicFolders)) {
            Album album = createAlbum(mediaFile);
            album.setLastPlayed(mediaFile.getLastPlayed());
            result.add(album);
        }
        return result;
    }

    private List<Album> getNewest(int offset, int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : mediaFileService.getNewestAlbums(offset, count, musicFolders)) {
            Album album = createAlbum(file);
            Date created = file.getCreated();
            if (created == null) {
                created = file.getChanged();
            }
            album.setCreated(created);
            result.add(album);
        }
        return result;
    }

    private List<Album> getStarred(int offset, int count, String username, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : mediaFileService.getStarredAlbums(offset, count, username, musicFolders)) {
            result.add(createAlbum(file));
        }
        return result;
    }

    private List<Album> getRandom(int count, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : searchService.getRandomAlbums(count, musicFolders)) {
            result.add(createAlbum(file));
        }
        return result;
    }

    private List<Album> getAlphabetical(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : mediaFileService.getAlphabeticalAlbums(offset, count, byArtist, musicFolders)) {
            result.add(createAlbum(file));
        }
        return result;
    }

    private List<Album> getByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : mediaFileService.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders)) {
            Album album = createAlbum(file);
            album.setYear(file.getYear());
            result.add(album);
        }
        return result;
    }

    private List<Integer> createDecades() {
        List<Integer> result = new ArrayList<>();
        int decade = Calendar.getInstance().get(Calendar.YEAR) / 10;
        for (int i = 0; i < 10; i++) {
            result.add((decade - i) * 10);
        }
        return result;
    }

    private List<Album> getByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        List<Album> result = new ArrayList<>();
        for (MediaFile file : searchService.getAlbumsByGenres(genre, offset, count, musicFolders)) {
            result.add(createAlbum(file));
        }
        return result;
    }

    private Album createAlbum(MediaFile file) {
        Album album = new Album();
        album.setId(file.getId());
        album.setPath(file.getPath());
        album.setArtist(file.getArtist());
        album.setAlbumTitle(file.getAlbumName());
        album.setCoverArtPath(file.getCoverArtPath());
        return album;
    }

    /**
     * Contains info for a single album.
     */
    public static class Album {
        private String path;
        private String coverArtPath;
        private String artist;
        private String albumTitle;
        private Date created;
        private Date lastPlayed;
        private Integer playCount;
        private Integer rating;
        private int id;
        private Integer year;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCoverArtPath() {
            return coverArtPath;
        }

        public void setCoverArtPath(String coverArtPath) {
            this.coverArtPath = coverArtPath;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbumTitle() {
            return albumTitle;
        }

        public void setAlbumTitle(String albumTitle) {
            this.albumTitle = albumTitle;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public Date getLastPlayed() {
            return lastPlayed;
        }

        public void setLastPlayed(Date lastPlayed) {
            this.lastPlayed = lastPlayed;
        }

        public Integer getPlayCount() {
            return playCount;
        }

        public void setPlayCount(Integer playCount) {
            this.playCount = playCount;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public Integer getYear() {
            return year;
        }
    }
}
