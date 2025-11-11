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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.SessionTrackingMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Usually not desirable. But that's spring justice
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@DependsOn("liquibase")
@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
@EnableMethodSecurity(securedEnabled = true)
public class GlobalSecurityConfig extends GlobalAuthenticationConfigurerAdapter {

    private static final String FAILURE_URL = "/login?error=1";
    private static final String DEVELOPMENT_REMEMBER_ME_KEY = "jpsonic";

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return context -> context.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
    }

    @Bean
    public PasswordEncoder delegatingPasswordEncoder() {
        PasswordEncoder defaultEncoder = NoOpPasswordEncoder.getInstance();
        String defaultIdForEncode = "noop";
        Map<String, PasswordEncoder> encoders = LegacyMap.of(defaultIdForEncode, defaultEncoder);
        DelegatingPasswordEncoder passworEncoder = new DelegatingPasswordEncoder(defaultIdForEncode,
                encoders);
        passworEncoder.setDefaultPasswordEncoderForMatches(defaultEncoder);
        return passworEncoder;
    }

    @EnableWebSecurity
    public static class AuthenticationManagerConfig {

        @Autowired
        public void configure(SettingsService settingsService, SecurityService securityService,
                AuthenticationManagerBuilder auth,
                CustomUserDetailsContextMapper customUserDetailsContextMapper) throws Exception {
            if (settingsService.isLdapEnabled()) {
                auth
                    .ldapAuthentication()
                    .contextSource()
                    .managerDn(settingsService.getLdapManagerDn())
                    .managerPassword(settingsService.getLdapManagerPassword())
                    .url(settingsService.getLdapUrl())
                    .and()
                    .userSearchFilter(settingsService.getLdapSearchFilter())
                    .userDetailsContextMapper(customUserDetailsContextMapper);
            }
            auth.userDetailsService(securityService);
            String jwtKey = settingsService.getJWTKey();
            if (StringUtils.isBlank(jwtKey)) {
                LoggerFactory.getLogger(GlobalSecurityConfig.class).warn("Generating new jwt key");
                jwtKey = JWTSecurityService.generateKey();
                settingsService.setJWTKey(jwtKey);
                settingsService.save();
            }
            auth.authenticationProvider(new JWTAuthenticationProvider(jwtKey));
        }

        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration authenticationConfiguration) throws Exception {
            return authenticationConfiguration.getAuthenticationManager();
        }
    }

    @EnableWebSecurity
    @Order(1)
    public static class ExtSecurityConfig {

        @Bean
        public JWTRequestParameterProcessingFilter jwtRPPFilter(
                AuthenticationManager authenticationManager) {
            return new JWTRequestParameterProcessingFilter(authenticationManager, FAILURE_URL);
        }

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
                    .requestMatchers("/ext/stream/**", "/ext/coverArt*", "/ext/share/**",
                            "/ext/hls/**")
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

    @EnableWebSecurity
    @Order(2)
    public static class SecurityConfig {

        @Bean
        public RESTRequestParameterProcessingFilter restRPPFilter(SecurityService service,
                AuthenticationManager manager, ApplicationEventPublisher publisher) {
            return new RESTRequestParameterProcessingFilter(manager, service, publisher);
        }

        @Bean
        public RememberMeKeyGenerator rememberMeKeyGenerator(SettingsService settingsService) {
            return new RememberMeKeyGenerator(settingsService);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                RESTRequestParameterProcessingFilter restRPPFilter,
                RememberMeKeyGenerator keyGenerator, CsrfSecurityRequestMatcher csrfMatcher)
                throws Exception {

            http
                // Add REST authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(restRPPFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable CSRF protection for requests matching csrfMatcher
                .csrf(csrf -> csrf.ignoringRequestMatchers(csrfMatcher))

                // Allow framing only from the same origin
                .headers(config -> config.frameOptions(FrameOptionsConfig::sameOrigin))

                // Start authorization configuration
                .authorizeHttpRequests(config -> config
                    // Allow requests with FORWARD and ERROR dispatcher types without authentication
                    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                    .permitAll()

                    // Permit all access to login, error pages and static resources
                    .requestMatchers("/recover*", "/accessDenied*", "/style/**", "/icons/**",
                            "/flash/**", "/script/**", "/login", "/login.view", "/error")
                    .permitAll()

                    // Require SETTINGS role for settings related URLs
                    .requestMatchers("/personalSettings*", "/passwordSettings*", "/playerSettings*",
                            "/shareSettings*")
                    .hasRole("SETTINGS")

                    // Require ADMIN role for admin-related URLs
                    .requestMatchers("/generalSettings*", "/advancedSettings*", "/userSettings*",
                            "/internalhelp*", "/musicFolderSettings*", "/databaseSettings*",
                            "/transcodeSettings*", "/rest/startScan*")
                    .hasRole("ADMIN")

                    // Require PLAYLIST role for playlist modification URLs
                    .requestMatchers("/deletePlaylist*", "/savePlaylist*")
                    .hasRole("PLAYLIST")

                    // Require DOWNLOAD role for download URLs
                    .requestMatchers("/download*")
                    .hasRole("DOWNLOAD")

                    // Require UPLOAD role for upload URLs
                    .requestMatchers("/upload*")
                    .hasRole("UPLOAD")

                    // Require SHARE role for share creation URLs
                    .requestMatchers("/createShare*")
                    .hasRole("SHARE")

                    // Require COVERART role for cover art modification URLs
                    .requestMatchers("/changeCoverArt*", "/editTags*")
                    .hasRole("COVERART")

                    // Require COMMENT role for music file info setting URLs
                    .requestMatchers("/setMusicFileInfo*")
                    .hasRole("COMMENT")

                    // Require PODCAST role for podcast receiver admin URLs
                    .requestMatchers("/podcastReceiverAdmin*")
                    .hasRole("PODCAST")

                    // Any request not matched by previous rules requires the user to be
                    // authenticated with the "USER" role
                    .anyRequest()
                    .hasRole("USER"))

                // Configure form login
                .formLogin(config -> config
                    .loginPage("/login") // Custom login page URL
                    .permitAll() // Allow all to access login page
                    .defaultSuccessUrl("/index", true) // Redirect after successful login
                    .failureUrl(FAILURE_URL) // Redirect after failed login
                    .usernameParameter(Attributes.Request.J_USERNAME.value()) // Username parameter
                                                                              // name
                    .passwordParameter(Attributes.Request.J_PASSWORD.value())) // Password parameter
                                                                               // name

                // Configure logout(POST Only)
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))

                // Configure remember-me with injected key
                .rememberMe(config -> config.key(keyGenerator.get()))

                // Enable anonymous access handling. Without this, unauthenticated users
                // have no Authentication object in the SecurityContext, which can cause
                // issues in security checks or cause NullPointerExceptions.
                .anonymous(Customizer.withDefaults());

            return http.build();
        }
    }

    public static class RememberMeKeyGenerator {

        private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
        private final SettingsService settingsService;
        private final Random random = new SecureRandom();

        public RememberMeKeyGenerator(SettingsService settingsService) {
            super();
            this.settingsService = settingsService;
        }

        private String generateRememberMeKey() {
            byte[] array = new byte[32];
            random.nextBytes(array);
            return new String(array, StandardCharsets.UTF_8);
        }

        public String get() {

            // Try to load the 'remember me' key.
            //
            // Note that using a fixed key compromises security as perfect
            // forward secrecy is not guaranteed anymore.
            //
            // An external entity can then re-use our authentication cookies before
            // the expiration time, or even, given enough time, recover the password
            // from the MD5 hash.
            //
            // See:
            // https://docs.spring.io/spring-security/site/docs/3.0.x/reference/remember-me.html

            String rememberMeKey = settingsService.getRememberMeKey();
            boolean development = SettingsService.isDevelopmentMode();
            if (StringUtils.isBlank(rememberMeKey) && !development) {
                // ...if it is empty, generate a random key on startup (default).
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Generating a new ephemeral 'remember me' key in a secure way.");
                }
                rememberMeKey = generateRememberMeKey();
            } else if (StringUtils.isBlank(rememberMeKey) && development) {
                // ...if we are in development mode, we can use a fixed key.
                if (LOG.isWarnEnabled()) {
                    LOG
                        .warn("Using a fixed 'remember me' key because we're in development mode, this is INSECURE.");
                }
                rememberMeKey = DEVELOPMENT_REMEMBER_ME_KEY;
            } else {
                // ...otherwise, use the custom key directly.
                if (LOG.isInfoEnabled()) {
                    LOG
                        .info("Using a fixed 'remember me' key from system properties, this is insecure.");
                }
            }
            return rememberMeKey;
        }
    }
}
