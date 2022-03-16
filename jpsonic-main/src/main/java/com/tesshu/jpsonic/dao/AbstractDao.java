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

package com.tesshu.jpsonic.dao;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Abstract superclass for all DAO's.
 *
 * @author Sindre Mehus
 */
public class AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDao.class);

    private final DaoHelper daoHelper;

    public AbstractDao(DaoHelper daoHelper) {
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

    protected String questionMarks(String columns) {
        int numberOfColumns = StringUtils.countMatches(columns, ",") + 1;
        return StringUtils.repeat("?", ", ", numberOfColumns);
    }

    protected static String prefix(String columns, String prefix) {
        List<String> l = Arrays.asList(columns.split(", "));
        l.replaceAll(s -> prefix + "." + s);
        return String.join(", ", l);
    }

    protected int update(String sql, Object... args) {
        long t = System.nanoTime();
        LOG.trace("Executing query: [{}]", sql);
        int result = getJdbcTemplate().update(sql, args);
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

    protected <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        long t = System.nanoTime();
        List<T> result = getJdbcTemplate().query(sql, rowMapper, args);
        writeLog(sql, t);
        return result;
    }

    protected <T> List<T> namedQuery(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        long t = System.nanoTime();
        List<T> result = getNamedParameterJdbcTemplate().query(sql, args, rowMapper);
        writeLog(sql, t);
        return result;
    }

    protected List<String> queryForStrings(String sql, Object... args) {
        long t = System.nanoTime();
        List<String> result = getJdbcTemplate().queryForList(sql, String.class, args);
        writeLog(sql, t);
        return result;
    }

    protected List<Integer> queryForInts(String sql, Object... args) {
        long t = System.nanoTime();
        List<Integer> result = getJdbcTemplate().queryForList(sql, Integer.class, args);
        writeLog(sql, t);
        return result;
    }

    protected List<String> namedQueryForStrings(String sql, Map<String, Object> args) {
        long t = System.nanoTime();
        List<String> result = getNamedParameterJdbcTemplate().queryForList(sql, args, String.class);
        writeLog(sql, t);
        return result;
    }

    protected Integer queryForInt(String sql, Integer defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Integer> list = getJdbcTemplate().queryForList(sql, Integer.class, args);
        Integer result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    protected Integer namedQueryForInt(String sql, Integer defaultValue, Map<String, Object> args) {
        long t = System.nanoTime();
        List<Integer> list = getNamedParameterJdbcTemplate().queryForList(sql, args, Integer.class);
        Integer result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    protected Date queryForDate(String sql, Date defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Date> list = getJdbcTemplate().queryForList(sql, Date.class, args);
        Date result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    protected Long queryForLong(String sql, Long defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Long> list = getJdbcTemplate().queryForList(sql, Long.class, args);
        Long result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        writeLog(sql, t);
        return result;
    }

    protected <T> T queryOne(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> list = query(sql, rowMapper, args);
        return list.isEmpty() ? null : list.get(0);
    }

    protected <T> T namedQueryOne(String sql, RowMapper<T> rowMapper, Map<String, Object> args) {
        List<T> list = namedQuery(sql, rowMapper, args);
        return list.isEmpty() ? null : list.get(0);
    }

    public void checkpoint() {
        daoHelper.checkpoint();
    }

}
