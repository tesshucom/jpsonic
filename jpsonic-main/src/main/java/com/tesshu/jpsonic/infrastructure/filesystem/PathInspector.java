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

import static java.io.File.separator;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component for inspecting path structures, physical relationships, and
 * formatting path-related metadata.
 * <p>
 * This class provides geometric analysis of paths (e.g., hierarchy validation)
 * and utility methods for human-readable identification and size formatting.
 * </p>
 */
@Component
public class PathInspector {

    private static final Logger LOG = LoggerFactory.getLogger(PathInspector.class);
    private static final Pattern DOTS_IN_SLASH = Pattern.compile(".*(/|\\\\)\\.\\.(/|\\\\|$).*");

    private String sanitizeForLog(String value) {
        return value.replace('\r', '_').replace('\n', '_');
    }

    /**
     * Determines if a file is located within a specific folder hierarchy while
     * preventing directory traversal.
     * <p>
     * This performs a structural comparison of path components and explicitly
     * denies access if the path contains ".." sequences.
     * </p>
     *
     * @param file   The path to verify.
     * @param folder The parent folder that should contain the file.
     * @return true if the file is confirmed to be within the folder hierarchy;
     *         false otherwise.
     */
    public boolean isWithinHierarchy(final String file, final String folder) {
        if (isEmpty(file)) {
            return false;
        } else if (file.length() > 1_000 && LOG.isWarnEnabled()) {
            String head = sanitizeForLog(file.substring(0, 10));
            String tail = sanitizeForLog(file.substring(file.length() - 10, file.length()));
            LOG.warn("File path exceeds 1000 characters　:{} ... {}", head, tail);
        }

        // Deny access if file contains ".." surrounded by slashes (or end of line).
        if (DOTS_IN_SLASH.matcher(file).matches()) {
            return false;
        }

        Iterator<Path> fileIterator = Path.of(file).iterator();
        Iterator<Path> folderIterator = Path.of(folder).iterator();
        while (folderIterator.hasNext() && fileIterator.hasNext()) {
            String prefix = fileIterator.next().toString();
            String suffix = folderIterator.next().toString();
            if (prefix.length() != suffix.length()) {
                return false;
            }
            if (!prefix.regionMatches(true, 0, suffix, 0, suffix.length())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates a simplified identifier using the parent directory and filename.
     * <p>
     * Based on the heuristic that the parent directory represents an album and the
     * file represents a song. Intended for logging and diagnostic purposes where a
     * full path is too long but context is required.
     * </p>
     * 
     * @param path The path to identify.
     * @return A string combining the parent name and filename (e.g.,
     *         "Album/Song.mp3").
     */
    public static @Nullable String toIdentityName(@Nullable Path path) {
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
            return separator + fileName.toString();
        }
        return parentFileName.toString() + separator + fileName.toString();
    }

    /**
     * Converts a byte count into a human-readable string (e.g., "1.2 MB").
     *
     * @param size The size in bytes.
     * @return The formatted display size.
     */
    public static String byteCountToDisplaySize(final long size) {
        return FileUtils.byteCountToDisplaySize(size);
    }

    /**
     * Extracts the base name (filename without extension) from a path string.
     *
     * @param pathString The string representation of the path.
     * @return The base name of the file.
     */
    public static String getBaseName(final String pathString) {
        return FilenameUtils.getBaseName(pathString);
    }

    /**
     * Extracts the base name (filename without extension) from a Path object.
     *
     * @param path The Path object.
     * @return The base name of the file.
     */
    public static String getBaseName(final Path path) {
        return FilenameUtils.getBaseName(path.toString());
    }

    /**
     * Extracts the file extension from a path string.
     *
     * @param fileNamePath The string representation of the path.
     * @return The file extension, or an empty string if none exists.
     */
    public static String getExtension(final String fileNamePath) {
        return FilenameUtils.getExtension(fileNamePath);
    }

    /**
     * Extracts the file extension from a Path object.
     *
     * @param path The Path object.
     * @return The file extension, or an empty string if none exists.
     */
    public static String getExtension(final Path path) {
        return FilenameUtils.getExtension(path.getFileName().toString());
    }
}
