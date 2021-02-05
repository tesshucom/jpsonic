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

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object o,
            Exception e) {
        // This happens often and outside of the control of the server, so
        // we catch Tomcat/Jetty "connection aborted by client" exceptions
        // and display a short error message.
        boolean shouldCatch = PlayerUtils.isInstanceOfClassName(e,
                "org.apache.catalina.connector.ClientAbortException");
        if (shouldCatch) {
            if (LOG.isInfoEnabled()) {
                LOG.info("{}: Client unexpectedly closed connection while loading {} ({})", request.getRemoteAddr(),
                        PlayerUtils.getAnonymizedURLForRequest(request), e.getCause().toString());
            }
            return null;
        }

        // Display a full stack trace in all other cases
        if (LOG.isErrorEnabled()) {
            LOG.error("{}: An exception occurred while loading {}", request.getRemoteAddr(),
                    PlayerUtils.getAnonymizedURLForRequest(request), e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
