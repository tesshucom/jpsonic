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

import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import com.tesshu.jpsonic.dao.MenuItemDao;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MenuItemService {

    private final SettingsService settingsService;
    private final MenuItemDao menuItemDao;
    @Lazy
    @Resource(name = "menuItemSource")
    private final MessageSource menuItemSource;

    public MenuItemService(SettingsService settingsService, MenuItemDao menuItemDao, MessageSource menuItemSource) {
        this.settingsService = settingsService;
        this.menuItemDao = menuItemDao;
        this.menuItemSource = menuItemSource;
    }

    String getItemName(MenuItemId id) {
        Locale locale = Locale.JAPAN.getLanguage().equals(settingsService.getLocale().getLanguage()) ? Locale.JAPAN
                : Locale.US;
        return menuItemSource.getMessage("defaultname." + id.toString().toLowerCase(Locale.US), null, locale);
    }

    public List<MenuItem> getTopMenuItems(ViewType viewType) {
        List<MenuItem> menuItems = menuItemDao.getTopMenuItems(viewType);
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }

    public List<MenuItem> getChildlenOf(ViewType viewType, MenuItemId id) {
        List<MenuItem> menuItems = menuItemDao.getChildlenOf(viewType, id);
        menuItems.stream().filter(item -> item.getName().isBlank())
                .forEach(item -> item.setName(getItemName(item.getId())));
        return menuItems;
    }
}
