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

package com.tesshu.jpsonic.util;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PathValidator {

    private static final Pattern NO_TRAVERSAL = Pattern.compile("^(?!.*(\\.\\./|\\.\\.\\\\)).*$");

    private PathValidator() {
    }

    public static boolean isNoTraversal(String path) {
        return NO_TRAVERSAL.matcher(path).matches();
    }

    /**
     * Returns a path string pointing to the music, playlist, and podcast folders, if acceptable. otherwise empty.
     */
    public static Optional<String> validateFolderPath(String folderPath) {
        if (folderPath == null || PathValidator.isNoTraversal(folderPath)) {
            return Optional.ofNullable(folderPath);
        }
        return Optional.empty();
    }
}
