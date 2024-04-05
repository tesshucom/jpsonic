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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.dao.MenuItemDao;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MenuItemService {

    private final SettingsService settingsService;
    private final MenuItemDao menuItemDao;

    @Resource
    private final MessageSource menuItemSource;

    public MenuItemService(SettingsService settingsService, MenuItemDao menuItemDao,
            @Lazy @Qualifier("menuItemSource") MessageSource menuItemSource) {
        this.settingsService = settingsService;
        this.menuItemDao = menuItemDao;
        this.menuItemSource = menuItemSource;
    }

    String getItemName(MenuItemId id) {
        Locale locale = Locale.JAPAN.getLanguage().equals(settingsService.getLocale().getLanguage()) ? Locale.JAPAN
                : Locale.US;
        return menuItemSource.getMessage("defaultname." + id.name().replaceAll("_", "").toLowerCase(Locale.ROOT), null,
                locale);
    }

    public MenuItem getMenuItem(String id) {
        return menuItemDao.getMenuItem(Integer.parseInt(id));
    }

    public MenuItem getMenuItem(MenuItemId id) {
        return menuItemDao.getMenuItem(id.value());
    }

    public int getTopMenuItemCount(ViewType viewType) {
        return menuItemDao.getTopMenuItemCount(viewType);
    }

    public List<MenuItem> getTopMenuItems(ViewType viewType, boolean enabledOnly, long offset, long count) {
        List<MenuItem> menuItems = menuItemDao.getTopMenuItems(viewType, enabledOnly, offset, count);
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }

    public List<MenuItemWithDefaultName> getTopMenuItems(ViewType viewType) {
        return getTopMenuItems(viewType, false, 0, Integer.MAX_VALUE).stream()
                .map(menuItem -> new MenuItemWithDefaultName(menuItem, getItemName(menuItem.getId()))).toList();
    }

    public int getChildSizeOf(ViewType viewType, MenuItemId id) {
        return menuItemDao.getChildSizeOf(viewType, id);
    }

    public List<MenuItem> getChildlenOf(ViewType viewType, MenuItemId id, boolean enabledOnly, long offset,
            long count) {
        List<MenuItem> menuItems = menuItemDao.getChildlenOf(viewType, id, enabledOnly, offset, count);
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }

    public void updateMenuItem(MenuItem menuItem) {
        MenuItem stored = menuItemDao.getMenuItem(menuItem.getId().value());
        String defaultName = getItemName(menuItem.getId());
        stored.setEnabled(menuItem.isEnabled());
        if (stored.getName().equals(defaultName) || menuItem.getName().isBlank()) {
            stored.setName("");
        } else {
            stored.setName(menuItem.getName());
        }
        stored.setMenuItemOrder(menuItem.getMenuItemOrder());
        menuItemDao.updateMenuItem(stored);
    }

    public void updateMenuItemOrder(ViewType viewType, int menuItemId) {
        List<MenuItem> menuItems = getTopMenuItems(viewType, false, 0, Integer.MAX_VALUE);
        int position = -1;
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItemId == menuItems.get(i).getId().value()) {
                position = i;
                break;
            }
        }
        if (0 < position) {
            Collections.swap(menuItems, position - 1, position);
            for (int i = 0; i < menuItems.size(); i++) {
                MenuItem menuItem = menuItems.get(i);
                menuItem.setMenuItemOrder(i);
                menuItemDao.updateMenuItem(menuItem);
            }
        }
    }

    public List<MenuItemWithDefaultName> getSubMenuItems(ViewType viewType) {
        return menuItemDao.getSubMenuItems(viewType).stream().map(item -> {
            String defaultName = getItemName(item.getId());
            if (item.getName().isBlank()) {
                item.setName(defaultName);
            }
            return new MenuItemWithDefaultName(item, defaultName);
        }).toList();
    }

    public static class MenuItemWithDefaultName extends MenuItem {

        private final String defaultName;

        public MenuItemWithDefaultName(MenuItem menuItem, String defaultName) {
            super(menuItem.getViewType(), menuItem.getId(), menuItem.getParent(), menuItem.getName(),
                    menuItem.isEnabled(), menuItem.getMenuItemOrder());
            this.defaultName = defaultName;
        }

        public String getDefaultName() {
            return defaultName;
        }
    }
}
