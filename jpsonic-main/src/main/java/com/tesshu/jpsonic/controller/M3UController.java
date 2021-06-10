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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.NetworkUtils;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller which produces the M3U playlist.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/play.m3u")
public class M3UController {

    private final PlayerService playerService;
    private final TranscodingService transcodingService;
    private final JWTSecurityService jwtSecurityService;

    public M3UController(PlayerService playerService, TranscodingService transcodingService,
            JWTSecurityService jwtSecurityService) {
        super();
        this.playerService = playerService;
        this.transcodingService = transcodingService;
        this.jwtSecurityService = jwtSecurityService;
    }

    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException, IOException {
        response.setContentType("audio/x-mpegurl");
        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);

        Player player = playerService.getPlayer(request, response);

        String url = NetworkUtils.getBaseUrl(request);
        url = url + "ext/stream?";

        if (player.isExternalWithPlaylist()) {
            createClientSidePlaylist(response.getWriter(), player, url);
        } else {
            createServerSidePlaylist(response.getWriter(), player, url);
        }
        return null;
    }

    private void createClientSidePlaylist(PrintWriter out, Player player, String url) {
        if (player.isM3uBomEnabled()) {
            out.print("\ufeff");
        }
        out.println("#EXTM3U");
        List<MediaFile> result;
        synchronized (player.getPlayQueue()) {
            result = player.getPlayQueue().getFiles();
        }
        for (MediaFile mediaFile : result) {
            Integer duration = mediaFile.getDurationSeconds();
            if (duration == null) {
                duration = -1;
            }
            out.println("#EXTINF:" + duration + "," + mediaFile.getArtist() + " - " + mediaFile.getTitle());

            String urlNoAuth = url + "player=" + player.getId() + "&id=" + mediaFile.getId() + "&suffix=."
                    + transcodingService.getSuffix(player, mediaFile, null);
            String urlWithAuth = jwtSecurityService.addJWTToken(urlNoAuth);
            out.println(urlWithAuth);
        }
    }

    private void createServerSidePlaylist(PrintWriter out, Player player, final String urlStr) {

        String url = urlStr;

        url += Attributes.Request.PLAYER.value() + "=" + player.getId();

        // Get suffix of current file, e.g., ".mp3".
        String suffix = getSuffix(player);
        if (suffix != null) {
            url = new StringBuilder(url).append('&').append(Attributes.Request.SUFFIX.value()).append("=.")
                    .append(suffix).toString();
        }

        if (player.isM3uBomEnabled()) {
            out.print("\ufeff");
        }
        out.println("#EXTM3U");
        out.println("#EXTINF:-1,Jpsonic");
        out.println(jwtSecurityService.addJWTToken(url));
    }

    private String getSuffix(Player player) {
        PlayQueue playQueue = player.getPlayQueue();
        return playQueue.isEmpty() ? null : transcodingService.getSuffix(player, playQueue.getFile(0), null);
    }

}
