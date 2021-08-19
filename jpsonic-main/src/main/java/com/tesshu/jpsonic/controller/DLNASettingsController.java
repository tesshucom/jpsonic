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
 * (C) 2013 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.UPnPService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the UPnP server settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/dlnaSettings", "/dlnaSettings.view" })
public class DLNASettingsController {

    private final UPnPService upnpService;
    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ShareService shareService;

    public DLNASettingsController(UPnPService upnpService, SettingsService settingsService,
            SecurityService securityService, ShareService shareService) {
        super();
        this.upnpService = upnpService;
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
    }

    @GetMapping
    public String handleGet(HttpServletRequest request, Model model) {

        Map<String, Object> map = LegacyMap.of();

        map.put("dlnaEnabled", settingsService.isDlnaEnabled());
        map.put("dlnaServerName", settingsService.getDlnaServerName());
        map.put("dlnaBaseLANURL", settingsService.getDlnaBaseLANURL());

        map.put("dlnaAlbumVisible", settingsService.isDlnaAlbumVisible());
        map.put("dlnaArtistVisible", settingsService.isDlnaArtistVisible());
        map.put("dlnaArtistByFolderVisible", settingsService.isDlnaArtistByFolderVisible());
        map.put("dlnaAlbumByGenreVisible", settingsService.isDlnaAlbumByGenreVisible());
        map.put("dlnaSongByGenreVisible", settingsService.isDlnaSongByGenreVisible());
        map.put("dlnaGenreCountVisible", settingsService.isDlnaGenreCountVisible());
        map.put("dlnaFolderVisible", settingsService.isDlnaFolderVisible());
        map.put("dlnaPlaylistVisible", settingsService.isDlnaPlaylistVisible());
        map.put("dlnaRecentAlbumVisible", settingsService.isDlnaRecentAlbumVisible());
        map.put("dlnaRecentAlbumId3Visible", settingsService.isDlnaRecentAlbumId3Visible());
        map.put("dlnaRandomAlbumVisible", settingsService.isDlnaRandomAlbumVisible());
        map.put("dlnaRandomSongVisible", settingsService.isDlnaRandomSongVisible());
        map.put("dlnaRandomSongByArtistVisible", settingsService.isDlnaRandomSongByArtistVisible());
        map.put("dlnaRandomSongByFolderArtistVisible", settingsService.isDlnaRandomSongByFolderArtistVisible());
        map.put("dlnaIndexVisible", settingsService.isDlnaIndexVisible());
        map.put("dlnaIndexId3Visible", settingsService.isDlnaIndexId3Visible());
        map.put("dlnaPodcastVisible", settingsService.isDlnaPodcastVisible());
        map.put("dlnaRandomMax", settingsService.getDlnaRandomMax());
        map.put("dlnaGuestPublish", settingsService.isDlnaGuestPublish());
        map.put("shareCount", shareService.getAllShares().size());

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        map.put("isOpenDetailSetting", userSettings.isOpenDetailSetting());

        map.put("useRadio", settingsService.isUseRadio());
        map.put("useSonos", settingsService.isUseSonos());

        model.addAttribute("model", map);
        return "dlnaSettings";
    }

