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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@SuppressWarnings("PMD.UseVarargs")
final class RememberMeLogic {

    private static final Logger LOG = LoggerFactory.getLogger(RememberMeServices.class);
    private static final String DELIMITER = ":";
    private static final RememberMeTokenAlgorithm DEFAULT_ALGORITHM = RememberMeTokenAlgorithm.SHA256;
    private static final String COOKIE_NAME = AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    private final RememberMeKeyManager rememberMeKeyManager;

    RememberMeLogic(RememberMeKeyManager rememberMeKeyManager) {
        this.rememberMeKeyManager = rememberMeKeyManager;
    }

    @Nullable
    String extractRememberMeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if ((cookies == null) || (cookies.length == 0)) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return (contextPath.length() > 0) ? contextPath : "/";
    }

    void cancelCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setPath(getCookiePath(request));
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures")
    String[] decodeCookie(String cookieValue) throws InvalidCookieException {
        byte[] decodedBytes = Base64
            .getDecoder()
            .decode(cookieValue.getBytes(StandardCharsets.UTF_8));
        String cookieAsPlainText = new String(decodedBytes, StandardCharsets.UTF_8);
        String[] tokens = StringUtils.delimitedListToStringArray(cookieAsPlainText, DELIMITER);
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = URLDecoder.decode(tokens[i], StandardCharsets.UTF_8);
        }
        return tokens;
    }

    private boolean isValidCookieTokensLength(String[] cookieTokens) {
        return cookieTokens.length == 3;
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private long getTokenExpiryTime(String[] cookieTokens) {
        try {
            return Long.parseLong(cookieTokens[1]);
        } catch (NumberFormatException nfe) {
            throw new InvalidCookieException(
                    "Cookie token[1] did not contain a valid number ('" + cookieTokens[1] + "')");
        }
    }

    private boolean isTokenExpired(long tokenExpiryTime) {
        return tokenExpiryTime < System.currentTimeMillis();
    }

    String makeTokenSignature(long tokenExpiryTime, String username, @Nullable String password) {
        String data = username + ":" + tokenExpiryTime + ":" + password + ":"
                + rememberMeKeyManager.getKey();
        try {
            MessageDigest digest = MessageDigest
                .getInstance(DEFAULT_ALGORITHM.getDigestAlgorithm());
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(hash));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "No " + DEFAULT_ALGORITHM.name() + " algorithm available!", ex);
        }
    }

    private static byte @Nullable [] bytesUtf8(String s) {
        return (s != null) ? Utf8.encode(s) : null;
    }

    private static boolean equals(String expected, String actual) {
        byte[] expectedBytes = bytesUtf8(expected);
        byte[] actualBytes = bytesUtf8(actual);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    void validateCookieTokens(String[] cookieTokens) {
        if (!isValidCookieTokensLength(cookieTokens)) {
            throw new InvalidCookieException(
                    "Cookie token did not contain 3 or 4 tokens, but contained '"
                            + Arrays.asList(cookieTokens) + "'");
        }
        long tokenExpiryTime = getTokenExpiryTime(cookieTokens);
        if (isTokenExpired(tokenExpiryTime)) {
            LocalDate tokenExpiryDate = Instant
                .ofEpochMilli(tokenExpiryTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            throw new InvalidCookieException("Cookie token[1] has expired (expired on '"
                    + tokenExpiryDate + "'; current time is '" + LocalDate.now() + "')");
        }
    }

    UserDetails processAutoLoginCookie(String[] cookieTokens, @NonNull UserDetails userDetails) {
        long tokenExpiryTime = getTokenExpiryTime(cookieTokens);
        String actualTokenSignature = cookieTokens[2];
        String expectedTokenSignature = makeTokenSignature(tokenExpiryTime,
                userDetails.getUsername(), userDetails.getPassword());

        if (!equals(expectedTokenSignature, actualTokenSignature)) {
            throw new InvalidCookieException("Cookie contained signature '" + actualTokenSignature
                    + "' but expected '" + expectedTokenSignature + "'");
        }
        return userDetails;
    }

    Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
        RememberMeAuthenticationToken auth = new RememberMeAuthenticationToken(
                rememberMeKeyManager.getKey(), user, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

    String retrieveUserName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Assert.notNull(principal, "Authentication.getPrincipal() cannot be null");
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    @Nullable
    String retrievePassword(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getPassword();
        }
        if (authentication.getCredentials() != null) {
            return authentication.getCredentials().toString();
        }
        return null;
    }

    private String encodeCookie(String[] cookieTokens) {
        String joined = Arrays
            .stream(cookieTokens)
            .map(t -> URLEncoder.encode(t, StandardCharsets.UTF_8))
            .collect(Collectors.joining(DELIMITER));
        return Base64.getEncoder().encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    String[] createCookieTokens(String username, long expiryTime, String signature) {
        return new String[] { username, Long.toString(expiryTime), signature };
    }

    void setCookie(String[] tokens, int maxAge, HttpServletRequest request,
            HttpServletResponse response) {
        String cookieValue = encodeCookie(tokens);
        Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath(getCookiePath(request));
        cookie.setSecure(request.isSecure());
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    boolean rememberMeRequested(HttpServletRequest request, String parameter) {
        String paramValue = request.getParameter(parameter);
        if (paramValue != null
                && ("true".equalsIgnoreCase(paramValue) || "on".equalsIgnoreCase(paramValue)
                        || "yes".equalsIgnoreCase(paramValue) || "1".equals(paramValue))) {
            return true;
        }
        LOG
            .debug("Did not send remember-me cookie (principal did not set parameter '{}')",
                    parameter);
        return false;
    }

    void userDetailsCheck(UserDetails toCheck) {
        this.userDetailsChecker.check(toCheck);
    }
}
