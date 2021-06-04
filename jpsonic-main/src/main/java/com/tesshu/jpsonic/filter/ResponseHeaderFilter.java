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
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Configurable filter for setting HTTP response headers. Can be used, for instance, to set cache control directives for
 * certain resources.
 *
 * @author Sindre Mehus
 */
public class ResponseHeaderFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;

        // Sets the provided HTTP response parameters
        for (Enumeration<String> e = filterConfig.getInitParameterNames(); e.hasMoreElements();) {
            String headerName = e.nextElement();
            response.setHeader(headerName, filterConfig.getInitParameter(headerName));
        }

        // pass the request/response on
        chain.doFilter(req, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @SuppressWarnings("PMD.NullAssignment") // (filterConfig) Intentional allocation to destroy
    @Override
    public void destroy() {
        this.filterConfig = null;
    }
}
