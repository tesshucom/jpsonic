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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider.PathGeometry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Validator for primary entry points of the library (Music, Playlist, and
 * Podcast folders).
 * <p>
 * This guard ensures that root-level paths registered in the system are
 * structurally sound and free from traversal risks or illegal OS-specific
 * formatting.
 * </p>
 */
@Component
public final class RootPathEntryGuard {

    private static final Pattern NO_TRAVERSAL = Pattern.compile("^(?!.*(\\.\\./|\\.\\.\\\\)).*$");

    private RootPathEntryGuard() {
    }

    /**
     * Verifies that the path string does not contain directory traversal sequences
     * (../ or ..\).
     *
     * @param path The path string to check.
     * @return true if the path is clean of traversal sequences; false otherwise.
     */
    public static boolean isStrictPath(String path) {
        return NO_TRAVERSAL.matcher(path).matches();
    }

    /**
     * Validates and normalizes the given folder path for registration as a system
     * root.
     * <p>
     * Checks for null/empty values, directory traversal, and illegal leading
     * backslashes (except for Windows UNC paths). Also ensures the path is
     * recognizable by the NIO provider.
     * </p>
     * 
     * @param folderPath The raw folder path string to validate.
     * @return An Optional containing the validated path string, or empty if
     *         unacceptable.
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    public static Optional<String> validateFolderPath(PathGeometry pathGeometry,
            String folderPath) {
        if (StringUtils.trimToNull(folderPath) == null) {
            return Optional.empty();
        }
        if (!isStrictPath(folderPath)) {
            return Optional.empty();
        }
        if ((pathGeometry != PathGeometry.WINDOWS || !folderPath.startsWith("\\\\"))
                && folderPath.charAt(0) == '\\') {
            return Optional.empty();
        }
        try {
            Path path = Path.of(folderPath);
            if (path.getFileName() == null) {
                return Optional.empty();
            }

            if (pathGeometry == PathGeometry.WSL) {
                // Full segment scan to reject any node starting with '.'
                for (Path segment : path) {
                    if (segment.toString().charAt(0) == '.') {
                        return Optional.empty();
                    }
                }

                // Enforce strict hierarchy depth
                int minCount = path.isAbsolute() ? 2 : 1;
                if (path.getNameCount() <= minCount) {
                    return Optional.empty();
                }
            }

            return Optional.of(path.toString());
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
