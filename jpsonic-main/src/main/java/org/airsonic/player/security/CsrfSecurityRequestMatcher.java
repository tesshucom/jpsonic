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

package org.airsonic.player.security;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * See
 *
 * http://blogs.sourceallies.com/2014/04/customizing-csrf-protection-in-spring-security/
 * https://docs.spring.io/spring-security/site/docs/current/reference/html/appendix-namespace.html#nsa-csrf
 *
 *
 */
@Component
public class CsrfSecurityRequestMatcher implements RequestMatcher {
    private static List<String> allowedMethods = Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS");
    private List<RegexRequestMatcher> whiteListedMatchers;

    public CsrfSecurityRequestMatcher() {
        this.whiteListedMatchers = Arrays.asList(new RegexRequestMatcher("/dwr/.*\\.dwr", "POST"),
                new RegexRequestMatcher("/rest/.*\\.view(\\?.*)?", "POST"),
                new RegexRequestMatcher("/search(?:\\.view)?", "POST"));
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return !(allowedMethods.contains(request.getMethod())
                || whiteListedMatchers.stream().anyMatch(matcher -> matcher.matches(request)));
    }
}
