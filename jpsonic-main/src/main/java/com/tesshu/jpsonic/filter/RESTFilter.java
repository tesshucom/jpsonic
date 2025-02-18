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

import com.tesshu.jpsonic.controller.JAXBWriter;
import com.tesshu.jpsonic.controller.SubsonicRESTController;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestBindingException;

/**
 * Intercepts exceptions thrown by RESTController.
 * <p>
 * Also adds the CORS response header (http://enable-cors.org)
 *
 * @author Sindre Mehus
 */
public class RESTFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RESTFilter.class);

    private final JAXBWriter jaxbWriter;

    public RESTFilter() {
        jaxbWriter = new JAXBWriter(null);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        try {
            chain.doFilter(req, res);
        } catch (IOException | ServletException e) {
            handleException(e, (HttpServletRequest) req, (HttpServletResponse) res);
        }
    }

    private void handleException(final Throwable x, HttpServletRequest request, HttpServletResponse response) {
        Throwable t = x;
        if (t instanceof ServletException && t.getCause() != null) {
            t = t.getCause();
        }

        SubsonicRESTController.ErrorCode code = t instanceof ServletRequestBindingException
                ? SubsonicRESTController.ErrorCode.MISSING_PARAMETER
                : SubsonicRESTController.ErrorCode.GENERIC;
        String msg = getErrorMessage(t);
        if (LOG.isWarnEnabled()) {
            LOG.warn("Error in REST API: " + msg, t);
        }
        jaxbWriter.writeErrorResponse(request, response, code, msg);
    }

    private String getErrorMessage(Throwable x) {
        if (x.getMessage() != null) {
            return x.getMessage();
        }
        return x.getClass().getSimpleName();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Don't remove this method.
    }

    @Override
    public void destroy() {
        // Don't remove this method.
    }
}
