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

package com.tesshu.jpsonic.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.tesshu.jpsonic.service.JWTSecurityService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class JWTAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationProvider.class);

    private final String jwtKey;

    public JWTAuthenticationProvider(String jwtSignAndVerifyKey) {
        this.jwtKey = jwtSignAndVerifyKey;
    }

    @Override
    public Authentication authenticate(Authentication auth) {
        JWTAuthenticationToken authentication = (JWTAuthenticationToken) auth;
        if (!(authentication.getCredentials() instanceof String)) {
            LOG.error("Credentials not present");
            return null;
        }
        String rawToken = (String) auth.getCredentials();
        DecodedJWT token = JWTSecurityService.verify(jwtKey, rawToken);
        Claim path = token.getClaim(JWTSecurityService.CLAIM_PATH);
        authentication.setAuthenticated(true);

        // TODO:AD This is super unfortunate, but not sure there is a better way when using JSP
        if (StringUtils.contains(authentication.getRequestedPath(), "/WEB-INF/jsp/")) {
            LOG.warn("BYPASSING AUTH FOR WEB-INF page");
        } else if (!roughlyEqual(path.asString(), authentication.getRequestedPath())) {
            throw new InsufficientAuthenticationException("Credentials not valid for path "
                    + authentication.getRequestedPath() + ". They are valid for " + path.asString());
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("IS_AUTHENTICATED_FULLY"));
        authorities.add(new SimpleGrantedAuthority("ROLE_TEMP"));
        return new JWTAuthenticationToken(authorities, rawToken, authentication.getRequestedPath());
    }

    private static boolean roughlyEqual(String expectedRaw, String requestedPathRaw) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Comparing expected [{}] vs requested [{}]", expectedRaw, requestedPathRaw);
        }
        if (StringUtils.isEmpty(expectedRaw)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("False: empty expected");
            }
            return false;
        }
        UriComponents expected = UriComponentsBuilder.fromUriString(expectedRaw).build();
        UriComponents requested = UriComponentsBuilder.fromUriString(requestedPathRaw).build();
        if (!Objects.equals(expected.getPath(), requested.getPath())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("False: expected path [{}] does not match requested path [{}]", expected.getPath(),
                        requested.getPath());
            }
            return false;
        }

        MapDifference<String, List<String>> difference = Maps.difference(expected.getQueryParams(),
                requested.getQueryParams());
        if (!difference.entriesDiffering().isEmpty() || !difference.entriesOnlyOnLeft().isEmpty()
                || difference.entriesOnlyOnRight().size() != 1
                || difference.entriesOnlyOnRight().get(JWTSecurityService.JWT_PARAM_NAME) == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("False: expected query params [{}] do not match requested query params [{}]",
                        expected.getQueryParams(), requested.getQueryParams());
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JWTAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
