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

package com.tesshu.jpsonic.feature.security;

import com.tesshu.jpsonic.feature.auth.jwt.JWTRequestParameterProcessingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class ExtSecurityConfig {

    static final String FAILURE_URL = "/login?error=1";

    @Bean
    public JWTRequestParameterProcessingFilter jwtRPPFilter(
            AuthenticationManager authenticationManager) {
        return new JWTRequestParameterProcessingFilter(authenticationManager, FAILURE_URL);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Bean
    public SecurityFilterChain extSecurityFilterChain(HttpSecurity http,
            JWTRequestParameterProcessingFilter jwtRPPFilter,
            CsrfSecurityRequestMatcher csrfMatcher) throws Exception {

        http
            // Add the JWT filter before the UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtRPPFilter, UsernamePasswordAuthenticationFilter.class)

            // Limit this security filter chain to requests matching /ext/**
            .securityMatchers(matchers -> matchers.requestMatchers("/ext/**"))

            // Configure CSRF protection using a custom matcher
            .csrf(config -> config.requireCsrfProtectionMatcher(csrfMatcher))

            // Set X-Frame-Options header to SAMEORIGIN (e.g., for embedded content)
            .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))

            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/ext/stream/**", "/ext/coverArt*", "/ext/share/**", "/ext/hls/**")
                .hasAnyRole("TEMP", "USER") // Require TEMP or USER roles for these paths
                .anyRequest()
                .authenticated() // All other /ext/** requests require authentication
            )

            // Use stateless session management (e.g., token-based authentication)
            .sessionManagement(
                    sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Use default exception handling
            .exceptionHandling(Customizer.withDefaults())

            // Use default security context management
            .securityContext(Customizer.withDefaults())

            // Use default request cache
            .requestCache(Customizer.withDefaults())

            // Enable anonymous authentication (can be disabled if not needed)
            .anonymous(Customizer.withDefaults())

            // Enable Servlet API integration
            .servletApi(Customizer.withDefaults());

        return http.build();
    }
}
