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
import java.nio.file.Path;

/**
 * Miscellaneous file utility methods.
 *
 * @author Sindre Mehus
 */
public final class FileUtil {

    /**
     * Disallow external instantiation.
     */
    private FileUtil() {
    }

    /**
     * Returns a short path for the given file. The path consists of the name of the parent directory and the given
     * file.
     */
    public static String getShortPath(Path path) {
        if (path == null) {
            return null;
        }
        Path parent = path.getParent();
        if (parent == null) {
            return path.getFileName().toString();
        }
        return parent.getFileName().toString() + File.separator + path.getFileName().toString();
    }
}