    @PostMapping
    public ModelAndView handlePost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        handleParameters(request);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        return new ModelAndView(new RedirectView(ViewName.DLNA_SETTINGS.value()));
    }

    private void handleParameters(HttpServletRequest request) {
        boolean dlnaEnabled = ServletRequestUtils.getBooleanParameter(request, Attributes.Request.DLNA_ENABLED.value(),
                false);
        String dlnaServerName = trimToNull(request.getParameter(Attributes.Request.DLNA_SERVER_NAME.value()));
        if (dlnaServerName == null) {
            dlnaServerName = "Jpsonic";
        }
        String dlnaBaseLANURL = trimToNull(request.getParameter(Attributes.Request.DLNA_BASE_LAN_URL.value()));

        boolean dlnaAlbumVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_ALBUM_VISIBLE.value(), false);
        boolean dlnaArtistVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_ARTIST_VISIBLE.value(), false);
        boolean dlnaArtistByFolderVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_ARTIST_BY_FOLDER_VISIBLE.value(), false);
        boolean dlnaAlbumByGenreVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_ALBUM_BYGENRE_VISIBLE.value(), false);
        boolean dlnaSongByGenreVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_SONG_BY_GENRE_VISIBLE.value(), false);
        boolean dlnaGenreCountVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_GENRE_COUNT_VISIBLE.value(), false);
        boolean dlnaFolderVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_FOLDER_VISIBLE.value(), false);
        boolean dlnaPlaylistVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_PLAYLIST_VISIBLE.value(), false);
        boolean dlnaRecentAlbumVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RECENT_ALBUM_VISIBLE.value(), false);
        boolean dlnaRecentAlbumId3Visible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RECENT_ALBUM_ID3_VISIBLE.value(), false);
        boolean dlnaRandomAlbumVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RANDOM_ALBUM_VISIBLE.value(), false);
        boolean dlnaRandomSongVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RANDOM_SONG_VISIBLE.value(), false);
        boolean dlnaRandomSongByArtistVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE.value(), false);
        boolean dlnaRandomSongByFolderArtistVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE.value(), false);
        boolean dlnaIndexVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_INDEX_VISIBLE.value(), false);
        boolean dlnaIndexId3Visible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_INDEX_ID3_VISIBLE.value(), false);
        boolean dlnaPodcastVisible = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_PODCAST_VISIBLE.value(), false);
        int dlnaRandomMax = ServletRequestUtils.getIntParameter(request, Attributes.Request.DLNA_RANDOM_MAX.value(),
                50);
        boolean dlnaGuestPublish = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.DLNA_GUEST_PUBLISH.value(), false);

        settingsService.setDlnaEnabled(dlnaEnabled);
        settingsService.setDlnaServerName(dlnaServerName);
        settingsService.setDlnaBaseLANURL(dlnaBaseLANURL);

        settingsService.setDlnaAlbumVisible(dlnaAlbumVisible);
        settingsService.setDlnaArtistVisible(dlnaArtistVisible);
        settingsService.setDlnaArtistByFolderVisible(dlnaArtistByFolderVisible);
        settingsService.setDlnaAlbumByGenreVisible(dlnaAlbumByGenreVisible);
        settingsService.setDlnaSongByGenreVisible(dlnaSongByGenreVisible);
        settingsService.setDlnaGenreCountVisible(!dlnaGuestPublish && dlnaGenreCountVisible);
        settingsService.setDlnaFolderVisible(dlnaFolderVisible);
        settingsService.setDlnaPlaylistVisible(dlnaPlaylistVisible);
        settingsService.setDlnaRecentAlbumVisible(dlnaRecentAlbumVisible);
        settingsService.setDlnaRecentAlbumId3Visible(dlnaRecentAlbumId3Visible);
        settingsService.setDlnaRandomAlbumVisible(dlnaRandomAlbumVisible);
        settingsService.setDlnaRandomSongVisible(dlnaRandomSongVisible);
        settingsService.setDlnaRandomSongByArtistVisible(dlnaRandomSongByArtistVisible);
        settingsService.setDlnaRandomSongByFolderArtistVisible(dlnaRandomSongByFolderArtistVisible);
        settingsService.setDlnaIndexVisible(dlnaIndexVisible);
        settingsService.setDlnaIndexId3Visible(dlnaIndexId3Visible);
        settingsService.setDlnaPodcastVisible(dlnaPodcastVisible);
        settingsService.setDlnaRandomMax(dlnaRandomMax);
        settingsService.setDlnaGuestPublish(dlnaGuestPublish);

        settingsService.save();

        boolean isEnabledStateChange = !(settingsService.isDlnaEnabled() == dlnaEnabled && !isEmpty(dlnaServerName)
                && dlnaServerName.equals(settingsService.getDlnaServerName()) && !isEmpty(dlnaBaseLANURL)
                && dlnaBaseLANURL.equals(settingsService.getDlnaBaseLANURL()));
        if (isEnabledStateChange) {
            upnpService.setMediaServerEnabled(dlnaEnabled);
        }

    }
}
