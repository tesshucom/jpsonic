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

package com.tesshu.jpsonic.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class VersionTest {

    /**
     * Tests that equals(), hashCode(), toString() and compareTo() works.
     */
    @Test
    void testVersion() {
        assertTrue(doTestVersion("0.0", "0.1"));
        assertTrue(doTestVersion("1.5", "2.3"));
        assertTrue(doTestVersion("2.3", "2.34"));

        assertTrue(doTestVersion("1.5", "1.5.1"));
        assertTrue(doTestVersion("1.5.1", "1.5.2"));
        assertTrue(doTestVersion("1.5.2", "1.5.11"));

        assertTrue(doTestVersion("1.4", "1.5.beta1"));
        assertTrue(doTestVersion("1.4.1", "1.5.beta1"));
        assertTrue(doTestVersion("1.5.beta1", "1.5"));
        assertTrue(doTestVersion("1.5.beta1", "1.5.1"));
        assertTrue(doTestVersion("1.5.beta1", "1.6"));
        assertTrue(doTestVersion("1.5.beta1", "1.5.beta2"));
        assertTrue(doTestVersion("1.5.beta2", "1.5.beta11"));

        assertTrue(doTestVersion("6.2-SNAPSHOT", "6.11-SNAPSHOT"));
    }

    @Test
    void testIsPreview() {
        Version version = new Version("1.6.0-SNAPSHOT");
        assertTrue(version.isPreview(), "Version should be snapshot");

        version = new Version("1.6.0-beta2");
        assertTrue(version.isPreview(), "Version should be snapshot");

        version = new Version("1.6.0");
        assertFalse(version.isPreview(), "Version should not be snapshot");

        version = new Version("1.6.0-RELEASE");
        assertFalse(version.isPreview(), "Version should not be snapshot");
    }

    /**
     * Tests that equals(), hashCode(), toString() and compareTo() works.
     * 
     * @param v1
     *            A lower version.
     * @param v2
     *            A higher version.
     */
    private boolean doTestVersion(String v1, String v2) {
        Version ver1 = new Version(v1);
        Version ver2 = new Version(v2);

        assertEquals(v1, ver1.toString(), "Error in toString().");
        assertEquals(v2, ver2.toString(), "Error in toString().");

        assertEquals(ver1, ver1, "Error in equals().");

        assertEquals(0, ver1.compareTo(ver1), "Error in compareTo().");
        assertEquals(0, ver2.compareTo(ver2), "Error in compareTo().");
        assertTrue(ver1.compareTo(ver2) < 0, "Error in compareTo().");
        assertTrue(ver2.compareTo(ver1) > 0, "Error in compareTo().");
        return true;
    }
}
