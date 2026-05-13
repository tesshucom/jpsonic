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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.feature.auth.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class RememberMeLogicTest {

    @Mock
    RememberMeKeyManager keyManager;

    RememberMeLogic logic;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        logic = new RememberMeLogic(keyManager);
    }

    // ============================================================
    // 1 CookieExtractionAndSettingTest
    // ============================================================
    @Nested
    class CookieExtractionAndSettingTest {

        @Test
        void extractRememberMeCookieReturnsNullWhenNoCookies() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setCookies((Cookie[]) null);

            assertThat(logic.extractRememberMeCookie(req)).isNull();
        }

        @Test
        void extractRememberMeCookieReturnsNullWhenCookieMissing() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setCookies(new Cookie("OTHER", "x"));

            assertThat(logic.extractRememberMeCookie(req)).isNull();
        }

        @Test
        void extractRememberMeCookieReturnsValueWhenCookiePresent() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setCookies(new Cookie("remember-me", "VALUE"));

            assertThat(logic.extractRememberMeCookie(req)).isEqualTo("VALUE");
        }

        @Test
        void cancelCookieSetsMaxAgeZeroAndCorrectPath() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setContextPath("/ctx");
            MockHttpServletResponse res = new MockHttpServletResponse();

            logic.cancelCookie(req, res);

            Cookie c = res.getCookies()[0];
            assertThat(c.getMaxAge()).isZero();
            assertThat(c.getPath()).isEqualTo("/ctx");
        }

        @Test
        void setCookieSetsEncodedValueAndHttpOnlyAndSecure() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setSecure(true);
            MockHttpServletResponse res = new MockHttpServletResponse();

            String[] tokens = { "user", "123", "sig" };
            logic.setCookie(tokens, 100, req, res);

            Cookie c = res.getCookies()[0];
            assertThat(c.isHttpOnly()).isTrue();
            assertThat(c.getSecure()).isTrue();
            assertThat(c.getMaxAge()).isEqualTo(100);

            // Base64 decode check
            String decoded = new String(Base64.getDecoder().decode(c.getValue()),
                    StandardCharsets.UTF_8);
            assertThat(decoded).contains("user");
        }
    }

    // ============================================================
    // 2 CookieDecodingAndValidationTest
    // ============================================================
    @Nested
    class CookieDecodingAndValidationTest {

        @Test
        void decodeCookieDecodesBase64AndUrlDecodesTokens() {
            String raw = "user:123:abc";
            String encoded = Base64
                .getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            String[] tokens = logic.decodeCookie(encoded);

            assertThat(tokens).containsExactly("user", "123", "abc");
        }

        @Test
        void decodeCookieThrowsWhenInvalidBase64() {
            assertThatThrownBy(() -> logic.decodeCookie("%%%"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void validateCookieTokensThrowsWhenTokenCountInvalid() {
            assertThatThrownBy(() -> logic.validateCookieTokens(new String[] { "a", "b" }))
                .isInstanceOf(InvalidCookieException.class);
        }

        @Test
        void validateCookieTokensThrowsWhenExpiryNotNumber() {
            assertThatThrownBy(
                    () -> logic.validateCookieTokens(new String[] { "u", "notNum", "sig" }))
                .isInstanceOf(InvalidCookieException.class);
        }

        @Test
        void validateCookieTokensThrowsWhenExpired() {
            long past = System.currentTimeMillis() - 10_000;
            assertThatThrownBy(() -> logic
                .validateCookieTokens(new String[] { "u", Long.toString(past), "sig" }))
                .isInstanceOf(InvalidCookieException.class);
        }

        @Test
        void validateCookieTokensPassesWhenValid() {
            long future = System.currentTimeMillis() + 10_000;
            assertDoesNotThrow(() -> logic
                .validateCookieTokens(new String[] { "u", Long.toString(future), "sig" }));
        }
    }

    // ============================================================
    // 3 SignatureVerificationTest
    // ============================================================
    @Nested
    class SignatureVerificationTest {

        @BeforeEach
        void setupKey() {
            when(keyManager.getKey()).thenReturn("DUMMY_KEY");
        }

        @Test
        void makeTokenSignatureGeneratesDeterministicHash() {
            long expiry = 12_345L;
            String sig1 = logic.makeTokenSignature(expiry, "user", "pass");
            String sig2 = logic.makeTokenSignature(expiry, "user", "pass");

            assertThat(sig1).isEqualTo(sig2);
        }

        @Test
        void processAutoLoginCookieReturnsUserWhenSignatureMatches() {
            long expiry = System.currentTimeMillis() + 10_000;
            UserDetails user = new User("user", "pass", AuthorityUtils.NO_AUTHORITIES);

            String sig = logic.makeTokenSignature(expiry, "user", "pass");
            String[] tokens = { "user", Long.toString(expiry), sig };

            UserDetails result = logic.processAutoLoginCookie(tokens, user);

            assertThat(result).isEqualTo(user);
        }

        @Test
        void processAutoLoginCookieThrowsWhenSignatureMismatch() {
            long expiry = System.currentTimeMillis() + 10_000;
            UserDetails user = new User("user", "pass", AuthorityUtils.NO_AUTHORITIES);

            String[] tokens = { "user", Long.toString(expiry), "WRONG" };

            assertThatThrownBy(() -> logic.processAutoLoginCookie(tokens, user))
                .isInstanceOf(InvalidCookieException.class);
        }
    }

    // ============================================================
    // 4 AuthenticationAndUserInfoTest
    // ============================================================
    @Nested
    class AuthenticationAndUserInfoTest {

        @BeforeEach
        void setupKey() {
            when(keyManager.getKey()).thenReturn("DUMMY_KEY");
        }

        @Test
        void createSuccessfulAuthenticationCreatesRememberMeToken() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            UserDetails user = new User("user", "pass", AuthorityUtils.NO_AUTHORITIES);

            Authentication auth = logic.createSuccessfulAuthentication(req, user);

            assertThat(auth).isInstanceOf(RememberMeAuthenticationToken.class);
            assertThat(auth.getPrincipal()).isEqualTo(user);
            assertThat(((RememberMeAuthenticationToken) auth).getKeyHash())
                .isEqualTo("DUMMY_KEY".hashCode());
            assertThat(auth.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        }

        @Test
        void retrieveUserNameReturnsUsernameWhenPrincipalIsUserDetails() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(new User("u", "p", AuthorityUtils.NO_AUTHORITIES));

            assertThat(logic.retrieveUserName(auth)).isEqualTo("u");
        }

        @Test
        void retrieveUserNameReturnsToStringWhenPrincipalIsString() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("abc");

            assertThat(logic.retrieveUserName(auth)).isEqualTo("abc");
        }

        @Test
        void retrievePasswordReturnsPasswordWhenPrincipalIsUserDetails() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(new User("u", "p", AuthorityUtils.NO_AUTHORITIES));

            assertThat(logic.retrievePassword(auth)).isEqualTo("p");
        }

        @Test
        void retrievePasswordReturnsCredentialsWhenPrincipalNotUserDetails() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("x");
            when(auth.getCredentials()).thenReturn("cred");

            assertThat(logic.retrievePassword(auth)).isEqualTo("cred");
        }

        @Test
        void retrievePasswordReturnsNullWhenNoPasswordAvailable() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("x");
            when(auth.getCredentials()).thenReturn(null);

            assertThat(logic.retrievePassword(auth)).isNull();
        }

        @Test
        void userDetailsCheckThrowsWhenCheckerFails() {
            UserDetails bad = mock(UserDetails.class);
            when(bad.isAccountNonLocked()).thenReturn(false);

            assertThatThrownBy(() -> logic.userDetailsCheck(bad)).isInstanceOf(Exception.class);
        }
    }

    // ============================================================
    // 5 Resilience and Security Edge Cases
    // ============================================================
    @Nested
    class ResilienceAndSecurityTest {

        /**
         * Verify that usernames containing the delimiter (:) do not break token
         * splitting. This ensures that URLEncode correctly protects special characters,
         * a common failure point in standard Spring Security implementations.
         */
        @Test
        void decodeCookieHandlesUsernamesContainingDelimiters() {
            String usernameWithDelimiter = "admin:tesshu";
            long expiry = System.currentTimeMillis() + 10_000;
            String sig = "signature";
            String[] tokens = { usernameWithDelimiter, String.valueOf(expiry), sig };

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            // setCookie internally calls encodeCookie which performs URLEncoding
            logic.setCookie(tokens, 100, req, res);

            String cookieValue = res
                .getCookie(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY)
                .getValue();
            String[] decoded = logic.decodeCookie(cookieValue);

            assertThat(decoded[0]).isEqualTo(usernameWithDelimiter);
            assertThat(decoded).hasSize(3);
        }

        /**
         * Verify that cookies missing Base64 padding (=) can still be decoded by the
         * standard API. This justifies the removal of the redundant padding-completion
         * loop found in the original Spring code.
         */
        @Test
        void decodeCookieIsResilientToMissingBase64Padding() {
            // "user:123:sig" -> "dXNlcjoxMjM6c2ln" (No padding needed)
            // "user1:123:sig" -> "dXNlcjE6MTIzOnNpZw==" (Padding needed)
            String raw = "user1:123:sig";
            String base64WithPadding = Base64
                .getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            String base64WithoutPadding = base64WithPadding.replace("=", "");

            String[] decoded = logic.decodeCookie(base64WithoutPadding);

            assertThat(decoded).containsExactly("user1", "123", "sig");
        }

        /**
         * Verify that an old cookie is rejected if the user's password has been
         * changed. This ensures security integrity remains intact across server
         * restarts or password updates.
         */
        @Test
        void processAutoLoginCookieRejectsWhenPasswordWasChanged() {
            long expiry = System.currentTimeMillis() + 10_000;
            when(keyManager.getKey()).thenReturn("FIXED_KEY");

            // Create signature with the old password
            String oldSig = logic.makeTokenSignature(expiry, "admin", "OLD_PASSWORD");
            String[] tokens = { "admin", String.valueOf(expiry), oldSig };

            // Attempt to validate against UserDetails with a new password
            UserDetails updatedUser = new User("admin", "NEW_PASSWORD",
                    AuthorityUtils.NO_AUTHORITIES);

            assertThatThrownBy(() -> logic.processAutoLoginCookie(tokens, updatedUser))
                .isInstanceOf(InvalidCookieException.class)
                .hasMessageContaining("signature");
        }

        /**
         * Verify that all existing cookies are immediately invalidated when the server
         * key is rotated. This proves that the KeyManager's "Sovereignty (Force
         * Logout)" is functioning as designed.
         */
        @Test
        void processAutoLoginCookieRejectsWhenServerKeyWasRotated() {
            long expiry = System.currentTimeMillis() + 10_000;
            UserDetails user = new User("admin", "pass", AuthorityUtils.NO_AUTHORITIES);

            // Create signature with the old key
            when(keyManager.getKey()).thenReturn("OLD_KEY");
            String oldSig = logic.makeTokenSignature(expiry, "admin", "pass");
            String[] tokens = { "admin", String.valueOf(expiry), oldSig };

            // Rotate the key in KeyManager
            when(keyManager.getKey()).thenReturn("NEW_KEY");

            assertThatThrownBy(() -> logic.processAutoLoginCookie(tokens, user))
                .isInstanceOf(InvalidCookieException.class);
        }
    }
}
