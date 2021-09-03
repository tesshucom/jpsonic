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
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.MusicFolderSettingsCommand;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
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

    public MusicFolderSettingsController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, MediaScannerService mediaScannerService, ShareService shareService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaScannerService = mediaScannerService;
        this.shareService = shareService;
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request,
            @RequestParam(value = Attributes.Request.NameConstants.SCAN_NOW, required = false) String scanNow,
            @RequestParam(value = Attributes.Request.NameConstants.EXPUNGE, required = false) String expunge,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast, Model model) {

        MusicFolderSettingsCommand command = new MusicFolderSettingsCommand();
        if (!ObjectUtils.isEmpty(scanNow)) {
            musicFolderService.clearMusicFolderCache();
            mediaScannerService.scanLibrary();
        }
        if (!ObjectUtils.isEmpty(expunge)) {
            mediaScannerService.expunge();
        }

        // Specify folder
        command.setMusicFolders(wrap(musicFolderService.getAllMusicFolders(true, true)));
        command.setNewMusicFolder(new MusicFolderSettingsCommand.MusicFolderInfo());

        // Run a scan
        command.setFullScanNext(
                settingsService.isIgnoreFileTimestamps() || settingsService.isIgnoreFileTimestampsNext());
        command.setScanning(mediaScannerService.isScanning());
        command.setInterval(String.valueOf(settingsService.getIndexCreationInterval()));
        command.setHour(String.valueOf(settingsService.getIndexCreationHour()));

        // Exclusion settings
        command.setExcludePatternString(settingsService.getExcludePatternString());
        command.setIgnoreSymLinks(settingsService.isIgnoreSymLinks());

        // Other operations
        command.setFastCache(settingsService.isFastCacheEnabled());
        command.setFileModifiedCheckScheme(
                FileModifiedCheckScheme.valueOf(settingsService.getFileModifiedCheckSchemeName()));
        command.setIgnoreFileTimestamps(settingsService.isIgnoreFileTimestamps());
        command.setIgnoreFileTimestampsForEachAlbum(settingsService.isIgnoreFileTimestampsForEachAlbum());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    private List<MusicFolderSettingsCommand.MusicFolderInfo> wrap(List<MusicFolder> musicFolders) {
        return musicFolders.stream().map(MusicFolderSettingsCommand.MusicFolderInfo::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @GetMapping
    protected String get() {
        return "musicFolderSettings";
    }

    @PostMapping
    protected ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) MusicFolderSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        // Specify folder
        for (MusicFolderSettingsCommand.MusicFolderInfo musicFolderInfo : command.getMusicFolders()) {
            if (musicFolderInfo.isDelete()) {
                musicFolderService.deleteMusicFolder(musicFolderInfo.getId());
            } else {
                MusicFolder musicFolder = musicFolderInfo.toMusicFolder();
                if (musicFolder != null) {
                    musicFolderService.updateMusicFolder(musicFolder);
                }
            }
        }
        MusicFolder newMusicFolder = command.getNewMusicFolder().toMusicFolder();
        if (newMusicFolder != null) {
            musicFolderService.createMusicFolder(newMusicFolder);
        }

        // Run a scan
        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));

        // Exclusion settings
        settingsService.setExcludePatternString(command.getExcludePatternString());
        settingsService.setIgnoreSymLinks(command.isIgnoreSymLinks());

        // Other operations
        settingsService.setFastCacheEnabled(command.isFastCache());
        settingsService.setFileModifiedCheckSchemeName(command.getFileModifiedCheckScheme().name());
        settingsService
                .setIgnoreFileTimestamps(FileModifiedCheckScheme.LAST_MODIFIED == command.getFileModifiedCheckScheme()
                        && command.isIgnoreFileTimestamps());
        settingsService.setIgnoreFileTimestampsForEachAlbum(
                FileModifiedCheckScheme.LAST_SCANNED == command.getFileModifiedCheckScheme()
                        || command.isIgnoreFileTimestampsForEachAlbum());

        settingsService.save();

        // for view page control
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.MUSIC_FOLDER_SETTINGS.value()));
    }
}
