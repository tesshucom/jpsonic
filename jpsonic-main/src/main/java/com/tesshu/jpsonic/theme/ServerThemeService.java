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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.util.StringUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Provides access to the server-wide theme configuration.
 *
 * <p>
 * This service manages the <b>System Theme</b> stored in
 * {@link SettingsFacade}. It does not handle user-specific theme preferences or
 * request-based theme resolution.
 * </p>
 *
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Load the list of available themes from {@code themes.txt} (lazy,
 * cached).</li>
 * <li>Provide access to the currently configured System Theme ID.</li>
 * <li>Stage theme changes via {@link SettingsFacade#staging}.</li>
 * </ul>
 */
@Service
public class ServerThemeService {

    private static final String THEMES_FILE = "/com/tesshu/jpsonic/theme/themes.txt";

    private final SettingsFacade settingsFacade;

    private final ReentrantLock availableThemesLock = new ReentrantLock();
    private List<Theme> themes = new ArrayList<>();

    public ServerThemeService(SettingsFacade settingsFacade) {
        super();
        this.settingsFacade = settingsFacade;
    }

    /**
     * Returns the list of themes available to the server.
     *
     * <p>
     * The list is loaded lazily from {@code themes.txt} and cached as an
     * unmodifiable list. Subsequent calls return the cached instance.
     * </p>
     */
    public List<Theme> getAvailableThemes() {
        if (!themes.isEmpty()) {
            return themes;
        }

        availableThemesLock.lock();
        try {
            if (!themes.isEmpty()) {
                return themes;
            }

            try (InputStream in = ServerThemeService.class.getResourceAsStream(THEMES_FILE)) {
                themes = StringUtil
                    .readLines(in)
                    .stream()
                    .map(StringUtil::split)
                    .map(this::parseTheme)
                    .toList();
                themes = Collections.unmodifiableList(themes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return themes;
        } finally {
            availableThemesLock.unlock();
        }
    }

    private Theme parseTheme(List<String> elements) {
        return new Theme(elements.get(0), elements.get(1));
    }

    /**
     * Returns the server-wide default theme configured by the administrator.
     *
     * <p>
     * This theme ID is used in contexts where no user-specific theme applies.
     * </p>
     */
    public @NonNull String getThemeId() {
        return settingsFacade.get(ThemeSKeys.themeId);
    }

    /**
     * Stages a new server theme without persisting it.
     *
     * <p>
     * The change is applied to the staging area only. Persistence is performed by
     * {@code SettingsFacade.commitAll()}.
     * </p>
     */
    public void stagingThemeId(@NonNull String s) {
        settingsFacade.staging(ThemeSKeys.themeId, s);
    }
}
