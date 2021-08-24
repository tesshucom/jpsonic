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

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for changing cover art.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/changeCoverArt", "/changeCoverArt.view" })
public class ChangeCoverArtController {

    private final SecurityService securityService;
    private final MediaFileService mediaFileService;

    public ChangeCoverArtController(SecurityService securityService, MediaFileService mediaFileService) {
        super();
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

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
        UserSettings userSettings = securityService.getUserSettings(username);
        return new ModelAndView("changeCoverArt", "model",
                LegacyMap.of("id", id, "artist", artist, "album", album, "ancestors", getAncestors(dir),
                        "breadcrumbIndex", userSettings.isBreadcrumbIndex(), "dir", dir, "selectedMusicFolder",
                        securityService.getSelectedMusicFolder(username)));
    }

    private List<MediaFile> getAncestors(MediaFile dir) {
        LinkedList<MediaFile> result = new LinkedList<>();
        if (securityService.isInPodcastFolder(dir.getFile())) {
            // For podcasts, don't use ancestors
            return result;
        }

        MediaFile parent = mediaFileService.getParentOf(dir);
        while (parent != null && !mediaFileService.isRoot(parent)) {
            result.addFirst(parent);
            parent = mediaFileService.getParentOf(parent);
        }
        result.add(dir);
        return result;
    }
}
