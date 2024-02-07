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

import java.io.IOException;
import java.util.Optional;

import com.tesshu.jpsonic.service.JWTSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JWTRequestParameterProcessingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTRequestParameterProcessingFilter.class);

    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;

    protected JWTRequestParameterProcessingFilter(AuthenticationManager authenticationManager, String failureUrl) {
        super();
        this.authenticationManager = authenticationManager;
        failureHandler = new SimpleUrlAuthenticationFailureHandler(failureUrl);
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Optional<JWTAuthenticationToken> token = findToken(request);
        if (token.isPresent()) {
            return authenticationManager.authenticate(token.get());
        }
        throw new AuthenticationServiceException("Invalid auth method");
    }

    private static Optional<JWTAuthenticationToken> findToken(HttpServletRequest request) {
        String token = request.getParameter(JWTSecurityService.JWT_PARAM_NAME);
        if (!ObjectUtils.isEmpty(token)) {
            return Optional.of(new JWTAuthenticationToken(AuthorityUtils.NO_AUTHORITIES, token,
                    request.getRequestURI() + "?" + request.getQueryString()));
        }
        return Optional.empty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!findToken(request).isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Request is to process authentication");
        }

        Authentication authResult;
        try {
            authResult = attemptAuthentication(request, response);
            if (authResult == null) {
                // return immediately as subclass has indicated that it hasn't completed
                // authentication
                return;
            }
        } catch (InternalAuthenticationServiceException failed) {
            LOG.error("An internal error occurred while trying to authenticate the user.", failed);
            unsuccessfulAuthentication(request, response, failed);
            return;
        } catch (TokenExpiredException | SignatureVerificationException e) {
            // TODO #1192 Insufficient handling
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
            }
            unsuccessfulAuthentication(request, response, e);
            return;
        } catch (BadCredentialsException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            unsuccessfulAuthentication(request, response, e);
            return;
        } catch (AuthenticationException e) {
            unsuccessfulAuthentication(request, response, e);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Authentication success. Updating SecurityContextHolder to contain: " + authResult);
        }

        SecurityContextHolder.getContext().setAuthentication(authResult);

        filterChain.doFilter(request, response);
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Authentication request failed: " + failed.toString(), failed);
            LOG.debug("Updated SecurityContextHolder to contain null Authentication");
            LOG.debug("Delegating to authentication failure handler " + failureHandler);
        }

        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
