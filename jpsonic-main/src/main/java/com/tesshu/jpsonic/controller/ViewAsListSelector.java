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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestUtils;

@Component
public class ViewAsListSelector {

    private final SecurityService securityService;

    public ViewAsListSelector(SecurityService securityService) {
        super();
        this.securityService = securityService;
    }

    public boolean isViewAsList(HttpServletRequest request, String username) {
        UserSettings userSettings = securityService.getUserSettings(username);
        boolean viewAsList = ServletRequestUtils
            .getBooleanParameter(request, Attributes.Request.VIEW_AS_LIST.value(),
                    userSettings.isViewAsList());
        if (viewAsList != userSettings.isViewAsList()) {
            userSettings.setViewAsList(viewAsList);
            userSettings.setChanged(now());
            securityService.updateUserSettings(userSettings);
        }
        return viewAsList;
    }
}
