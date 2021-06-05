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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Share;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for sharing music on Twitter, Facebook etc.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/createShare")
public class ShareManagementController {

    private final MediaFileService mediaFileService;
    private final ShareService shareService;
    private final PlayerService playerService;
    private final PlaylistService playlistService;
    private final SecurityService securityService;

    public ShareManagementController(MediaFileService mediaFileService, ShareService shareService,
            PlayerService playerService, PlaylistService playlistService, SecurityService securityService) {
        super();
        this.mediaFileService = mediaFileService;
        this.shareService = shareService;
        this.playerService = playerService;
        this.playlistService = playlistService;
        this.securityService = securityService;
    }

    @GetMapping
    public ModelAndView createShare(HttpServletRequest request) throws ServletRequestBindingException {

        List<MediaFile> files = getMediaFiles(request);
        MediaFile dir = null;
        if (!files.isEmpty()) {
            dir = files.get(0);
            if (!dir.isAlbum()) {
                dir = mediaFileService.getParentOf(dir);
            }
        }

        Share share = shareService.createShare(request, files);
        String description = getDescription(request);
        if (description != null) {
            share.setDescription(description);
            shareService.updateShare(share);
        }

        return new ModelAndView("createShare", "model", LegacyMap.of("dir", dir, "user",
                securityService.getCurrentUser(request), "playUrl", shareService.getShareUrl(request, share)));
    }

    private String getDescription(HttpServletRequest request) throws ServletRequestBindingException {
        Integer playlistId = ServletRequestUtils.getIntParameter(request, Attributes.Request.PLAYLIST.value());
        return playlistId == null ? null : playlistService.getPlaylist(playlistId).getName();
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    private List<MediaFile> getMediaFiles(HttpServletRequest request) throws ServletRequestBindingException {
        Integer id = ServletRequestUtils.getIntParameter(request, Attributes.Request.ID.value());
        Integer playerId = ServletRequestUtils.getIntParameter(request, Attributes.Request.PLAYER.value());
        Integer playlistId = ServletRequestUtils.getIntParameter(request, Attributes.Request.PLAYLIST.value());

        List<MediaFile> result = new ArrayList<>();
        if (id != null) {
            MediaFile album = mediaFileService.getMediaFile(id);
            int[] indexes = ServletRequestUtils.getIntParameters(request, Attributes.Request.I.value());
            if (indexes.length == 0) {
                return Arrays.asList(album);
            }
            List<MediaFile> children = mediaFileService.getChildrenOf(album, true, false, true);
            for (int index : indexes) {
                result.add(children.get(index));
            }
        } else if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            PlayQueue playQueue = player.getPlayQueue();
            result = playQueue.getFiles();
        } else if (playlistId != null) {
            result = playlistService.getFilesInPlaylist(playlistId);
        }

        return result;
    }

}
