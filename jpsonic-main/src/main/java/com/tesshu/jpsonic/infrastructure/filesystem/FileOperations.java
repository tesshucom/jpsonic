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

package com.tesshu.jpsonic.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low-level file system operations with robust error handling and retry logic.
 * <p>
 * This utility encapsulates Jpsonic's "fail-safe" philosophy, ensuring that
 * common I/O exceptions are handled gracefully with appropriate logging rather
 * than interrupting the application flow.
 * </p>
 */
public final class FileOperations {

    private static final Logger LOG = LoggerFactory.getLogger(FileOperations.class);

    private FileOperations() {
        // no-op
    }

    /**
     * Creates a directory and all non-existent parent directories. Unlike standard
     * NIO, this method suppresses exceptions and returns null on failure, while
     * explicitly handling cases where the directory already exists.
     *
     * @param dir The path to the directory to create.
     * @return The created directory path, or null if an error occurred.
     */
    public static @Nullable Path createDirectories(@NonNull Path dir) {
        try {
            return Files.createDirectories(dir);
        } catch (FileAlreadyExistsException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("directory already exists: {}", dir);
            }
            return dir;
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not create directory due to I/O error: {}", dir);
            }
        } catch (SecurityException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not create directory due to security check: {}", dir);
            }
        }
        return null;
    }

    /**
     * Deletes the file if it exists. Captures and logs various I/O and security
     * exceptions to ensure silent failure when the file cannot be removed (e.g.,
     * directory not empty or access denied).
     *
     * @param path The path to the file to delete.
     * @return true if the file was successfully deleted; false otherwise.
     */
    public static boolean deleteIfExists(Path path) {
        boolean isDeleted = false;
        try {
            isDeleted = Files.deleteIfExists(path);
            if (!isDeleted && LOG.isTraceEnabled()) {
                LOG.trace("Could not be delete file because it did not exist: {}", path);
            }
        } catch (DirectoryNotEmptyException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not be delete file because the directory is not empty: {}", path);
            }
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not be delete file due to I/O error: {}", path);
            }
        } catch (SecurityException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not be delete file due to security check: {}", path);
            }
        }
        return isDeleted;
    }

    /**
     * Recursively deletes a directory and all its contents. Utilizes legacy File
     * API via FileUtils to ensure synchronous execution and reliable behavior on
     * certain operating systems.
     *
     * @param path The path to the directory to be removed.
     */
    public static void deleteDirectory(Path path) {
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not be delete file recursively: {}", path);
            }
        }
    }

    /**
     * Updates the last modified time of the file, creating it if it does not exist.
     *
     * @param path The path to the file.
     * @throws IOException If an I/O error occurs.
     */
    public static void touch(final Path path) throws IOException {
        FileUtils.touch(path.toFile());
    }

    /**
     * Moves a file to a target location using an atomic move operation. Primarily
     * used for backup operations where maintaining file integrity during the
     * transition is critical.
     *
     * @param source The source file path.
     * @param target The destination file path.
     * @return The resulting path if successful; otherwise null.
     */
    public static @Nullable Path atomicMove(@NonNull Path source, @NonNull Path target) {
        if (Files.exists(target)) {
            try {
                Path path = Files
                    .move(source, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                if (path != null && LOG.isInfoEnabled()) {
                    LOG.info("Backed up old image file to " + path);
                }
                return path;
            } catch (IOException | SecurityException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to create image file backup :{}", target);
                }
                return null;
            }
        }
        return null;
    }

    /**
     * A background task that attempts to delete a temporary file with retries.
     * <p>
     * Necessary for environments (like Windows) where files may be temporarily
     * locked by other processes (e.g., transcoding or streaming). Since the system
     * is designed for long-running uptime, it retries to prevent accumulation of
     * orphaned files.
     * </p>
     */
    public static class DeleteTmpFileTask implements Runnable {

        private final Path tmpFile;
        private static final int TRIAL_MAX = 3;

        public DeleteTmpFileTask(Path tmpFile) {
            super();
            this.tmpFile = tmpFile;
        }

        @SuppressWarnings("PMD.GuardLogStatement")
        @Override
        public void run() {
            for (int i = 0; i < TRIAL_MAX; i++) {
                try {
                    if (Files.deleteIfExists(tmpFile)) {
                        break;
                    } else {
                        Thread.sleep(3000);
                    }
                } catch (IOException | SecurityException e) {
                    if (i == TRIAL_MAX - 1) {
                        LOG.warn("Failed to delete tmp file: " + tmpFile);
                        break;
                    }
                } catch (InterruptedException e) {
                    LOG.warn("The deleting tmp file has been interrupted.: " + tmpFile, e);
                    break;
                }
            }
            if (Files.exists(tmpFile)) {
                LOG.warn("Failed to delete tmp file: " + tmpFile);
            }
        }
    }
}
