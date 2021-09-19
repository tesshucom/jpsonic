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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@SuppressWarnings("PMD.AvoidReassigningParameters")
/*
 * Spring manners. Usually not desirable. Code that issues this warning in the future needs scrutiny.
 */
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "False positive. See spotbugs/spotbugs#1539.")
public class GlobalSecurityConfig extends GlobalAuthenticationConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalSecurityConfig.class);
    private static final String FAILURE_URL = "/login?error=1";
    private static final String DEVELOPMENT_REMEMBER_ME_KEY = "jpsonic";

    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final CustomUserDetailsContextMapper customUserDetailsContextMapper;

    public GlobalSecurityConfig(SecurityService securityService, SettingsService settingsService,
            CustomUserDetailsContextMapper customUserDetailsContextMapper) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.customUserDetailsContextMapper = customUserDetailsContextMapper;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap and rethrow due to constraints of 'springframework' {@link
     * AuthenticationManagerBuilder#ldapAuthentication()} {@link
     * AuthenticationManagerBuilder#userDetailsService(org.springframework.security.core.userdetails.UserDetailsService)
     * }
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws ExecutionException {
        if (settingsService.isLdapEnabled()) {
            try {
                auth.ldapAuthentication().contextSource().managerDn(settingsService.getLdapManagerDn())
                        .managerPassword(settingsService.getLdapManagerPassword()).url(settingsService.getLdapUrl())
                        .and().userSearchFilter(settingsService.getLdapSearchFilter())
                        .userDetailsContextMapper(customUserDetailsContextMapper);
            } catch (Exception e) {
                throw new ExecutionException("Ldap authentication failed.", e);
            }
        }
        try {
            auth.userDetailsService(securityService);
        } catch (Exception e) {
            throw new ExecutionException("Ldap additional authentication failed.", e);
        }
        String jwtKey = settingsService.getJWTKey();
        if (StringUtils.isBlank(jwtKey)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Generating new jwt key");
            }
            jwtKey = JWTSecurityService.generateKey();
            settingsService.setJWTKey(jwtKey);
            settingsService.save();
        }
        auth.authenticationProvider(new JWTAuthenticationProvider(jwtKey));
    }

    @Bean
    public PasswordEncoder delegatingPasswordEncoder() {

        // Spring Security 5 require storing the encoder id alongside the encoded password
        // (e.g. "{md5}hash" for an MD5-encoded password hash), which differs from previous
        // versions.
        //
        // Airsonic unfortunately stores passwords in plain-text, which is why we are setting
        // the "no-op" (plain-text) password encoder as a default here. This default will be
        // used when no encoder id is present.
        //
        // This means that legacy Airsonic passwords (stored simply as "password" in the db)
        // will be matched like "{noop}password" and will be recognized successfully. In the
        // future password encoding updates will be done here.

        PasswordEncoder defaultEncoder = NoOpPasswordEncoder.getInstance();
        String defaultIdForEncode = "noop";

        Map<String, PasswordEncoder> encoders = LegacyMap.of(defaultIdForEncode, defaultEncoder);
        DelegatingPasswordEncoder passworEncoder = new DelegatingPasswordEncoder(defaultIdForEncode, encoders);
        passworEncoder.setDefaultPasswordEncoderForMatches(defaultEncoder);

        return passworEncoder;
    }

    @Configuration
    @Order(1)
    public static class ExtSecurityConfiguration extends WebSecurityConfigurerAdapter {

        private final CsrfSecurityRequestMatcher csrfSecurityRequestMatcher;

        public ExtSecurityConfiguration(CsrfSecurityRequestMatcher csrfSecurityRequestMatcher) {
            super(true);
            this.csrfSecurityRequestMatcher = csrfSecurityRequestMatcher;
        }

        @SuppressWarnings("PMD.AvoidCatchingGenericException")
        /*
         * Wrap and rethrow due to constraints of 'springframework' {@link
         * WebSecurityConfigurerAdapter#authenticationManager()}
         */
        @Bean(name = "jwtAuthenticationFilter")
        public JWTRequestParameterProcessingFilter jwtAuthFilter() throws ExecutionException {
            try {
                return new JWTRequestParameterProcessingFilter(authenticationManager(), FAILURE_URL);
            } catch (Exception e) {
                throw new ExecutionException("AuthenticationManager initialization failed.", e);
            }
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http = http.addFilter(new WebAsyncManagerIntegrationFilter());
            http = http.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

            http.antMatcher("/ext/**").csrf().requireCsrfProtectionMatcher(csrfSecurityRequestMatcher).and().headers()
                    .frameOptions().sameOrigin().and().authorizeRequests()
                    .antMatchers("/ext/stream/**", "/ext/coverArt*", "/ext/share/**", "/ext/hls/**")
                    .hasAnyRole("TEMP", "USER").and().sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().exceptionHandling().and()
                    .securityContext().and().requestCache().and().anonymous().and().servletApi();
        }
    }

    @Configuration
    @Order(2)
    public static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

        private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

        private final CsrfSecurityRequestMatcher csrfSecurityRequestMatcher;
        private final ApplicationEventPublisher eventPublisher;
        private final Random random = new SecureRandom();
        private final SecurityService securityService;
        private final SettingsService settingsService;

        public WebSecurityConfiguration(CsrfSecurityRequestMatcher csrfSecurityRequestMatcher,
                ApplicationEventPublisher eventPublisher, SecurityService securityService,
                SettingsService settingsService) {
            super();
            this.csrfSecurityRequestMatcher = csrfSecurityRequestMatcher;
            this.eventPublisher = eventPublisher;
            this.securityService = securityService;
            this.settingsService = settingsService;
        }

        private String generateRememberMeKey() {
            byte[] array = new byte[32];
            random.nextBytes(array);
            return new String(array, StandardCharsets.UTF_8);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            RESTRequestParameterProcessingFilter restAuthenticationFilter = new RESTRequestParameterProcessingFilter();
            restAuthenticationFilter.setAuthenticationManager(authenticationManagerBean());
            restAuthenticationFilter.setSecurityService(securityService);
            restAuthenticationFilter.setEventPublisher(eventPublisher);
            http = http.addFilterBefore(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            // Try to load the 'remember me' key.
            //
            // Note that using a fixed key compromises security as perfect
            // forward secrecy is not guaranteed anymore.
            //
            // An external entity can then re-use our authentication cookies before
            // the expiration time, or even, given enough time, recover the password
            // from the MD5 hash.
            //
            // See: https://docs.spring.io/spring-security/site/docs/3.0.x/reference/remember-me.html

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
                    LOG.warn("Using a fixed 'remember me' key because we're in development mode, this is INSECURE.");
                }
                rememberMeKey = DEVELOPMENT_REMEMBER_ME_KEY;
            } else {
                // ...otherwise, use the custom key directly.
                if (LOG.isInfoEnabled()) {
                    LOG.info("Using a fixed 'remember me' key from system properties, this is insecure.");
                }
            }

            http.csrf().requireCsrfProtectionMatcher(csrfSecurityRequestMatcher).and().headers().frameOptions()
                    .sameOrigin().and().authorizeRequests()
                    .antMatchers("/recover*", "/accessDenied*", "/style/**", "/icons/**", "/flash/**", "/script/**",
                            "/login", "/error")
                    .permitAll()
                    .antMatchers("/personalSettings*", "/passwordSettings*", "/playerSettings*", "/shareSettings*",
                            "/passwordSettings*")
                    .hasRole("SETTINGS")
                    .antMatchers("/generalSettings*", "/advancedSettings*", "/userSettings*", "/internalhelp*",
                            "/musicFolderSettings*", "/databaseSettings*", "/transcodeSettings*", "/rest/startScan*")
                    .hasRole("ADMIN").antMatchers("/deletePlaylist*", "/savePlaylist*").hasRole("PLAYLIST")
                    .antMatchers("/download*").hasRole("DOWNLOAD").antMatchers("/upload*").hasRole("UPLOAD")
                    .antMatchers("/createShare*").hasRole("SHARE").antMatchers("/changeCoverArt*", "/editTags*")
                    .hasRole("COVERART").antMatchers("/setMusicFileInfo*").hasRole("COMMENT")
                    .antMatchers("/podcastReceiverAdmin*").hasRole("PODCAST").antMatchers("/**").hasRole("USER")
                    .anyRequest().authenticated().and().formLogin().loginPage("/login").permitAll()
                    .defaultSuccessUrl("/index", true).failureUrl(FAILURE_URL)
                    .usernameParameter(Attributes.Request.J_USERNAME.value())
                    .passwordParameter(Attributes.Request.J_PASSWORD.value())
                    // see
                    // http://docs.spring.io/spring-security/site/docs/3.2.4.RELEASE/reference/htmlsingle/#csrf-logout
                    .and().logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                    .logoutSuccessUrl("/login?logout").and().rememberMe().key(rememberMeKey);
        }

    }
}
