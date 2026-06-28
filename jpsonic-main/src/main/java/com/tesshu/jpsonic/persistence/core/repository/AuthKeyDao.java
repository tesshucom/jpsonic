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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.persistence.core.repository;

import java.sql.ResultSet;
import java.time.Instant;

import com.drew.lang.annotations.Nullable;
import com.tesshu.jpsonic.domain.model.AuthKey;
import com.tesshu.jpsonic.domain.model.AuthKey.AuthKeyType;
import com.tesshu.jpsonic.domain.repository.AuthKeyRepository;
import com.tesshu.jpsonic.persistence.base.TemplateWrapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuthKeyDao implements AuthKeyRepository {

    private static final String QUERY_COLUMNS = "key_type, value, last_update\s";

    private final RowMapper<AuthKey> rowMapper = (ResultSet rs, int num) -> new AuthKey(
            AuthKeyType.of(rs.getInt(1)), rs.getString(2), rs.getTimestamp(3).toInstant());

    private final TemplateWrapper template;

    public AuthKeyDao(TemplateWrapper template) {
        super();
        this.template = template;
    }

    @Override
    public void create(AuthKeyType keyType, String value, Instant lastUpdate) {
        String sql = "insert into auth_key (%s) values (?, ?, ?)".formatted(QUERY_COLUMNS);
        template.update(sql, keyType.value(), value, lastUpdate);
    }

    @Override
    public void update(AuthKeyType keyType, String value, Instant lastUpdate) {
        String sql = "update auth_key set value=?, last_update=? where key_type=?";
        template.update(sql, value, lastUpdate, keyType.value());
    }

    @Override
    @Nullable
    public AuthKey get(AuthKeyType keyType) {
        return template
            .queryOne("select %s from auth_key where key_type=?".formatted(QUERY_COLUMNS),
                    rowMapper, keyType.value());
    }

    @Override
    public void remove(AuthKeyType keyType) {
        template.update("delete from auth_key where key_type = ?", keyType.value());
    }
}
