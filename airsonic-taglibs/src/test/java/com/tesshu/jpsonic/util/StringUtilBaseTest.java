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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link StringUtil}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class StringUtilBaseTest {

    @Test
    void testFormatBytes() {
        Locale locale = Locale.ENGLISH;
        assertEquals("918 B", StringUtilBase.formatBytes(918, locale), "Error in formatBytes().");
        assertEquals("1023 B", StringUtilBase.formatBytes(1_023, locale),
                "Error in formatBytes().");
        assertEquals("1 KB", StringUtilBase.formatBytes(1_024, locale), "Error in formatBytes().");
        assertEquals("96 KB", StringUtilBase.formatBytes(98_765, locale),
                "Error in formatBytes().");
        assertEquals("1024 KB", StringUtilBase.formatBytes(1_048_575, locale),
                "Error in formatBytes().");
        assertEquals("1.2 MB", StringUtilBase.formatBytes(1_238_476, locale),
                "Error in formatBytes().");
        assertEquals("3.50 GB", StringUtilBase.formatBytes(3_758_096_384L, locale),
                "Error in formatBytes().");
        assertEquals("410.00 TB", StringUtilBase.formatBytes(450_799_767_388_160L, locale),
                "Error in formatBytes().");
        assertEquals("4413.43 TB", StringUtilBase.formatBytes(4_852_617_603_375_432L, locale),
                "Error in formatBytes().");

        locale = Locale.FRANCE;
        assertEquals("918 B", StringUtilBase.formatBytes(918, locale), "Error in formatBytes().");
        assertEquals("1023 B", StringUtilBase.formatBytes(1023, locale), "Error in formatBytes().");
        assertEquals("1 KB", StringUtilBase.formatBytes(1024, locale), "Error in formatBytes().");
        assertEquals("96 KB", StringUtilBase.formatBytes(98_765, locale),
                "Error in formatBytes().");
        assertEquals("1024 KB", StringUtilBase.formatBytes(1_048_575, locale),
                "Error in formatBytes().");
        assertEquals("1,2 MB", StringUtilBase.formatBytes(1_238_476, locale),
                "Error in formatBytes().");
        assertEquals("3,50 GB", StringUtilBase.formatBytes(3_758_096_384L, locale),
                "Error in formatBytes().");
        assertEquals("410,00 TB", StringUtilBase.formatBytes(450_799_767_388_160L, locale),
                "Error in formatBytes().");
        assertEquals("4413,43 TB", StringUtilBase.formatBytes(4_852_617_603_375_432L, locale),
                "Error in formatBytes().");
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
        assertEquals(s, StringUtilBase.utf8HexDecode(StringUtilBase.utf8HexEncode(s)),
                "Error in utf8hex.");
        return true;
    }
}
