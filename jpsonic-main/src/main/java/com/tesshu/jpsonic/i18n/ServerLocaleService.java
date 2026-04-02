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

package com.tesshu.jpsonic.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.service.settings.SettingsFacade;
import com.tesshu.jpsonic.util.StringUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Provides access to the server-wide locale configuration.
 *
 * <p>
 * This service manages the <b>System Locale</b> stored in
 * {@link SettingsFacade}. It does not handle user-specific locales or
 * request-based locale resolution.
 * </p>
 *
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Load the list of available locales from {@code locales.txt} (lazy,
 * cached).</li>
 * <li>Construct and cache the current System Locale from settings.</li>
 * <li>Stage locale changes via {@link SettingsFacade#staging}, and invalidate
 * the cache.</li>
 * </ul>
 *
 */
@Service
public class ServerLocaleService {

    static final String LOCALES_FILE = "/com/tesshu/jpsonic/i18n/locales.txt";

    private final SettingsFacade settingsFacade;

    private final ReentrantLock availableLocalesLock = new ReentrantLock();
    private final AtomicReference<Locale> locale = new AtomicReference<>();
    private List<Locale> locales = new ArrayList<>();

    public ServerLocaleService(SettingsFacade settingsFacade) {
        super();
        this.settingsFacade = settingsFacade;
    }

    /**
     * Provides access to the server-wide locale configuration.
     *
     * <p>
     * This service manages the <b>System Locale</b> stored in
     * {@link SettingsFacade}. It does not handle user-specific locales or
     * request-based locale resolution.
     * </p>
     *
     * <p>
     * Responsibilities:
     * </p>
     * <ul>
     * <li>Load the list of available locales from {@code locales.txt} (lazy,
     * cached).</li>
     * <li>Construct and cache the current System Locale from settings.</li>
     * <li>Stage locale changes via {@link SettingsFacade#staging}, and invalidate
     * the cache.</li>
     * </ul>
     *
     */
    public List<Locale> getAvailableLocales() {
        if (!locales.isEmpty()) {
            return locales;
        }
        availableLocalesLock.lock();
        try {
            if (!locales.isEmpty()) {
                return locales;
            }

            try (InputStream in = ServerLocaleService.class.getResourceAsStream(LOCALES_FILE)) {
                locales = StringUtil.readLines(in).stream().map(StringUtil::parseLocale).toList();
                locales = Collections.unmodifiableList(locales);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return locales;
        } finally {
            availableLocalesLock.unlock();
        }
    }

    /**
     * Returns the server-wide default locale configured by the administrator.
     *
     * <p>
     * This locale is used in contexts where UserLocale cannot be applied, such as
     * the login screen, UPnP clients, or before UserLocale resolution.
     * </p>
     */
    @NonNull
    public Locale getLocale() {
        Locale cached = locale.get();
        if (cached != null) {
            return cached;
        }

        String lang = settingsFacade.get(I18nSKeys.localeLanguage);
        String country = settingsFacade.get(I18nSKeys.localeCountry);
        String variant = settingsFacade.get(I18nSKeys.localeVariant);

        Locale locale = new Locale.Builder()
            .setLanguage(lang)
            .setRegion(country)
            .setVariant(variant == null ? "" : variant)
            .build();

        this.locale.set(locale);
        return locale;
    }

    /**
     * Stages a new server locale without persisting it.
     *
     * <p>
     * The change is applied to the staging area only. Persistence is performed by
     * {@code SettingsFacade.commitAll()}.
     * </p>
     */
    public void stagingLocale(@NonNull Locale locale) {
        if (Objects.equals(getLocale(), locale)) {
            return;
        }

        settingsFacade.staging(I18nSKeys.localeLanguage, locale.getLanguage());
        settingsFacade.staging(I18nSKeys.localeCountry, locale.getCountry());
        settingsFacade.staging(I18nSKeys.localeVariant, locale.getVariant());

        this.locale.set(null);
    }
}
