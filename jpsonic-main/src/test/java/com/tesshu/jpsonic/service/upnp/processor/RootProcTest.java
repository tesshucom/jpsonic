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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.system.MenuItemId;
import com.tesshu.jpsonic.persistence.core.entity.MenuItem;
import com.tesshu.jpsonic.service.MenuItemService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.DIDLObject.Property.UPNP;
import org.jupnp.support.model.DIDLObject.Property.UPNP.STORAGE_USED;
import org.jupnp.support.model.WriteStatus;
import org.jupnp.support.model.container.Container;
import org.springframework.beans.factory.annotation.Autowired;

class RootProcTest extends AbstractNeedsScan {

    @Autowired
    private MenuItemService menuItemService;
    @Autowired
    private RootUpnpProc proc;

    @Test
    void testGetProcId() {
        assertEquals(ProcId.ROOT, proc.getProcId());
    }

    @Test
    void testGetProcTitle() {
        assertEquals("Jpsonic Media", proc.getProcTitle());
    }

    @Test
    void testSetProcTitle() {
        proc.setProcTitle("Not wrirable");
        assertEquals("Jpsonic Media", proc.getProcTitle());
    }

    @Test
    void testCreateRootContainer() {
        Container root = proc.createRootContainer();
        assertEquals(ProcId.ROOT.getValue(), root.getId());
        assertEquals("-1", root.getParentID());
        assertEquals(-1L, root.getFirstProperty(STORAGE_USED.class).getValue());
        assertEquals("Jpsonic Media", root.getTitle());
        assertTrue(root.isRestricted());
        assertTrue(root.isSearchable());
        assertEquals(WriteStatus.NOT_WRITABLE, root.getWriteStatus());
        assertEquals(proc.getDirectChildrenCount(), root.getChildCount());
    }

    @Test
    void testCreateContainer() {
        MenuItem menuItem = menuItemService.getMenuItem(MenuItemId.FOLDER);
        Container menuContainer = proc.createContainer(menuItem);
        assertEquals("0/10", menuContainer.getId());
        assertEquals("0", menuContainer.getParentID());
        assertEquals(-1, menuContainer.getFirstPropertyValue(UPNP.STORAGE_USED.class));
        assertEquals("", menuContainer.getTitle());
        assertTrue(menuContainer.isRestricted());
        assertTrue(menuContainer.isSearchable());
        assertEquals(WriteStatus.NOT_WRITABLE, menuContainer.getWriteStatus());
        assertEquals(0, menuContainer.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        List<MenuItem> menuItems = proc.getDirectChildren(0, 4);
        assertEquals(4, menuItems.size());
        assertEquals(MenuItemId.FOLDER, menuItems.get(0).getId());
        assertEquals(MenuItemId.ARTIST, menuItems.get(1).getId());
        assertEquals(MenuItemId.ALBUM, menuItems.get(2).getId());
        assertEquals(MenuItemId.GENRE, menuItems.get(3).getId());
        menuItems = proc.getDirectChildren(4, Integer.MAX_VALUE);
        assertEquals(4, menuItems.size());
        assertEquals(MenuItemId.PODCAST, menuItems.get(0).getId());
        assertEquals(MenuItemId.PLAYLISTS, menuItems.get(1).getId());
        assertEquals(MenuItemId.RECENTLY, menuItems.get(2).getId());
        assertEquals(MenuItemId.SHUFFLE, menuItems.get(3).getId());
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(8, proc.getDirectChildrenCount());
    }

    @Test
    void testGetDirectChild() {
        MenuItem menuItem = proc.getDirectChild(Integer.toString(MenuItemId.FOLDER.value()));
        assertEquals(MenuItemId.FOLDER, menuItem.getId());
    }

    @Nested
    class GetChildSizeOfTest {

        @Test
        void testSingleSubMenues() {
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.FOLDER)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.ARTIST)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.ALBUM)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.GENRE)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.PODCAST)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.PLAYLISTS)));
            assertEquals(0, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.RECENTLY)));
            assertEquals(50, proc.getChildSizeOf(menuItemService.getMenuItem(MenuItemId.SHUFFLE)));
        }

        @Test
        void testMultiSubMenues() {
            MenuItem parent = menuItemService.getMenuItem(MenuItemId.FOLDER);
            MenuItem sub1 = menuItemService.getMenuItem(MenuItemId.MEDIA_FILE);
            assertTrue(sub1.isEnabled());
            MenuItem sub2 = menuItemService.getMenuItem(MenuItemId.INDEX);
            assertFalse(sub2.isEnabled());
            assertEquals(0, proc.getChildSizeOf(parent));

            sub2.setEnabled(true);
            menuItemService.updateMenuItem(sub2);
            assertEquals(2, proc.getChildSizeOf(parent));

            sub2.setEnabled(false);
            menuItemService.updateMenuItem(sub2);
        }
    }

    @Test
    void testGetChildren() {
        List<MenuItem> children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.FOLDER), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.MEDIA_FILE, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.ARTIST), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.ALBUM_ARTIST, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.ALBUM), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.ALBUM_ID3, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.GENRE), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.ALBUM_ID3_BY_GENRE, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.PODCAST), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.PODCAST_DEFALT, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.PLAYLISTS), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.PLAYLISTS_DEFALT, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.RECENTLY), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.RECENTLY_ADDED_ALBUM, children.get(0).getId());

        children = proc
            .getChildren(menuItemService.getMenuItem(MenuItemId.SHUFFLE), 0, Integer.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals(MenuItemId.RANDOM_SONG, children.get(0).getId());
    }

    @Test
    void testAddChild() {
        DIDLContent parent = new DIDLContent();
        assertEquals(0, parent.getItems().size());
        assertEquals(0, parent.getContainers().size());

        MenuItem subMenuItem = menuItemService.getMenuItem(MenuItemId.MEDIA_FILE);
        proc.addChild(parent, subMenuItem);
        assertEquals(0, parent.getItems().size());
        assertEquals(1, parent.getContainers().size());
    }

    @Nested
    class BrowseLeafTest {

        @Test
        void testSingleSubMenues() throws ExecutionException {
            BrowseResult result = proc
                .browseLeaf(Integer.toString(MenuItemId.MEDIA_FILE.value()), null, 0,
                        Integer.MAX_VALUE);
            assertEquals(0L, result.getCount().getValue());
        }

        @Test
        void testMultiSubMenues() throws ExecutionException {
            MenuItem sub1 = menuItemService.getMenuItem(MenuItemId.MEDIA_FILE);
            assertTrue(sub1.isEnabled());
            MenuItem sub2 = menuItemService.getMenuItem(MenuItemId.INDEX);
            assertFalse(sub2.isEnabled());
            BrowseResult result = proc
                .browseLeaf(Integer.toString(MenuItemId.MEDIA_FILE.value()), null, 0,
                        Integer.MAX_VALUE);
            assertEquals(0L, result.getCount().getValue());

            sub2.setEnabled(true);
            menuItemService.updateMenuItem(sub2);
            result = proc
                .browseLeaf(Integer.toString(MenuItemId.FOLDER.value()), null, 0,
                        Integer.MAX_VALUE);
            assertEquals(2L, result.getCount().getValue());

            sub2.setEnabled(false);
            menuItemService.updateMenuItem(sub2);
        }
    }
}
