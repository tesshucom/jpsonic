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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class AdaptiveRememberMeServicesTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private RememberMeLogic logic;

    private SettingsFacade settings;
    private AdaptiveRememberMeServices services;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        settings = SettingsFacadeBuilder
            .create()
            .withBoolean(RMSKeys.enable, true)
            .withBoolean(RMSKeys.slidingExpirationEnable, false)
            .withInt(RMSKeys.tokenValidityPeriod, 1)
            .build();

        services = new AdaptiveRememberMeServices(settings, userDetailsService, logic);
    }

    // ============================================================
    // 1. onLoginSuccess tests
    // ============================================================
    @Nested
    class OnLoginSuccessTest {

        @Test
        void testOnLoginSuccessSetsCookie() {
            final MockHttpServletRequest req = new MockHttpServletRequest();
            final MockHttpServletResponse res = new MockHttpServletResponse();

            Authentication auth = mock(Authentication.class);
            when(logic.retrieveUserName(auth)).thenReturn("alice");

            UserDetails user = new User("alice", "pw", AuthorityUtils.NO_AUTHORITIES);
            when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

            when(logic.makeTokenSignature(anyLong(), eq("alice"), eq("pw"))).thenReturn("SIG");

            when(logic.createCookieTokens(eq("alice"), anyLong(), eq("SIG")))
                .thenReturn(new String[] { "alice", "EXP", "SIG" });

            services.onLoginSuccess(req, res, auth);

            verify(logic).setCookie(any(String[].class), anyInt(), eq(req), eq(res));
        }
    }

    // ============================================================
    // 2. autoLogin tests
    // ============================================================
    @Nested
    class AutoLoginTest {

        @Test
        void testAutoLoginDisabled() {
            settings = SettingsFacadeBuilder.create().withBoolean(RMSKeys.enable, false).build();

            services = new AdaptiveRememberMeServices(settings, userDetailsService, logic);

            Authentication result = services
                .autoLogin(new MockHttpServletRequest(), new MockHttpServletResponse());

            assertThat(result).isNull();
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testAutoLoginNoCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            when(logic.extractRememberMeCookie(req)).thenReturn(null);

            Authentication result = services.autoLogin(req, new MockHttpServletResponse());

            assertThat(result).isNull();
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testAutoLoginEmptyCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            when(logic.extractRememberMeCookie(req)).thenReturn("");

            Authentication result = services.autoLogin(req, res);

            assertThat(result).isNull();
            verify(logic).cancelCookie(req, res);
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testAutoLoginValid() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenReturn(new String[] { "alice", "EXP", "SIG" });
            doNothing().when(logic).validateCookieTokens(any());

            UserDetails user = new User("alice", "pw", AuthorityUtils.NO_AUTHORITIES);
            when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

            when(logic.processAutoLoginCookie(any(), eq(user))).thenReturn(user);

            doNothing().when(logic).userDetailsCheck(user);

            Authentication expectedAuth = mock(Authentication.class);
            when(logic.createSuccessfulAuthentication(req, user)).thenReturn(expectedAuth);

            Authentication result = services.autoLogin(req, res);

            assertThat(result).isEqualTo(expectedAuth);
        }

        @Test
        void testAutoLoginSlidingExpiration() {
            settings = SettingsFacadeBuilder
                .create()
                .withBoolean(RMSKeys.enable, true)
                .withBoolean(RMSKeys.slidingExpirationEnable, true)
                .withInt(RMSKeys.tokenValidityPeriod, 1)
                .build();

            services = new AdaptiveRememberMeServices(settings, userDetailsService, logic);

            final MockHttpServletRequest req = new MockHttpServletRequest();
            final MockHttpServletResponse res = new MockHttpServletResponse();

            // autoLogin flow
            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenReturn(new String[] { "alice", "EXP", "SIG" });
            doNothing().when(logic).validateCookieTokens(any());

            UserDetails user = new User("alice", "pw", AuthorityUtils.NO_AUTHORITIES);
            when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

            when(logic.processAutoLoginCookie(any(), eq(user))).thenReturn(user);
            doNothing().when(logic).userDetailsCheck(user);

            Authentication auth = mock(Authentication.class);
            when(logic.createSuccessfulAuthentication(req, user)).thenReturn(auth);

            // onLoginSuccess flow (sliding expiration)
            when(logic.retrieveUserName(auth)).thenReturn("alice");
            when(logic.makeTokenSignature(anyLong(), eq("alice"), eq("pw"))).thenReturn("SIG2");
            when(logic.createCookieTokens(eq("alice"), anyLong(), eq("SIG2")))
                .thenReturn(new String[] { "alice", "EXP2", "SIG2" });

            services.autoLogin(req, res);

            verify(logic).setCookie(any(), anyInt(), eq(req), eq(res));
        }

        @Test
        void testAutoLoginInvalidCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenThrow(new InvalidCookieException("bad"));

            Authentication result = services.autoLogin(req, res);

            assertThat(result).isNull();
            verify(logic).cancelCookie(req, res);
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testAutoLoginAccountStatus() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenReturn(new String[] { "alice", "EXP", "SIG" });
            doNothing().when(logic).validateCookieTokens(any());

            when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new AccountStatusException("bad") {
                });

            Authentication result = services.autoLogin(req, res);

            assertThat(result).isNull();
            verify(logic).cancelCookie(req, res);
        }

        @Test
        void testAutoLoginUserNotFound() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenReturn(new String[] { "alice", "EXP", "SIG" });
            doNothing().when(logic).validateCookieTokens(any());

            when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new UsernameNotFoundException("not found"));

            Authentication result = services.autoLogin(req, res);

            assertThat(result).isNull();
            verify(logic).cancelCookie(req, res);
        }

        @Test
        void testAutoLoginInvalidTokensShouldNotAccessDb() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            when(logic.extractRememberMeCookie(req)).thenReturn("COOKIE");
            when(logic.decodeCookie("COOKIE")).thenReturn(new String[] { "a", "b", "c" });

            doThrow(new InvalidCookieException("expired")).when(logic).validateCookieTokens(any());

            services.autoLogin(req, new MockHttpServletResponse());

            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(logic).cancelCookie(any(), any());
        }

    }

    // ============================================================
    // 3. loginSuccess tests
    // ============================================================
    @Nested
    class LoginSuccessTest {

        @Test
        void testLoginSuccessNotRequested() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            Authentication auth = mock(Authentication.class);

            when(logic.rememberMeRequested(req, AbstractRememberMeServices.DEFAULT_PARAMETER))
                .thenReturn(false);

            services.loginSuccess(req, res, auth);

            verify(logic, never()).setCookie(any(), anyInt(), any(), any());
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testLoginSuccessWhenNotRequestedShouldNotSetCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            Authentication auth = mock(Authentication.class);

            when(logic.rememberMeRequested(req, AbstractRememberMeServices.DEFAULT_PARAMETER))
                .thenReturn(false);

            services.loginSuccess(req, res, auth);

            verify(logic, never()).setCookie(any(), anyInt(), any(), any());
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        void testLoginSuccessWhenRequestedShouldSetCookie() {
            final MockHttpServletRequest req = new MockHttpServletRequest();
            final MockHttpServletResponse res = new MockHttpServletResponse();
            Authentication auth = mock(Authentication.class);

            when(logic.rememberMeRequested(req, AbstractRememberMeServices.DEFAULT_PARAMETER))
                .thenReturn(true);
            when(logic.retrieveUserName(auth)).thenReturn("alice");

            UserDetails user = new User("alice", "pw", AuthorityUtils.NO_AUTHORITIES);
            when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

            when(logic.makeTokenSignature(anyLong(), any(), any())).thenReturn("SIG");
            when(logic.createCookieTokens(any(), anyLong(), any()))
                .thenReturn(new String[] { "alice", "EXP", "SIG" });

            services.loginSuccess(req, res, auth);

            verify(logic).setCookie(any(), anyInt(), eq(req), eq(res));
        }
    }

    // ============================================================
    // 4. logout and loginFail tests
    // ============================================================
    @Nested
    class LogoutAndFailTest {

        @Test
        void testLoginFailCancelsCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            services.loginFail(req, res);

            verify(logic).cancelCookie(req, res);
        }

        @Test
        void testLogoutCancelsCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            services.logout(req, res, null);

            verify(logic).cancelCookie(req, res);
        }

        @Test
        void testLogoutWithAuthenticationShouldStillCancelCookie() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("alice");
            services.logout(req, res, auth);
            verify(logic).cancelCookie(req, res);
        }
    }
}
