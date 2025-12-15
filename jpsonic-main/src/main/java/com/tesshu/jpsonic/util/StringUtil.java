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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtil {

    private static final Pair<String> ENV_MIME_DSF = Pair.of("jpsonic.mime.dsf", "audio/x-dsd");
    private static final Pair<String> ENV_MIME_DFF = Pair.of("jpsonic.mime.dff", "audio/x-dsd");

    public static final String ENCODING_UTF8 = "UTF-8";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private static final String MP4 = "audio/mp4";
    private static final long DURATION_FORMAT_THRESHOLD = 3600;
    private static final String[] FILE_SYSTEM_UNSAFE = { "/", "\\", "..", ":", "\"", "?", "*",
            "|" };
    private static final String[][] MIME_TYPES = { { "mp3", "audio/mpeg" }, { "ogg", "audio/ogg" },
            { "oga", "audio/ogg" }, { "opus", "audio/ogg" }, { "ogx", "application/ogg" },
            { "aac", MP4 }, { "m4a", MP4 }, { "m4b", MP4 }, { "flac", "audio/flac" },
            { "wav", "audio/x-wav" }, { "wma", "audio/x-ms-wma" },
            { "ape", "audio/x-monkeys-audio" }, { "mpc", "audio/x-musepack" },
            { "shn", "audio/x-shn" },
            { "dsf", Optional
                .ofNullable(System.getProperty(ENV_MIME_DSF.key))
                .orElse(ENV_MIME_DSF.defaultValue) },
            { "dff", Optional
                .ofNullable(System.getProperty(ENV_MIME_DFF.key))
                .orElse(ENV_MIME_DFF.defaultValue) },

            { "flv", "video/x-flv" }, { "avi", "video/avi" }, { "mpg", "video/mpeg" },
            { "mpeg", "video/mpeg" }, { "mp4", "video/mp4" }, { "m4v", "video/x-m4v" },
            { "mkv", "video/x-matroska" }, { "mov", "video/quicktime" },
            { "wmv", "video/x-ms-wmv" }, { "ogv", "video/ogg" }, { "divx", "video/divx" },
            { "m2ts", "video/MP2T" }, { "ts", "video/MP2T" }, { "webm", "video/webm" },

            { "gif", "image/gif" }, { "jpg", "image/jpeg" }, { "jpeg", "image/jpeg" },
            { "png", "image/png" }, { "bmp", "image/bmp" }, };

    /**
     * Disallow external instantiation.
     */
    private StringUtil() {
    }

    /**
     * Returns the proper MIME type for the given suffix.
     *
     * @param suffix The suffix, e.g., "mp3" or ".mp3".
     *
     * @return The corresponding MIME type, e.g., "audio/mpeg". If no MIME type is
     *         found, <code>application/octet-stream</code> is returned.
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

    public static String getSuffix(String mimeType) {
        for (String[] map : MIME_TYPES) {
            if (map[1].equalsIgnoreCase(mimeType)) {
                return map[0];
            }
        }
        return null;
    }

    public static String formatBytes(long byteCount, Locale locale) {
        return StringUtilBase.formatBytes(byteCount, locale);
    }

    /**
     * Formats a duration with minutes and seconds, e.g., "4:34" or "93:45"
     */
    public static String formatDurationMSS(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds must be >= 0");
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Formats a duration with H:MM:SS, e.g., "1:33:45"
     */
    public static String formatDurationHMMSS(final long sec) {
        long seconds = sec;
        long hours = seconds == 0 ? 0 : seconds / 3600;
        seconds -= hours * 3600;
        return String
            .format("%d:%s%s", hours, seconds < 600 ? "0" : "", formatDurationMSS(seconds));
    }

    /**
     * Formats a duration to M:SS or H:MM:SS
     */
    public static String formatDuration(int seconds) {
        if (seconds >= DURATION_FORMAT_THRESHOLD) {
            return formatDurationHMMSS(seconds);
        }
        return formatDurationMSS(seconds);
    }

    /**
     * Splits the input string. White space is interpreted as separator token.
     * Double quotes are interpreted as grouping operator. <br/>
     * For instance, the input <code>"u2 rem "greatest hits""</code> will return an
     * array with three elements: <code>{"u2", "rem", "greatest hits"}</code>
     *
     * @param input The input string.
     *
     * @return Array of elements.
     */
    public static List<String> split(String input) {
        if (input == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        Matcher m = SPLIT_PATTERN.matcher(input);
        while (m.find()) {
            if (m.group(1) == null) {
                result.add(m.group(2)); // unquoted string
            } else {
                result.add(m.group(1)); // quoted string
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Reads lines from the given input stream. All lines are trimmed. Empty lines
     * and lines starting with "#" are skipped. The input stream is always closed by
     * this method.
     *
     * @param in The input stream to read from.
     *
     * @return Array of lines.
     *
     * @throws IOException If an I/O error occurs.
     */
    public static List<String> readLines(@NonNull InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> result = new ArrayList<>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String trimed = line.trim();
                if (!trimed.isEmpty() && trimed.charAt(0) != '#') {
                    result.add(trimed);
                }
            }
            return result;
        }
    }

    /**
     * Converts the given string of whitespace-separated integers to an
     * <code>int</code> array.
     *
     * @param s String consisting of integers separated by whitespace.
     *
     * @return The corresponding array of ints.
     *
     * @throws NumberFormatException If string contains non-parseable text.
     */
    public static @NonNull int[] parseInts(String s) {
        if (s == null) {
            return new int[0];
        }

        return Stream.of(StringUtils.split(s)).mapToInt(Integer::parseInt).toArray();
    }

    /**
     * Parses a locale from the given string.
     *
     * @param s The locale string. Should be formatted as per the documentation in
     *          {@link Locale#toString()}.
     *
     * @return The locale.
     */
    public static @NonNull Locale parseLocale(String s) {
        if (s == null || s.isEmpty() || s.equals("_")) {
            return Locale.getDefault();
        }

        try {
            return new Locale.Builder().setLanguageTag(s.replace('_', '-')).build();
        } catch (java.util.IllformedLocaleException e) {
            return Locale.getDefault();
        }
    }

    /**
     * URL-encodes the input value using UTF-8.
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * URL-decodes the input value using UTF-8.
     */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new CompletionException(x);
        }
    }

    public static @Nullable String utf8HexEncode(String s) {
        return StringUtilBase.utf8HexEncode(s);
    }

    public static @Nullable String utf8HexDecode(String s) throws DecoderException {
        return StringUtilBase.utf8HexDecode(s);
    }

    /**
     * Returns the file part of an URL. For instance:
     * <p/>
     * <code>
     * getUrlFile("http://archive.ncsa.uiuc.edu:80/SDG/Software/Mosaic/Demo/url-primer.html")
     * </code>
     * <p/>
     * will return "url-primer.html".
     *
     * @param url The URL in question.
     *
     * @return The file part, or <code>null</code> if no file can be resolved.
     */
    public static @Nullable String getUrlFile(String url) {
        try {
            String pathString = new URL(url).getPath();
            if (StringUtils.isBlank(pathString) || pathString.endsWith("/")) {
                return null;
            }

            Path path = Path.of(pathString);
            Path filename = path.getFileName();
            if (filename == null) {
                return null;
            }
            return filename.toString();

        } catch (MalformedURLException x) {
            return null;
        }
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes ("/"
     * and "\") with dashes ("-").
     *
     * @param filename The filename in question.
     *
     * @return The filename with special characters replaced by underscores.
     */
    public static String fileSystemSafe(final String filename) {
        String result = filename;
        for (String s : FILE_SYSTEM_UNSAFE) {
            result = result.replaceAll("\\.$", "").replace(s, "-");
        }
        return result;
    }

    public static @Nullable String removeMarkup(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("<.*?>", "");
    }

    @SuppressWarnings("PMD.ShortClassName")
    static final class Pair<V> {
        final String key;
        final V defaultValue;

        private Pair(String key, V defaultValue) {
            super();
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public static <V> Pair<V> of(String key, V defaultValue) {
            return new Pair<>(key, defaultValue);
        }
    }

    /**
     * Compares two strings for equality, considering nulls. This method performs a
     * case-sensitive comparison.
     *
     * @param a first string, may be null
     * @param b second string, may be null
     * @return true if both are null or equal by {@link String#equals(Object)}
     */
    @SuppressWarnings("PMD.UseEqualsToCompareStrings")
    public static boolean equals(String a, String b) {
        return a == b || (a != null && a.equals(b));
    }

    /*
     * Utility methods for string comparisons ignoring case differences, implemented
     * similarly to Apache Commons Lang3 but without locale-sensitive handling.
     * These methods use {@link String#regionMatches(boolean, int, String, int,
     * int)} with case-insensitive flag for comparisons.
     *
     * <p><b>Note:</b> These implementations do not consider locale-specific case
     * mappings (e.g., Turkish dotted/dotless I, German ß). Therefore, comparisons
     * may yield unexpected results in such locales.
     *
     * <p>This is a simplified replacement for deprecated Lang3 methods like {@code
     * StringUtils.equalsIgnoreCase} and similar, aimed to reduce dependencies.
     * Locale-aware comparison can be added later if needed.
     */

    /**
     * Compares two strings for equality ignoring case differences.
     * <p>
     * Uses {@link String#regionMatches(boolean, int, String, int, int)} with
     * case-insensitive flag, but does NOT handle locale-specific rules.
     *
     * @param a first string, may be null
     * @param b second string, may be null
     * @return true if both strings are equal ignoring case, or both null
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.length() == b.length() && a.regionMatches(true, 0, b, 0, a.length());
    }

    /**
     * Checks if {@code str} contains {@code searchStr} ignoring case.
     * <p>
     * Uses {@link String#regionMatches(boolean, int, String, int, int)} for
     * comparison, without locale-specific case mapping.
     *
     * @param str       string to search in, may be null
     * @param searchStr string to search for, may be null
     * @return true if {@code searchStr} is found within {@code str} ignoring case
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int length = searchStr.length();
        final int max = str.length() - length;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, length)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if {@code str} starts with {@code prefix} ignoring case.
     * <p>
     * Uses {@link String#regionMatches(boolean, int, String, int, int)} with
     * case-insensitive flag, without locale-specific rules.
     *
     * @param str    string to check, may be null
     * @param prefix prefix to find, may be null
     * @return true if {@code str} starts with {@code prefix} ignoring case
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str != null && prefix != null && prefix.length() <= str.length()
                && str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Checks if {@code str} ends with {@code suffix} ignoring case.
     * <p>
     * Uses {@link String#regionMatches(boolean, int, String, int, int)} with
     * case-insensitive flag, without locale-specific rules.
     *
     * @param str    string to check, may be null
     * @param suffix suffix to find, may be null
     * @return true if {@code str} ends with {@code suffix} ignoring case
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }

        int strLen = str.length();
        int suffixLen = suffix.length();
        return suffixLen <= strLen
                && str.regionMatches(true, strLen - suffixLen, suffix, 0, suffixLen);
    }

    /**
     * Removes the specified prefix from the start of the given string, if present.
     * <p>
     * If {@code str} starts with {@code remove}, the prefix is removed and the
     * resulting substring is returned. Otherwise, the original string is returned
     * unchanged.
     * <p>
     * If either {@code str} or {@code remove} is {@code null}, the original
     * {@code str} is returned.
     *
     * @param str    the string to process, may be {@code null}
     * @param remove the prefix to remove, may be {@code null}
     * @return the substring without the prefix if present, or the original string
     *         if not; returns {@code null} if {@code str} is {@code null}
     */
    public static String removeStart(String str, String remove) {
        if (str != null && remove != null && str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }
}
