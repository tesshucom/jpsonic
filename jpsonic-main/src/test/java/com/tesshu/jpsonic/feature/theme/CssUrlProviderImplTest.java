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

package com.tesshu.jpsonic.feature.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.persistence.core.entity.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
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
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.SingularField" })
class CssUrlProviderImplTest {

    @Mock
    private SecurityService securityService;
    @Mock
    private PageContext pageContext;
    @Mock
    private HttpServletRequest request;
    @Mock
    private UserSettings userSettings;
    private SettingsFacade settingsFacade;
    private CssUrlProviderImpl provider;

    @BeforeEach
    void setUp() {
        settingsFacade = SettingsFacadeBuilder.create().build();
        ServerThemeService serverThemeService = new ServerThemeService(settingsFacade);
        provider = new CssUrlProviderImpl(securityService, serverThemeService);
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
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(ThemeSKeys.themeId, "testTheme")
            .build();
        ServerThemeService serverThemeService = new ServerThemeService(settingsFacade);
        provider = new CssUrlProviderImpl(securityService, serverThemeService);

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
