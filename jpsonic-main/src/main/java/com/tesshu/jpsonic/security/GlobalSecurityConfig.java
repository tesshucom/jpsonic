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

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

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
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// Usually not desirable. But that's spring justice
@SuppressWarnings({ "PMD.AvoidReassigningParameters", "PMD.SignatureDeclareThrowsException" })
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
                .addFilter(new WebAsyncManagerIntegrationFilter())
                .addFilterBefore(jwtRPPFilter, UsernamePasswordAuthenticationFilter.class)
                .securityMatchers((matchers) -> matchers.requestMatchers(antMatcher("/ext/**")))
                .csrf(config -> config.requireCsrfProtectionMatcher(csrfMatcher))
                .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests((authz) -> authz
                    .requestMatchers(antMatcher("/ext/stream/**"), antMatcher("/ext/coverArt*"),
                            antMatcher("/ext/share/**"), antMatcher("/ext/hls/**"))
                    .hasAnyRole("TEMP", "USER")
                    .anyRequest()
                    .authenticated())
                .sessionManagement((sessions) -> sessions
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(Customizer.withDefaults())
                .securityContext(Customizer.withDefaults())
                .requestCache(Customizer.withDefaults())
                .anonymous(Customizer.withDefaults())
                .servletApi(Customizer.withDefaults());
            return http.build();
        }
    }

    @EnableWebSecurity
    @Order(2)
    public static class SecurityConfig {

        @Bean
        public RESTRequestParameterProcessingFilter restRPPFilter(SecurityService securityService,
                AuthenticationManager authenticationManager,
                ApplicationEventPublisher eventPublisher) {
            RESTRequestParameterProcessingFilter restRPPFilter = new RESTRequestParameterProcessingFilter();
            restRPPFilter.setAuthenticationManager(authenticationManager);
            restRPPFilter.setSecurityService(securityService);
            restRPPFilter.setEventPublisher(eventPublisher);
            return restRPPFilter;
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
                .addFilterBefore(restRPPFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(config -> config.requireCsrfProtectionMatcher(csrfMatcher))
                .headers(config -> config.frameOptions(FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(config -> config
                    .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(antMatcher("/recover*"), antMatcher("/accessDenied*"),
                            antMatcher("/style/**"), antMatcher("/icons/**"),
                            antMatcher("/flash/**"), antMatcher("/script/**"), antMatcher("/login"),
                            antMatcher("/login.view"), antMatcher("/error"))
                    .permitAll()
                    .requestMatchers(antMatcher("/personalSettings*"),
                            antMatcher("/passwordSettings*"), antMatcher("/playerSettings*"),
                            antMatcher("/shareSettings*"), antMatcher("/passwordSettings*"))
                    .hasRole("SETTINGS")
                    .requestMatchers(antMatcher("/generalSettings*"),
                            antMatcher("/advancedSettings*"), antMatcher("/userSettings*"),
                            antMatcher("/internalhelp*"), antMatcher("/musicFolderSettings*"),
                            antMatcher("/databaseSettings*"), antMatcher("/transcodeSettings*"),
                            antMatcher("/rest/startScan*"))
                    .hasRole("ADMIN")
                    .requestMatchers(antMatcher("/deletePlaylist*"), antMatcher("/savePlaylist*"))
                    .hasRole("PLAYLIST")
                    .requestMatchers(antMatcher("/download*"))
                    .hasRole("DOWNLOAD")
                    .requestMatchers(antMatcher("/upload*"))
                    .hasRole("UPLOAD")
                    .requestMatchers(antMatcher("/createShare*"))
                    .hasRole("SHARE")
                    .requestMatchers(antMatcher("/changeCoverArt*"), antMatcher("/editTags*"))
                    .hasRole("COVERART")
                    .requestMatchers(antMatcher("/setMusicFileInfo*"))
                    .hasRole("COMMENT")
                    .requestMatchers(antMatcher("/podcastReceiverAdmin*"))
                    .hasRole("PODCAST")
                    .requestMatchers(antMatcher("/**"))
                    .hasRole("USER")
                    .anyRequest()
                    .authenticated())
                .formLogin(config -> config
                    .loginPage("/login")
                    .permitAll()
                    .defaultSuccessUrl("/index", true)
                    .failureUrl(FAILURE_URL)
                    .usernameParameter(Attributes.Request.J_USERNAME.value())
                    .passwordParameter(Attributes.Request.J_PASSWORD.value()))
                .logout(config -> config
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                    .logoutSuccessUrl("/login?logout"))
                .rememberMe(config -> config.key(keyGenerator.get()));
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
