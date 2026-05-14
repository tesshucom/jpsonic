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

/**
 * Utility for enforcing file system compatibility by neutralizing potentially
 * hazardous character sequences.
 */
public class FileNameSanitizer {

    private static final String[] FILE_SYSTEM_UNSAFE = { "/", "\\", "..", ":", "\"", "?", "*",
            "|" };

    private FileNameSanitizer() {
        // no-op
    }

    /**
     * Transforms input strings (such as user input or external metadata) into a
     * format that is guaranteed to be safe for file system persistence.
     * <p>
     * The primary concern is ensuring the resulting string can be safely used as a
     * physical filename, regardless of whether the original semantic meaning of the
     * input is preserved.
     * </p>
     *
     * @param filename The raw input string to be sanitized.
     * @return A cleansed string where OS-prohibited characters and trailing dots
     *         are removed or replaced.
     */
    public static String sanitize(final String filename) {
        // Trim trailing dots
        int end = filename.length();
        while (end > 0 && filename.charAt(end - 1) == '.') {
            end--;
        }
        String result = (end == filename.length()) ? filename : filename.substring(0, end);

        // replace all unsafe characters
        for (String s : FILE_SYSTEM_UNSAFE) {
            result = result.replace(s, "-");
        }
        return result;
    }
}
