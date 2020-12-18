package org.airsonic.player.filter;

import org.airsonic.player.monitor.MetricsManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Created by remi on 12/01/17.
 */
public class MetricsFilter implements Filter {

    @Autowired
    private MetricsManager metricsManager;

    @Override
    public void init(FilterConfig filterConfig) {
        // Don't remove this method.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;

        String timerName = httpServletRequest.getRequestURI();
        // Add a metric that measures the time spent for each http request for the /main.view url.
        try (MetricsManager.Timer t = metricsManager.condition(timerName.contains("main.view")).timer(this,timerName)) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Don't remove this method.
    }
}
