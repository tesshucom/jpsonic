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

package com.tesshu.jpsonic.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.monitor.MetricsManager;
import org.springframework.beans.factory.annotation.Autowired;

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String timerName = httpServletRequest.getRequestURI();
        // Add a metric that measures the time spent for each http request for the /ViewName.MAIN.value() url.
        try (MetricsManager.Timer t = metricsManager.condition(timerName.contains(ViewName.MAIN.value())).timer(this,
                timerName)) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Don't remove this method.
    }
}
