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

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItem.ViewType;
import com.tesshu.jpsonic.domain.MenuItemId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
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

    public List<MenuItem> getTopMenuItems(ViewType viewType) {
        return template.query("select " + QUERY_COLUMNS + """
                from menu_item
                where view_type=? and parent=?
                order by menu_item_order, id
                """, rowMapper, viewType.value(), MenuItemId.ROOT.value());
    }

    public List<MenuItem> getChildlenOf(ViewType viewType, MenuItemId id) {
        return template.query("select " + QUERY_COLUMNS + """
                from menu_item
                where view_type=? and parent=?
                order by menu_item_order, id
                """, rowMapper, viewType.value(), id.value());
    }
}
