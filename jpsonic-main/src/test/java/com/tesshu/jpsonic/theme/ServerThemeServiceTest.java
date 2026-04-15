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

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServerThemeServiceTest {

    @Test
    void testGetAvailableThemesLoadsOnce() {
        SettingsFacade facade = mock(SettingsFacade.class);
        ServerThemeService service = new ServerThemeService(facade);

        List<Theme> first = service.getAvailableThemes();
        List<Theme> second = service.getAvailableThemes();

        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.add(null));
    }

    @Test
    void testGetThemeId() {
        SettingsFacade facade = SettingsFacadeBuilder
            .create()
            .withString(ThemeSKeys.themeId, "dark")
            .build();

        ServerThemeService service = new ServerThemeService(facade);
        assertEquals("dark", service.getThemeId());
    }

    @Test
    void testGetDefaultThemeId() {
        SettingsFacade facade = SettingsFacadeBuilder.create().buildWithDefault();
        assertEquals("jpsonic", facade.get(ThemeSKeys.themeId));
        ServerThemeService service = new ServerThemeService(facade);
        assertEquals("jpsonic", service.getThemeId());
    }

    @Test
    void testStagingThemeId() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        SettingsFacade facade = SettingsFacadeBuilder
            .create()
            .captureString(ThemeSKeys.themeId, captor)
            .build();
        ServerThemeService service = new ServerThemeService(facade);
        service.stagingThemeId("light");
        assertEquals(1, captor.getAllValues().size());
        assertEquals("light", captor.getValue());
    }
}
