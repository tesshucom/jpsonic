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

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.domain.repository.AuthKeyRepository;
import com.tesshu.jpsonic.feature.auth.rememberme.AdaptiveRememberMeAuthenticationProvider;
import com.tesshu.jpsonic.feature.auth.rememberme.AdaptiveRememberMeServices;
import com.tesshu.jpsonic.feature.auth.rememberme.RememberMeKeyManager;
import com.tesshu.jpsonic.feature.auth.rest.RESTRequestParameterProcessingFilter;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.service.UserService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class ApiSecurityConfig {

    @Bean
    public RESTRequestParameterProcessingFilter restRPPFilter(UserService userService,
            AuthenticationManager manager, ApplicationEventPublisher publisher) {
        return new RESTRequestParameterProcessingFilter(manager, userService, publisher);
    }

    @Bean
    public RememberMeKeyManager rememberMeKeyManager(SettingsFacade settingsFacade,
            AuthKeyRepository authKeyRepository) {
        return new RememberMeKeyManager(settingsFacade, authKeyRepository);
    }

    @Bean
    public AdaptiveRememberMeServices adaptiveRememberMeServices(SettingsFacade settingsFacade,
            UserDetailsService userDetailsService, RememberMeKeyManager rememberMeKeyManager) {
        return new AdaptiveRememberMeServices(settingsFacade, userDetailsService,
                rememberMeKeyManager);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            RESTRequestParameterProcessingFilter restRPPFilter,
            AdaptiveRememberMeServices adaptiveRememberMeServices,
            RememberMeKeyManager rememberMeKeyManager, CsrfSecurityRequestMatcher csrfMatcher)
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
                .failureUrl(ExtSecurityConfig.FAILURE_URL) // Redirect after failed login
                .usernameParameter(Attributes.Request.J_USERNAME.value()) // Username parameter
                                                                          // name
                .passwordParameter(Attributes.Request.J_PASSWORD.value())) // Password parameter
                                                                           // name

            // Configure logout(POST Only)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(adaptiveRememberMeServices)
                .logoutSuccessUrl("/login?logout"))

            // Configure remember-me
            .rememberMe(config -> config.rememberMeServices(adaptiveRememberMeServices))
            .authenticationProvider(
                    new AdaptiveRememberMeAuthenticationProvider(rememberMeKeyManager))

            // Enable anonymous access handling. Without this, unauthenticated users
            // have no Authentication object in the SecurityContext, which can cause
            // issues in security checks or cause NullPointerExceptions.
            .anonymous(Customizer.withDefaults());

        return http.build();
    }
}
