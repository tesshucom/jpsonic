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

import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import com.tesshu.jpsonic.infrastructure.core.NeedsHome;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NeedsHome
class ServerLocaleServiceRealTest {

    private SettingsFacade settingsFacade;
    private ServerLocaleService serverLocaleService;

    @BeforeEach
    void setUp() {
        settingsFacade = SettingsFacadeBuilder.create().buildReal();
        serverLocaleService = new ServerLocaleService(settingsFacade);
    }

    @Test
    void testSettingsDefault() {
        Locale locale = serverLocaleService.getLocale();
        assertNotNull(locale);
        assertEquals("ja", settingsFacade.get(I18nSKeys.localeLanguage));
        assertEquals("jp", settingsFacade.get(I18nSKeys.localeCountry));
        assertEquals("", settingsFacade.get(I18nSKeys.localeVariant));
    }

    @Test
    void stagingLocaleInvalidatesCacheAndChangesLocale() {
        Locale first = serverLocaleService.getLocale();
        serverLocaleService
            .stagingLocale(
                    new Locale.Builder().setLanguage("en").setRegion("US").setVariant("").build());
        Locale second = serverLocaleService.getLocale();

        assertNotSame(first, second);
        assertEquals("en", second.getLanguage());
        assertEquals("US", second.getCountry());
    }
}
