/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.filter;

import org.airsonic.player.controller.JAXBWriter;
import org.airsonic.player.controller.SubsonicRESTController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.util.NestedServletException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Intercepts exceptions thrown by RESTController.
 *
 * Also adds the CORS response header (http://enable-cors.org)
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2006/03/01 16:58:08 $
 */
public class RESTFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RESTFilter.class);

    private final JAXBWriter jaxbWriter = new JAXBWriter();

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        try {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setHeader("Access-Control-Allow-Origin", "*");
            chain.doFilter(req, res);
        } catch (Throwable x) {
            handleException(x, (HttpServletRequest) req, (HttpServletResponse) res);
        }
    }

    private void handleException(final Throwable x, HttpServletRequest request, HttpServletResponse response) {
        Throwable t = x;
        if (t instanceof NestedServletException && t.getCause() != null) {
            t = t.getCause();
        }

        SubsonicRESTController.ErrorCode code = t instanceof ServletRequestBindingException
                ? SubsonicRESTController.ErrorCode.MISSING_PARAMETER
                : SubsonicRESTController.ErrorCode.GENERIC;
        String msg = getErrorMessage(t);
        if (LOG.isWarnEnabled()) {
            LOG.warn("Error in REST API: " + msg, t);
        }

        try {
            jaxbWriter.writeErrorResponse(request, response, code, msg);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to write error response.", e);
            }
        }
    }

    private String getErrorMessage(Throwable x) {
        if (x.getMessage() != null) {
            return x.getMessage();
        }
        return x.getClass().getSimpleName();
    }

    public void init(FilterConfig filterConfig) {
        // Don't remove this method.
    }

    public void destroy() {
        // Don't remove this method.
    }
}
