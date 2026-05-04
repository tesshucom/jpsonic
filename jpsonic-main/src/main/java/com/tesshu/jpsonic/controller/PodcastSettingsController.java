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

import com.tesshu.jpsonic.controller.form.PodcastSettingsCommand;
import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider.PathGeometry;
import com.tesshu.jpsonic.infrastructure.filesystem.RootPathEntryGuard;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.service.ScannerStateService;
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
 * Controller for the page used to administrate the Podcast receiver.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/podcastSettings", "/podcastSettings.view" })
public class PodcastSettingsController {

    private final SettingsFacade settingsFacade;
    private final ScannerStateService scannerStateService;

    public PodcastSettingsController(SettingsFacade settingsFacade,
            ScannerStateService scannerStateService) {
        super();
        this.settingsFacade = settingsFacade;
        this.scannerStateService = scannerStateService;
    }

    @GetMapping
    protected String formBackingObject(Model model) {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        command.setInterval(String.valueOf(settingsFacade.get(SKeys.podcast.updateInterval)));
        command
            .setEpisodeRetentionCount(
                    String.valueOf(settingsFacade.get(SKeys.podcast.episodeRetentionCount)));
        command
            .setEpisodeDownloadCount(
                    String.valueOf(settingsFacade.get(SKeys.podcast.episodeDownloadCount)));
        command.setFolder(settingsFacade.get(SKeys.podcast.folder));

        // for view page control
        command.setScanning(scannerStateService.isScanning());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "podcastSettings";
    }

    @PostMapping
    protected ModelAndView doSubmitAction(
            @ModelAttribute(Attributes.Model.Command.VALUE) PodcastSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        if (!scannerStateService.isScanning()) {
            settingsFacade
                .staging(SKeys.podcast.updateInterval, Integer.parseInt(command.getInterval()));
            settingsFacade
                .staging(SKeys.podcast.episodeRetentionCount,
                        Integer.parseInt(command.getEpisodeRetentionCount()));
            settingsFacade
                .staging(SKeys.podcast.episodeDownloadCount,
                        Integer.parseInt(command.getEpisodeDownloadCount()));
            PathGeometry pathGeometry = EnvironmentProvider.getInstance().getPathGeometry();
            RootPathEntryGuard
                .validateFolderPath(pathGeometry, command.getFolder())
                .ifPresent(folder -> settingsFacade.staging(SKeys.podcast.folder, folder));

            settingsFacade.commitAll();
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        }

        return new ModelAndView(new RedirectView(ViewName.PODCAST_SETTINGS.value()));
    }
}
