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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.infrastructure.NeedsHome;
import com.tesshu.jpsonic.service.settings.SettingsFacade;
import com.tesshu.jpsonic.service.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@NeedsHome
@SuppressWarnings("PMD.TooManyStaticImports")
class ServerLocaleServiceTest {

    @Test
    void testSettingsDefault() {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        assertEquals("ja", settingsFacade.get(I18nSKeys.localeLanguage));
        assertEquals("jp", settingsFacade.get(I18nSKeys.localeCountry));
        assertEquals("", settingsFacade.get(I18nSKeys.localeVariant));
    }

    @Test
    void loadsLocalesFromFile() {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        ServerLocaleService serverLocaleService = new ServerLocaleService(settingsFacade);

        // Ensures that locales.txt is loaded and parsed into Locale objects.
        List<Locale> locales = serverLocaleService.getAvailableLocales();

        // locales.txt must not be empty.
        assertFalse(locales.isEmpty());

        // ENGLISH must be included (historical behavior).
        assertTrue(locales.contains(Locale.ENGLISH));
    }

    @Test
    void returnsSameInstanceOnRepeatedCalls() {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        ServerLocaleService serverLocaleService = new ServerLocaleService(settingsFacade);
        List<Locale> first = serverLocaleService.getAvailableLocales();
        List<Locale> second = serverLocaleService.getAvailableLocales();
        assertSame(first, second);
    }

    @Test
    void preservesOrderDefinedInLocalesTxt() throws Exception {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        ServerLocaleService serverLocaleService = new ServerLocaleService(settingsFacade);

        // Reads locales.txt directly to determine the expected first locale.
        try (InputStream in = ServerLocaleService.class
            .getResourceAsStream(ServerLocaleService.LOCALES_FILE)) {

            assertNotNull(in);

            List<String> lines = StringUtil.readLines(in);

            // Extracts the first non-empty, non-comment line.
            String firstLocaleLine = lines
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .findFirst()
                .orElseThrow();

            Locale expectedFirst = StringUtil.parseLocale(firstLocaleLine);

            List<Locale> locales = serverLocaleService.getAvailableLocales();

            // The order in locales.txt must be preserved.
            assertEquals(expectedFirst, locales.get(0));
        }
    }

    @Test
    void returnsCachedLocale() {
        SettingsFacade facade = SettingsFacadeBuilder.create().buildWithDefault();
        ServerLocaleService svc = new ServerLocaleService(facade);
        Locale first = svc.getLocale();
        Locale second = svc.getLocale();
        assertSame(first, second);
    }

    @Test
    void stagingLocaleCallsStagingMethods() {
        ArgumentCaptor<String> lang = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> country = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> variant = ArgumentCaptor.forClass(String.class);
        SettingsFacade facade = SettingsFacadeBuilder
            .create()
            .captureString(I18nSKeys.localeLanguage, lang)
            .captureString(I18nSKeys.localeCountry, country)
            .captureString(I18nSKeys.localeVariant, variant)
            .build();

        ServerLocaleService svc = new ServerLocaleService(facade);
        svc
            .stagingLocale(
                    new Locale.Builder().setLanguage("en").setRegion("US").setVariant("").build());

        assertEquals("en", lang.getValue());
        assertEquals("US", country.getValue());
        assertNull(variant.getValue()); // Treated as null
    }

    @Test
    void emptyVariant() {
        SettingsFacade facade = SettingsFacadeBuilder.create().buildReal();
        ServerLocaleService serverLocaleService = new ServerLocaleService(facade);

        Locale defaultLocale = serverLocaleService.getLocale();
        assertEquals("ja", defaultLocale.getLanguage());
        assertEquals("JP", defaultLocale.getCountry());
        assertEquals("", defaultLocale.getVariant());

        serverLocaleService
            .stagingLocale(
                    new Locale.Builder().setLanguage("en").setRegion("US").setVariant("").build());
        facade.commitAll();

        Locale sanitized = serverLocaleService.getLocale();
        assertEquals("en", sanitized.getLanguage());
        assertEquals("US", sanitized.getCountry());
        assertEquals("", sanitized.getVariant()); // Treated as brank
    }
}
