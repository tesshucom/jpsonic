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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LegacyHsqlUtilTest {

    private void setHome(String s) {
        System.setProperty("jpsonic.home", s);
    }

    @Nested
    class GetHsqldbDatabaseVersionTest {

        @Test
        void testInvalidPath() {
            setHome("src/test/resources");
            assertNull(LegacyHsqlUtil.getHsqldbDatabaseVersion());
        }

        @Test
        void testValidPath() {
            setHome("src/test/resources/db/pre-liquibase");
            assertEquals("1.8.0", LegacyHsqlUtil.getHsqldbDatabaseVersion());
        }
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // Confirmed by coverage
    @Nested
    class CheckHsqldbDatabaseVersionTest {

        @Test
        void testInvalidPath() {
            setHome("src/test/resources");
            LegacyHsqlUtil.checkHsqldbDatabaseVersion();
        }

        @Test
        void testOldDb() {
            setHome("src/test/resources/db/pre-liquibase");
            LegacyHsqlUtil.checkHsqldbDatabaseVersion();
            setHome("src/test/resources/db/old");
            LegacyHsqlUtil.checkHsqldbDatabaseVersion();
        }

        @Test
        void testCurrentDb() {
            setHome("src/test/resources/db/current");
            LegacyHsqlUtil.checkHsqldbDatabaseVersion();
        }

        @Test
        void testUnreachable() {
            setHome("src/test/resources/db/unreachable");
            LegacyHsqlUtil.checkHsqldbDatabaseVersion();
        }
    }
}
