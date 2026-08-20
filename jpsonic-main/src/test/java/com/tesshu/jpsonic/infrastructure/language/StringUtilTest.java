package com.tesshu.jpsonic.infrastructure.language;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class StringUtilTest {

    @Test
    void testParseLocale() {
        assertEquals(new Locale.Builder().setLanguage("en").build(), StringUtil.parseLocale("en"),
                "Error in parseLocale().");
        assertEquals(Locale.getDefault(), StringUtil.parseLocale("en_"), "Error in parseLocale().");
        assertEquals(Locale.getDefault(), StringUtil.parseLocale("en__"),
                "Error in parseLocale().");
        assertEquals(new Locale.Builder().setLanguage("en").setRegion("US").build(),
                StringUtil.parseLocale("en_US"), "Error in parseLocale().");
        assertEquals(Locale.getDefault(), StringUtil.parseLocale("en_US_WIN"),
                "Error in parseLocale().");
        assertEquals(Locale.getDefault(), StringUtil.parseLocale("en__WIN"),
                "Error in parseLocale().");
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
}
