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

package com.tesshu.jpsonic.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous file utility methods.
 *
 * @author Sindre Mehus
 */
public final class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Disallow external instantiation.
     */
    private FileUtil() {
    }

    /**
     * Returns a short path for the given file. The path consists of the name of the parent directory and the given
     * file.
     */
    public static @Nullable String getShortPath(@Nullable Path path) {
        if (path == null) {
            return null;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        Path parent = path.getParent();
        if (parent == null) {
            return fileName.toString(); // Probably unreachable
        }
        Path parentFileName = parent.getFileName();
        if (parentFileName == null) {
            return File.separator + fileName.toString();
        }
        return parentFileName.toString() + File.separator + fileName.toString();
    }

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

    public static void deleteDirectory(Path path) {
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not be delete file recursively: {}", path);
            }
        }
    }

    public static void touch(final Path path) throws IOException {
        FileUtils.touch(path.toFile());
    }

    public static String byteCountToDisplaySize(final long size) {
        return FileUtils.byteCountToDisplaySize(size);
    }

    public static long sizeOfDirectory(final Path directory) {
        try {
            return PathUtils.countDirectory(directory).getByteCounter().get();
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not get size of directory: {}", directory);
            }
            return -1;
        }
    }
}
