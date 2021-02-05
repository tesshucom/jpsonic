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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewAsListSelector;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.RatingService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/main")
public class MainController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private JpsonicComparators jpsonicComparator;

    @Autowired
    private RatingService ratingService;
    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private ViewAsListSelector viewSelector;

    @SuppressWarnings("PMD.EmptyCatchBlock") // Triage in #824
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(name = Attributes.Request.NameConstants.SHOW_ALL, required = false) Boolean showAll)
            throws IOException {

        List<MediaFile> mediaFiles = getMediaFiles(request);
        if (mediaFiles.isEmpty()) {
            return new ModelAndView(new RedirectView(ViewName.NOTFOUND.value()));
        }

        MediaFile dir = mediaFiles.get(0);
        if (dir.isFile()) {
            dir = mediaFileService.getParentOf(dir);
        }

        // Redirect if root directory.
        if (mediaFileService.isRoot(dir)) {
            return new ModelAndView(new RedirectView(ViewName.HOME.value() + "?"));
        }

        String username = securityService.getCurrentUsername(request);
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            return new ModelAndView(new RedirectView(ViewName.ACCESS_DENIED.value()));
        }

        UserSettings userSettings = settingsService.getUserSettings(username);

        List<MediaFile> children = mediaFiles.size() == 1 ? mediaFileService.getChildrenOf(dir, true, true, true)
                : getMultiFolderChildren(mediaFiles);

        int userPaginationPreference = userSettings.getPaginationSize();

        boolean isShowAll = userPaginationPreference <= 0 || null != showAll && showAll;

        mediaFileService.populateStarredDate(dir, username);
        mediaFileService.populateStarredDate(children, username);

        Map<String, Object> map = LegacyMap.of();
        map.put("dir", dir);

        List<MediaFile> files = children.stream().filter(f -> f.isFile()).collect(Collectors.toList());
        map.put("files", files);

        List<MediaFile> subDirs = children.stream().filter(f -> !f.isFile()).collect(Collectors.toList());
        map.put("subDirs", subDirs);

        map.put("ancestors", getAncestors(dir));
        map.put("coverArtSizeMedium", CoverArtScheme.MEDIUM.getSize());
        map.put("coverArtSizeLarge", CoverArtScheme.LARGE.getSize());
        map.put("user", securityService.getCurrentUser(request));
        map.put("visibility", userSettings.getMainVisibility());
        map.put("showAlbumYear", settingsService.isSortAlbumsByYear());
        map.put("showArtistInfo", userSettings.isShowArtistInfoEnabled());
        map.put("showTopSongs", userSettings.isShowTopSongs());
        map.put("showSimilar", userSettings.isShowSimilar());
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("brand", settingsService.getBrand());
        map.put("viewAsList", viewSelector.isViewAsList(request, userSettings.getUsername()));
        map.put("showDownload", userSettings.isShowDownload());
        map.put("showTag", userSettings.isShowTag());
        map.put("showChangeCoverArt", userSettings.isShowChangeCoverArt());
        map.put("showComment", userSettings.isShowComment());
        map.put("showShare", userSettings.isShowShare());
        map.put("showRate", userSettings.isShowRate());
        map.put("showAlbumSearch", userSettings.isShowAlbumSearch());
        map.put("showLastPlay", userSettings.isShowLastPlay());
        map.put("showSibling", userSettings.isShowSibling());
        map.put("showAlbumActions", userSettings.isShowAlbumActions());
        map.put("breadcrumbIndex", userSettings.isBreadcrumbIndex());
        map.put("simpleDisplay", userSettings.isSimpleDisplay());
        map.put("selectedMusicFolder", settingsService.getSelectedMusicFolder(username));

        boolean thereIsMoreSiblingAlbums = false;
        if (dir.isAlbum()) {
            if (userSettings.isShowSibling()) {
                List<MediaFile> siblingAlbums = getSiblingAlbums(dir);
                thereIsMoreSiblingAlbums = trimToSize(isShowAll, siblingAlbums, userPaginationPreference);
                map.put("siblingAlbums", siblingAlbums);
            }
            map.put("artist", guessArtist(children));
            map.put("album", guessAlbum(children));
            map.put("musicBrainzReleaseId", guessMusicBrainzReleaseId(children));
        }

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !mediaFileService.isRoot(parent));
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }

        map.put("thereIsMore", getThereIsMore(thereIsMoreSiblingAlbums, isShowAll, subDirs, userPaginationPreference));
        map.put("userRating", getUserRating(username, dir));
        map.put("averageRating", getAverageRating(dir));
        map.put("starred", mediaFileService.getMediaFileStarredDate(dir.getId(), username) != null);
        map.put("useRadio", settingsService.isUseRadio());

        String view = getTargetView(dir, children);
        return new ModelAndView(view, "model", map);
    }

    private boolean getThereIsMore(boolean thereIsMoreSiblingAlbums, boolean isShowAll, List<MediaFile> subDirs,
            int userPaginationPreference) {
        boolean thereIsMoreSubDirs = trimToSize(isShowAll, subDirs, userPaginationPreference);
        return (thereIsMoreSubDirs || thereIsMoreSiblingAlbums) && !isShowAll;
    }

    private Integer getUserRating(String username, MediaFile dir) {
        Integer userRating = ratingService.getRatingForUser(username, dir);
        if (userRating == null) {
            userRating = 0;
        }
        return 10 * userRating;
    }

    private long getAverageRating(MediaFile dir) {
        Double averageRating = ratingService.getAverageRating(dir);
        if (averageRating == null) {
            averageRating = 0.0D;
        }
        return Math.round(10.0D * averageRating);
    }

    private String getTargetView(MediaFile dir, List<MediaFile> children) {
        String view;
        if (isVideoOnly(children)) {
            view = "videoMain";
        } else if (dir.isAlbum()) {
            view = "albumMain";
        } else {
            view = "artistMain";
        }
        return view;
    }

    private <T> boolean trimToSize(Boolean showAll, List<T> list, int userPaginationPreference) {
        boolean trimmed = false;
        if (!BooleanUtils.isTrue(showAll) && list.size() > userPaginationPreference) {
            trimmed = true;
            list.subList(userPaginationPreference, list.size()).clear();
        }
        return trimmed;
    }

    private boolean isVideoOnly(List<MediaFile> children) {
        boolean videoFound = false;
        for (MediaFile child : children) {
            if (child.isAudio()) {
                return false;
            }
            if (child.isVideo()) {
                videoFound = true;
            }
        }
        return videoFound;
    }

    private List<MediaFile> getMediaFiles(HttpServletRequest request) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        for (String path : ServletRequestUtils.getStringParameters(request, "path")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        for (int id : ServletRequestUtils.getIntParameters(request, "id")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        return mediaFiles;
    }

    private String guessArtist(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getArtist();
            }
        }
        return null;
    }

    private String guessAlbum(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getAlbumName();
            }
        }
        return null;
    }

    private String guessMusicBrainzReleaseId(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getMusicBrainzReleaseId() != null) {
                return child.getMusicBrainzReleaseId();
            }
        }
        return null;
    }

    public List<MediaFile> getMultiFolderChildren(List<MediaFile> mediaFiles) throws IOException {
        SortedSet<MediaFile> result = new TreeSet<>(jpsonicComparator.mediaFileOrder(null));
        for (MediaFile mediaFile : mediaFiles) {
            MediaFile m = mediaFile;
            if (m.isFile()) {
                m = mediaFileService.getParentOf(m);
            }
            result.addAll(mediaFileService.getChildrenOf(m, true, true, true));
        }
        return new ArrayList<>(result);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock") // Triage in #824
    private List<MediaFile> getAncestors(MediaFile dir) {
        LinkedList<MediaFile> result = new LinkedList<>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null && !mediaFileService.isRoot(parent)) {
                result.addFirst(parent);
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private List<MediaFile> getSiblingAlbums(MediaFile dir) {
        List<MediaFile> result = new ArrayList<>();

        MediaFile parent = mediaFileService.getParentOf(dir);
        if (!mediaFileService.isRoot(parent)) {
            List<MediaFile> siblings = mediaFileService.getChildrenOf(parent, false, true, true);
            result.addAll(siblings.stream().filter(sibling -> sibling.isAlbum() && !sibling.equals(dir))
                    .collect(Collectors.toList()));
        }
        return result;
    }

}
