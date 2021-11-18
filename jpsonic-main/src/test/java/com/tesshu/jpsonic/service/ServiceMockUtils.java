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

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.util.StringUtil;
import org.mockito.Mockito;

public final class ServiceMockUtils {

    public static final String ADMIN_NAME = "admin";

    private static final List<Transcoding> DEFAULT_TRANSCODINGS = ImmutableList.of(
            new Transcoding(0, "mp3 audio", "mp3 ogg oga aac m4a flac wav wma aif aiff ape mpc shn", "mp3",
                    "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -", null, null, true),
            new Transcoding(1, "flv/h264 video", "avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts", "flv",
                    "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -",
                    null, null, true),
            new Transcoding(2, "mkv video", "avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts", "mkv",
                    "ffmpeg -ss %o -i %s -c:v libx264 -preset superfast -b:v %bk -c:a libvorbis -f matroska -threads 0 -",
                    null, null, true),
            new Transcoding(3, "mp4/h264 video", "avi flv mpg mpeg m4v mkv mov wmv ogv divx m2ts", "mp4",
                    "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mp4 -vcodec libx264 -preset superfast -threads 0 -movflags frag_keyframe+empty_moov -",
                    null, null, true));

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
            Mockito.when(securityService.getUserSettings(ADMIN_NAME)).thenReturn(new UserSettings(ADMIN_NAME));

            User guestUser = new User(User.USERNAME_GUEST, User.USERNAME_GUEST, "");
            guestUser.setStreamRole(true);
            Mockito.when(securityService.getGuestUser()).thenReturn(guestUser);
            Mockito.when(securityService.getUserSettings(User.USERNAME_GUEST))
                    .thenReturn(new UserSettings(User.USERNAME_GUEST));

            Mockito.when(securityService.getUserSettings(JWTAuthenticationToken.USERNAME_ANONYMOUS))
                    .thenReturn(new UserSettings(JWTAuthenticationToken.USERNAME_ANONYMOUS));

            mock = securityService;
        } else if (PlayerService.class == classToMock) {
            PlayerService playerService = Mockito.mock(PlayerService.class);
            Player player = new Player();
            player.setId(99);
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
            Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
            Mockito.when(settingsService.isIgnoreFullWidth()).thenReturn(true);
            Mockito.when(settingsService.isDeleteDiacritic()).thenReturn(true);
            mock = settingsService;
        } else if (AirsonicLocaleResolver.class == classToMock) {
            String language = SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE.defaultValue;
            String country = SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY.defaultValue;
            String variant = SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT.defaultValue;
            Locale locale = new Locale(language, country, variant);
            AirsonicLocaleResolver localeResolver = Mockito.mock(AirsonicLocaleResolver.class);
            Mockito.when(localeResolver.resolveLocale(Mockito.nullable(HttpServletRequest.class))).thenReturn(locale);
            mock = localeResolver;
        } else if (TranscodingDao.class == classToMock) {
            TranscodingDao transcodingDao = Mockito.mock(TranscodingDao.class);
            Mockito.when(transcodingDao.getAllTranscodings()).thenReturn(DEFAULT_TRANSCODINGS);
            mock = transcodingDao;
        } else {
            mock = Mockito.mock(classToMock);
        }
        return (T) mock;
    }

}
