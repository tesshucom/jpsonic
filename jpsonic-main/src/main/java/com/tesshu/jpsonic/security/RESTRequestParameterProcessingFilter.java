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

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.JAXBWriter;
import com.tesshu.jpsonic.controller.SubsonicRESTController;
import com.tesshu.jpsonic.domain.system.Version;
import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.StringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Performs authentication based on credentials being present in the HTTP
 * request parameters. Also checks API versions and license information.
 * <p/>
 * The username should be set in parameter "u", and the password should be set
 * in parameter "p". The REST protocol version should be set in parameter "v".
 * <p/>
 * The password can either be in plain text or be UTF-8 hexencoded preceded by
 * "enc:".
 *
 * @author Sindre Mehus
 */
public class RESTRequestParameterProcessingFilter extends OncePerRequestFilter {

    private static final RequestMatcher REQUIRES_AUTHENTICATION_REQUEST_MATCHER = new RegexRequestMatcher(
            "/rest/.+", null);

    private final JAXBWriter jaxbWriter = new JAXBWriter(null);
    private final AuthenticationManager authenticationManager;
    private final SecurityService securityService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public RESTRequestParameterProcessingFilter(AuthenticationManager authenticationManager,
            SecurityService securityService, ApplicationEventPublisher eventPublisher) {
        super();
        this.authenticationManager = authenticationManager;
        this.securityService = securityService;
        this.eventPublisher = eventPublisher;
        authenticationDetailsSource = new WebAuthenticationDetailsSource();
    }

    protected boolean requiresAuthentication(HttpServletRequest request,
            HttpServletResponse response) {
        return REQUIRES_AUTHENTICATION_REQUEST_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!requiresAuthentication(request, response)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = StringUtils
            .trimToNull(request.getParameter(Attributes.Request.U.value()));
        String password = decrypt(
                StringUtils.trimToNull(request.getParameter(Attributes.Request.P.value())));
        String salt = StringUtils.trimToNull(request.getParameter(Attributes.Request.S.value()));
        String token = StringUtils.trimToNull(request.getParameter(Attributes.Request.T.value()));
        String version = StringUtils.trimToNull(request.getParameter(Attributes.Request.V.value()));
        String client = StringUtils.trimToNull(request.getParameter(Attributes.Request.C.value()));

        SubsonicRESTController.ErrorCode errorCode = getErrorCode(request, username, password, salt,
                token, version, client);
        if (errorCode == null) {
            filterChain.doFilter(request, response);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
            sendErrorXml(request, response, errorCode);
        }
    }

    private SubsonicRESTController.ErrorCode getErrorCode(HttpServletRequest httpRequest,
            String username, String password, String salt, String token, String version,
            String client) {
        SubsonicRESTController.ErrorCode errorCode = null;
        // The username and credentials parameters are not required if the user
        // was previously authenticated, for example using Basic Auth.
        boolean passwordOrTokenPresent = password != null || salt != null && token != null;
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean missingCredentials = previousAuth == null
                && (username == null || !passwordOrTokenPresent);
        if (missingCredentials || version == null || client == null) {
            errorCode = SubsonicRESTController.ErrorCode.MISSING_PARAMETER;
        }
        if (errorCode == null) {
            errorCode = checkAPIVersion(version);
        }
        if (errorCode == null) {
            errorCode = authenticate(httpRequest, username, password, salt, token, previousAuth);
        }
        return errorCode;
    }

    private SubsonicRESTController.ErrorCode checkAPIVersion(String version) {
        Version serverVersion = new Version(jaxbWriter.getRestProtocolVersion());
        Version clientVersion = new Version(version);

        if (serverVersion.getMajor() > clientVersion.getMajor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_CLIENT_TOO_OLD;
        } else if (serverVersion.getMajor() < clientVersion.getMajor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        } else if (serverVersion.getMinor() < clientVersion.getMinor()) {
            return SubsonicRESTController.ErrorCode.PROTOCOL_MISMATCH_SERVER_TOO_OLD;
        }
        return null;
    }

    private SubsonicRESTController.ErrorCode authenticate(HttpServletRequest httpRequest,
            String username, final String password, String salt, String token,
            Authentication previousAuth) {

        // Previously authenticated and username not overridden?
        if (username == null && previousAuth != null) {
            return null;
        }

        String pass = password;

        if (salt != null && token != null) {
            User user = securityService.getUserByName(username);
            if (user == null) {
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }
            String expectedToken = DigestUtils.md5Hex(user.getPassword() + salt);
            if (!expectedToken.equals(token)) {
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }

            pass = user.getPassword();
        }

        if (pass != null) {
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                    username, pass);
            authRequest.setDetails(authenticationDetailsSource.buildDetails(httpRequest));
            try {
                Authentication authResult = authenticationManager.authenticate(authRequest);
                SecurityContextHolder.getContext().setAuthentication(authResult);
                return null;
            } catch (AuthenticationException x) {
                eventPublisher
                    .publishEvent(new AuthenticationFailureBadCredentialsEvent(authRequest, x));
                return SubsonicRESTController.ErrorCode.NOT_AUTHENTICATED;
            }
        }

        return SubsonicRESTController.ErrorCode.MISSING_PARAMETER;
    }

    public static String decrypt(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("enc:")) {
            return s;
        }
        try {
            return StringUtil.utf8HexDecode(s.substring(4));
        } catch (DecoderException e) {
            return s;
        }
    }

    private void sendErrorXml(HttpServletRequest request, HttpServletResponse response,
            SubsonicRESTController.ErrorCode errorCode) {
        jaxbWriter.writeErrorResponse(request, response, errorCode, errorCode.getMessage());
    }
}
