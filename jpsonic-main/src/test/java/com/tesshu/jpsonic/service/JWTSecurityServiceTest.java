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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("PMD.TooManyStaticImports")
class JWTSecurityServiceTest {

    private SettingsService settingsService;
    private JWTSecurityService jwtSecurityService;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        jwtSecurityService = new JWTSecurityService(mock(SettingsService.class));
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // false positive
    @Test
    void testAddJWTToken() {
        // Originally Parameterized was used. If possible, it is better to rewrite to the new
        // spring-method.
        Stream.of(new Object[][] { { "http://localhost:8080/jpsonic/stream?id=4", "/jpsonic/stream?id=4" },
                { "/jpsonic/stream?id=4", "/jpsonic/stream?id=4" } }).forEach(o -> {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(o[0].toString());
                    String actualUri = jwtSecurityService.addJWTToken(builder).build().toUriString();
                    String jwtToken = UriComponentsBuilder.fromUriString(actualUri).build().getQueryParams()
                            .getFirst(JWTSecurityService.JWT_PARAM_NAME);
                    Algorithm algorithm = JWTSecurityService.getAlgorithm(settingsService.getJWTKey());
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    DecodedJWT verify = verifier.verify(jwtToken);
                    Claim claim = verify.getClaim(JWTSecurityService.CLAIM_PATH);
                    assertEquals(o[1], claim.asString());
                });
    }

    /**
     * There are 5 exception types thrown in the JWT
     */
    @Nested
    class VerifyTest {

        private String key = "key";
        private String path = "path";

        private Date toDate(LocalDateTime l) {
            return Date.from(ZonedDateTime.of(l, ZoneId.systemDefault()).toInstant());
        }

        @Test
        void testVerify() {
            Date current = toDate(LocalDateTime.now());
            String token = JWTSecurityService.createToken(key, path, current);
            DecodedJWT decoded = JWTSecurityService.verify(key, token);

            assertEquals("HS256", decoded.getAlgorithm());
            assertNull(decoded.getAudience());
            assertEquals(2, decoded.getClaims().size());
            assertEquals("\"" + path + "\"", decoded.getClaim("path").toString());
            assertNull(decoded.getContentType());
            Date truncated = DateUtils.truncate(current, Calendar.SECOND);
            assertEquals(truncated, decoded.getExpiresAt());
            assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9", decoded.getHeader());
            assertNull(decoded.getId());
            assertNull(decoded.getIssuer());
            assertNull(decoded.getKeyId());
            assertNull(decoded.getNotBefore());
            assertNull(decoded.getSubject());
            assertEquals(token, decoded.getToken());
            assertEquals("JWT", decoded.getType());

            token = token + ".mp3";
            assertNotNull(JWTSecurityService.verify(key, token));
        }

        @Test
        void testTokenExpired() {
            LocalDateTime now = LocalDateTime.now();
            Date after = toDate(now.plus(7, ChronoUnit.DAYS));
            Date before = toDate(now.minus(7, ChronoUnit.DAYS));

            assertNotNull(JWTSecurityService.verify(key, JWTSecurityService.createToken(key, path, after)));
            Throwable t = assertThrows(com.tesshu.jpsonic.security.TokenExpiredException.class,
                    () -> JWTSecurityService.verify(key, JWTSecurityService.createToken(key, path, before)));
            assertInstanceOf(com.auth0.jwt.exceptions.TokenExpiredException.class, t.getCause());
        }

        @Test
        void testSignatureVerification() {
            Date current = toDate(LocalDateTime.now());
            String invalidToken = JWTSecurityService.createToken(key, path, current);
            Throwable t = assertThrows(com.tesshu.jpsonic.security.SignatureVerificationException.class,
                    () -> jwtSecurityService.verify(invalidToken));
            assertInstanceOf(com.auth0.jwt.exceptions.SignatureVerificationException.class, t.getCause());
        }

        @Test
        void testJWTDecode() {
            Date current = toDate(LocalDateTime.now());
            String invalidToken = "foo" + JWTSecurityService.createToken(key, path, current).substring(3);
            Throwable t = assertThrows(BadCredentialsException.class, () -> jwtSecurityService.verify(invalidToken));
            assertInstanceOf(JWTDecodeException.class, t.getCause());
        }

        @Test
        void testAlgorithmMismatch() {
            String invalidToken = JWT.create().sign(Algorithm.HMAC512(key));
            Throwable t = assertThrows(BadCredentialsException.class, () -> jwtSecurityService.verify(invalidToken));
            assertInstanceOf(AlgorithmMismatchException.class, t.getCause());
        }

        @Test
        void testInvalidClaim() {
            final String token = JWT.create().withExpiresAt(toDate(LocalDateTime.now())).sign(Algorithm.HMAC256(key));
            Throwable t = assertThrows(BadCredentialsException.class, () -> JWTSecurityService.verify(key, token));
            assertInstanceOf(InvalidClaimException.class, t.getCause());
        }
    }
}
