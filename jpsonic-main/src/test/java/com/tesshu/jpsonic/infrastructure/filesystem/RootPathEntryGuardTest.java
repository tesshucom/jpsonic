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
import java.util.Optional;
import java.util.stream.Stream;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider.PathGeometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RootPathEntryGuardTest {

    private PathGeometry pathGeometry;

    @BeforeEach
    void setup() {
        // Cases c01 to c08 were originally created for legacy Windows and Linux
        // environments.
        // RootPathEntryGuardOnWSLTest is specifically designed for WSL-specific
        // geometry.
        pathGeometry = EnvironmentProvider.getInstance().getPathGeometry();
        if (pathGeometry == PathGeometry.WSL) {
            pathGeometry = PathGeometry.LINUX;
        }
    }

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
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, null).isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.Traversal
        @ValidatePathDecisions.Results.Empty
        void c02() {
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "/../foo").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.NonTraversal
        @ValidatePathDecisions.Results.NotEmpty
        void c03() {
            assertFalse(RootPathEntryGuard.validateFolderPath(pathGeometry, "/foo").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.Root
        @ValidatePathDecisions.Results.Empty
        void c04() {
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "/").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "\\").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "\\\\test").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.BackSlashe
        @ValidatePathDecisions.Results.Empty
        @EnabledOnOs(OS.WINDOWS)
        void c05() {
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "/:").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "\\:").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.BackSlashe
        @ValidatePathDecisions.Results.NotEmpty
        @EnabledOnOs(OS.LINUX)
        void c06() {
            assertFalse(RootPathEntryGuard.validateFolderPath(pathGeometry, "/:").isEmpty());
            assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "\\:").isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.DoubleBackSlashes
        @ValidatePathDecisions.Results.NotEmpty
        @EnabledOnOs(OS.WINDOWS)
        void c07() {
            assertFalse(RootPathEntryGuard
                .validateFolderPath(pathGeometry, "\\\\192.168.1.1/shared/testDirectory")
                .isEmpty());
            assertTrue(RootPathEntryGuard
                .validateFolderPath(pathGeometry, "\\\\192.168.1.1\\shared")
                .isEmpty());
        }

        @Test
        @ValidatePathDecisions.Conditions.Path.NonNull.DoubleBackSlashes
        @ValidatePathDecisions.Results.Empty
        @EnabledOnOs(OS.LINUX)
        void c08() {
            assertTrue(RootPathEntryGuard
                .validateFolderPath(pathGeometry, "\\\\192.168.1.1/shared/testDirectory")
                .isEmpty());
            assertTrue(RootPathEntryGuard
                .validateFolderPath(pathGeometry, "\\\\192.168.1.1\\shared")
                .isEmpty());
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void validateFolderPathShouldStrictlyRejectRelativeAndRootOnlyPathsOnWin() {
        // [Case 1] Drive letter only (considered as a relative path in NIO)
        assertFalse(RootPathEntryGuard.validateFolderPath(pathGeometry, "C:").isPresent(),
                "Should reject relative drive-letter-only paths.");

        // [Case 2] Single root only (getFileName() returns null)
        assertFalse(RootPathEntryGuard.validateFolderPath(pathGeometry, "C:\\").isPresent(),
                "Should reject root-only paths to enforce directory-level management.");

        // [Case 3] Valid absolute directory path
        assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "C:\\Music").isPresent(),
                "Should accept valid absolute Windows paths.");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void validateFolderPathShouldStrictlyRejectRelativeAndRootOnlyPathsOnLinux() {
        // [Case 1] Simple relative path
        assertTrue(RootPathEntryGuard.validateFolderPath(pathGeometry, "music/rock").isPresent(),
                """
                        Relative paths are not explicitly prohibited at this stage;
                        verification is deferred to downstream physical existence checks.
                        """);

        // [Case 2] POSIX Root only (getFileName() returns null)
        assertFalse(RootPathEntryGuard.validateFolderPath(pathGeometry, "/").isPresent(),
                "Should reject root-only paths.");

        // [Case 3] Valid absolute directory path
        assertTrue(
                RootPathEntryGuard.validateFolderPath(pathGeometry, "/home/user/music").isPresent(),
                "Should accept valid absolute POSIX paths.");
    }

    @Nested
    @EnabledOnOs(OS.LINUX)
    class RootPathEntryGuardOnWSLTest {

        @ParameterizedTest
        @MethodSource("provideWslPathCases")
        void validateFolderPathWSLGeometry(String input, boolean expectedSuccess) {
            // Validation logic for WSL
            Optional<String> result = RootPathEntryGuard
                .validateFolderPath(PathGeometry.WSL, input);

            if (expectedSuccess) {
                assertTrue(result.isPresent(), "Should accept: " + input);
            } else {
                assertTrue(result.isEmpty(), "Should reject: " + input);
            }
        }

        private static Stream<Arguments> provideWslPathCases() {
            return Stream
                .of(
                        // --- (1) Reject segment starting with dot ---
                        Arguments.of("/home/user/.hidden", false), // Hidden dir
                        Arguments.of("/mnt/c/My.Music/.", false), // Single dot segment
                        Arguments.of("//./C:/Music", false), // Poisoned normalized path

                        // --- (2) if isAbsolute: false, NameCount > 1 ---
                        // (Linux JRE treats "C:\Music" as relative, NameCount=1)
                        Arguments.of("C:\\Music", false), // Reject Win-style in WSL
                        Arguments.of("relative/path", true), // Valid relative (if ever used)

                        // --- (3) if isAbsolute: true, NameCount > 2 ---
                        Arguments.of("/", false), // System Root (Count 0)
                        Arguments.of("/mnt", false), // Mount Root (Count 1)
                        Arguments.of("/mnt/c", false), // Drive Root (Count 2)
                        Arguments.of("/mnt/c/Music", true), // Valid User Dir (Count 3)
                        Arguments.of("/home/user", false), // Valid User Dir (Count 2, Absolute)

                        // --- Edge Cases / P9 ---
                        Arguments.of("/wsl$", false), // P9 Root (Count 1)
                        Arguments.of("/wsl$/Ubuntu", false) // Distro Root (Count 2)
                );
        }
    }
}
