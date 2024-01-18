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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service("jwtSecurityService")
public class JWTSecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(JWTSecurityService.class);
    public static final String JWT_PARAM_NAME = "jwt";
    public static final String CLAIM_PATH = "path";
    public static final int DEFAULT_DAYS_VALID_FOR = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int WITH_FILE_EXTENSION = 4;

    private final SettingsService settingsService;

    public JWTSecurityService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public static String generateKey() {
        BigInteger randomInt = new BigInteger(130, SECURE_RANDOM);
        return randomInt.toString(32);
    }

    static Algorithm getAlgorithm(String jwtKey) {
        return Algorithm.HMAC256(jwtKey);
    }

    static String createToken(String jwtKey, String path, Instant expireDate) {
        UriComponents components = UriComponentsBuilder.fromUriString(path).build();
        String query = components.getQuery();
        String claim = components.getPath() + (StringUtils.isBlank(query) ? "" : "?" + components.getQuery());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating token with claim " + claim);
        }
        return JWT.create().withClaim(CLAIM_PATH, claim).withExpiresAt(expireDate).sign(getAlgorithm(jwtKey));
    }

    public String addJWTToken(String uri) {
        return addJWTToken(UriComponentsBuilder.fromUriString(uri)).build().toString();
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder) {
        return addJWTToken(builder, now().plus(DEFAULT_DAYS_VALID_FOR, ChronoUnit.DAYS));
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder, Instant expires) {
        String token = createToken(settingsService.getJWTKey(), builder.toUriString(), expires);
        builder.queryParam(JWT_PARAM_NAME, token);
        return builder;
    }

    public static DecodedJWT verify(String jwtKey, String token) {
        Algorithm algorithm = getAlgorithm(jwtKey);
        JWTVerifier verifier = JWT.require(algorithm).withClaimPresence(CLAIM_PATH).build();
        try {
            DecodedJWT decoded = verifier.verify(
                    token.split("\\.").length == WITH_FILE_EXTENSION ? FilenameUtils.removeExtension(token) : token);
            return verifier.verify(decoded);
        } catch (TokenExpiredException e) {
            throw new com.tesshu.jpsonic.security.TokenExpiredException("The token has expired.", e);
        } catch (SignatureVerificationException e) {
            throw new com.tesshu.jpsonic.security.SignatureVerificationException(
                    "The token's signature resulted invalid.", e);
        } catch (JWTVerificationException e) {
            throw new BadCredentialsException("The token is incorrect.", e);
        }
    }

    public DecodedJWT verify(String credentials) {
        return verify(settingsService.getJWTKey(), credentials);
    }
}
