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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.theme;

import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.taglib.CssUrlProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.PageContext;
import org.springframework.stereotype.Component;

/**
 * Implementation of CssUrlProvider that resolves the CSS URL based on user
 * settings or system default settings.
 */
@Component
public class CssUrlProviderImpl implements CssUrlProvider {

    private static final String CSS_BASE_PATH = "/style/jpsonic/";
    private static final String CSS_SUFFIX = ".css";
    private static final String DEFAULT_THEME_ID = "jpsonic";

    private final SecurityService securityService;
    private final ServerThemeService serverThemeService;

    public CssUrlProviderImpl(SecurityService securityService,
            ServerThemeService serverThemeService) {
        this.securityService = securityService;
        this.serverThemeService = serverThemeService;
    }

    /**
     * Returns the CSS URL for the current request or user settings.
     *
     * @param jspContext the JSP context
     * @return the resolved CSS URL
     */
    @Override
    public String getCssUrl(JspContext jspContext) {
        PageContext pageContext = (PageContext) jspContext;
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        String contextPath = request.getContextPath();
        String username = securityService.getCurrentUsername(request);
        String themeId = username != null ? securityService.getUserSettings(username).getThemeId()
                : serverThemeService.getThemeId();

        return generateCssPath(contextPath, themeId);
    }

    private String generateCssPath(String contextPath, String themeId) {
        String resolvedThemeId = (themeId == null || themeId.isBlank()) ? DEFAULT_THEME_ID
                : themeId;

        return contextPath + CSS_BASE_PATH + resolvedThemeId + CSS_SUFFIX;
    }
}
