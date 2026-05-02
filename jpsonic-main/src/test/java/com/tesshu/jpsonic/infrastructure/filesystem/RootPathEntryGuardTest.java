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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.filesystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class RootPathEntryGuardTest {

    @Test
    void testIsStrictPath() {
        assertTrue(RootPathEntryGuard.isStrictPath("/foo/bar"));
        assertFalse(RootPathEntryGuard.isStrictPath("/foo/../bar"));
        assertFalse(RootPathEntryGuard.isStrictPath("C:\\foo\\..\\bar"));
    }

    @Documented
    private @interface ValidatePathDecisions {
        @interface Conditions {

            @interface Path {
                @interface Null {
                }

                @interface NonNull {

                    @interface Root {
                    }

                    @interface Traversal {
                    }

                    @interface NonTraversal {
                    }

                    @interface BackSlashe {
                    }

                    @interface DoubleBackSlashes {
                    }
                }
            }
        }

        @interface Results {
            @interface Empty {
            }

            @interface NotEmpty {
            }
        }
    }

    @Nested
    class ValidatePathTest {

        @Test
        @ValidatePathDecisions.Conditions.Path.Null
        @ValidatePathDecisions.Results.Empty
        void c01() {
            assertTrue(RootPathEntryGuard.validateFolderPath(null).isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.Traversal
        @ValidatePathDecisions.Results.Empty
        void c02() {
            assertTrue(RootPathEntryGuard.validateFolderPath("/../foo").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.NonTraversal
        @ValidatePathDecisions.Results.NotEmpty
        void c03() {
            assertFalse(RootPathEntryGuard.validateFolderPath("/foo").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.Root
        @ValidatePathDecisions.Results.Empty
        void c04() {
            assertTrue(RootPathEntryGuard.validateFolderPath("/").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\\\test").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.BackSlashe
        @ValidatePathDecisions.Results.Empty
        @EnabledOnOs(OS.WINDOWS)
        void c05() {
            assertTrue(RootPathEntryGuard.validateFolderPath("/:").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\:").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.BackSlashe
        @ValidatePathDecisions.Results.NotEmpty
        @EnabledOnOs(OS.LINUX)
        void c06() {
            assertFalse(RootPathEntryGuard.validateFolderPath("/:").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\:").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.DoubleBackSlashes
        @ValidatePathDecisions.Results.NotEmpty
        @EnabledOnOs(OS.WINDOWS)
        void c07() {
            assertFalse(RootPathEntryGuard
                .validateFolderPath("\\\\192.168.1.1/shared/testDirectory")
                .isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\\\192.168.1.1\\shared").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.DoubleBackSlashes
        @ValidatePathDecisions.Results.Empty
        @EnabledOnOs(OS.LINUX)
        void c08() {
            assertTrue(RootPathEntryGuard
                .validateFolderPath("\\\\192.168.1.1/shared/testDirectory")
                .isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath("\\\\192.168.1.1\\shared").isEmpty());
        }
    }
}
