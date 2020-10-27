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
package com.tesshu.jpsonic.filter;

import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@DependsOn({ "settingsService", "securityService" })
public class FontSchemeFilter implements Filter {

    private SettingsService settingsService;
    private SecurityService securityService;

    @Override
    public void init(FilterConfig filterConfig) {
        settingsService = WebApplicationContextUtils
                .getRequiredWebApplicationContext(filterConfig.getServletContext())
                .getBean(SettingsService.class);
        securityService = WebApplicationContextUtils
                .getRequiredWebApplicationContext(filterConfig.getServletContext())
                .getBean(SecurityService.class);
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        /*
         * There is a problem with AbstractAirsonicRestApiJukeboxIntTest and service is
         * not set correctly. No abnormalities are seen in all other tests. This
         * judgment block can be deleted by improving
         * AbstractAirsonicRestApiJukeboxIntTest.
         */
        if (!isEmpty(settingsService) && !isEmpty(securityService)) {
            User user = securityService.getCurrentUser(request);
            if (!isEmpty(user)) {
                UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
                request.setAttribute("filter.fontSchemeName", userSettings.getFontSchemeName());
            }
        }
        chain.doFilter(request, res);
    }

}
