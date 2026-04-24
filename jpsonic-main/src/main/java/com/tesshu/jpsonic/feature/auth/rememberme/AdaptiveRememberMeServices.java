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
 *
 * ---
 * This product includes software developed by the Spring Framework 
 * (https://springframework.org).
 * Derived from Spring Security's Remember-Me implementation.
 * Licensed under the Apache License, Version 2.0.
 */

package com.tesshu.jpsonic.feature.auth.rememberme;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

public final class AdaptiveRememberMeServices implements RememberMeServices, LogoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RememberMeServices.class);
    private final SettingsFacade settingsFacade;
    private final UserDetailsService userDetailsService;
    private final RememberMeLogic rememberMeLogic;

    public AdaptiveRememberMeServices(SettingsFacade settingsFacade,
            UserDetailsService userDetailsService, RememberMeKeyManager rememberMeKeyManager) {
        this.settingsFacade = settingsFacade;
        this.userDetailsService = userDetailsService;
        this.rememberMeLogic = new RememberMeLogic(rememberMeKeyManager);
    }

    AdaptiveRememberMeServices(SettingsFacade settingsFacade, UserDetailsService userDetailsService,
            RememberMeLogic rememberMeLogic) {
        this.settingsFacade = settingsFacade;
        this.userDetailsService = userDetailsService;
        this.rememberMeLogic = rememberMeLogic;
    }

    void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication successfulAuthentication) {

        // expiryTime
        int tokenValidityPeriod = settingsFacade.get(RMSKeys.tokenValidityPeriod);
        int validitySeconds = TokenValidityPeriod.of(tokenValidityPeriod).getSeconds();
        long expiryTime = System.currentTimeMillis() + (1000L * validitySeconds);

        // tokens
        String username = rememberMeLogic.retrieveUserName(successfulAuthentication);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        String signatureValue = rememberMeLogic
            .makeTokenSignature(expiryTime, user.getUsername(), user.getPassword());
        String[] tokens = rememberMeLogic
            .createCookieTokens(user.getUsername(), expiryTime, signatureValue);

        // set expiryTime, tokens to Cookie
        rememberMeLogic.setCookie(tokens, validitySeconds, request, response);

        if (LOG.isDebugEnabled()) {
            LocalDate expiryDate = Instant
                .ofEpochMilli(expiryTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            LOG.debug("Added remember-me cookie for user '{}', expiry: '{}'", username, expiryDate);
        }
    }

    @Override
    public @Nullable Authentication autoLogin(HttpServletRequest request,
            HttpServletResponse response) {

        if (!settingsFacade.get(RMSKeys.enable)) {
            return null;
        }

        String rememberMeCookie = rememberMeLogic.extractRememberMeCookie(request);
        if (rememberMeCookie == null) {
            return null;
        }

        if (rememberMeCookie.length() == 0) {
            rememberMeLogic.cancelCookie(request, response);
            return null;
        }

        try {
            String[] cookieTokens = rememberMeLogic.decodeCookie(rememberMeCookie);
            rememberMeLogic.validateCookieTokens(cookieTokens);

            String username = cookieTokens[0];
            UserDetails user = userDetailsService.loadUserByUsername(username);
            user = rememberMeLogic.processAutoLoginCookie(cookieTokens, user);
            rememberMeLogic.userDetailsCheck(user);

            Authentication auth = rememberMeLogic.createSuccessfulAuthentication(request, user);
            if (settingsFacade.get(RMSKeys.slidingExpirationEnable)) {
                onLoginSuccess(request, response, auth);
            }
            return auth;

        } catch (CookieTheftException ex) {
            rememberMeLogic.cancelCookie(request, response);
            handleAuthenticationFailure(request, response);
            throw ex;
        } catch (UsernameNotFoundException ex) {
            LOG.debug("Remember-me login was valid but corresponding user not found.", ex);
            handleAuthenticationFailure(request, response);
        } catch (InvalidCookieException ex) {
            LOG.debug("Invalid remember-me cookie: " + ex.getMessage());
            handleAuthenticationFailure(request, response);
        } catch (AccountStatusException ex) {
            LOG.debug("Invalid UserDetails: " + ex.getMessage());
            handleAuthenticationFailure(request, response);
        } catch (RememberMeAuthenticationException ex) {
            LOG.debug(ex.getMessage());
            handleAuthenticationFailure(request, response);
        }
        return null;
    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication successfulAuthentication) {

        if (!settingsFacade.get(RMSKeys.enable)) {
            return;
        }

        if (!rememberMeLogic
            .rememberMeRequested(request, AbstractRememberMeServices.DEFAULT_PARAMETER)) {
            LOG.debug("Remember-me login not requested.");
            return;
        }
        onLoginSuccess(request, response, successfulAuthentication);
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        LOG.debug("Interactive login attempt was unsuccessful.");
        rememberMeLogic.cancelCookie(request, response);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
            @Nullable Authentication authentication) {
        LOG
            .debug("Logout of user {}",
                    authentication != null ? authentication.getName() : "Unknown");
        rememberMeLogic.cancelCookie(request, response);
    }

    private void handleAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response) {
        rememberMeLogic.cancelCookie(request, response);
        invalidateSession(request);
        LOG.info("""
                Sovereign Kill executed: \
                Session and Cookie have been synchronized \
                to 'Deleted' state to prevent inconsistent authentication.
                """);
    }

    /**
     * Invalidate the current session to ensure a clean state when remember-me
     * authentication fails.
     */
    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            LOG.debug("Invalidating session to prevent inconsistent authentication state.");
            session.invalidate();
        }
    }
}
