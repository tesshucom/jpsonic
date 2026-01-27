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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeedsHomeExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(NeedsHomeExtension.class);
    private static final String PROP = "jpsonic.home";
    private Path home;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path parent = Path.of(System.getProperty("java.io.tmpdir"), "jpsonic_test_" + date);
        Files.createDirectories(parent);

        String timestamp = LocalDateTime
            .now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
        home = Files.createDirectory(parent.resolve(timestamp));

        System.setProperty(PROP, home.toString());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (home != null) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(home.toFile());
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The behavior of this callback process depends on the platform.", e);
                }
            }
        }
    }
}
