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

package org.airsonic.player.spring;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.airsonic.player.util.PlayerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public class LoggingExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionResolver.class);

    /**
     * A list of frequently exceptions that occur outside the control of the server. It is managed by a string to store
     * vendor-specific exceptions. (Depending on packaging, these classes may not exist in the classpath)
     */
    private static final List<String> TO_BE_SUPPRESSED_CLASS_NAME = Arrays
            .asList("org.apache.catalina.connector.ClientAbortException", "org.eclipse.jetty.io.EofException");

    private static boolean isInstanceOfClassName(Object o, String className) {
        try {
            return Class.forName(className).isInstance(o);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isSuppressedException(Exception e) {
        return TO_BE_SUPPRESSED_CLASS_NAME.stream().anyMatch(name -> isInstanceOfClassName(e, name));
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object o,
            Exception e) {

        // Trace specific exceptions.
        if (isSuppressedException(e)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(request.getRemoteAddr() + ": Client unexpectedly closed connection while loading "
                        + request.getRemoteAddr() + " (" + PlayerUtils.getAnonymizedURLForRequest(request) + ")", e);
            }
            return null;
        }

        // Display a full stack trace in all other cases.
        if (LOG.isErrorEnabled()) {
            LOG.error(request.getRemoteAddr() + ": An exception occurred while loading "
                    + PlayerUtils.getAnonymizedURLForRequest(request), e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
