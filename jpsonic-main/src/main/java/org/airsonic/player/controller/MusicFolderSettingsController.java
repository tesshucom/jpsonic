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

package org.airsonic.player.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.command.MusicFolderSettingsCommand;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.ShareService;
import org.airsonic.player.service.search.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/musicFolderSettings")
public class MusicFolderSettingsController {

    private static final Logger LOG = LoggerFactory.getLogger(MusicFolderSettingsController.class);
    private static final AtomicBoolean IS_EXPUNGING = new AtomicBoolean();

    private final SettingsService settingsService;
    private final MediaScannerService mediaScannerService;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final MediaFileDao mediaFileDao;
    private final IndexManager indexManager;
    private final SecurityService securityService;
    private final ShareService shareService;

    public MusicFolderSettingsController(SettingsService settingsService, MediaScannerService mediaScannerService,
            ArtistDao artistDao, AlbumDao albumDao, MediaFileDao mediaFileDao, IndexManager indexManager,
            SecurityService securityService, ShareService shareService) {
        super();
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.mediaFileDao = mediaFileDao;
        this.indexManager = indexManager;
        this.securityService = securityService;
        this.shareService = shareService;
    }

    @GetMapping
    protected String displayForm() {
        return "musicFolderSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request,
            @RequestParam(value = Attributes.Request.NameConstants.SCAN_NOW, required = false) String scanNow,
            @RequestParam(value = Attributes.Request.NameConstants.EXPUNGE, required = false) String expunge,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast, Model model) {

        MusicFolderSettingsCommand command = new MusicFolderSettingsCommand();
        if (!ObjectUtils.isEmpty(scanNow)) {
            settingsService.clearMusicFolderCache();
            mediaScannerService.scanLibrary();
        }
        if (!ObjectUtils.isEmpty(expunge)) {
            expunge();
        }

        command.setInterval(String.valueOf(settingsService.getIndexCreationInterval()));
        command.setHour(String.valueOf(settingsService.getIndexCreationHour()));
        command.setFastCache(settingsService.isFastCacheEnabled());
        command.setOrganizeByFolderStructure(settingsService.isOrganizeByFolderStructure());
        command.setScanning(mediaScannerService.isScanning());
        command.setMusicFolders(wrap(settingsService.getAllMusicFolders(true, true)));
        command.setNewMusicFolder(new MusicFolderSettingsCommand.MusicFolderInfo());
        command.setExcludePatternString(settingsService.getExcludePatternString());
        command.setIgnoreSymLinks(settingsService.isIgnoreSymLinks());
        command.setIndexEnglishPrior(settingsService.isIndexEnglishPrior());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    private void expunge() {

        if (IS_EXPUNGING.get()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup is already running.");
            }
            return;
        }
        IS_EXPUNGING.set(true);

        MediaLibraryStatistics statistics = indexManager.getStatistics();

        /*
         * indexManager#expunge depends on DB delete flag. For consistency, clean of DB and Lucene must run in one
         * block.
         *
         * Lucene's writing is exclusive. This process cannot be performed while during scan or scan has never been
         * performed.
         */
        if (statistics != null && !mediaScannerService.isScanning()) {

            // to be before dao#expunge
            indexManager.startIndexing();
            indexManager.expunge();
            indexManager.stopIndexing(statistics);

            // to be after indexManager#expunge
            artistDao.expunge();
            albumDao.expunge();
            mediaFileDao.expunge();

            mediaFileDao.checkpoint();

        } else {
            LOG.warn(
                    "Index hasn't been created yet or during scanning. Plese execute clean up after scan is completed.");
        }

        IS_EXPUNGING.set(false);

    }

    private List<MusicFolderSettingsCommand.MusicFolderInfo> wrap(List<MusicFolder> musicFolders) {
        return musicFolders.stream().map(MusicFolderSettingsCommand.MusicFolderInfo::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @PostMapping
    protected ModelAndView onSubmit(@ModelAttribute(Attributes.Model.Command.VALUE) MusicFolderSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        for (MusicFolderSettingsCommand.MusicFolderInfo musicFolderInfo : command.getMusicFolders()) {
            if (musicFolderInfo.isDelete()) {
                settingsService.deleteMusicFolder(musicFolderInfo.getId());
            } else {
                MusicFolder musicFolder = musicFolderInfo.toMusicFolder();
                if (musicFolder != null) {
                    settingsService.updateMusicFolder(musicFolder);
                }
            }
        }

        MusicFolder newMusicFolder = command.getNewMusicFolder().toMusicFolder();
        if (newMusicFolder != null) {
            settingsService.createMusicFolder(newMusicFolder);
        }

        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));
        settingsService.setFastCacheEnabled(command.isFastCache());
        settingsService.setOrganizeByFolderStructure(command.isOrganizeByFolderStructure());
        settingsService.setExcludePatternString(command.getExcludePatternString());
        settingsService.setIgnoreSymLinks(command.isIgnoreSymLinks());
        settingsService.setIndexEnglishPrior(command.isIndexEnglishPrior());

        settingsService.save();

        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.MUSIC_FOLDER_SETTINGS.value()));
    }

}
