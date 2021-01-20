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

import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.LinkedList;
import java.util.List;

/**
 * Controller for changing cover art.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/changeCoverArt")
public class ChangeCoverArtController {

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        String artist = request.getParameter(Attributes.Request.ARTIST.value());
        String album = request.getParameter(Attributes.Request.ALBUM.value());
        MediaFile dir = mediaFileService.getMediaFile(id);

        if (StringUtils.isBlank(artist)) {
            artist = dir.getArtist();
        }
        if (StringUtils.isBlank(album)) {
            album = dir.getAlbumName();
        }

        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);
        return new ModelAndView("changeCoverArt", "model", LegacyMap.of(
                "id", id,
                "artist", artist,
                "album", album,
                "ancestors", getAncestors(dir),
                "breadcrumbIndex", userSettings.isBreadcrumbIndex(),
                "dir", dir,
                "selectedMusicFolder", settingsService.getSelectedMusicFolder(username)));
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
        result.add(dir);
        return result;
    }

}
