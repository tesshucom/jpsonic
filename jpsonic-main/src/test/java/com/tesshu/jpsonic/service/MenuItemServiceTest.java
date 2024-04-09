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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MenuItemServiceTest {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MenuItemService menuItemService;

    @BeforeEach
    public void setup() throws URISyntaxException {
        Locale otherThanEnJp = settingsService.getAvailableLocales().get(5);
        assertEquals("ca", otherThanEnJp.getLanguage());
        settingsService.setLocale(otherThanEnJp);
        settingsService.save();
    }

    @Test
    void testGetTopMenuItemCount() {
        assertEquals(8, menuItemService.getTopMenuItemCount(ViewType.UPNP));
    }

    @Test
    void testGetTopMenuItems() {
        List<MenuItem> menuItems = menuItemService.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE);
        assertEquals(menuItemService.getTopMenuItemCount(ViewType.UPNP), menuItems.size());
        assertEquals("Folder", menuItems.get(0).getName());
        assertEquals("Album Artist", menuItems.get(1).getName());
        assertEquals("Album", menuItems.get(2).getName());
        assertEquals("Genre", menuItems.get(3).getName());
        assertEquals("Podcast", menuItems.get(4).getName());
        assertEquals("Playlists", menuItems.get(5).getName());
        assertEquals("Recently", menuItems.get(6).getName());
        assertEquals("Shuffle", menuItems.get(7).getName());
    }

    @Test
    void testGetChildlenOf() {
        List<MenuItem> menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.FOLDER, false, 0,
                Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());
        assertEquals("With Index", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ARTIST, false, 0, Integer.MAX_VALUE);
        assertEquals(3, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());
        assertEquals("By Folder", menuItems.get(1).getName());
        assertEquals("With Index", menuItems.get(2).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ALBUM, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("Music Genres", menuItems.get(0).getName());
        assertEquals("Subsonic Style", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PODCAST, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Channels List", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PLAYLISTS, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.RECENTLY, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("Recently Added Albums", menuItems.get(0).getName());
        assertEquals("Recently Tagged Albums", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.SHUFFLE, false, 0, Integer.MAX_VALUE);
        assertEquals(4, menuItems.size());
        assertEquals("Music", menuItems.get(0).getName());
        assertEquals("Music By Artist", menuItems.get(1).getName());
        assertEquals("Music By Folder/Artist", menuItems.get(2).getName());
        assertEquals("Album", menuItems.get(3).getName());
    }
}
