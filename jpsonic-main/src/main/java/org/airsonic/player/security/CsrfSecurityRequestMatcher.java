
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
