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
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/exportPlaylist")
public class ExportPlayListController {

    private final PlaylistService playlistService;
    private final SecurityService securityService;

    public ExportPlayListController(PlaylistService playlistService, SecurityService securityService) {
        super();
        this.playlistService = playlistService;
        this.securityService = securityService;
    }

    @GetMapping
    public ModelAndView exportPlaylist(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException, ExecutionException {

        int id = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        Playlist playlist = playlistService.getPlaylist(id);
        if (!playlistService.isReadAllowed(playlist, securityService.getCurrentUsername(request))) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
            return null;
        }

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + StringUtil.fileSystemSafe(playlist.getName()) + ".m3u8\"");
        try {
            playlistService.exportPlaylist(id, response.getOutputStream());
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

}
