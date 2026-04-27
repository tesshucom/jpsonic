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

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;

/**
 * Utility for mapping file extensions to MIME types and vice versa.
 * <p>
 * This class serves as a central dictionary for media types supported within
 * the application, ensuring consistent identification of audio, video, and
 * image formats.
 * </p>
 */
public final class MediaTypeDetector {

    private static final String MIME_MP4 = "audio/mp4";
    private static final String[][] MIME_TYPES = { { "mp3", "audio/mpeg" }, { "ogg", "audio/ogg" },
            { "oga", "audio/ogg" }, { "opus", "audio/ogg" }, { "ogx", "application/ogg" },
            { "aac", MIME_MP4 }, { "m4a", MIME_MP4 }, { "m4b", MIME_MP4 }, { "flac", "audio/flac" },
            { "wav", "audio/x-wav" }, { "wma", "audio/x-ms-wma" },
            { "ape", "audio/x-monkeys-audio" }, { "mpc", "audio/x-musepack" },
            { "shn", "audio/x-shn" }, { "dsf", EnvironmentProvider.getInstance().getMemeDsf() },
            { "dff", EnvironmentProvider.getInstance().getMemeDff() }, { "flv", "video/x-flv" },
            { "avi", "video/avi" }, { "mpg", "video/mpeg" }, { "mpeg", "video/mpeg" },
            { "mp4", "video/mp4" }, { "m4v", "video/x-m4v" }, { "mkv", "video/x-matroska" },
            { "mov", "video/quicktime" }, { "wmv", "video/x-ms-wmv" }, { "ogv", "video/ogg" },
            { "divx", "video/divx" }, { "m2ts", "video/MP2T" }, { "ts", "video/MP2T" },
            { "webm", "video/webm" },

            { "gif", "image/gif" }, { "jpg", "image/jpeg" }, { "jpeg", "image/jpeg" },
            { "png", "image/png" }, { "bmp", "image/bmp" }, };

    private MediaTypeDetector() {
        // no-op
    }

    /**
     * Determines the appropriate MIME type for a given file extension.
     * <p>
     * Supports suffixes both with and without a leading dot (e.g., ".mp3" or
     * "mp3").
     * </p>
     *
     * @param suffix The file extension to look up.
     * @return The corresponding MIME type, or "application/octet-stream" if no
     *         match is found.
     */
    public static String getMimeType(String suffix) {
        for (String[] typeAndValue : MIME_TYPES) {
            String type = typeAndValue[0];
            String value = typeAndValue[1];
            if (type.equalsIgnoreCase(suffix)) {
                return value;
            } else {
                String typeWithDot = '.' + type;
                if (typeWithDot.equalsIgnoreCase(suffix)) {
                    return typeAndValue[1];
                }
            }
        }
        return "application/octet-stream";
    }

    /**
     * Finds the primary file extension associated with a given MIME type.
     *
     * @param mimeType The MIME type to look up (e.g., "audio/mpeg").
     * @return The canonical extension without a dot (e.g., "mp3"), or null if
     *         unknown.
     */
    public static String getSuffix(String mimeType) {
        for (String[] map : MIME_TYPES) {
            if (map[1].equalsIgnoreCase(mimeType)) {
                return map[0];
            }
        }
        return null;
    }
}
