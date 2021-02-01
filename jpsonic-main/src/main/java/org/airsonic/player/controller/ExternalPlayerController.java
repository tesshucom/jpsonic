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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileWithUrlInfo;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.Share;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.ShareService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for the page used to play shared music (Twitter, Facebook etc).
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/ext/share/**")
public class ExternalPlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPlayerController.class);

    private static final String MAX_BIT_RATE_VALUE = "1200";
    private static final String MAX_SIZE_VALUE = "500";

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private ShareService shareService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private JWTSecurityService jwtSecurityService;

    @SuppressWarnings("PMD.NullAssignment") // (share) Intentional allocation to register null
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String shareName = ControllerUtils.extractMatched(request);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Share name is {}", shareName);
        }

        if (StringUtils.isBlank(shareName)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not find share with shareName " + shareName);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        Share share = shareService.getShareByName(shareName);

        if (share != null && share.getExpires() != null && share.getExpires().before(new Date())) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Share " + shareName + " is expired");
            }
            share = null;
        }

        if (share != null) {
            share.setLastVisited(new Date());
            share.setVisitCount(share.getVisitCount() + 1);
            shareService.updateShare(share);
        }

        Player player = playerService.getGuestPlayer(request);

        return new ModelAndView("externalPlayer", "model",
                LegacyMap.of("share", share, "songs", getSongs(request, share, player)));
    }

    private List<MediaFileWithUrlInfo> getSongs(HttpServletRequest request, Share share, Player player) {
        Date expires = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JWTAuthenticationToken) {
            DecodedJWT token = jwtSecurityService.verify((String) authentication.getCredentials());
            expires = token.getExpiresAt();
        }
        Date finalExpires = expires;

        List<MediaFileWithUrlInfo> result = new ArrayList<>();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(player.getUsername());

        if (share != null) {
            for (MediaFile file : shareService.getSharedFiles(share.getId(), musicFolders)) {
                if (file.getFile().exists()) {
                    if (file.isDirectory()) {
                        List<MediaFile> childrenOf = mediaFileService.getChildrenOf(file, true, false, true);
                        result.addAll(childrenOf.stream().map(mf -> addUrlInfo(request, player, mf, finalExpires))
                                .collect(Collectors.toList()));
                    } else {
                        result.add(addUrlInfo(request, player, file, finalExpires));
                    }
                }
            }
        }
        return result;
    }

    public MediaFileWithUrlInfo addUrlInfo(HttpServletRequest request, Player player, MediaFile mediaFile,
            Date expires) {
        String prefix = "ext";
        String streamUrl = jwtSecurityService
                .addJWTToken(UriComponentsBuilder.fromHttpUrl(NetworkService.getBaseUrl(request) + prefix + "/stream")
                        .queryParam(Attributes.Request.ID.value(), mediaFile.getId())
                        .queryParam(Attributes.Request.PLAYER.value(), player.getId())
                        .queryParam(Attributes.Request.MAX_BIT_RATE.value(), MAX_BIT_RATE_VALUE), expires)
                .build().toUriString();

        String coverArtUrl = jwtSecurityService.addJWTToken(UriComponentsBuilder
                .fromHttpUrl(NetworkService.getBaseUrl(request) + prefix + "/" + ViewName.COVER_ART.value())
                .queryParam(Attributes.Request.ID.value(), mediaFile.getId())
                .queryParam(Attributes.Request.SIZE.value(), MAX_SIZE_VALUE), expires).build().toUriString();
        return new MediaFileWithUrlInfo(mediaFile, coverArtUrl, streamUrl);
    }
}
