package com.tesshu.jpsonic.infrastructure.language;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language and string utility methods used by Jpsonic.
 *
 * <p>
 * This class contains Jpsonic-specific implementations and lightweight wrappers
 * around standard Java APIs. It does not provide the legacy functionality
 * formerly supplied by the Airsonic utility class.
 */
public class StringUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

    private StringUtil() {
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
        if (s == null || s.isEmpty() || "_".equals(s)) {
            return Locale.getDefault();
        }

        try {
            return new Locale.Builder().setLanguageTag(s.replace('_', '-')).build();
        } catch (java.util.IllformedLocaleException e) {
            return Locale.getDefault();
        }
    }

    public static @Nullable URL parseURL(String s) throws MalformedURLException {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        try {
            return URI.create(s).toURL();
        } catch (IllegalArgumentException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Invalid URL rejected by strict parsing (Java 20+): [{}]", s, e);
            }
            return null;
        }
    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
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
