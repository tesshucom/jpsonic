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
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.NetworkUtils;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
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

    private final PlayerService playerService;
    private final StatusService statusService;
    private final SettingsService settingsService;
    private final MediaScannerService mediaScannerService;

    public NowPlayingService(PlayerService playerService, StatusService statusService, SettingsService settingsService,
            MediaScannerService mediaScannerService) {
        super();
        this.playerService = playerService;
        this.statusService = statusService;
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
    }

    /**
     * Returns details about what the current player is playing.
     *
     * @return Details about what the current player is playing, or <code>null</code> if not playing anything.
     * 
     * @throws ServletRequestBindingException
     */
    public NowPlayingInfo getNowPlayingForCurrentPlayer() throws ServletRequestBindingException {
        WebContext webContext = WebContextFactory.get();
        Player player = playerService.getPlayer(webContext.getHttpServletRequest(),
                webContext.getHttpServletResponse());

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
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
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
            UserSettings userSettings = settingsService.getUserSettings(username);
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

            String avatarUrl = null;
            if (userSettings.getAvatarScheme() == AvatarScheme.SYSTEM) {
                avatarUrl = url + ViewName.AVATAR.value() + "?id=" + userSettings.getSystemAvatarId();
            } else if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM
                    && settingsService.getCustomAvatar(username) != null) {
                avatarUrl = url + ViewName.AVATAR.value() + "?usernameUtf8Hex=" + StringUtil.utf8HexEncode(username);
            }

            String tooltip = StringEscapeUtils.escapeHtml(artist) + " &ndash; " + StringEscapeUtils.escapeHtml(title);

            if (StringUtils.isNotBlank(player.getName())) {
                builder.setLength(0);
                username = builder.append(username).append('@').append(player.getName()).toString();
            }
            artist = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(artist, 25));
            title = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(title, 25));
            username = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(username, 25));

            long minutesAgo = status.getMinutesAgo();

            if (minutesAgo < LIMIT_OF_HISTORY_TO_BE_PRESENTED) {
                result.add(new NowPlayingInfo(player.getId(), username, artist, title, tooltip, streamUrl, albumUrl,
                        lyricsUrl, coverArtUrl, avatarUrl, (int) minutesAgo));
            }
        }
        return result;
    }
}
