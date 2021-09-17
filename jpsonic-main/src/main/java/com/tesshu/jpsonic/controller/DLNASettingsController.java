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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.DLNASettingsCommand;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.UPnPService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final PlayerService playerService;
    private final TranscodingService transcodingService;
    private final UPnPService upnpService;
    private final ShareService shareService;

    public DLNASettingsController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, PlayerService playerService, TranscodingService transcodingService,
            UPnPService upnpService, ShareService shareService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.transcodingService = transcodingService;
        this.upnpService = upnpService;
        this.shareService = shareService;
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        DLNASettingsCommand command = new DLNASettingsCommand();

        // UPnP basic settings
        command.setDlnaEnabled(settingsService.isDlnaEnabled());
        command.setDlnaServerName(settingsService.getDlnaServerName());
        command.setDlnaBaseLANURL(settingsService.getDlnaBaseLANURL());
        command.setAllMusicFolders(musicFolderService.getAllMusicFolders());
        User guestUser = securityService.getGuestUser();
        command.setAllowedMusicFolderIds(musicFolderService.getMusicFoldersForUser(guestUser.getUsername()).stream()
                .mapToInt(m -> m.getId()).toArray());
        Player guestPlayer = playerService.getGuestPlayer(null);
        command.setTranscodeScheme(guestPlayer.getTranscodeScheme());
        command.setAllTranscodings(transcodingService.getAllTranscodings());
        command.setActiveTranscodingIds(
                transcodingService.getTranscodingsForPlayer(guestPlayer).stream().mapToInt(m -> m.getId()).toArray());
        command.setUriWithFileExtensions(settingsService.isUriWithFileExtensions());

        // Items to display
        command.setDlnaIndexVisible(settingsService.isDlnaIndexVisible());
        command.setDlnaIndexId3Visible(settingsService.isDlnaIndexId3Visible());
        command.setDlnaFolderVisible(settingsService.isDlnaFolderVisible());
        command.setDlnaArtistVisible(settingsService.isDlnaArtistVisible());
        command.setDlnaArtistByFolderVisible(settingsService.isDlnaArtistByFolderVisible());
        command.setDlnaAlbumVisible(settingsService.isDlnaAlbumVisible());
        command.setDlnaPlaylistVisible(settingsService.isDlnaPlaylistVisible());
        command.setDlnaAlbumByGenreVisible(settingsService.isDlnaAlbumByGenreVisible());
        command.setDlnaSongByGenreVisible(settingsService.isDlnaSongByGenreVisible());
        command.setDlnaRecentAlbumVisible(settingsService.isDlnaRecentAlbumVisible());
        command.setDlnaRecentAlbumId3Visible(settingsService.isDlnaRecentAlbumId3Visible());
        command.setDlnaRandomSongVisible(settingsService.isDlnaRandomSongVisible());
        command.setDlnaRandomAlbumVisible(settingsService.isDlnaRandomAlbumVisible());
        command.setDlnaRandomSongByArtistVisible(settingsService.isDlnaRandomSongByArtistVisible());
        command.setDlnaRandomSongByFolderArtistVisible(settingsService.isDlnaRandomSongByFolderArtistVisible());
        command.setDlnaPodcastVisible(settingsService.isDlnaPodcastVisible());

        // Display options / Access control
        command.setDlnaGenreCountVisible(settingsService.isDlnaGenreCountVisible());
        command.setDlnaRandomMax(settingsService.getDlnaRandomMax());
        command.setDlnaGuestPublish(settingsService.isDlnaGuestPublish());

        // for view page control
        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setShareCount(shareService.getAllShares().size());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @GetMapping
    public String get(HttpServletRequest request, Model model) {
        return "dlnaSettings";
    }

    @PostMapping
    public ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) DLNASettingsCommand command,
            RedirectAttributes redirectAttributes) {

        final boolean isEnabledChanged = settingsService.isDlnaEnabled() != command.isDlnaEnabled();
        final boolean isNameOrUrlChanged = !isEmpty(command.getDlnaServerName())
                && !command.getDlnaServerName().equals(settingsService.getDlnaServerName())
                || !isEmpty(command.getDlnaBaseLANURL())
                        && !command.getDlnaBaseLANURL().equals(settingsService.getDlnaBaseLANURL());

        // # Changes to property file

        // UPnP basic settings
        settingsService.setDlnaEnabled(command.isDlnaEnabled());
        settingsService
                .setDlnaServerName(StringUtils.defaultIfEmpty(command.getDlnaServerName(), SettingsService.getBrand()));
        settingsService.setDlnaBaseLANURL(command.getDlnaBaseLANURL());
        settingsService.setUriWithFileExtensions(command.isUriWithFileExtensions());

        // Items to display
        settingsService.setDlnaIndexVisible(command.isDlnaIndexVisible());
        settingsService.setDlnaIndexId3Visible(command.isDlnaIndexId3Visible());
        settingsService.setDlnaFolderVisible(command.isDlnaFolderVisible());
        settingsService.setDlnaArtistVisible(command.isDlnaArtistVisible());
        settingsService.setDlnaArtistByFolderVisible(command.isDlnaArtistByFolderVisible());
        settingsService.setDlnaAlbumVisible(command.isDlnaAlbumVisible());
        settingsService.setDlnaPlaylistVisible(command.isDlnaPlaylistVisible());
        settingsService.setDlnaAlbumByGenreVisible(command.isDlnaAlbumByGenreVisible());
        settingsService.setDlnaSongByGenreVisible(command.isDlnaSongByGenreVisible());
        settingsService.setDlnaRecentAlbumVisible(command.isDlnaRecentAlbumVisible());
        settingsService.setDlnaRecentAlbumId3Visible(command.isDlnaRecentAlbumId3Visible());
        settingsService.setDlnaRandomSongVisible(command.isDlnaRandomSongVisible());
        settingsService.setDlnaRandomAlbumVisible(command.isDlnaRandomAlbumVisible());
        settingsService.setDlnaRandomSongByArtistVisible(command.isDlnaRandomSongByArtistVisible());
        settingsService.setDlnaRandomSongByFolderArtistVisible(command.isDlnaRandomSongByFolderArtistVisible());
        settingsService.setDlnaPodcastVisible(command.isDlnaPodcastVisible());

        // Display options / Access control
        final List<Integer> allowedIds = Arrays.stream(command.getAllowedMusicFolderIds()).boxed()
                .collect(Collectors.toList());
        List<Integer> allIds = musicFolderService.getAllMusicFolders().stream().map(m -> m.getId())
                .collect(Collectors.toList());
        settingsService.setDlnaGenreCountVisible(command.isDlnaGenreCountVisible() && allIds.equals(allowedIds));
        settingsService.setDlnaGuestPublish(command.isDlnaGuestPublish());

        settingsService.save();

        // # Changes to the database
        User guestUser = securityService.getGuestUser();
        musicFolderService.setMusicFoldersForUser(guestUser.getUsername(), allowedIds);
        UserSettings userSettings = securityService.getUserSettings(guestUser.getUsername());
        userSettings.setTranscodeScheme(command.getTranscodeScheme());
        userSettings.setChanged(new Date());
        Player guestPlayer = playerService.getGuestPlayer(null);
        guestPlayer.setTranscodeScheme(command.getTranscodeScheme());
        playerService.updatePlayer(guestPlayer);
        transcodingService.setTranscodingsForPlayer(guestPlayer, command.getActiveTranscodingIds());

        // If some properties are changed, UPnP will be started, stopped and restarted.
        if (isEnabledChanged) {
            upnpService.setMediaServerEnabled(command.isDlnaEnabled());
        } else if (isNameOrUrlChanged && settingsService.isDlnaEnabled()) {
            upnpService.setMediaServerEnabled(false);
            upnpService.setMediaServerEnabled(true);
        }

        // for view page control
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.DLNA_SETTINGS.value()));
    }
}
