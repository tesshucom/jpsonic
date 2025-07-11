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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.command.UserSettingsCommand;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.PlayerUtils;
import com.tesshu.jpsonic.validator.UserSettingsValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate users.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/userSettings", "/userSettings.view" })
public class UserSettingsController {

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final TranscodingService transcodingService;
    private final ShareService shareService;
    private final PlayerService playerService;

    public UserSettingsController(SettingsService settingsService,
            MusicFolderService musicFolderService, SecurityService securityService,
            TranscodingService transcodingService, ShareService shareService,
            PlayerService playerService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.transcodingService = transcodingService;
        this.shareService = shareService;
        this.playerService = playerService;
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder, HttpServletRequest request) {
        binder.addValidators(new UserSettingsValidator(securityService, settingsService, request));
    }

    @GetMapping
    protected String get(HttpServletRequest request, Model model,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast)
            throws ServletRequestBindingException {

        UserSettingsCommand command;
        if (model.containsAttribute(Attributes.Model.Command.VALUE)) {
            command = (UserSettingsCommand) model.asMap().get(Attributes.Model.Command.VALUE);
        } else {
            command = new UserSettingsCommand();
            User user = getUser(request);
            if (user == null) {
                // User creation
                command.setNewUser(true);
                command.setStreamRole(true);
                command.setSettingsRole(true);
                command.setTranscodeScheme(TranscodeScheme.OFF);
            } else {
                // User update
                command.setUser(user);
                command.setEmail(user.getEmail());
                UserSettings userSettings = securityService.getUserSettings(user.getUsername());
                command.setTranscodeScheme(userSettings.getTranscodeScheme());
                command
                    .setAllowedMusicFolderIds(
                            PlayerUtils.toIntArray(getAllowedMusicFolderIds(user)));
                command
                    .setCurrentUser(securityService
                        .getCurrentUserStrict(request)
                        .getUsername()
                        .equals(user.getUsername()));
                command.setNowPlayingAllowed(userSettings.isNowPlayingAllowed());
            }
        }

        command.setLdapEnabled(settingsService.isLdapEnabled());
        command
            .setUsers(securityService
                .getAllUsers()
                .stream()
                .filter(u -> !User.USERNAME_GUEST.equals(u.getUsername()))
                .collect(Collectors.toList()));
        command.setAllMusicFolders(musicFolderService.getAllMusicFolders());
        command.setTranscodingSupported(transcodingService.isTranscodingSupported(null));

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "userSettings";
    }

    private User getUser(HttpServletRequest request) throws ServletRequestBindingException {
        Integer userIndex = ServletRequestUtils
            .getIntParameter(request, Attributes.Redirect.USER_INDEX.value());
        if (userIndex != null) {
            List<User> users = securityService
                .getAllUsers()
                .stream()
                .filter(u -> !User.USERNAME_GUEST.equals(u.getUsername()))
                .collect(Collectors.toList());
            if (userIndex >= 0 && userIndex < users.size()) {
                return users.get(userIndex);
            }
        }
        return null;
    }

    private List<Integer> getAllowedMusicFolderIds(User user) {
        List<Integer> result = new ArrayList<>();
        List<MusicFolder> allowedMusicFolders = user == null
                ? musicFolderService.getAllMusicFolders()
                : musicFolderService.getMusicFoldersForUser(user.getUsername());

        for (MusicFolder musicFolder : allowedMusicFolders) {
            result.add(musicFolder.getId());
        }
        return result;
    }

    @PostMapping
    protected ModelAndView post(
            @ModelAttribute(Attributes.Model.Command.VALUE) @Validated UserSettingsCommand command,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.COMMAND.value(), command);
            redirectAttributes
                .addFlashAttribute(Attributes.Redirect.BINDING_RESULT.value(), bindingResult);
        } else {
            if (command.isDeleteUser()) {
                deleteUser(command);
            } else if (command.isNewUser()) {
                createUser(command);
            } else {
                updateUser(command);
            }
            redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);
        }

        redirectAttributes
            .addFlashAttribute(Attributes.Redirect.USER_INDEX.value(),
                    getUserIndex(command.getUsername()));

        return new ModelAndView(new RedirectView(ViewName.USER_SETTINGS.value()));
    }

    private Integer getUserIndex(String userName) {
        List<User> users = securityService
            .getAllUsers()
            .stream()
            .filter(u -> !User.USERNAME_GUEST.equals(u.getUsername()))
            .collect(Collectors.toList());
        for (int i = 0; i < users.size(); i++) {
            if (StringUtils.equalsIgnoreCase(users.get(i).getUsername(), userName)) {
                return i;
            }
        }
        return null;
    }

    public void deleteUser(UserSettingsCommand command) {
        securityService.deleteUser(command.getUsername());
    }

    public void createUser(UserSettingsCommand command) {
        User user = new User(command.getUsername(), command.getPassword(),
                StringUtils.trimToNull(command.getEmail()));
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        securityService.createUser(user);
        updateUser(command);
    }

    public void updateUser(UserSettingsCommand command) {
        User user = securityService.getUserByNameStrict(command.getUsername());
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        if (command.isPasswordChange()) {
            user.setPassword(command.getPassword());
        }
        user.setEmail(StringUtils.trimToNull(command.getEmail()));
        user.setAdminRole(command.isAdminRole());
        user.setSettingsRole(command.isSettingsRole());
        user.setStreamRole(command.isStreamRole());
        user.setDownloadRole(command.isDownloadRole());
        user.setUploadRole(command.isUploadRole());
        user.setShareRole(command.isShareRole());
        user.setCoverArtRole(command.isCoverArtRole());
        user.setCommentRole(command.isCommentRole());
        user.setPodcastRole(command.isPodcastRole());

        securityService.updateUser(user);

        UserSettings userSettings = securityService.getUserSettings(command.getUsername());
        userSettings.setTranscodeScheme(command.getTranscodeScheme());
        if (command.getTranscodeScheme() != TranscodeScheme.OFF) {
            List<Player> userPlayers = playerService
                .getAllPlayers()
                .stream()
                .filter(p -> command.getUsername().equals(p.getUsername()))
                .collect(Collectors.toList());
            for (Player player : userPlayers) {
                if (player.getTranscodeScheme() == TranscodeScheme.OFF || player
                    .getTranscodeScheme()
                    .getMaxBitRate() > command.getTranscodeScheme().getMaxBitRate()) {
                    player.setTranscodeScheme(command.getTranscodeScheme());
                    playerService.updatePlayer(player);
                }
            }
        }
        userSettings.setNowPlayingAllowed(command.isNowPlayingAllowed());
        userSettings.setChanged(now());
        securityService.updateUserSettings(userSettings);

        List<Integer> allowedMusicFolderIds = PlayerUtils
            .toIntegerList(command.getAllowedMusicFolderIds());
        musicFolderService.setMusicFoldersForUser(command.getUsername(), allowedMusicFolderIds);
    }
}
