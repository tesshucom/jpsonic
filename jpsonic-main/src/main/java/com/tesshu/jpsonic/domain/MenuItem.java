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

package com.tesshu.jpsonic.domain;

import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;

public class MenuItem {

    private ViewType viewType;
    private MenuItemId id;
    private MenuItemId parent;
    private String name;
    private boolean enabled;
    private int menuItemOrder;

    public MenuItem(ViewType viewType, MenuItemId id, MenuItemId parent, String name,
            boolean enabled, int menuItemOrder) {
        super();
        this.viewType = viewType;
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.enabled = enabled;
        this.menuItemOrder = menuItemOrder;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public void setViewType(ViewType viewType) {
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

    public enum ViewType {

        ANY(0), UPNP(1), WEB(2);

        private final int v;

        ViewType(int v) {
            this.v = v;
        }

        public int value() {
            return v;
        }

        public static @NonNull ViewType of(int value) {
            return Stream.of(values()).filter(id -> id.v == value).findFirst().orElse(ANY);
        }
    }
}
