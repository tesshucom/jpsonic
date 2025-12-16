package com.tesshu.jpsonic.theme;

import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
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
    private final SettingsService settingsService;

    public CssUrlProviderImpl(SecurityService securityService, SettingsService settingsService) {
        this.securityService = securityService;
        this.settingsService = settingsService;
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
                : settingsService.getThemeId();

        return generateCssPath(contextPath, themeId);
    }

    private String generateCssPath(String contextPath, String themeId) {
        String resolvedThemeId = (themeId == null || themeId.isBlank()) ? DEFAULT_THEME_ID
                : themeId;

        return contextPath + CSS_BASE_PATH + resolvedThemeId + CSS_SUFFIX;
    }
}
