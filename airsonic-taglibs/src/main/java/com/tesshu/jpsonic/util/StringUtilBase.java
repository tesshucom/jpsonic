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

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtilBase {

    private static final long BINARY_1KB = 1024L;

    /**
     * Disallow external instantiation.
     */
    private StringUtilBase() {
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
}
