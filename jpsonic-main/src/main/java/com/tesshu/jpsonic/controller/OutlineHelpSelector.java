/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */
package com.tesshu.jpsonic.controller;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;

@Component
public class OutlineHelpSelector {

    @Autowired
    private SettingsService settingsService;

    public boolean isShowOutlineHelp(HttpServletRequest request, String username) {
        UserSettings userSettings = settingsService.getUserSettings(username);
        boolean showOutlineHelp = ServletRequestUtils.getBooleanParameter(request, "showOutlineHelp",
                userSettings.isShowOutlineHelp());
        if (showOutlineHelp != userSettings.isShowOutlineHelp()) {
            userSettings.setShowOutlineHelp(showOutlineHelp);
            userSettings.setChanged(new Date());
            settingsService.updateUserSettings(userSettings);
        }
        return showOutlineHelp;
    }
}
