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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link StringUtil}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
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
        assertEquals("application/octet-stream", StringUtil.getMimeType("koko"), "Error in getMimeType().");
        assertEquals("application/octet-stream", StringUtil.getMimeType(""), "Error in getMimeType().");
        assertEquals("application/octet-stream", StringUtil.getMimeType(null), "Error in getMimeType().");
    }

    @Test
    void testFormatBytes() {
        Locale locale = Locale.ENGLISH;
        assertEquals("918 B", StringUtil.formatBytes(918, locale), "Error in formatBytes().");
        assertEquals("1023 B", StringUtil.formatBytes(1_023, locale), "Error in formatBytes().");
        assertEquals("1 KB", StringUtil.formatBytes(1_024, locale), "Error in formatBytes().");
        assertEquals("96 KB", StringUtil.formatBytes(98_765, locale), "Error in formatBytes().");
        assertEquals("1024 KB", StringUtil.formatBytes(1_048_575, locale), "Error in formatBytes().");
        assertEquals("1.2 MB", StringUtil.formatBytes(1_238_476, locale), "Error in formatBytes().");
        assertEquals("3.50 GB", StringUtil.formatBytes(3_758_096_384L, locale), "Error in formatBytes().");
        assertEquals("410.00 TB", StringUtil.formatBytes(450_799_767_388_160L, locale), "Error in formatBytes().");
        assertEquals("4413.43 TB", StringUtil.formatBytes(4_852_617_603_375_432L, locale), "Error in formatBytes().");

        locale = new Locale("no", "", "");
        assertEquals("918 B", StringUtil.formatBytes(918, locale), "Error in formatBytes().");
        assertEquals("1023 B", StringUtil.formatBytes(1023, locale), "Error in formatBytes().");
        assertEquals("1 KB", StringUtil.formatBytes(1024, locale), "Error in formatBytes().");
        assertEquals("96 KB", StringUtil.formatBytes(98_765, locale), "Error in formatBytes().");
        assertEquals("1024 KB", StringUtil.formatBytes(1_048_575, locale), "Error in formatBytes().");
        assertEquals("1,2 MB", StringUtil.formatBytes(1_238_476, locale), "Error in formatBytes().");
        assertEquals("3,50 GB", StringUtil.formatBytes(3_758_096_384L, locale), "Error in formatBytes().");
        assertEquals("410,00 TB", StringUtil.formatBytes(450_799_767_388_160L, locale), "Error in formatBytes().");
        assertEquals("4413,43 TB", StringUtil.formatBytes(4_852_617_603_375_432L, locale), "Error in formatBytes().");
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
        assertEquals("0:00:00", StringUtil.formatDurationHMMSS(0), "Error in formatDurationHMMSS().");
        assertEquals("0:00:05", StringUtil.formatDurationHMMSS(5), "Error in formatDurationHMMSS().");
        assertEquals("0:00:10", StringUtil.formatDurationHMMSS(10), "Error in formatDurationHMMSS().");
        assertEquals("0:00:59", StringUtil.formatDurationHMMSS(59), "Error in formatDurationHMMSS().");
        assertEquals("0:01:00", StringUtil.formatDurationHMMSS(60), "Error in formatDurationHMMSS().");
        assertEquals("0:01:01", StringUtil.formatDurationHMMSS(61), "Error in formatDurationHMMSS().");
        assertEquals("0:01:10", StringUtil.formatDurationHMMSS(70), "Error in formatDurationHMMSS().");
        assertEquals("0:10:00", StringUtil.formatDurationHMMSS(600), "Error in formatDurationHMMSS().");
        assertEquals("0:45:50", StringUtil.formatDurationHMMSS(2750), "Error in formatDurationHMMSS().");
        assertEquals("1:23:45", StringUtil.formatDurationHMMSS(5025), "Error in formatDurationHMMSS().");
        assertEquals("2:01:40", StringUtil.formatDurationHMMSS(7300), "Error in formatDurationHMMSS().");
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

        assertEquals(0, StringUtil.split("").length);
        assertEquals(0, StringUtil.split(" ").length);
        assertEquals(0, StringUtil.split(null).length);
    }

    private void doTestSplit(String input, String... expected) {
        String[] actual = StringUtil.split(input);
        assertEquals(expected.length, actual.length, "Wrong number of elements.");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "Wrong criteria.");
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
        assertEquals(Arrays.toString(expected), Arrays.toString(StringUtil.parseInts(s)), "Error in parseInts().");
    }

    @Test
    void testParseLocale() {
        assertEquals(new Locale("en"), StringUtil.parseLocale("en"), "Error in parseLocale().");
        assertEquals(new Locale("en"), StringUtil.parseLocale("en_"), "Error in parseLocale().");
        assertEquals(new Locale("en"), StringUtil.parseLocale("en__"), "Error in parseLocale().");
        assertEquals(new Locale("en", "US"), StringUtil.parseLocale("en_US"), "Error in parseLocale().");
        assertEquals(new Locale("en", "US", "WIN"), StringUtil.parseLocale("en_US_WIN"), "Error in parseLocale().");
        assertEquals(new Locale("en", "", "WIN"), StringUtil.parseLocale("en__WIN"), "Error in parseLocale().");
    }

    @Test
    void testUtf8Hex() throws DecoderException {
        assertTrue(doTestUtf8Hex(null));
        assertTrue(doTestUtf8Hex(""));
        assertTrue(doTestUtf8Hex("a"));
        assertTrue(doTestUtf8Hex("abcdefg"));
        assertTrue(doTestUtf8Hex("abc������"));
        assertTrue(doTestUtf8Hex("NRK P3 � FK Fotball"));
    }

    private boolean doTestUtf8Hex(String s) throws DecoderException {
        assertEquals(s, StringUtil.utf8HexDecode(StringUtil.utf8HexEncode(s)), "Error in utf8hex.");
        return true;
    }

    @Test
    void testGetUrlFile() {
        assertEquals("foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/foo.mp3"), "Error in getUrlFile().");
        assertEquals("foo.mp3", StringUtil.getUrlFile("http://www.asdf.com/bar/foo.mp3"), "Error in getUrlFile().");
        assertEquals("foo", StringUtil.getUrlFile("http://www.asdf.com/bar/foo"), "Error in getUrlFile().");
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
        assertEquals("foo-bar", StringUtil.fileSystemSafe("foo/bar"), "Error in fileSystemSafe().");
        assertEquals("foo-bar", StringUtil.fileSystemSafe("foo\\bar"), "Error in fileSystemSafe().");
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

}
