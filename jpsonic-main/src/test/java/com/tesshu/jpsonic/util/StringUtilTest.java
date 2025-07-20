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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link StringUtil}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class StringUtilTest {

    @Test
    void testToHtml() {
        assertNull(StringEscapeUtils.escapeHtml4(null));
        assertEquals(StringEscapeUtils.escapeHtml4(""), "");
        assertEquals(StringEscapeUtils.escapeHtml4(" "), " ");
        assertEquals(StringEscapeUtils.escapeHtml4("q & a"), "q &amp; a");
        assertEquals(StringEscapeUtils.escapeHtml4("q & a <> b"), "q &amp; a &lt;&gt; b");
    }

    @Test
    void testGetMimeType() {
        assertEquals("audio/mpeg", StringUtil.getMimeType("mp3"), "Error in getMimeType().");
        assertEquals("audio/mpeg", StringUtil.getMimeType(".mp3"), "Error in getMimeType().");
        assertEquals("audio/mpeg", StringUtil.getMimeType(".MP3"), "Error in getMimeType().");
        assertEquals("application/octet-stream", StringUtil.getMimeType("koko"),
                "Error in getMimeType().");
        assertEquals("application/octet-stream", StringUtil.getMimeType(""),
                "Error in getMimeType().");
        assertEquals("application/octet-stream", StringUtil.getMimeType(null),
                "Error in getMimeType().");
    }

    @Test
    void testFormatDurationMSS() {
        assertEquals("0:00", StringUtil.formatDurationMSS(0), "Error in formatDurationMSS().");
        assertEquals("0:05", StringUtil.formatDurationMSS(5), "Error in formatDurationMSS().");
        assertEquals("0:10", StringUtil.formatDurationMSS(10), "Error in formatDurationMSS().");
        assertEquals("0:59", StringUtil.formatDurationMSS(59), "Error in formatDurationMSS().");
        assertEquals("1:00", StringUtil.formatDurationMSS(60), "Error in formatDurationMSS().");
        assertEquals("1:01", StringUtil.formatDurationMSS(61), "Error in formatDurationMSS().");
        assertEquals("1:10", StringUtil.formatDurationMSS(70), "Error in formatDurationMSS().");
        assertEquals("10:00", StringUtil.formatDurationMSS(600), "Error in formatDurationMSS().");
        assertEquals("45:50", StringUtil.formatDurationMSS(2750), "Error in formatDurationMSS().");
        assertEquals("83:45", StringUtil.formatDurationMSS(5025), "Error in formatDurationMSS().");
        assertEquals("121:40", StringUtil.formatDurationMSS(7300), "Error in formatDurationMSS().");
    }

    @Test
    void testFormatDurationHMMSS() {
        assertEquals("0:00:00", StringUtil.formatDurationHMMSS(0),
                "Error in formatDurationHMMSS().");
        assertEquals("0:00:05", StringUtil.formatDurationHMMSS(5),
                "Error in formatDurationHMMSS().");
        assertEquals("0:00:10", StringUtil.formatDurationHMMSS(10),
                "Error in formatDurationHMMSS().");
        assertEquals("0:00:59", StringUtil.formatDurationHMMSS(59),
                "Error in formatDurationHMMSS().");
        assertEquals("0:01:00", StringUtil.formatDurationHMMSS(60),
                "Error in formatDurationHMMSS().");
        assertEquals("0:01:01", StringUtil.formatDurationHMMSS(61),
                "Error in formatDurationHMMSS().");
        assertEquals("0:01:10", StringUtil.formatDurationHMMSS(70),
                "Error in formatDurationHMMSS().");
        assertEquals("0:10:00", StringUtil.formatDurationHMMSS(600),
                "Error in formatDurationHMMSS().");
        assertEquals("0:45:50", StringUtil.formatDurationHMMSS(2750),
                "Error in formatDurationHMMSS().");
        assertEquals("1:23:45", StringUtil.formatDurationHMMSS(5025),
                "Error in formatDurationHMMSS().");
        assertEquals("2:01:40", StringUtil.formatDurationHMMSS(7300),
                "Error in formatDurationHMMSS().");
    }

    @Test
    void testFormatDuration() {
        assertEquals("0:00", StringUtil.formatDuration(0), "Error in formatDuration().");
        assertEquals("0:05", StringUtil.formatDuration(5), "Error in formatDuration().");
        assertEquals("0:10", StringUtil.formatDuration(10), "Error in formatDuration().");
        assertEquals("0:59", StringUtil.formatDuration(59), "Error in formatDuration().");
        assertEquals("1:00", StringUtil.formatDuration(60), "Error in formatDuration().");
        assertEquals("1:01", StringUtil.formatDuration(61), "Error in formatDuration().");
        assertEquals("1:10", StringUtil.formatDuration(70), "Error in formatDuration().");
        assertEquals("10:00", StringUtil.formatDuration(600), "Error in formatDuration().");
        assertEquals("45:50", StringUtil.formatDuration(2750), "Error in formatDuration().");
        assertEquals("1:23:45", StringUtil.formatDuration(5025), "Error in formatDuration().");
        assertEquals("2:01:40", StringUtil.formatDuration(7300), "Error in formatDuration().");
    }

    @Test
    void testSplit() {
        doTestSplit("u2 rem \"greatest hits\"", "u2", "rem", "greatest hits");
        doTestSplit("u2", "u2");
        doTestSplit("u2 rem", "u2", "rem");
        doTestSplit(" u2  \t rem ", "u2", "rem");
        doTestSplit("u2 \"rem\"", "u2", "rem");
        doTestSplit("u2 \"rem", "u2", "\"rem");
        doTestSplit("\"", "\"");

        assertEquals(0, StringUtil.split("").size());
        assertEquals(0, StringUtil.split(" ").size());
        assertEquals(0, StringUtil.split(null).size());
    }

    private void doTestSplit(String input, String... expected) {
        List<String> actual = StringUtil.split(input);
        assertEquals(expected.length, actual.size(), "Wrong number of elements.");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i), "Wrong criteria.");
        }
    }

    @Test
    void testParseInts() {
        doTestParseInts("123", 123);
        doTestParseInts("1 2 3", 1, 2, 3);
        doTestParseInts("10  20 \t\n 30", 10, 20, 30);

        assertSame(StringUtil.parseInts(null).length, 0, "Error in parseInts().");
        assertSame(StringUtil.parseInts("").length, 0, "Error in parseInts().");
        assertSame(StringUtil.parseInts(" ").length, 0, "Error in parseInts().");
        assertSame(StringUtil.parseInts("  ").length, 0, "Error in parseInts().");
    }

    private void doTestParseInts(String s, int... expected) {
        assertEquals(Arrays.toString(expected), Arrays.toString(StringUtil.parseInts(s)),
                "Error in parseInts().");
    }

    @Test
    void testParseLocale() {
        assertEquals(new Locale("en"), StringUtil.parseLocale("en"), "Error in parseLocale().");
        assertEquals(new Locale("en"), StringUtil.parseLocale("en_"), "Error in parseLocale().");
        assertEquals(new Locale("en"), StringUtil.parseLocale("en__"), "Error in parseLocale().");
        assertEquals(new Locale("en", "US"), StringUtil.parseLocale("en_US"),
                "Error in parseLocale().");
        assertEquals(new Locale("en", "US", "WIN"), StringUtil.parseLocale("en_US_WIN"),
                "Error in parseLocale().");
        assertEquals(new Locale("en", "", "WIN"), StringUtil.parseLocale("en__WIN"),
                "Error in parseLocale().");
    }

    @Test
    void testGetUrlFile() {
        assertEquals("foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/foo.mp3"),
                "Error in getUrlFile().");
        assertEquals("foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/bar/foo.mp3"),
                "Error in getUrlFile().");
        assertEquals("foo", StringUtil.getUrlFile("http://www.asdf.com/bar/foo"),
                "Error in getUrlFile().");
        assertEquals("foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/bar/foo.mp3?a=1&b=2"),
                "Error in getUrlFile().");
        assertNull(StringUtil.getUrlFile("not a url"), "Error in getUrlFile().");
        assertNull(StringUtil.getUrlFile("http://www.asdf.com"), "Error in getUrlFile().");
        assertNull(StringUtil.getUrlFile("http://www.asdf.com/"), "Error in getUrlFile().");
        assertNull(StringUtil.getUrlFile("http://www.asdf.com/foo/"), "Error in getUrlFile().");
    }

    @Test
    void testFileSystemSafe() {
        assertEquals("foo", StringUtil.fileSystemSafe("foo"), "Error in fileSystemSafe().");
        assertEquals("foo.mp3", StringUtil.fileSystemSafe("foo.mp3"), "Error in fileSystemSafe().");
        assertEquals("foo.mp3", StringUtil.fileSystemSafe("foo.mp3..."),
                "Error in fileSystemSafe().");
        assertEquals("foo-bar", StringUtil.fileSystemSafe("foo/bar"), "Error in fileSystemSafe().");
        assertEquals("foo-bar", StringUtil.fileSystemSafe("foo\\bar"),
                "Error in fileSystemSafe().");
        assertEquals("foo-bar", StringUtil.fileSystemSafe("foo:bar"), "Error in fileSystemSafe().");
    }

    @Test
    void testRemoveMarkup() {
        assertEquals("foo", StringUtil.removeMarkup("<b>foo</b>"), "Error in removeMarkup()");
        assertEquals("foobar", StringUtil.removeMarkup("<b>foo</b>bar"), "Error in removeMarkup()");
        assertEquals("foo", StringUtil.removeMarkup("foo"), "Error in removeMarkup()");
        assertEquals("foo", StringUtil.removeMarkup("<b>foo"), "Error in removeMarkup()");
        assertNull(StringUtil.removeMarkup(null), "Error in removeMarkup()");
    }

    @Test
    void testEquals() {
        assertTrue(StringUtil.equals(null, null));
        assertFalse(StringUtil.equals(null, "a"));
        assertFalse(StringUtil.equals("a", null));
        assertTrue(StringUtil.equals("a", "a"));
        assertFalse(StringUtil.equals("a", "A"));
        assertFalse(StringUtil.equals("a", "b"));
        assertFalse(StringUtil.equals("abc", "c"));
        assertFalse(StringUtil.equals("abc", "d"));
        assertFalse(StringUtil.equals("i", "İ"));

        String s = "a";
        assertTrue(StringUtil.equals(s, s));
        assertTrue(StringUtil.equals("", ""));
        assertFalse(StringUtil.equals("", "a"));
        assertFalse(StringUtil.equals("a", ""));
        assertTrue(StringUtil.equals("こんにちは", "こんにちは"));
        assertFalse(StringUtil.equals("こんにちは", "こんばんは"));
        assertFalse(StringUtil.equals("abcd", "abce"));
        assertFalse(StringUtil.equals("a\nb", "a b"));
    }

    @Test
    void testEqualsIgnoreCase() {
        assertTrue(StringUtil.equalsIgnoreCase(null, null));
        assertFalse(StringUtil.equalsIgnoreCase(null, "a"));
        assertFalse(StringUtil.equalsIgnoreCase("a", null));
        assertTrue(StringUtil.equalsIgnoreCase("a", "a"));
        assertTrue(StringUtil.equalsIgnoreCase("a", "A"));
        assertFalse(StringUtil.equalsIgnoreCase("a", "b"));
        assertFalse(StringUtil.equalsIgnoreCase("abc", "c"));
        assertFalse(StringUtil.equalsIgnoreCase("abc", "d"));
        assertTrue(StringUtil.containsIgnoreCase("i", "İ"));
    }

    @Test
    void testContainsIgnoreCase() {
        assertFalse(StringUtil.containsIgnoreCase(null, "a"));
        assertFalse(StringUtil.containsIgnoreCase("a", null));
        assertTrue(StringUtil.containsIgnoreCase("a", "a"));
        assertTrue(StringUtil.containsIgnoreCase("a", "A"));
        assertTrue(StringUtil.containsIgnoreCase("abc", "c"));
        assertFalse(StringUtil.containsIgnoreCase("abc", "d"));
        assertTrue(StringUtil.containsIgnoreCase("i", "İ"));
    }

    @Test
    void testStartsWithIgnoreCase() {
        assertTrue(StringUtil.startsWithIgnoreCase("abc", "a"));
        assertTrue(StringUtil.startsWithIgnoreCase("abc", "abc"));
        assertTrue(StringUtil.startsWithIgnoreCase("abc", ""));
        assertFalse(StringUtil.startsWithIgnoreCase("abc", "abcd"));
        assertFalse(StringUtil.startsWithIgnoreCase("abc", "b"));
        assertFalse(StringUtil.startsWithIgnoreCase(null, "a"));
        assertFalse(StringUtil.startsWithIgnoreCase("abc", null));
        assertFalse(StringUtil.startsWithIgnoreCase(null, null));
        assertTrue(StringUtil.startsWithIgnoreCase("Straße", "str"));
        assertFalse(StringUtil.startsWithIgnoreCase("Straße", "sse"));
    }

    @Test
    void testEndsWithIgnoreCase() {
        assertTrue(StringUtil.endsWithIgnoreCase("hello", "LO"));
        assertTrue(StringUtil.endsWithIgnoreCase("HELLO", "lo"));
        assertTrue(StringUtil.endsWithIgnoreCase("hello", "hello"));
        assertTrue(StringUtil.endsWithIgnoreCase("HelloWorld", "world"));
        assertFalse(StringUtil.endsWithIgnoreCase("hello", "HELLOO"));
        assertFalse(StringUtil.endsWithIgnoreCase("hello", "hell"));

        assertFalse(StringUtil.endsWithIgnoreCase(null, "a"));
        assertFalse(StringUtil.endsWithIgnoreCase("a", null));
        assertFalse(StringUtil.endsWithIgnoreCase(null, null));
        assertTrue(StringUtil.endsWithIgnoreCase("", ""));
        assertTrue(StringUtil.endsWithIgnoreCase("a", ""));
        assertFalse(StringUtil.endsWithIgnoreCase("", "a"));

        assertFalse(StringUtil.endsWithIgnoreCase("Straße", "SSE"));
        assertTrue(StringUtil.endsWithIgnoreCase("İstanbul", "istanbul"));
    }

    @Test
    void testRemoveStart() {
        assertEquals("bar", StringUtil.removeStart("foobar", "foo"));
        assertEquals("foobar", StringUtil.removeStart("foobar", "baz"));
        assertNull(StringUtil.removeStart(null, "foo"));
        assertEquals("foobar", StringUtil.removeStart("foobar", null));
    }
}
