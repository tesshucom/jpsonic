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

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MenuItemDao {

    private static final String QUERY_COLUMNS = """
            view_type, id, parent, name, enabled, menu_item_order\s
            """;
    private final TemplateWrapper template;
    private final RowMapper<MenuItem> rowMapper = (ResultSet rs, int num) -> new MenuItem(ViewType.of(rs.getInt(1)),
            MenuItemId.of(rs.getInt(2)), MenuItemId.of(rs.getInt(3)), rs.getString(4), rs.getBoolean(5), rs.getInt(6));

    public MenuItemDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
    }

    public MenuItem getMenuItem(int id) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from menu_item
                where id=?
                """, rowMapper, id);
    }

    public int getTopMenuItemCount(ViewType viewType) {
        return template.queryForInt("""
                select count(id) from menu_item
                where view_type=? and parent=? and enabled=?
                """, 0, viewType.value(), MenuItemId.ROOT.value(), true);
    }

    public List<MenuItem> getTopMenuItems(ViewType viewType, boolean enabledOnly, long offset, long count) {
        Map<String, Object> args = Map.of("type", viewType.value(), "parentId", MenuItemId.ROOT.value(), "enabledOnly",
                enabledOnly, "count", count, "offset", offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from menu_item
                where view_type=:type and parent=:parentId %s
                order by menu_item_order, id
                limit :count offset :offset
                """.formatted(enabledOnly ? "and enabled=:enabledOnly" : ""), rowMapper, args);
    }

    public int getChildSizeOf(ViewType viewType, MenuItemId id) {
        return template.queryForInt("""
                select count(id) from menu_item
                where view_type=? and parent=? and enabled=?
                """, 0, viewType.value(), id.value(), true);
    }

    public List<MenuItem> getChildlenOf(ViewType viewType, MenuItemId id, boolean enabledOnly, long offset,
            long count) {
        Map<String, Object> args = Map.of("type", viewType.value(), "parentId", id.value(), "enabledOnly", enabledOnly,
                "count", count, "offset", offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from menu_item
                where view_type=:type and parent=:parentId %s
                order by menu_item_order, id
                limit :count offset :offset
                """.formatted(enabledOnly ? "and enabled=:enabledOnly" : ""), rowMapper, args);
    }

    public void updateMenuItem(MenuItem menuItem) {
        String sql = """
                update menu_item
                set view_type=?, parent=?, name=?, enabled=?, menu_item_order=?
                where id=?
                """;
        template.update(sql, menuItem.getViewType().value(), menuItem.getParent().value(), menuItem.getName(),
                menuItem.isEnabled(), menuItem.getMenuItemOrder(), menuItem.getId().value());
    }

    public List<MenuItem> getSubMenuItems(ViewType viewType) {
        Map<String, Object> args = Map.of("type", viewType.value(), "rootId", MenuItemId.ROOT.value());
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from menu_item sub
                join menu_item top
                on sub.parent = top.id
                where sub.view_type=:type and sub.parent <> :rootId
                order by top.menu_item_order, top.id, sub.menu_item_order, sub.id
                """, rowMapper, args);
    }
}
