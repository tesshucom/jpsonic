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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MenuItemDaoTest extends AbstractNeedsScan {

    @Autowired
    private MenuItemDao menuItemDao;

    @BeforeEach
    public void setup() {
        setSortAlphanum(true);
        setSortStrict(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    void testGetTopMenuItems() {
        List<MenuItem> menuItems = menuItemDao.getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE);
        assertEquals(8, menuItems.size());
        assertEquals(MenuItemId.FOLDER, menuItems.get(0).getId());
        assertEquals(MenuItemId.ARTIST, menuItems.get(1).getId());
        assertEquals(MenuItemId.ALBUM, menuItems.get(2).getId());
        assertEquals(MenuItemId.GENRE, menuItems.get(3).getId());
        assertEquals(MenuItemId.PODCAST, menuItems.get(4).getId());
        assertEquals(MenuItemId.PLAYLISTS, menuItems.get(5).getId());
        assertEquals(MenuItemId.RECENTLY, menuItems.get(6).getId());
        assertEquals(MenuItemId.SHUFFLE, menuItems.get(7).getId());
    }

    @Test
    void testGetChildlenOf() {
        List<MenuItem> menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.FOLDER, false, 0,
                Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals(MenuItemId.INDEX, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());
        assertEquals(MenuItemId.MEDIA_FILE, menuItems.get(1).getId());
        assertFalse(menuItems.get(1).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.ARTIST, false, 0, Integer.MAX_VALUE);
        assertEquals(3, menuItems.size());
        assertEquals(MenuItemId.INDEX_ID3, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());
        assertEquals(MenuItemId.ALBUM_ARTIST, menuItems.get(1).getId());
        assertFalse(menuItems.get(1).isEnabled());
        assertEquals(MenuItemId.ALBUM_ARTIST_BY_FOLDER, menuItems.get(2).getId());
        assertFalse(menuItems.get(2).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.ALBUM, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals(MenuItemId.ALBUM_ID3, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.GENRE, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals(MenuItemId.ALBUM_BY_GENRE, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());
        assertEquals(MenuItemId.SONG_BY_GENRE, menuItems.get(1).getId());
        assertFalse(menuItems.get(1).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.PODCAST, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals(MenuItemId.PODCAST_DEFALT, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.PLAYLISTS, false, 0, Integer.MAX_VALUE);
        assertEquals(1, menuItems.size());
        assertEquals(MenuItemId.PLAYLISTS_DEFALT, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.RECENTLY, false, 0, Integer.MAX_VALUE);
        assertEquals(2, menuItems.size());
        assertEquals(MenuItemId.RECENTLY_ADDED_ALBUM, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());
        assertEquals(MenuItemId.RECENTLY_TAGGED_ALBUM, menuItems.get(1).getId());
        assertFalse(menuItems.get(1).isEnabled());

        menuItems = menuItemDao.getChildlenOf(ViewType.UPNP, MenuItemId.SHUFFLE, false, 0, Integer.MAX_VALUE);
        assertEquals(4, menuItems.size());
        assertEquals(MenuItemId.RANDOM_ALBUM, menuItems.get(0).getId());
        assertTrue(menuItems.get(0).isEnabled());
        assertEquals(MenuItemId.RANDOM_SONG, menuItems.get(1).getId());
        assertFalse(menuItems.get(1).isEnabled());
        assertEquals(MenuItemId.RANDOM_SONG_BY_ARTIST, menuItems.get(2).getId());
        assertFalse(menuItems.get(2).isEnabled());
        assertEquals(MenuItemId.RANDOM_SONG_BY_FOLDER_ARTIST, menuItems.get(3).getId());
        assertFalse(menuItems.get(3).isEnabled());
    }
}
