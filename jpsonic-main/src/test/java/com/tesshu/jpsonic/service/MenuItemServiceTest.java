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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MenuItemDao;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import com.tesshu.jpsonic.service.MenuItemService.MenuItemWithDefaultName;
import com.tesshu.jpsonic.service.MenuItemService.ResetMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    MenuItemDao menuItemDao;
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
    void testGetTopMenuItemsWithType() {
        List<MenuItemWithDefaultName> menuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
        assertEquals(menuItemService.getTopMenuItemCount(ViewType.UPNP), menuItems.size());
        assertEquals("Folder", menuItems.get(0).getName());
        assertEquals("Album Artist", menuItems.get(1).getName());
        assertEquals("Album", menuItems.get(2).getName());
        assertEquals("Genre", menuItems.get(3).getName());
        assertEquals("Podcast", menuItems.get(4).getName());
        assertEquals("Playlists", menuItems.get(5).getName());
        assertEquals("Recently", menuItems.get(6).getName());
        assertEquals("Shuffle", menuItems.get(7).getName());

        assertEquals("Folder", menuItems.get(0).getDefaultName());
        assertEquals("Album Artist", menuItems.get(1).getDefaultName());
        assertEquals("Album", menuItems.get(2).getDefaultName());
        assertEquals("Genre", menuItems.get(3).getDefaultName());
        assertEquals("Podcast", menuItems.get(4).getDefaultName());
        assertEquals("Playlists", menuItems.get(5).getDefaultName());
        assertEquals("Recently", menuItems.get(6).getDefaultName());
        assertEquals("Shuffle", menuItems.get(7).getDefaultName());
    }

    @Test
    void testGetChildlenOf() {
        List<MenuItem> menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.FOLDER, false, 0,
                Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("File Structure", menuItems.get(0).getName());
        assertEquals("Folder with Index", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ARTIST, false, 0, Integer.MAX_VALUE);
        assertEquals(3, menuItems.size());
        assertEquals("Album Artist All", menuItems.get(0).getName());
        assertEquals("Album Artist All(by Folder)", menuItems.get(1).getName());
        assertEquals("Album Artist with Index", menuItems.get(2).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ALBUM, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Album All(ID3)", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("All Music by Genre", menuItems.get(0).getName());
        assertEquals("Albums by Genre(File Structure)", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PODCAST, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Podcast Channels All", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PLAYLISTS, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Playlists All", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.RECENTLY, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("Recently Added Albums", menuItems.get(0).getName());
        assertEquals("Recently Tagged Albums", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.SHUFFLE, false, 0, Integer.MAX_VALUE);
        assertEquals(4, menuItems.size());
        assertEquals("Random Music", menuItems.get(0).getName());
        assertEquals("Random Music by Artist(ID3)", menuItems.get(1).getName());
        assertEquals("Random Music by Artist(by Folder/ID3)", menuItems.get(2).getName());
        assertEquals("Random Album(ID3)", menuItems.get(3).getName());
    }

    @Test
    void testUpdateMenuItem() {
        MenuItem menuItem = menuItemService.getMenuItem(MenuItemId.FOLDER);
        assertTrue(menuItem.getName().isBlank());

        // Blank if the same value as the default name is attempted to be registered.
        menuItem.setName("Folder");
        menuItemService.updateMenuItem(menuItem);
        menuItem = menuItemService.getMenuItem(MenuItemId.FOLDER);
        assertTrue(menuItem.getName().isBlank());

        menuItem.setName("Folder2");
        menuItemService.updateMenuItem(menuItem);
        menuItem = menuItemService.getMenuItem(MenuItemId.FOLDER);
        assertFalse(menuItem.getName().isBlank());
        assertEquals("Folder2", menuItem.getName());

        menuItem.setName("Folder");
        menuItemService.updateMenuItem(menuItem);
        menuItem = menuItemService.getMenuItem(MenuItemId.FOLDER);
        assertTrue(menuItem.getName().isBlank());
    }

    @Nested
    class EnsureUPnPSubMenuEnabledTest {

        @Test
        void testDoNothing() {
            int topMenuItemCount = menuItemService.getTopMenuItemCount(ViewType.UPNP);
            int enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(menuItem -> menuItem.isEnabled()).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
            menuItemService.ensureUPnPSubMenuEnabled();
            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(menuItem -> menuItem.isEnabled()).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
        }

        @Test
        void testEnsureUPnPSubMenuEnabled() {
            int topMenuItemCount = menuItemService.getTopMenuItemCount(ViewType.UPNP);
            int enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(menuItem -> menuItem.isEnabled()).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);

            menuItemService.getSubMenuItems(ViewType.UPNP).stream().forEach(menuItem -> {
                menuItem.setEnabled(false);
                menuItemService.updateMenuItem(menuItem);
            });

            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(menuItem -> menuItem.isEnabled()).count();
            assertEquals(0, enabledSubMenuCount);

            menuItemService.ensureUPnPSubMenuEnabled();
            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(menuItem -> menuItem.isEnabled()).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
        }
    }

    @Test
    void testUpdateMenuItems() {
        List<MenuItemWithDefaultName> topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> assertTrue(menuItem.isEnabled()), () -> fail());
        int enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                .filter(menuItem -> menuItem.isEnabled()).count();
        assertEquals(topMenuItems.size(), enabledSubMenuCount);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresent(menuItem -> menuItem.setEnabled(false));

        List<MenuItemWithDefaultName> subMenuItems = menuItemService.getSubMenuItems(ViewType.UPNP);
        subMenuItems.stream().forEach(menuItem -> menuItem.setEnabled(false));

        menuItemService.updateMenuItems(Stream.concat(topMenuItems.stream(), subMenuItems.stream()));

        topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> assertFalse(menuItem.isEnabled()), () -> fail());
        enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                .filter(menuItem -> menuItem.isEnabled()).count();
        assertEquals(topMenuItems.size(), enabledSubMenuCount);

        // tearDown
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> {
                    menuItem.setEnabled(true);
                    menuItemService.updateMenuItem(menuItem);
                }, () -> fail());
    }

    @Nested
    class UpdateMenuItemOrderTest {

        @Test
        void testUpdateMenuItemOrder() {
            List<MenuItemWithDefaultName> topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
            assertEquals(MenuItemId.FOLDER, topMenuItems.get(0).getId());
            assertEquals(MenuItemId.ARTIST, topMenuItems.get(1).getId());
            assertEquals(MenuItemId.ALBUM, topMenuItems.get(2).getId());
            assertEquals(MenuItemId.GENRE, topMenuItems.get(3).getId());
            assertEquals(MenuItemId.PODCAST, topMenuItems.get(4).getId());
            assertEquals(MenuItemId.PLAYLISTS, topMenuItems.get(5).getId());
            assertEquals(MenuItemId.RECENTLY, topMenuItems.get(6).getId());
            assertEquals(MenuItemId.SHUFFLE, topMenuItems.get(7).getId());

            menuItemService.updateMenuItemOrder(ViewType.UPNP, MenuItemId.ARTIST.value());

            topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
            assertEquals(MenuItemId.ARTIST, topMenuItems.get(0).getId());
            assertEquals(MenuItemId.FOLDER, topMenuItems.get(1).getId());
            assertEquals(MenuItemId.ALBUM, topMenuItems.get(2).getId());
            assertEquals(MenuItemId.GENRE, topMenuItems.get(3).getId());
            assertEquals(MenuItemId.PODCAST, topMenuItems.get(4).getId());
            assertEquals(MenuItemId.PLAYLISTS, topMenuItems.get(5).getId());
            assertEquals(MenuItemId.RECENTLY, topMenuItems.get(6).getId());
            assertEquals(MenuItemId.SHUFFLE, topMenuItems.get(7).getId());

            menuItemService.resetMenuItem(ViewType.UPNP, ResetMode.TOP_MENU);
        }

        @Test
        void testFirstItem() {
            List<MenuItemWithDefaultName> topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
            assertEquals(MenuItemId.FOLDER, topMenuItems.get(0).getId());
            assertEquals(MenuItemId.ARTIST, topMenuItems.get(1).getId());
            assertEquals(MenuItemId.ALBUM, topMenuItems.get(2).getId());
            assertEquals(MenuItemId.GENRE, topMenuItems.get(3).getId());
            assertEquals(MenuItemId.PODCAST, topMenuItems.get(4).getId());
            assertEquals(MenuItemId.PLAYLISTS, topMenuItems.get(5).getId());
            assertEquals(MenuItemId.RECENTLY, topMenuItems.get(6).getId());
            assertEquals(MenuItemId.SHUFFLE, topMenuItems.get(7).getId());

            menuItemService.updateMenuItemOrder(ViewType.UPNP, MenuItemId.FOLDER.value());

            topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
            assertEquals(MenuItemId.FOLDER, topMenuItems.get(0).getId());
            assertEquals(MenuItemId.ARTIST, topMenuItems.get(1).getId());
            assertEquals(MenuItemId.ALBUM, topMenuItems.get(2).getId());
            assertEquals(MenuItemId.GENRE, topMenuItems.get(3).getId());
            assertEquals(MenuItemId.PODCAST, topMenuItems.get(4).getId());
            assertEquals(MenuItemId.PLAYLISTS, topMenuItems.get(5).getId());
            assertEquals(MenuItemId.RECENTLY, topMenuItems.get(6).getId());
            assertEquals(MenuItemId.SHUFFLE, topMenuItems.get(7).getId());
        }
    }

    @Test
    void testResetMenuItem() {
        Function<List<MenuItem>, Boolean> validateDefaultSubMenuItems = (subMenuItems) -> {
            assertEquals(MenuItemId.MEDIA_FILE, subMenuItems.get(0).getId());
            assertEquals(MenuItemId.INDEX, subMenuItems.get(1).getId());
            assertEquals(MenuItemId.ALBUM_ARTIST, subMenuItems.get(2).getId());
            assertEquals(MenuItemId.ALBUM_ARTIST_BY_FOLDER, subMenuItems.get(3).getId());
            assertEquals(MenuItemId.INDEX_ID3, subMenuItems.get(4).getId());
            assertEquals(MenuItemId.ALBUM_ID3, subMenuItems.get(5).getId());
            assertEquals(MenuItemId.SONG_BY_GENRE, subMenuItems.get(6).getId());
            assertEquals(MenuItemId.ALBUM_BY_GENRE, subMenuItems.get(7).getId());
            assertEquals(MenuItemId.PODCAST_DEFALT, subMenuItems.get(8).getId());
            assertEquals(MenuItemId.PLAYLISTS_DEFALT, subMenuItems.get(9).getId());
            assertEquals(MenuItemId.RECENTLY_ADDED_ALBUM, subMenuItems.get(10).getId());
            assertEquals(MenuItemId.RECENTLY_TAGGED_ALBUM, subMenuItems.get(11).getId());
            assertEquals(MenuItemId.RANDOM_SONG, subMenuItems.get(12).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_ARTIST, subMenuItems.get(13).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_FOLDER_ARTIST, subMenuItems.get(14).getId());
            assertEquals(MenuItemId.RANDOM_ALBUM, subMenuItems.get(15).getId());
            subMenuItems.forEach(menuItem -> {
                assertTrue(menuItem.getName().isBlank());
                boolean enabled = switch (menuItem.getId()) {
                case MEDIA_FILE, ALBUM_ARTIST, ALBUM_ID3, SONG_BY_GENRE, PODCAST_DEFALT, PLAYLISTS_DEFALT, RECENTLY_ADDED_ALBUM, RANDOM_SONG -> true;
                default -> false;
                };
                assertEquals(enabled, menuItem.isEnabled());
            });
            return true;
        };

        List<MenuItem> subMenuItems = menuItemDao.getSubMenuItems(ViewType.UPNP);
        assertTrue(validateDefaultSubMenuItems.apply(subMenuItems));

        subMenuItems.forEach(menuItem -> {
            menuItem.setEnabled(!menuItem.isEnabled());
            menuItem.setName("dummy");
            menuItemDao.updateMenuItem(menuItem);
        });

        menuItemService.resetMenuItem(ViewType.UPNP, ResetMode.SUB_MENU);
        subMenuItems = menuItemDao.getSubMenuItems(ViewType.UPNP);
        assertTrue(validateDefaultSubMenuItems.apply(subMenuItems));
    }
}
