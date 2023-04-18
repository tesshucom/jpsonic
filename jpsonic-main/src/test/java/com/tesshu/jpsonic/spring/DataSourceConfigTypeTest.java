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

package com.tesshu.jpsonic.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DataSourceConfigTypeTest {

    @Test
    void testOf() {
        assertEquals("host", DataSourceConfigType.of("HOST").getName());
        assertEquals("host", DataSourceConfigType.of("LEGACY").getName());
        assertEquals("url", DataSourceConfigType.of("URL").getName());
        assertEquals("url", DataSourceConfigType.of("EMBED").getName());
        assertEquals("jndi", DataSourceConfigType.of("JNDI").getName());
        assertEquals("host", DataSourceConfigType.of("host").getName());
        assertEquals("host", DataSourceConfigType.of("legacy").getName());
        assertEquals("url", DataSourceConfigType.of("url").getName());
        assertEquals("url", DataSourceConfigType.of("embed").getName());
        assertEquals("jndi", DataSourceConfigType.of("jndi").getName());
        assertEquals("host", DataSourceConfigType.of("undefined").getName());
        assertEquals("host", DataSourceConfigType.of(null).getName());
    }
}
