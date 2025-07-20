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

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Custom {@link RequestMatcher} for CSRF protection in Spring Security.
 * <p>
 * This matcher excludes certain HTTP methods and specific URL patterns from
 * CSRF protection.
 * <p>
 * <strong>Important notes and caveats:</strong>
 * <ul>
 * <li>Allowed HTTP methods that do not require CSRF protection are: GET, HEAD,
 * TRACE, OPTIONS.</li>
 * <li>URL patterns exempted from CSRF protection use
 * {@link PathPatternRequestMatcher} with HTTP method restrictions.</li>
 * <li>Due to deprecation of
 * {@link org.springframework.security.web.util.matcher.AntPathRequestMatcher},
 * this class uses the newer {@link PathPatternRequestMatcher} builder pattern
 * introduced in Spring Security 6.5+.</li>
 * <li>{@code PathPatternRequestMatcher} instances are constructed via
 * {@code PathPatternRequestMatcher.withDefaults().matcher(HttpMethod, String)},
 * since constructors are not publicly available.</li>
 * <li>Ensure that URL patterns are specified relative to the application
 * context root, and be aware of any trailing wildcard behaviors with
 * {@code PathPatternRequestMatcher}.</li>
 * <li>This implementation reflects the shift to
 * {@code PathPatternRequestMatcher} in preparation for Spring Security 7.</li>
 * </ul>
 * <p>
 * References:
 * <ul>
 * <li><a href=
 * "https://docs.spring.io/spring-security/site/docs/current/reference/html5/#csrf">Spring
 * Security CSRF Documentation</a></li>
 * <li><a href=
 * "https://docs.spring.io/spring-security/site/docs/current/reference/html5/#requestmatcher">RequestMatcher
 * in Spring Security</a></li>
 * <li><a href=
 * "https://spring.io/blog/2023/05/23/spring-security-6-2-0-rc1-available-now#pathpatternrequestmatcher">PathPatternRequestMatcher
 * usage</a></li>
 * </ul>
 */
@Component
public class CsrfSecurityRequestMatcher implements RequestMatcher {

    private static final List<String> ALLOWED_METHODS = Arrays
        .asList("GET", "HEAD", "TRACE", "OPTIONS");

    private final List<RequestMatcher> whiteListedMatchers;

    public CsrfSecurityRequestMatcher() {
        this.whiteListedMatchers = List
            .of(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/dwr/*.dwr"),
                    PathPatternRequestMatcher
                        .withDefaults()
                        .matcher(HttpMethod.POST, "/rest/*.view"));
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return !(ALLOWED_METHODS.contains(request.getMethod())
                || whiteListedMatchers.stream().anyMatch(m -> m.matches(request)));
    }
}
