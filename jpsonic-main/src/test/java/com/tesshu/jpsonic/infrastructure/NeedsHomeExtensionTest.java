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

package com.tesshu.jpsonic.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

@NeedsHome
class NeedsHomeExtensionTest {

    private static Path home;

    @BeforeAll
    static void captureHome() {
        // Retrieve the JPSONIC_HOME set by NeedsHomeExtension.beforeAll()
        home = Path.of(System.getProperty("jpsonic.home"));
    }

    @Test
    void homeDirectoryShouldExistDuringTest() {
        // The directory should exist during the test
        assertThat(home).exists();

        // The parent directory should follow the jpsonic_test_YYYYMMDD format
        Path parent = home.getParent();
        assertThat(parent.getFileName().toString()).startsWith("jpsonic_test_");

        // The child directory name should follow the timestamp format
        // (YYYYMMDD_HHmmssSSS)
        assertThat(home.getFileName().toString()).matches("\\d{8}_\\d{9,}");
    }

    @Test
    void beforeAllAndAfterAllShouldCreateAndDeleteHome() throws Exception {
        NeedsHomeExtension ext = new NeedsHomeExtension();

        // A mocked ExtensionContext is sufficient
        ExtensionContext context = mock(ExtensionContext.class);

        // beforeAll → create the directory
        ext.beforeAll(context);

        Path home = Path.of(System.getProperty("jpsonic.home"));
        assertThat(home).exists();

        // afterAll → delete the directory
        ext.afterAll(context);
        assertThat(home).doesNotExist();
    }
}
