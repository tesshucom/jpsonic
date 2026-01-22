package com.tesshu.jpsonic.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.persistence.core.entity.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CssUrlProviderImpl}.
 *
 * Verifies CSS path resolution based on: - context path - authenticated user
 * theme - system default theme
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class CssUrlProviderImplTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private PageContext pageContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserSettings userSettings;

    private CssUrlProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new CssUrlProviderImpl(securityService, settingsService);
        when(pageContext.getRequest()).thenReturn(request);
    }

    /**
     * When an authenticated user exists, the CSS path should be resolved using the
     * user's theme ID.
     *
     * Expected format: {contextPath}/style/jpsonic/{themeId}.css
     */
    @Test
    void returnsUserThemeCssPathWhenUserIsAuthenticated() {
        when(request.getContextPath()).thenReturn("/jpsonic");
        when(securityService.getCurrentUsername(request)).thenReturn("alice");
        when(securityService.getUserSettings("alice")).thenReturn(userSettings);
        when(userSettings.getThemeId()).thenReturn("dark");

        String cssUrl = provider.getCssUrl(pageContext);

        assertEquals("/jpsonic/style/jpsonic/dark.css", cssUrl);
    }

    /**
     * When no authenticated user exists, the CSS path should be resolved using the
     * system default theme.
     *
     * Expected format: {contextPath}/style/jpsonic/{themeId}.css
     */
    @Test
    void returnsSystemThemeCssPathWhenUserIsNotAuthenticated() {
        when(request.getContextPath()).thenReturn("");
        when(securityService.getCurrentUsername(request)).thenReturn(null);
        when(settingsService.getThemeId()).thenReturn("testTheme");

        String cssUrl = provider.getCssUrl(pageContext);

        assertEquals("/style/jpsonic/testTheme.css", cssUrl);
    }

    /**
     * When an authenticated user exists but the user's theme ID is blank, the
     * system default theme should be used as a fallback.
     */
    @Test
    void fallsBackToSystemThemeWhenUserThemeIsBlank() {
        when(request.getContextPath()).thenReturn("/jpsonic");
        when(securityService.getCurrentUsername(request)).thenReturn("bob");
        when(securityService.getUserSettings("bob")).thenReturn(userSettings);
        when(userSettings.getThemeId()).thenReturn("   ");

        String cssUrl = provider.getCssUrl(pageContext);
        assertEquals("/jpsonic/style/jpsonic/jpsonic.css", cssUrl);
    }

    @Test
    void returnsDefaultThemeWhenUserThemeIdIsNull() {
        when(request.getContextPath()).thenReturn("/context");
        when(securityService.getCurrentUsername(request)).thenReturn("user");
        when(securityService.getUserSettings("user")).thenReturn(userSettings);
        when(userSettings.getThemeId()).thenReturn(null);

        String cssUrl = provider.getCssUrl(pageContext);

        assertEquals("/context/style/jpsonic/jpsonic.css", cssUrl);
    }

    @Test
    void returnsDefaultThemeWhenUserThemeIdIsEmpty() {
        when(request.getContextPath()).thenReturn("/context");
        when(securityService.getCurrentUsername(request)).thenReturn("user");
        when(securityService.getUserSettings("user")).thenReturn(userSettings);
        when(userSettings.getThemeId()).thenReturn("");

        String cssUrl = provider.getCssUrl(pageContext);

        assertEquals("/context/style/jpsonic/jpsonic.css", cssUrl);
    }
}
