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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

    private static final Pair<String> ENV_MIME_DSF = Pair.of("jpsonic.mime.dsf", "audio/x-dsd");
    private static final Pair<String> ENV_MIME_DFF = Pair.of("jpsonic.mime.dff", "audio/x-dsd");

    public static final String ENCODING_UTF8 = "UTF-8";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private static final String MP4 = "audio/mp4";
    private static final long BINARY_1KB = 1024L;
    private static final long DURATION_FORMAT_THRESHOLD = 3600;
    private static final String[] FILE_SYSTEM_UNSAFE = { "/", "\\", "..", ":", "\"", "?", "*", "|" };
    public static final Object FORMAT_LOCK = new Object();
    private static final String[][] MIME_TYPES = { { "mp3", "audio/mpeg" }, { "ogg", "audio/ogg" },
            { "oga", "audio/ogg" }, { "opus", "audio/ogg" }, { "ogx", "application/ogg" }, { "aac", MP4 },
            { "m4a", MP4 }, { "m4b", MP4 }, { "flac", "audio/flac" }, { "wav", "audio/x-wav" },
            { "wma", "audio/x-ms-wma" }, { "ape", "audio/x-monkeys-audio" }, { "mpc", "audio/x-musepack" },
            { "shn", "audio/x-shn" },
            { "dsf", Optional.ofNullable(System.getProperty(ENV_MIME_DSF.key)).orElse(ENV_MIME_DSF.defaultValue) },
            { "dff", Optional.ofNullable(System.getProperty(ENV_MIME_DFF.key)).orElse(ENV_MIME_DFF.defaultValue) },

            { "flv", "video/x-flv" }, { "avi", "video/avi" }, { "mpg", "video/mpeg" }, { "mpeg", "video/mpeg" },
            { "mp4", "video/mp4" }, { "m4v", "video/x-m4v" }, { "mkv", "video/x-matroska" },
            { "mov", "video/quicktime" }, { "wmv", "video/x-ms-wmv" }, { "ogv", "video/ogg" }, { "divx", "video/divx" },
            { "m2ts", "video/MP2T" }, { "ts", "video/MP2T" }, { "webm", "video/webm" },

            { "gif", "image/gif" }, { "jpg", "image/jpeg" }, { "jpeg", "image/jpeg" }, { "png", "image/png" },
            { "bmp", "image/bmp" }, };

    /**
     * Disallow external instantiation.
     */
    private StringUtil() {
    }

    /**
     * Returns the proper MIME type for the given suffix.
     *
     * @param suffix
     *            The suffix, e.g., "mp3" or ".mp3".
     *
     * @return The corresponding MIME type, e.g., "audio/mpeg". If no MIME type is found,
     *         <code>application/octet-stream</code> is returned.
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

    /**
     * Converts a byte-count to a formatted string suitable for display to the user. For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     *
     * @param byteCount
     *            The number of bytes.
     * @param locale
     *            The locale used for formatting.
     *
     * @return The formatted string.
     */
    public static String formatBytes(long byteCount, Locale locale) {
        synchronized (FORMAT_LOCK) {
            // More than 1 TB?
            if (byteCount >= BINARY_1KB * 1024 * 1024 * 1024) {
                NumberFormat teraByteFormat = new DecimalFormat("0.00 TB", new DecimalFormatSymbols(locale));
                return teraByteFormat.format(byteCount / ((double) 1024 * 1024 * 1024 * 1024));
            }

            // More than 1 GB?
            if (byteCount >= BINARY_1KB * 1024 * 1024) {
                NumberFormat gigaByteFormat = new DecimalFormat("0.00 GB", new DecimalFormatSymbols(locale));
                return gigaByteFormat.format(byteCount / ((double) 1024 * 1024 * 1024));
            }

            // More than 1 MB?
            if (byteCount >= BINARY_1KB * 1024) {
                NumberFormat megaByteFormat = new DecimalFormat("0.0 MB", new DecimalFormatSymbols(locale));
                return megaByteFormat.format(byteCount / ((double) 1024 * 1024));
            }

            // More than 1 KB?
            if (byteCount >= BINARY_1KB) {
                NumberFormat kiloByteFormat = new DecimalFormat("0 KB", new DecimalFormatSymbols(locale));
                return kiloByteFormat.format((double) byteCount / 1024);
            }

            return byteCount + " B";
        }
    }

    /**
     * Formats a duration with minutes and seconds, e.g., "4:34" or "93:45"
     */
    public static String formatDurationMSS(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds must be >= 0");
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Formats a duration with H:MM:SS, e.g., "1:33:45"
     */
    public static String formatDurationHMMSS(final int sec) {
        int seconds = sec;
        int hours = seconds / 3600;
        seconds -= hours * 3600;
        return String.format("%d:%s%s", hours, seconds < 600 ? "0" : "", formatDurationMSS(seconds));
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
     * Splits the input string. White space is interpreted as separator token. Double quotes are interpreted as grouping
     * operator. <br/>
     * For instance, the input <code>"u2 rem "greatest hits""</code> will return an array with three elements:
     * <code>{"u2", "rem", "greatest hits"}</code>
     *
     * @param input
     *            The input string.
     *
     * @return Array of elements.
     */
    public static @NonNull String[] split(String input) {
        if (input == null) {
            return new String[0];
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

        return result.toArray(new String[0]);
    }

    /**
     * Reads lines from the given input stream. All lines are trimmed. Empty lines and lines starting with "#" are
     * skipped. The input stream is always closed by this method.
     *
     * @param in
     *            The input stream to read from.
     *
     * @return Array of lines.
     *
     * @throws IOException
     *             If an I/O error occurs.
     */
    @SuppressWarnings("PMD.UseTryWithResources") // False positive. pmd/pmd/issues/2882
    public static String[] readLines(@NonNull InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> result = new ArrayList<>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String trimed = line.trim();
                if (!trimed.isEmpty() && trimed.charAt(0) != '#') {
                    result.add(trimed);
                }
            }
            return result.toArray(new String[0]);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Unable to close Stream.", e);
                    }
                }
            }
        }
    }

    /**
     * Converts the given string of whitespace-separated integers to an <code>int</code> array.
     *
     * @param s
     *            String consisting of integers separated by whitespace.
     *
     * @return The corresponding array of ints.
     *
     * @throws NumberFormatException
     *             If string contains non-parseable text.
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
     * @param s
     *            The locale string. Should be formatted as per the documentation in {@link Locale#toString()}.
     *
     * @return The locale.
     */
    public static @Nullable Locale parseLocale(String s) {
        if (s == null) {
            return null;
        }

        List<String> elements = new ArrayList<>(Arrays.asList(s.split("_", 3)));
        while (elements.size() < 3) {
            elements.add("");
        }
        return new Locale(elements.get(0), elements.get(1), elements.get(2));
    }

    /**
     * URL-encodes the input value using UTF-8.
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StringUtil.ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * URL-decodes the input value using UTF-8.
     */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StringUtil.ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s
     *            The string to encode.
     *
     * @return The encoded string.
     */
    public static @Nullable String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        utf8 = s.getBytes(StandardCharsets.UTF_8);
        return String.valueOf(Hex.encodeHex(utf8));
    }

    /**
     * Decodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s
     *            The string to decode.
     *
     * @return The decoded string.
     *
     * @throws DecoderException
     *             If an error occurs.
     */
    public static @Nullable String utf8HexDecode(String s) throws DecoderException {
        if (s == null) {
            return null;
        }
        return new String(Hex.decodeHex(s.toCharArray()), StandardCharsets.UTF_8);
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
     * @param url
     *            The URL in question.
     *
     * @return The file part, or <code>null</code> if no file can be resolved.
     */
    public static @Nullable String getUrlFile(String url) {
        try {
            String path = new URL(url).getPath();
            if (StringUtils.isBlank(path) || path.endsWith("/")) {
                return null;
            }

            File file = new File(path);
            String filename = file.getName();
            if (StringUtils.isBlank(filename)) {
                return null;
            }
            return filename;

        } catch (MalformedURLException x) {
            return null;
        }
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes ("/" and "\") with dashes ("-").
     *
     * @param filename
     *            The filename in question.
     *
     * @return The filename with special characters replaced by underscores.
     */
    public static String fileSystemSafe(final String filename) {
        String result = filename;
        for (String s : FILE_SYSTEM_UNSAFE) {
            result = result.replace(s, "-");
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
    static class Pair<V> {
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
}
