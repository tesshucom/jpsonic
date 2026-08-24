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

package com.tesshu.jpsonic.persistence.core.entity;

import com.tesshu.jpsonic.domain.system.MenuItemId;

public class MenuItem {

    private com.tesshu.jpsonic.domain.model.MenuItem.ViewType viewType;
    private MenuItemId id;
    private MenuItemId parent;
    private String name;
    private boolean enabled;
    private int menuItemOrder;

    public MenuItem(com.tesshu.jpsonic.domain.model.MenuItem.ViewType viewType, MenuItemId id,
            MenuItemId parent, String name, boolean enabled, int menuItemOrder) {
        super();
        this.viewType = viewType;
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.enabled = enabled;
        this.menuItemOrder = menuItemOrder;
    }

    public com.tesshu.jpsonic.domain.model.MenuItem.ViewType getViewType() {
        return viewType;
    }

    public void setViewType(com.tesshu.jpsonic.domain.model.MenuItem.ViewType viewType) {
        this.viewType = viewType;
    }

    public MenuItemId getId() {
        return id;
    }

    public void setId(MenuItemId id) {
        this.id = id;
    }

    public MenuItemId getParent() {
        return parent;
    }

    public void setParent(MenuItemId parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMenuItemOrder() {
        return menuItemOrder;
    }

    public void setMenuItemOrder(int menuItemOrder) {
        this.menuItemOrder = menuItemOrder;
    }
}
