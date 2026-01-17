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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.persistence.base;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TemplateWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateWrapper.class);

    private final DaoHelper daoHelper;

    public TemplateWrapper(DaoHelper daoHelper) {
        super();
        this.daoHelper = daoHelper;
    }

    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return daoHelper.getJdbcTemplate();
    }

    /**
     * Similar to {@link #getJdbcTemplate()}, but with named parameters.
     */
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return daoHelper.getNamedParameterJdbcTemplate();
    }

    @SuppressFBWarnings(value = "SQL_INJECTION_SPRING_JDBC", justification = "False positive. find-sec-bugs#385")
    public int update(String sql, Object... args) {
        long t = System.nanoTime();
        LOG.trace("Executing query: [{}]", sql);
        int result = getJdbcTemplate().update(sql, castArgs(args));
        LOG.trace("Updated {} rows", result);
        writeLog(sql, t);
        return result;
    }

    private void writeLog(String sql, long startTimeNano) {
        long millis = (System.nanoTime() - startTimeNano) / 1_000_000L;

        // Log queries that take more than 2 seconds.
        if (LOG.isDebugEnabled() && millis > TimeUnit.SECONDS.toMillis(2L)) {
            LOG.debug(millis + " ms:  " + sql);
        }
    }

    @SuppressFBWarnings(value = "SQL_INJECTION_SPRING_JDBC", justification = "False positive. find-sec-bugs#385")
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        long t = System.nanoTime();
        List<T> result = getJdbcTemplate().query(sql, rowMapper, castArgs(args));
        writeLog(sql, t);
        return result;
    }

    public <T> List<T> namedQuery(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        long t = System.nanoTime();
        List<T> result = getNamedParameterJdbcTemplate().query(sql, castArgs(args), rowMapper);
        writeLog(sql, t);
        return result;
    }

    public List<String> queryForStrings(String sql, Object... args) {
        long t = System.nanoTime();
        List<String> result = getJdbcTemplate().queryForList(sql, String.class, castArgs(args));
        writeLog(sql, t);
        return result;
    }

    public List<Integer> queryForInts(String sql, Object... args) {
        long t = System.nanoTime();
        List<Integer> result = getJdbcTemplate().queryForList(sql, Integer.class, castArgs(args));
        writeLog(sql, t);
        return result;
    }

    public List<String> namedQueryForStrings(String sql, Map<String, Object> args) {
        long t = System.nanoTime();
        List<String> result = getNamedParameterJdbcTemplate()
            .queryForList(sql, castArgs(args), String.class);
        writeLog(sql, t);
        return result;
    }

    public Integer queryForInt(String sql, Integer defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Integer> list = getJdbcTemplate().queryForList(sql, Integer.class, castArgs(args));
        Integer result = list.isEmpty() ? defaultValue
                : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    public Integer namedQueryForInt(String sql, Integer defaultValue, Map<String, Object> args) {
        long t = System.nanoTime();
        List<Integer> list = getNamedParameterJdbcTemplate()
            .queryForList(sql, castArgs(args), Integer.class);
        Integer result = list.isEmpty() ? defaultValue
                : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    public Instant queryForInstant(String sql, Instant defaultValue, Object... args) {
        long startTimeNano = System.nanoTime();
        Instant result = getJdbcTemplate()
            .queryForList(sql, Timestamp.class, castArgs(args))
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(Timestamp::toInstant)
            .orElse(defaultValue);
        writeLog(sql, startTimeNano);
        return result;
    }

    @SuppressFBWarnings(value = "SQL_INJECTION_SPRING_JDBC", justification = "False positive. find-sec-bugs#385")
    protected Long queryForLong(String sql, Long defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Long> list = getJdbcTemplate().queryForList(sql, Long.class, castArgs(args));
        Long result = list.isEmpty() ? defaultValue
                : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    @Nullable
    public <T> T queryOne(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> list = query(sql, rowMapper, castArgs(args));
        return list.isEmpty() ? null : list.get(0);
    }

    public <T> T namedQueryOne(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        List<T> list = namedQuery(sql, rowMapper, castArgs(args));
        return list.isEmpty() ? null : list.get(0);
    }

    static @Nullable Object[] castArgs(@Nullable Object... args) {
        return args == null ? null
                : Stream
                    .of(args)
                    .map(TemplateWrapper::castArg)
                    .collect(Collectors.toList())
                    .toArray();
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap") // Explicit use of null
    static @NonNull Map<String, Object> castArgs(@Nullable Map<String, Object> args) {
        Map<String, Object> result = new HashMap<>();
        if (args == null) {
            return result;
        }
        args.entrySet().stream().forEach(e -> result.put(e.getKey(), castArg(e.getValue())));
        return result;
    }

    static @Nullable Object castArg(@Nullable Object arg) {
        if (arg == null) {
            return null;
        } else if (arg instanceof Instant dateTime) {
            return Timestamp.from(dateTime);
        }
        return arg;
    }

    public void checkpoint() {
        daoHelper.checkpoint();
    }
}
