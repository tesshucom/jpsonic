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

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MenuItemDao;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import com.tesshu.jpsonic.service.MenuItemService.MenuItemWithDefaultName;
import com.tesshu.jpsonic.service.MenuItemService.ResetMode;
import org.junit.jupiter.api.Assertions;
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
    @Autowired
    private TemplateWrapper templateWrapper;

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
        assertEquals(3, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());
        assertEquals("By Folder", menuItems.get(1).getName());
        assertEquals("With Index", menuItems.get(2).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ARTIST, false, 0, Integer.MAX_VALUE);
        assertEquals(3, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());
        assertEquals("By Folder", menuItems.get(1).getName());
        assertEquals("With Index", menuItems.get(2).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.ALBUM, false, 0, Integer.MAX_VALUE);
        assertEquals(4, menuItems.size());
        assertEquals("Simple List(ID3)", menuItems.get(0).getName());
        assertEquals("By Folder(ID3)", menuItems.get(1).getName());
        assertEquals("Simple List(FileStructure)", menuItems.get(2).getName());
        assertEquals("By Folder(FileStructure)", menuItems.get(3).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE);
        assertEquals(6, menuItems.size());
        assertEquals("By Album", menuItems.get(0).getName());
        assertEquals("By Folder&Album", menuItems.get(1).getName());
        assertEquals("By Music", menuItems.get(2).getName());
        assertEquals("By Folder&Music", menuItems.get(3).getName());
        assertEquals("Audiobook", menuItems.get(4).getName());
        assertEquals("Subsonic Style", menuItems.get(5).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PODCAST, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Channels List", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.PLAYLISTS, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals("Simple List", menuItems.get(0).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.RECENTLY, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals("Added Albums", menuItems.get(0).getName());
        assertEquals("Tagged Albums", menuItems.get(1).getName());

        menuItems = menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.SHUFFLE, false, 0, Integer.MAX_VALUE);
        assertEquals(6, menuItems.size());
        assertEquals("Music", menuItems.get(0).getName());
        assertEquals("Music By Artist", menuItems.get(1).getName());
        assertEquals("Music By Folder/Artist", menuItems.get(2).getName());
        assertEquals("Music By Genre", menuItems.get(3).getName());
        assertEquals("Music By Folder/Genre", menuItems.get(4).getName());
        assertEquals("Album", menuItems.get(5).getName());
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
                    .filter(MenuItem::isEnabled).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
            menuItemService.ensureUPnPSubMenuEnabled();
            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(MenuItem::isEnabled).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
        }

        @Test
        void testEnsureUPnPSubMenuEnabled() {
            int topMenuItemCount = menuItemService.getTopMenuItemCount(ViewType.UPNP);
            int enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(MenuItem::isEnabled).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);

            menuItemService.getSubMenuItems(ViewType.UPNP).stream().forEach(menuItem -> {
                menuItem.setEnabled(false);
                menuItemService.updateMenuItem(menuItem);
            });

            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(MenuItem::isEnabled).count();
            assertEquals(0, enabledSubMenuCount);

            menuItemService.ensureUPnPSubMenuEnabled();
            enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                    .filter(MenuItem::isEnabled).count();
            assertEquals(topMenuItemCount, enabledSubMenuCount);
        }
    }

    @Test
    void testUpdateMenuItems() {
        List<MenuItemWithDefaultName> topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> assertTrue(menuItem.isEnabled()), Assertions::fail);
        int enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream()
                .filter(MenuItem::isEnabled).count();
        assertEquals(topMenuItems.size(), enabledSubMenuCount);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresent(menuItem -> menuItem.setEnabled(false));

        List<MenuItemWithDefaultName> subMenuItems = menuItemService.getSubMenuItems(ViewType.UPNP);
        subMenuItems.stream().forEach(menuItem -> menuItem.setEnabled(false));

        menuItemService.updateMenuItems(Stream.concat(topMenuItems.stream(), subMenuItems.stream()));

        topMenuItems = menuItemService.getTopMenuItems(ViewType.UPNP);
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> assertFalse(menuItem.isEnabled()), Assertions::fail);
        enabledSubMenuCount = (int) menuItemService.getSubMenuItems(ViewType.UPNP).stream().filter(MenuItem::isEnabled)
                .count();
        assertEquals(topMenuItems.size(), enabledSubMenuCount);

        // tearDown
        topMenuItems.stream().filter(menuItem -> menuItem.getId() == MenuItemId.FOLDER).findFirst()
                .ifPresentOrElse(menuItem -> {
                    menuItem.setEnabled(true);
                    menuItemService.updateMenuItem(menuItem);
                }, Assertions::fail);
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
            assertEquals(MenuItemId.MEDIA_FILE_BY_FOLDER, subMenuItems.get(1).getId());
            assertEquals(MenuItemId.INDEX, subMenuItems.get(2).getId());
            assertEquals(MenuItemId.ALBUM_ARTIST, subMenuItems.get(3).getId());
            assertEquals(MenuItemId.ALBUM_ARTIST_BY_FOLDER, subMenuItems.get(4).getId());
            assertEquals(MenuItemId.INDEX_ID3, subMenuItems.get(5).getId());
            assertEquals(MenuItemId.ALBUM_ID3, subMenuItems.get(6).getId());
            assertEquals(MenuItemId.ALBUM_ID3_BY_FOLDER, subMenuItems.get(7).getId());
            assertEquals(MenuItemId.ALBUM_FILE_STRUCTURE, subMenuItems.get(8).getId());
            assertEquals(MenuItemId.ALBUM_FILE_STRUCTURE_BY_FOLDER, subMenuItems.get(9).getId());
            assertEquals(MenuItemId.ALBUM_ID3_BY_GENRE, subMenuItems.get(10).getId());
            assertEquals(MenuItemId.ALBUM_ID3_BY_FOLDER_GENRE, subMenuItems.get(11).getId());
            assertEquals(MenuItemId.SONG_BY_GENRE, subMenuItems.get(12).getId());
            assertEquals(MenuItemId.SONG_BY_FOLDER_GENRE, subMenuItems.get(13).getId());
            assertEquals(MenuItemId.AUDIOBOOK_BY_GENRE, subMenuItems.get(14).getId());
            assertEquals(MenuItemId.ALBUM_BY_GENRE, subMenuItems.get(15).getId());
            assertEquals(MenuItemId.PODCAST_DEFALT, subMenuItems.get(16).getId());
            assertEquals(MenuItemId.PLAYLISTS_DEFALT, subMenuItems.get(17).getId());
            assertEquals(MenuItemId.RECENTLY_ADDED_ALBUM, subMenuItems.get(18).getId());
            assertEquals(MenuItemId.RECENTLY_TAGGED_ALBUM, subMenuItems.get(19).getId());
            assertEquals(MenuItemId.RANDOM_SONG, subMenuItems.get(20).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_ARTIST, subMenuItems.get(21).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_FOLDER_ARTIST, subMenuItems.get(22).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_GENRE, subMenuItems.get(23).getId());
            assertEquals(MenuItemId.RANDOM_SONG_BY_FOLDER_GENRE, subMenuItems.get(24).getId());
            assertEquals(MenuItemId.RANDOM_ALBUM, subMenuItems.get(25).getId());
            subMenuItems.forEach(menuItem -> {
                assertTrue(menuItem.getName().isBlank());
                boolean enabled = switch (menuItem.getId()) {
                case MEDIA_FILE, ALBUM_ARTIST, ALBUM_ID3, ALBUM_ID3_BY_GENRE, PODCAST_DEFALT, PLAYLISTS_DEFALT, RECENTLY_ADDED_ALBUM, RANDOM_SONG -> true;
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

    /**
     * This case will not occur in the latest version of Jpsonic. This case would occur if a new menu was added to the
     * latest version of the database and then the Jpsonic was launched with an older version. (When viewed from a
     * previous version of Jpsonic, there are menus with unknown IDs.)
     */
    @Nested
    class UnknownMenuTest {

        @Test
        void testUnknownTopMenu() {

            assertEquals(8, menuItemDao.getTopMenuIds(ViewType.UPNP).size());
            assertEquals(8, menuItemService.getTopMenuItemCount(ViewType.UPNP));

            assertEquals(8, menuItemDao.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).size());
            assertEquals(8, menuItemService.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).size());
            assertEquals(8, menuItemService.getTopMenuItems(ViewType.UPNP).size());

            // Add a dummy sub menu
            templateWrapper.update("""
                    insert into menu_item
                    (view_type, id, parent, name, enabled, menu_item_order)
                    values(?, ?, ?, ?, ?, ?);
                    """, ViewType.UPNP.value(), -1, MenuItemId.ROOT.value(), "dummy", true, 99);

            // Menus with unknown IDs are excluded in the Service layer.
            assertEquals(9, menuItemDao.getTopMenuIds(ViewType.UPNP).size());
            assertEquals(8, menuItemService.getTopMenuItemCount(ViewType.UPNP));

            assertEquals(9, menuItemDao.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).size());
            assertEquals(8, menuItemService.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).size());
            assertEquals(8, menuItemService.getTopMenuItems(ViewType.UPNP).size());

            templateWrapper.update("""
                    delete from menu_item where id < 0
                    """);

            assertEquals(8, menuItemDao.getTopMenuIds(ViewType.UPNP).size());
            assertEquals(8, menuItemDao.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).size());
        }

        @Test
        void testUnknownSubMenu() {

            assertEquals(1, menuItemDao.getChildIds(ViewType.UPNP, MenuItemId.GENRE).size());
            assertEquals(1, menuItemService.getChildSizeOf(ViewType.UPNP, MenuItemId.GENRE));

            int genreSize = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE)
                    .size();
            assertEquals(6, genreSize);
            assertEquals(genreSize,
                    menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE).size());

            int subMenuItemsSize = menuItemDao.getSubMenuItems(ViewType.UPNP).size();
            assertEquals(26, subMenuItemsSize);
            assertEquals(subMenuItemsSize, menuItemService.getSubMenuItems(ViewType.UPNP).size());

            // Add a dummy sub menu
            templateWrapper.update("""
                    insert into menu_item
                    (view_type, id, parent, name, enabled, menu_item_order)
                    values(?, ?, ?, ?, ?, ?);
                    """, ViewType.UPNP.value(), -1, MenuItemId.GENRE.value(), "", true, 99);

            // Menus with unknown IDs are excluded in the Service layer.

            assertEquals(2, menuItemDao.getChildIds(ViewType.UPNP, MenuItemId.GENRE).size());
            assertEquals(1, menuItemService.getChildSizeOf(ViewType.UPNP, MenuItemId.GENRE));

            assertEquals(7,
                    menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE).size());
            assertEquals(6,
                    menuItemService.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE).size());

            assertEquals(subMenuItemsSize + 1, menuItemDao.getSubMenuItems(ViewType.UPNP).size());
            assertEquals(subMenuItemsSize, menuItemService.getSubMenuItems(ViewType.UPNP).size());

            templateWrapper.update("""
                    delete from menu_item where id < 0
                    """);
            assertEquals(1, menuItemDao.getChildIds(ViewType.UPNP, MenuItemId.GENRE).size());
            assertEquals(6,
                    menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE).size());
            assertEquals(subMenuItemsSize, menuItemDao.getSubMenuItems(ViewType.UPNP).size());
        }
    }
}
