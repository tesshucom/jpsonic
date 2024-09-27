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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.MenuItemDao;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import jakarta.annotation.Resource;
import org.checkerframework.checker.nullness.qual.NonNull;
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
        return (int) menuItemDao.getTopMenuIds(viewType).stream().filter(childId -> childId != MenuItemId.ANY).count();
    }

    public List<MenuItem> getTopMenuItems(ViewType viewType, boolean enabledOnly, long offset, long count) {
        // To be modifiable
        List<MenuItem> menuItems = menuItemDao.getTopMenuItems(viewType, enabledOnly, offset, count).stream()
                .filter(menu -> menu.getId() != MenuItemId.ANY).collect(Collectors.toList());
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }

    public List<MenuItemWithDefaultName> getTopMenuItems(ViewType viewType) {
        // To be modifiable
        return getTopMenuItems(viewType, false, 0, Integer.MAX_VALUE).stream()
                .map(menuItem -> new MenuItemWithDefaultName(menuItem, getItemName(menuItem.getId())))
                .collect(Collectors.toList());
    }

    public int getChildSizeOf(ViewType viewType, MenuItemId id) {
        return (int) menuItemDao.getChildIds(viewType, id).stream().filter(childId -> childId != MenuItemId.ANY)
                .count();
    }

    public List<MenuItem> getChildlenOf(ViewType viewType, MenuItemId id, boolean enabledOnly, long offset,
            long count) {
        List<MenuItem> menuItems = menuItemDao.getChildlenOf(viewType, id, enabledOnly, offset, count).stream()
                .filter(item -> item.getId() != MenuItemId.ANY).toList();
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }

    public void updateMenuItem(MenuItem menuItem) {
        MenuItem stored = menuItemDao.getMenuItem(menuItem.getId().value());
        stored.setEnabled(menuItem.isEnabled());
        stored.setName(menuItem.getName().equals(getItemName(menuItem.getId())) ? "" : menuItem.getName());
        stored.setMenuItemOrder(menuItem.getMenuItemOrder());
        menuItemDao.updateMenuItem(stored);
    }

    /**
     * Ensure at least one submenu is enabled within a category. If not, the default SubMenu will be enabled.
     */
    public void ensureUPnPSubMenuEnabled() {
        List<MenuItem> subMenus = menuItemDao.getSubMenuItems(ViewType.UPNP);
        getTopMenuItems(ViewType.UPNP, false, 0, Integer.MAX_VALUE).forEach(topMenu -> {
            long enableCounts = subMenus.stream().filter(subMenu -> subMenu.getId() != MenuItemId.ANY)
                    .filter(subMenu -> subMenu.getParent() == topMenu.getId()).filter(MenuItem::isEnabled).count();
            if (enableCounts == 0) {
                MenuItemId defaultSubMenuItemId = switch (topMenu.getId()) {
                case FOLDER -> MenuItemId.MEDIA_FILE;
                case ARTIST -> MenuItemId.ALBUM_ARTIST;
                case ALBUM -> MenuItemId.ALBUM_ID3;
                case GENRE -> MenuItemId.ALBUM_ID3_BY_GENRE;
                case PODCAST -> MenuItemId.PODCAST_DEFALT;
                case PLAYLISTS -> MenuItemId.PLAYLISTS_DEFALT;
                case RECENTLY -> MenuItemId.RECENTLY_ADDED_ALBUM;
                case SHUFFLE -> MenuItemId.RANDOM_SONG;
                default -> throw new IllegalArgumentException("Unexpected value: " + topMenu);
                };
                subMenus.stream().filter(menuItem -> menuItem.getId() == defaultSubMenuItemId).findFirst()
                        .ifPresent(menuItem -> {
                            menuItem.setEnabled(true);
                            menuItemDao.updateMenuItem(menuItem);
                        });
            }
        });
    }

    public void updateMenuItems(Stream<MenuItem> menuItems) {
        menuItems.forEach(this::updateMenuItem);
        ensureUPnPSubMenuEnabled();
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
        return menuItemDao.getSubMenuItems(viewType).stream().filter(item -> item.getId() != MenuItemId.ANY)
                .map(item -> {
                    String defaultName = getItemName(item.getId());
                    if (item.getName().isBlank()) {
                        item.setName(defaultName);
                    }
                    return new MenuItemWithDefaultName(item, defaultName);
                }).toList();
    }

    public void resetMenuItem(ViewType viewType, ResetMode mode) {
        List<MenuItem> menuItems = switch (mode) {
        case TOP_MENU -> menuItemDao.getTopMenuItems(viewType, false, 0, Integer.MAX_VALUE);
        case SUB_MENU -> menuItemDao.getSubMenuItems(viewType);
        case ANY -> Collections.emptyList();
        };
        menuItems.sort(Comparator.comparingInt(m -> m.getId().getDefaultOrder()));
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem menuItem = menuItems.get(i);
            menuItem.setEnabled(mode == ResetMode.TOP_MENU);
            menuItem.setName("");
            menuItem.setMenuItemOrder(i);
            menuItemDao.updateMenuItem(menuItem);
        }
        ensureUPnPSubMenuEnabled();
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

    public enum ResetMode {

        TOP_MENU("topMenu"), SUB_MENU("subMenu"), ANY("");

        private final String v;

        ResetMode(String v) {
            this.v = v;
        }

        public static @NonNull ResetMode of(String value) {
            return Stream.of(values()).filter(id -> id.v.equals(value)).findFirst().orElse(ANY);
        }
    }
}
