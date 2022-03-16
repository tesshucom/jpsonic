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

package com.tesshu.jpsonic.ajax;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.NetworkUtils;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.ServletRequestBindingException;

/**
 * Provides AJAX-enabled services for retrieving the currently playing file and directory. This class is used by the DWR
 * framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxNowPlayingService")
public class NowPlayingService {

    private static final int LIMIT_OF_HISTORY_TO_BE_PRESENTED = 60;

    private final SecurityService securityService;
    private final PlayerService playerService;
    private final StatusService statusService;
    private final MediaScannerService mediaScannerService;
    private final AvatarService avatarService;
    private final AjaxHelper ajaxHelper;

    public NowPlayingService(SecurityService securityService, PlayerService playerService, StatusService statusService,
            MediaScannerService mediaScannerService, AvatarService avatarService, AjaxHelper ajaxHelper) {
        super();
        this.securityService = securityService;
        this.playerService = playerService;
        this.statusService = statusService;
        this.mediaScannerService = mediaScannerService;
        this.avatarService = avatarService;
        this.ajaxHelper = ajaxHelper;
    }

    /**
     * Returns details about what the current player is playing.
     *
     * @return Details about what the current player is playing, or <code>null</code> if not playing anything.
     *
     * @throws ServletRequestBindingException
     */
    public NowPlayingInfo getNowPlayingForCurrentPlayer() throws ServletRequestBindingException {
        Player player = playerService.getPlayer(ajaxHelper.getHttpServletRequest(),
                ajaxHelper.getHttpServletResponse());

        for (NowPlayingInfo info : getNowPlaying()) {
            if (player.getId().equals(info.getPlayerId())) {
                return info;
            }
        }
        return null;
    }

    /**
     * Returns details about what all users are currently playing.
     *
     * @return Details about what all users are currently playing.
     */
    public List<NowPlayingInfo> getNowPlaying() {
        return convert(statusService.getPlayStatuses());
    }

    /**
     * Returns media folder scanning status.
     */
    public ScanInfo getScanningStatus() {
        return new ScanInfo(mediaScannerService.isScanning(), mediaScannerService.getScanCount());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (NowPlayingInfo) Not reusable
    private List<NowPlayingInfo> convert(List<PlayStatus> playStatuses) {
        HttpServletRequest request = ajaxHelper.getHttpServletRequest();
        String url = NetworkUtils.getBaseUrl(request);
        List<NowPlayingInfo> result = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        for (PlayStatus status : playStatuses) {

            Player player = status.getPlayer();
            MediaFile mediaFile = status.getMediaFile();
            String username = player.getUsername();
            if (username == null) {
                continue;
            }
            UserSettings userSettings = securityService.getUserSettings(username);
            if (!userSettings.isNowPlayingAllowed()) {
                continue;
            }

            String artist = mediaFile.getArtist();
            String title = mediaFile.getTitle();
            String streamUrl = url + "stream?player=" + player.getId() + "&id=" + mediaFile.getId();
            String albumUrl = url + ViewName.MAIN.value() + "?id=" + mediaFile.getId();
            String lyricsUrl = null;
            if (!mediaFile.isVideo()) {
                lyricsUrl = url + ViewName.LYRICS.value() + "?artistUtf8Hex=" + StringUtil.utf8HexEncode(artist)
                        + "&songUtf8Hex=" + StringUtil.utf8HexEncode(title);
            }

            String coverArtUrl = url + ViewName.COVER_ART.value() + "?size=60&id=" + mediaFile.getId();
            String avatarUrl = avatarService.createAvatarUrl(url, userSettings);
            String tooltip = StringEscapeUtils.escapeHtml4(artist) + " &ndash; " + StringEscapeUtils.escapeHtml4(title);
            artist = StringEscapeUtils.escapeHtml4(StringUtils.abbreviate(artist, 25));
            title = StringEscapeUtils.escapeHtml4(StringUtils.abbreviate(title, 25));

            if (StringUtils.isNotBlank(player.getName())) {
                builder.setLength(0);
                username = builder.append(username).append('@').append(player.getName()).toString();
            }
            username = StringEscapeUtils.escapeHtml4(StringUtils.abbreviate(username, 25));

            long minutesAgo = status.getMinutesAgo();
            if (minutesAgo < LIMIT_OF_HISTORY_TO_BE_PRESENTED) {
                result.add(new NowPlayingInfo(player.getId(), username, artist, title, tooltip, streamUrl, albumUrl,
                        lyricsUrl, coverArtUrl, avatarUrl, (int) minutesAgo));
            }
        }
        return result;
    }
}
