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

import java.util.List;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for showing what's currently playing.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/nowPlaying.view")
public class NowPlayingController {

    private final PlayerService playerService;
    private final StatusService statusService;
    private final MediaFileService mediaFileService;
    private final SecurityService securityService;

    public NowPlayingController(PlayerService playerService, StatusService statusService,
            MediaFileService mediaFileService, SecurityService securityService) {
        super();
        this.playerService = playerService;
        this.statusService = statusService;
        this.mediaFileService = mediaFileService;
        this.securityService = securityService;
    }

    @GetMapping
    protected ModelAndView get(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        Player player = playerService.getPlayer(request, response);
        MediaFile current = getNowPlayingFile(player);
        MediaFile dir = current == null ? null : mediaFileService.getParentOf(current);

        String url;
        if (dir != null && !mediaFileService.isRoot(dir)) {
            url = ViewName.MAIN.value() + "?" + Attributes.Request.ID.value() + "=" + dir.getId();
        } else {
            url = ViewName.HOME.value();
        }

        return new ModelAndView(new RedirectView(url));
    }

    @Nullable
    MediaFile getNowPlayingFile(@NonNull Player player) {
        List<TransferStatus> statuses = statusService.getStreamStatusesForPlayer(player);
        return statuses.isEmpty() || !securityService.isReadAllowed(statuses.get(0).toPath()) ? null
                : mediaFileService.getMediaFile(statuses.get(0).toPath());
    }
}
