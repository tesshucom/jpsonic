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

package org.airsonic.player.service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service("jwtSecurityService")
public class JWTSecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(JWTSecurityService.class);
    public static final String JWT_PARAM_NAME = "jwt";
    public static final String CLAIM_PATH = "path";
    public static final int DEFAULT_DAYS_VALID_FOR = 7; // TODO make this configurable
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SettingsService settingsService;

    public JWTSecurityService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public static String generateKey() {
        BigInteger randomInt = new BigInteger(130, SECURE_RANDOM);
        return randomInt.toString(32);
    }

    public static Algorithm getAlgorithm(String jwtKey) {
        return Algorithm.HMAC256(jwtKey);
    }

    private static String createToken(String jwtKey, String path, Date expireDate) {
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
        return addJWTToken(builder, DateUtils.addDays(new Date(), DEFAULT_DAYS_VALID_FOR));
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder, Date expires) {
        String token = JWTSecurityService.createToken(settingsService.getJWTKey(), builder.toUriString(), expires);
        builder.queryParam(JWTSecurityService.JWT_PARAM_NAME, token);
        return builder;
    }

    public static DecodedJWT verify(String jwtKey, String token) {
        Algorithm algorithm = JWTSecurityService.getAlgorithm(jwtKey);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    public DecodedJWT verify(String credentials) {
        return verify(settingsService.getJWTKey(), credentials);
    }
}
