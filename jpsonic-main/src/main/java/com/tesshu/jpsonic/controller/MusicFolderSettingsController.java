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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.command.MusicFolderSettingsCommand;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.util.PathValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the set of music folders.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/musicFolderSettings", "/musicFolderSettings.view" })
public class MusicFolderSettingsController {

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final MediaScannerService mediaScannerService;
    private final ShareService shareService;
    private final OutlineHelpSelector outlineHelpSelector;

    public MusicFolderSettingsController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, MediaScannerService mediaScannerService, ShareService shareService,
            OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaScannerService = mediaScannerService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request,
            @RequestParam(value = Attributes.Request.NameConstants.SCAN_NOW, required = false) String scanNow,
            @RequestParam(value = Attributes.Request.NameConstants.SCAN_CANCEL, required = false) String scanCancel,
            @RequestParam(value = Attributes.Request.NameConstants.EXPUNGE, required = false) String expunge,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast, Model model) {

        if (!ObjectUtils.isEmpty(scanNow)) {
            musicFolderService.clearMusicFolderCache();
            mediaScannerService.scanLibrary();
        }
        if (!ObjectUtils.isEmpty(scanCancel)) {
            mediaScannerService.tryCancel();

        }
        if (!ObjectUtils.isEmpty(expunge)) {
            mediaScannerService.expunge();
        }

        MusicFolderSettingsCommand command = new MusicFolderSettingsCommand();

        // Specify folder
        command.setMusicFolders(wrap(musicFolderService.getAllMusicFolders(true, true)));
        command.setNewMusicFolder(new MusicFolderSettingsCommand.MusicFolderInfo());

        // Run a scan
        mediaScannerService.getLastScanEventType().ifPresent(type -> command.setLastScanEventType(type));
        command.setIgnoreFileTimestamps(settingsService.isIgnoreFileTimestamps());
        command.setInterval(String.valueOf(settingsService.getIndexCreationInterval()));
        command.setHour(String.valueOf(settingsService.getIndexCreationHour()));
        command.setUseCleanUp(settingsService.isUseCleanUp());

        // Exclusion settings
        command.setExcludePatternString(settingsService.getExcludePatternString());
        command.setIgnoreSymLinks(settingsService.isIgnoreSymLinks());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());

        User user = securityService.getCurrentUserStrict(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setScanning(mediaScannerService.isScanning());
        command.setCancel(mediaScannerService.isCancel());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    List<MusicFolderSettingsCommand.MusicFolderInfo> wrap(List<MusicFolder> musicFolders) {
        var folders = musicFolders.stream().map(MusicFolderSettingsCommand.MusicFolderInfo::new)
                .collect(Collectors.toCollection(ArrayList::new));
        if (settingsService.isRedundantFolderCheck()) {
            folders.forEach(folder -> {
                Path path = Path.of(folder.getPath());
                folder.setExisting(Files.exists(path) && Files.isDirectory(path));
            });
        }
        return folders;
    }

    @GetMapping
    protected String get() {
        return "musicFolderSettings";
    }

    @PostMapping
    protected ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) MusicFolderSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        final ModelAndView result = new ModelAndView(new RedirectView(ViewName.MUSIC_FOLDER_SETTINGS.value()));
        if (mediaScannerService.isScanning()) {
            return result;
        }

        Instant executed = now();

        // Specify folder
        for (MusicFolderSettingsCommand.MusicFolderInfo musicFolderInfo : command.getMusicFolders()) {
            if (musicFolderInfo.isDelete()) {
                musicFolderService.deleteMusicFolder(executed, musicFolderInfo.getId());
            } else {
                toMusicFolder(musicFolderInfo)
                        .ifPresent(folder -> musicFolderService.updateMusicFolder(executed, folder));
            }
        }
        toMusicFolder(command.getNewMusicFolder()).ifPresent(newFolder -> {
            if (musicFolderService.getAllMusicFolders(false, true).stream()
                    .noneMatch(oldFolder -> oldFolder.getPathString().equals(newFolder.getPathString()))) {
                musicFolderService.createMusicFolder(executed, newFolder);
            }
        });

        // Run a scan
        settingsService.setIgnoreFileTimestamps(command.isIgnoreFileTimestamps());
        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));

        // Exclusion settings
        settingsService.setExcludePatternString(command.getExcludePatternString());
        settingsService.setIgnoreSymLinks(command.isIgnoreSymLinks());

        settingsService.save();

        // for view page control
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);

        return result;
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Validated.")
    public Optional<MusicFolder> toMusicFolder(MusicFolderSettingsCommand.MusicFolderInfo info) {
        Optional<String> validated = PathValidator.validateFolderPath(StringUtils.trimToNull(info.getPath()));
        if (validated.isEmpty()) {
            return Optional.empty();
        }

        Path newPath = Path.of(validated.get());
        if (musicFolderService.getAllMusicFolders(true, true).stream().anyMatch(old -> !old.toPath().equals(newPath)
                && (old.toPath().startsWith(newPath) || newPath.startsWith(old.toPath())))) {
            return Optional.empty();
        }

        String name = StringUtils.trimToNull(info.getName());
        if (name == null) {
            name = newPath.getFileName().toString();
        }
        return Optional.of(new MusicFolder(info.getId(), validated.get(), name, info.isEnabled(), now()));
    }
}
