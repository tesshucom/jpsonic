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

import com.tesshu.jpsonic.command.PodcastSettingsCommand;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.SettingsService;
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

    private final SettingsService settingsService;
    private final MediaScannerService mediaScannerService;

    public PodcastSettingsController(SettingsService settingsService, MediaScannerService mediaScannerService) {
        super();
        this.settingsService = settingsService;
        this.mediaScannerService = mediaScannerService;
    }

    @GetMapping
    protected String formBackingObject(Model model) {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        command.setInterval(String.valueOf(settingsService.getPodcastUpdateInterval()));
        command.setEpisodeRetentionCount(String.valueOf(settingsService.getPodcastEpisodeRetentionCount()));
        command.setEpisodeDownloadCount(String.valueOf(settingsService.getPodcastEpisodeDownloadCount()));
        command.setFolder(settingsService.getPodcastFolder());

        // for view page control
        command.setScanning(mediaScannerService.isScanning());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "podcastSettings";
    }

    @PostMapping
    protected ModelAndView doSubmitAction(
            @ModelAttribute(Attributes.Model.Command.VALUE) PodcastSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        if (!mediaScannerService.isScanning()) {
            settingsService.setPodcastUpdateInterval(Integer.parseInt(command.getInterval()));
            settingsService.setPodcastEpisodeRetentionCount(Integer.parseInt(command.getEpisodeRetentionCount()));
            settingsService.setPodcastEpisodeDownloadCount(Integer.parseInt(command.getEpisodeDownloadCount()));
            settingsService.setPodcastFolder(command.getFolder());
            settingsService.save();
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        }

        return new ModelAndView(new RedirectView(ViewName.PODCAST_SETTINGS.value()));
    }
}
