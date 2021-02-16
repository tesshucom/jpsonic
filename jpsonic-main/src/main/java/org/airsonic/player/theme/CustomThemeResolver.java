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

package org.airsonic.player.theme;

import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.airsonic.player.domain.Theme;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ThemeResolver;

/**
 * Theme resolver implementation which returns the theme selected in the settings.
 *
 * @author Sindre Mehus
 */
@Component("themeResolver")
public class CustomThemeResolver implements ThemeResolver {

    private static final Object LOCK = new Object();

    private final SecurityService securityService;
    private final SettingsService settingsService;

    private Set<String> themeIds;

    public CustomThemeResolver(SecurityService securityService, SettingsService settingsService) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    /**
     * Resolve the current theme name via the given request.
     *
     * @param request
     *            Request to be used for resolution
     * 
     * @return The current theme name
     */
    @Override
    public String resolveThemeName(HttpServletRequest request) {
        String themeId = (String) request.getAttribute("airsonic.theme");
        if (themeId != null) {
            return themeId;
        }

        // Optimization: Cache theme in the request.
        themeId = doResolveThemeName(request);
        request.setAttribute("airsonic.theme", themeId);

        return themeId;
    }

    private String doResolveThemeName(HttpServletRequest request) {
        String themeId = null;

        // Look for user-specific theme.
        String username = securityService.getCurrentUsername(request);
        if (username != null) {
            UserSettings userSettings = settingsService.getUserSettings(username);
            if (userSettings != null) {
                themeId = userSettings.getThemeId();
            }
        }

        if (themeId != null && themeExists(themeId)) {
            return themeId;
        }

        // Return system theme.
        themeId = settingsService.getThemeId();
        return themeExists(themeId) ? themeId : "jpsonic";
    }

    /**
     * Returns whether the theme with the given ID exists.
     * 
     * @param themeId
     *            The theme ID.
     * 
     * @return Whether the theme with the given ID exists.
     */
    private boolean themeExists(String themeId) {
        synchronized (LOCK) {
            if (themeIds == null) {
                themeIds = SettingsService.getAvailableThemes().stream().map(Theme::getId).collect(Collectors.toSet());
            }
        }
        return themeIds.contains(themeId);
    }

    /**
     * Set the current theme name to the given one. This method is not supported.
     *
     * @param request
     *            Request to be used for theme name modification
     * @param response
     *            Response to be used for theme name modification
     * @param themeName
     *            The new theme name
     * 
     * @throws UnsupportedOperationException
     *             If the ThemeResolver implementation does not support dynamic changing of the theme
     */
    @Override
    public void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName) {
        throw new UnsupportedOperationException("Cannot change theme - use a different theme resolution strategy");
    }
}
