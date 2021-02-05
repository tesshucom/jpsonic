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

package com.tesshu.jpsonic.filter;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.controller.WebFontUtils;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.context.support.WebApplicationContextUtils;

@DependsOn({ "settingsService", "securityService" })
public class FontSchemeFilter implements Filter {

    private SettingsService settingsService;
    private SecurityService securityService;

    private final List<String> excludes = Arrays.asList("/".concat(ViewName.COVER_ART.value()));

    @Override
    public void init(FilterConfig filterConfig) {
        settingsService = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext())
                .getBean(SettingsService.class);
        securityService = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext())
                .getBean(SecurityService.class);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        /*
         * There is a problem with AbstractAirsonicRestApiJukeboxIntTest and service is not set correctly. No
         * abnormalities are seen in all other tests. This judgment block can be deleted by improving
         * AbstractAirsonicRestApiJukeboxIntTest.
         */
        if (!excludes.contains(request.getServletPath()) && !isEmpty(settingsService) && !isEmpty(securityService)) {
            User user = securityService.getCurrentUser(request);
            UserSettings settings = isEmpty(user) ? null : settingsService.getUserSettings(user.getUsername());
            WebFontUtils.setToRequest(settings, request);
        }
        chain.doFilter(request, res);
    }

}
