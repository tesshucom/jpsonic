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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.util.StringUtil;
import org.mockito.Mockito;

public final class ServiceMockUtils {

    public static final String ADMIN_NAME = "admin";

    private ServiceMockUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> classToMock) {
        Object mock;
        if (SecurityService.class == classToMock) {
            SecurityService securityService = Mockito.mock(SecurityService.class);
            Mockito.when(securityService.getCurrentUsername(Mockito.nullable(HttpServletRequest.class)))
                    .thenReturn(ADMIN_NAME);
            Mockito.when(securityService.getCurrentUser(Mockito.nullable(HttpServletRequest.class)))
                    .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ""));
            UserSettings settings = new UserSettings(ADMIN_NAME);
            Mockito.when(securityService.getUserSettings(ADMIN_NAME)).thenReturn(settings);
            mock = securityService;
        } else if (PlayerService.class == classToMock) {
            PlayerService playerService = Mockito.mock(PlayerService.class);
            Player player = new Player();
            player.setUsername(User.USERNAME_GUEST);
            Mockito.when(playerService.getGuestPlayer(Mockito.nullable(HttpServletRequest.class))).thenReturn(player);
            mock = playerService;
        } else if (SettingsService.class == classToMock) {
            SettingsService settingsService = Mockito.mock(SettingsService.class);
            Mockito.when(settingsService.getThemeId())
                    .thenReturn(SettingsConstants.General.ThemeAndLang.THEME_ID.defaultValue);
            String language = SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE.defaultValue;
            String country = SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY.defaultValue;
            String variant = SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT.defaultValue;
            Locale locale = new Locale(language, country, variant);
            Mockito.when(settingsService.getAvailableLocales()).thenReturn(new Locale[] { locale });
            Mockito.when(settingsService.getLocale()).thenReturn(locale);
            Mockito.when(settingsService.getIndexString())
                    .thenReturn(SettingsConstants.General.Index.INDEX_STRING.defaultValue);
            Mockito.when(settingsService.getIgnoredArticles())
                    .thenReturn(SettingsConstants.General.Index.IGNORED_ARTICLES.defaultValue);
            Mockito.when(settingsService.getShortcuts())
                    .thenReturn(SettingsConstants.General.Extension.SHORTCUTS.defaultValue);
            Mockito.when(settingsService.getShortcutsAsArray())
                    .thenReturn(StringUtil.split(SettingsConstants.General.Extension.SHORTCUTS.defaultValue));
            Mockito.when(settingsService.isGettingStartedEnabled()).thenReturn(false);
            Mockito.when(settingsService.getFileModifiedCheckSchemeName())
                    .thenReturn(FileModifiedCheckScheme.LAST_MODIFIED.name());
            Mockito.when(settingsService.getJWTKey()).thenReturn("SomeKey");
            mock = settingsService;
        } else if (AirsonicLocaleResolver.class == classToMock) {
            String language = SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE.defaultValue;
            String country = SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY.defaultValue;
            String variant = SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT.defaultValue;
            Locale locale = new Locale(language, country, variant);
            AirsonicLocaleResolver localeResolver = Mockito.mock(AirsonicLocaleResolver.class);
            Mockito.when(localeResolver.resolveLocale(Mockito.nullable(HttpServletRequest.class))).thenReturn(locale);
            mock = localeResolver;
        } else {
            mock = Mockito.mock(classToMock);
        }
        return (T) mock;
    }

}
