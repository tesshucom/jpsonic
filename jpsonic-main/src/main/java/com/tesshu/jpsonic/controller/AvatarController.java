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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.Avatar;
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.SecurityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;

/**
 * Controller which produces avatar images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/avatar.view")
public class AvatarController implements LastModified {

    private final SecurityService securityService;
    private final AvatarService avatarService;

    public AvatarController(SecurityService securityService, AvatarService avatarService) {
        super();
        this.securityService = securityService;
        this.avatarService = avatarService;
    }

    @Override
    public long getLastModified(HttpServletRequest request) {
        Avatar avatar = getAvatar(request);
        long result = avatar == null ? -1L : avatar.getCreatedDate().getTime();

        String username = request.getParameter(Attributes.Request.USER_NAME.value());
        if (username != null) {
            UserSettings userSettings = securityService.getUserSettings(username);
            result = Math.max(result, userSettings.getChanged().getTime());
        }

        return result;
    }

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Avatar avatar = getAvatar(request);

        if (avatar == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(avatar.getMimeType());
        response.getOutputStream().write(avatar.getData());
    }

    private Avatar getAvatar(HttpServletRequest request) {
        String id = request.getParameter(Attributes.Request.ID.value());
        if (id != null) {
            return avatarService.getSystemAvatar(Integer.parseInt(id));
        }

        String username = request.getParameter(Attributes.Request.USER_NAME.value());
        if (username == null) {
            return null;
        }

        boolean forceCustom = ServletRequestUtils.getBooleanParameter(request, Attributes.Request.FORCE_CUSTOM.value(),
                false);
        UserSettings userSettings = securityService.getUserSettings(username);
        if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM || forceCustom) {
            return avatarService.getCustomAvatar(username);
        }
        if (userSettings.getAvatarScheme() == AvatarScheme.NONE) {
            return null;
        }
        return avatarService.getSystemAvatar(userSettings.getSystemAvatarId());
    }

}
