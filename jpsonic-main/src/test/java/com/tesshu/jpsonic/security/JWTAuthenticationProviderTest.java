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

import com.tesshu.jpsonic.service.JWTSecurityService;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JWTAuthenticationProviderTest {

    @Test
    void testRoughlyEqual() {
        assertFalse(JWTAuthenticationProvider.roughlyEqual(null, null));
        assertThrows(IllegalArgumentException.class, () -> JWTAuthenticationProvider.roughlyEqual("A", null));
        assertFalse(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy1.com/test1").toString(),
                URI.create("http://dummy2.com/test2").toString()));
        assertFalse(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy.com/test").toString(),
                URI.create("http://dummy.com/test").toString()));
        assertFalse(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy.com/test?A=dummy1").toString(),
                URI.create("http://dummy.com/test?A=dummy2").toString()));
        assertFalse(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy.com/test").toString(),
                URI.create("http://dummy.com/test?A=dummy").toString()));
        assertFalse(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy.com/test?A=dummy").toString(),
                URI.create("http://dummy.com/test").toString()));
        assertTrue(JWTAuthenticationProvider.roughlyEqual(URI.create("http://dummy.com/test").toString(),
                URI.create("http://dummy.com/test?" + JWTSecurityService.JWT_PARAM_NAME + "=value").toString()));
    }
}
