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

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.command.DLNASettingsCommand;
import com.tesshu.jpsonic.command.DLNASettingsCommand.SubMenuItemRowInfo;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MenuItemService;
import com.tesshu.jpsonic.service.MenuItemService.MenuItemWithDefaultName;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.UPnPService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final MenuItemService menuItemService;
    private final OutlineHelpSelector outlineHelpSelector;

    public DLNASettingsController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, PlayerService playerService, TranscodingService transcodingService,
            UPnPService upnpService, ShareService shareService, MenuItemService menuItemService,
            OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.transcodingService = transcodingService;
        this.upnpService = upnpService;
        this.shareService = shareService;
        this.menuItemService = menuItemService;
        this.outlineHelpSelector = outlineHelpSelector;
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
        command.setAllTranscodings(transcodingService.getAllTranscodings());
        Player guestPlayer = playerService.getGuestPlayer(null);
        command.setActiveTranscodingIds(
                transcodingService.getTranscodingsForPlayer(guestPlayer).stream().mapToInt(m -> m.getId()).toArray());
        command.setTranscodingSupported(transcodingService.isTranscodingSupported(null));
        command.setTranscodeScheme(guestPlayer.getTranscodeScheme());
        command.setUriWithFileExtensions(settingsService.isUriWithFileExtensions());

        // Menu Details
        List<MenuItem> topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE);
        List<MenuItemWithDefaultName> subMenuItems = menuItemService.getSubMenuItems(ViewType.UPNP);
        Map<MenuItemId, SubMenuItemRowInfo> subMenuItemRowInfos = new ConcurrentHashMap<>();

        topMenuItems.stream().map(topMenu -> topMenu.getId()).forEach(topMenuItemId -> {
            int count = (int) subMenuItems.stream().filter(subMenuItem -> subMenuItem.getParent() == topMenuItemId)
                    .count();
            subMenuItems.stream().filter(subMenuItem -> subMenuItem.getParent() == topMenuItemId).findFirst().ifPresent(
                    firstChild -> subMenuItemRowInfos.put(topMenuItemId, new SubMenuItemRowInfo(firstChild, count)));
        });
        command.setSubMenuItems(subMenuItems);
        command.setSubMenuItemRowInfos(subMenuItemRowInfos);

        // Display options / Access control
        command.setDlnaGenreCountVisible(settingsService.isDlnaGenreCountVisible());
        command.setDlnaRandomMax(settingsService.getDlnaRandomMax());
        command.setDlnaGuestPublish(settingsService.isDlnaGuestPublish());

        // for view page control
        User user = securityService.getCurrentUserStrict(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setShareCount(shareService.getAllShares().size());
        command.setUseRadio(settingsService.isUseRadio());
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));

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

        /*
         * Changes to property file
         */

        // UPnP basic settings
        settingsService.setDlnaEnabled(command.isDlnaEnabled());
        settingsService
                .setDlnaServerName(StringUtils.defaultIfEmpty(command.getDlnaServerName(), SettingsService.getBrand()));
        settingsService.setDlnaBaseLANURL(command.getDlnaBaseLANURL());
        settingsService.setUriWithFileExtensions(command.isUriWithFileExtensions());

        // Display options / Access control
        final List<Integer> allowedIds = Arrays.stream(command.getAllowedMusicFolderIds()).boxed()
                .collect(Collectors.toList());
        List<Integer> allIds = musicFolderService.getAllMusicFolders().stream().map(m -> m.getId())
                .collect(Collectors.toList());
        settingsService.setDlnaGenreCountVisible(command.isDlnaGenreCountVisible() && allIds.equals(allowedIds));
        settingsService.setDlnaGuestPublish(command.isDlnaGuestPublish());

        settingsService.save();

        /*
         * Changes to the database
         */

        // UPnP basic settings
        User guestUser = securityService.getGuestUser();
        musicFolderService.setMusicFoldersForUser(guestUser.getUsername(), allowedIds);
        UserSettings userSettings = securityService.getUserSettings(guestUser.getUsername());
        userSettings.setTranscodeScheme(command.getTranscodeScheme());
        userSettings.setChanged(now());
        Player guestPlayer = playerService.getGuestPlayer(null);
        transcodingService.setTranscodingsForPlayer(guestPlayer, command.getActiveTranscodingIds());
        if (command.getActiveTranscodingIds().length == 0) {
            guestPlayer.setTranscodeScheme(TranscodeScheme.OFF);
        } else {
            guestPlayer.setTranscodeScheme(command.getTranscodeScheme());
        }
        playerService.updatePlayer(guestPlayer);

        // Menu detail settings
        command.getSubMenuItems().forEach(in -> {
            MenuItem menuItem = menuItemService.getMenuItem(in.getId());
            if (menuItem.isEnabled() == in.isEnabled() && menuItem.getName().equals(in.getName())) {
                return;
            }
            menuItem.setEnabled(in.isEnabled());
            menuItem.setName(in.getName());
            menuItemService.updateMenuItem(menuItem);
        });

        /*
         * Service reboot: If some properties are changed, UPnP will be restarted. (Do not restart if the settings
         * related to the contents that can be changed dynamically are changed.)
         */

        if (isEnabledChanged) {
            upnpService.setEnabled(command.isDlnaEnabled());
        } else if (isNameOrUrlChanged && settingsService.isDlnaEnabled()) {
            upnpService.setEnabled(false);
            upnpService.setEnabled(true);
        }

        /*
         * For view page control
         */
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.DLNA_SETTINGS.value()));
    }
}
