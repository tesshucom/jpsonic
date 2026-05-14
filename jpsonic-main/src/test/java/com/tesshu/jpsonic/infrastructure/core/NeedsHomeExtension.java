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

package com.tesshu.jpsonic.infrastructure.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeedsHomeExtension
        implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(NeedsHomeExtension.class);
    private static final String PROP = "jpsonic.home";

    // Static to ensure a single shared HOME state across test containers.
    // GitHub Actions on Windows Server may instantiate this extension separately
    // for outer and @Nested classes, so a static field prevents inconsistent
    // initialization and guarantees a single HOME lifecycle.
    private static Path home;

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

        EnvironmentProvider.getInstance().resetCache();

        createFfmpegLink();
    }

    private void createFfmpegLink() {

        String transcodePath = System.getProperty("jpsonic-transcodePath");
        if (transcodePath == null || transcodePath.isBlank()) {
            return;
        }

        Path link = EnvironmentProvider.getInstance().getTranscodeDirectory();
        Path target = Path.of(transcodePath);

        try {
            Files.deleteIfExists(link);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            if (EnvironmentProvider.getInstance().isWindows()) {
                createWindowsJunction(link, target);
            } else {
                Files.createSymbolicLink(link, target);
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Couldn't create SymbolicLink for ffmpeg", e);
        }
    }

    private void createWindowsJunction(Path link, Path target)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "mklink", "/J",
                link.toAbsolutePath().toString(), target.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (InputStream is = p.getInputStream()) {
            is.transferTo(OutputStream.nullOutputStream());
        }
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("mklink /J failed with exit code " + exit);
        }
    }

    /**
     * Ensures that the test-specific HOME directory is initialized even when
     * {@link #beforeAll(ExtensionContext)} has not been invoked yet.
     *
     * <p>
     * Under normal JUnit 5 lifecycle rules, {@code beforeAll()} is guaranteed to
     * run before any {@code beforeEach()} callbacks of the same test class
     * hierarchy. However, on some Windows environments (notably GitHub Actions
     * Windows Server / Docker), the initialization order of test containers may
     * differ, causing {@code beforeEach()} of a {@code @Nested} class to be invoked
     * before the outer test class's {@code beforeAll()}.
     *
     * <p>
     * This fallback ensures that HOME initialization (directory creation, system
     * property setup, and ffmpeg link creation) is performed exactly once, even if
     * {@code beforeAll()} is delayed. The operation is idempotent and safe to
     * invoke multiple times.
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (home == null) {
            beforeAll(context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (home != null) {
            try {
                if (EnvironmentProvider.getInstance().isWindows()) {
                    deleteDirectoryJunctionSafely(
                            EnvironmentProvider.getInstance().getTranscodeDirectory());
                }
                org.apache.commons.io.FileUtils.deleteDirectory(home.toFile());
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The behavior of this callback process depends on the platform.", e);
                }
            }
        }
    }

    private static void deleteDirectoryJunctionSafely(Path junction)
            throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", "fsutil", "reparsepoint", "delete",
                junction.toAbsolutePath().toString())
            .inheritIO()
            .start()
            .waitFor();
    }
}
