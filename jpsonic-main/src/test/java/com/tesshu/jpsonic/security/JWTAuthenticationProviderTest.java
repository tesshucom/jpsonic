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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.tesshu.jpsonic.service.JWTSecurityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.UseConcurrentHashMap" })
class JWTAuthenticationProviderTest {

    @Test
    void testRoughlyEqual() {
        assertFalse(JWTAuthenticationProvider.roughlyEqual(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> JWTAuthenticationProvider.roughlyEqual("A", null));
        assertFalse(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy1.com/test1").toString(),
                    URI.create("http://dummy2.com/test2").toString()));
        assertFalse(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy.com/test").toString(),
                    URI.create("http://dummy.com/test").toString()));
        assertFalse(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy.com/test?A=dummy1").toString(),
                    URI.create("http://dummy.com/test?A=dummy2").toString()));
        assertFalse(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy.com/test").toString(),
                    URI.create("http://dummy.com/test?A=dummy").toString()));
        assertFalse(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy.com/test?A=dummy").toString(),
                    URI.create("http://dummy.com/test").toString()));
        assertTrue(JWTAuthenticationProvider
            .roughlyEqual(URI.create("http://dummy.com/test").toString(), URI
                .create("http://dummy.com/test?" + JWTSecurityService.JWT_PARAM_NAME + "=value")
                .toString()));
    }

    @Nested
    class ExistsDifferenceTest {

        @Test
        void testCheckNotNull() {
            Map<String, List<String>> m = new HashMap<>();
            assertThrows(NullPointerException.class,
                    () -> JWTAuthenticationProvider.expectedJWTParam(null, m));
            assertThrows(NullPointerException.class,
                    () -> JWTAuthenticationProvider.expectedJWTParam(m, null));
            assertThrows(NullPointerException.class,
                    () -> JWTAuthenticationProvider.expectedJWTParam(null, null));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m, m));
        }

        @Test
        void testNullValue() {
            Map<String, List<String>> m1 = new HashMap<>();
            Map<String, List<String>> m2 = new HashMap<>();
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("m1A", null);
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m2.put("m2A", null);
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("m1A", null);
            m2.put("m2A", null);
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("m1A", null);
            m1.put("m1B", Arrays.asList("1", null));
            m2.put("m2A", null);
            m2.put("m2B", Arrays.asList("1", null));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));
        }

        /*
         * true if present in right
         */
        @Test
        void testExistJWT() {
            Map<String, List<String>> m1 = new ConcurrentHashMap<>();
            Map<String, List<String>> m2 = new ConcurrentHashMap<>();

            m1.clear();
            m2.clear();
            m1.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));
        }

        /*
         * List size is equivalent except for JWT_PARAM
         */
        @Test
        void testKeySizeWithJWT() {
            Map<String, List<String>> m1 = new ConcurrentHashMap<>();
            Map<String, List<String>> m2 = new ConcurrentHashMap<>();

            m1.clear();
            m2.clear();
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("A", Arrays.asList("A"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            m2.put("A", Arrays.asList("A"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));
        }

        /*
         * Lists excluding JWT_PARAM are equivalent
         */
        @Test
        void testListEquivalence() {
            Map<String, List<String>> m1 = new HashMap<>();
            Map<String, List<String>> m2 = new HashMap<>();

            m1.put("A", Arrays.asList("123"));
            m2.put("A", Arrays.asList("123"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("A", Arrays.asList("123"));
            m2.put("A", Arrays.asList("456"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("A", Arrays.asList("123"));
            m2.put("B", Arrays.asList("123"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertFalse(JWTAuthenticationProvider.expectedJWTParam(m1, m2));
        }

        /*
         * List equivalence will be determined ignoring the element order
         */
        @Test
        void testSorted() {
            SortedMap<String, List<String>> m1 = new TreeMap<>();
            SortedMap<String, List<String>> m2 = new TreeMap<>();

            m1.put("A", Arrays.asList("123"));
            m1.put("B", Arrays.asList("456"));
            m2.put("A", Arrays.asList("123"));
            m2.put("B", Arrays.asList("456"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("A", Arrays.asList("123"));
            m1.put("B", Arrays.asList("456"));
            m2.put("B", Arrays.asList("456"));
            m2.put("A", Arrays.asList("123"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));

            m1.clear();
            m2.clear();
            m1.put("B", Arrays.asList("456"));
            m1.put("A", Arrays.asList("123"));
            m2.put("A", Arrays.asList("123"));
            m2.put("B", Arrays.asList("456"));
            m2.put(JWTSecurityService.JWT_PARAM_NAME, Arrays.asList("param"));
            assertTrue(JWTAuthenticationProvider.expectedJWTParam(m1, m2));
        }
    }
}
