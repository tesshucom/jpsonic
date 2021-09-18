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
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.PlayerSettingsCommand;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the player settings page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/playerSettings", "/playerSettings.view" })
public class PlayerSettingsController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final PlayerService playerService;
    private final TranscodingService transcodingService;
    private final ShareService shareService;
    private final OutlineHelpSelector outlineHelpSelector;

    public PlayerSettingsController(SettingsService settingsService, SecurityService securityService,
            PlayerService playerService, TranscodingService transcodingService, ShareService shareService,
            OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.transcodingService = transcodingService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

    @GetMapping
    protected String displayForm() {
        return "playerSettings";
    }

    @ModelAttribute
    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    protected void formBackingObject(HttpServletRequest request, Model model,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast)
            throws ServletRequestBindingException {

        deleteOrClone(request, model);

        PlayerSettingsCommand command = new PlayerSettingsCommand();
        List<Player> players = getPlayers(request);
        command.setPlayers(
                players.stream().filter(p -> !User.USERNAME_GUEST.equals(p.getUsername())).toArray(Player[]::new));
        User user = securityService.getCurrentUser(request);
        command.setAdmin(user.isAdminRole());
        command.setTranscodingSupported(transcodingService.isTranscodingSupported(null));

        Player player = null;
        Integer playerId = ServletRequestUtils.getIntParameter(request, Attributes.Request.ID.value());
        if (playerId != null) {
            player = playerService.getPlayerById(playerId);
        } else if (!players.isEmpty()) {
            player = players.get(0);
        }

        if (player != null) {
            // Player settings
            command.setPlayerId(player.getId());
            command.setType(player.getType());
            command.setName(player.getName());
            command.setGuest(User.USERNAME_GUEST.equals(player.getUsername()));
            command.setAnonymous(JWTAuthenticationToken.USERNAME_ANONYMOUS.equals(player.getUsername()));
            command.setSameSegment(settingsService.isInUPnPRange(player.getIpAddress()));
            command.setPlayerTechnology(player.getTechnology());
            command.setTranscodeScheme(player.getTranscodeScheme());
            command.setAllTranscodings(transcodingService.getAllTranscodings());

            List<Transcoding> activeTranscodings = transcodingService.getTranscodingsForPlayer(player);
            int[] activeTranscodingIds = new int[activeTranscodings.size()];
            for (int i = 0; i < activeTranscodings.size(); i++) {
                activeTranscodingIds[i] = activeTranscodings.get(i).getId();
            }
            command.setActiveTranscodingIds(activeTranscodingIds);

            command.setDynamicIp(player.isDynamicIp());
            command.setAutoControlEnabled(player.isAutoControlEnabled());
            command.setM3uBomEnabled(player.isM3uBomEnabled());
            command.setLastSeen(player.getLastSeen());
        }

        // for view page control
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @PostMapping
    protected ModelAndView doSubmitAction(@ModelAttribute(Attributes.Model.Command.VALUE) PlayerSettingsCommand command,
            RedirectAttributes redirectAttributes) {
        Player player = playerService.getPlayerById(command.getPlayerId());
        if (player == null) {
            return new ModelAndView(new RedirectView("notFound"));
        } else {

            // Player settings
            player.setName(StringUtils.trimToNull(command.getName()));
            player.setTechnology(command.getPlayerTechnology());
            player.setTranscodeScheme(command.getTranscodeScheme());
            player.setDynamicIp(command.isDynamicIp());
            player.setAutoControlEnabled(command.isAutoControlEnabled());
            player.setM3uBomEnabled(command.isM3uBomEnabled());
            playerService.updatePlayer(player);
            transcodingService.setTranscodingsForPlayer(player, command.getActiveTranscodingIds());

            // for view page control
            redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);
            redirectAttributes.addFlashAttribute(Attributes.Redirect.PLAYER_ID.value(), player.getId());
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

            return new ModelAndView(new RedirectView(ViewName.PLAYER_SETTINGS.value()));
        }
    }

    private List<Player> getPlayers(HttpServletRequest request) {
        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        List<Player> players = playerService.getAllPlayers();
        List<Player> authorizedPlayers = new ArrayList<>();
        for (Player player : players) {
            // Only display authorized players.
            if (user.isAdminRole() || username.equals(player.getUsername())) {
                authorizedPlayers.add(player);
            }
        }
        return authorizedPlayers;
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    private void deleteOrClone(HttpServletRequest request, Model model) throws ServletRequestBindingException {
        if (request.getParameter(Attributes.Request.DELETE.value()) != null) {
            Integer delete = ServletRequestUtils.getIntParameter(request, Attributes.Request.DELETE.value());
            if (delete != null) {
                playerService.removePlayerById(delete);
            }
        } else if (request.getParameter(Attributes.Request.CLONE.value()) != null) {
            Integer clone = ServletRequestUtils.getIntParameter(request, Attributes.Request.CLONE.value());
            if (clone != null) {
                Player clonedPlayer = playerService.clonePlayer(clone);
                model.addAttribute(Attributes.Redirect.PLAYER_ID.value(), clonedPlayer.getId());
            }
        }
    }
}
