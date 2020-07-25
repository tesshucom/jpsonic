/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/uploadEntry")
public class UploadEntryController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);

        String uploadDirectory = null;
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        if (!musicFolders.isEmpty()) {
            uploadDirectory = new File(musicFolders.get(0).getPath(), "Incoming").getPath();
        }

        ModelAndView result = new ModelAndView();
        result.addObject("model", LegacyMap.of(
                "user", user,
                "uploadDirectory", uploadDirectory,
                "musicFolders", musicFolders));
        return result;
    }
}
