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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestUtils;

@Component
public class OutlineHelpSelector {

    private final SecurityService securityService;

    public OutlineHelpSelector(SecurityService securityService) {
        super();
        this.securityService = securityService;
    }

    public boolean isShowOutlineHelp(HttpServletRequest request, String username) {
        UserSettings userSettings = securityService.getUserSettings(username);
        boolean showOutlineHelp = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.SHOW_OUTLINE_HELP.value(), userSettings.isShowOutlineHelp());
        if (showOutlineHelp != userSettings.isShowOutlineHelp()) {
            userSettings.setShowOutlineHelp(showOutlineHelp);
            userSettings.setChanged(new Date());
            securityService.updateUserSettings(userSettings);
        }
        return showOutlineHelp;
    }
}
