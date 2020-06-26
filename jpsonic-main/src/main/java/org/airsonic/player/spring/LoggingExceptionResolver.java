package org.airsonic.player.spring;

import org.airsonic.player.util.PlayerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoggingExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionResolver.class);

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, Object o, Exception e
    ) {
        // This happens often and outside of the control of the server, so
        // we catch Tomcat/Jetty "connection aborted by client" exceptions
        // and display a short error message.
        boolean shouldCatch = PlayerUtils.isInstanceOfClassName(e, "org.apache.catalina.connector.ClientAbortException");
        if (shouldCatch) {
            if (LOG.isInfoEnabled()) {
                LOG.info("{}: Client unexpectedly closed connection while loading {} ({})", request.getRemoteAddr(), PlayerUtils.getAnonymizedURLForRequest(request), e.getCause().toString());
            }
            return null;
        }

        // Display a full stack trace in all other cases
        if (LOG.isErrorEnabled()) {
            LOG.error("{}: An exception occurred while loading {}", request.getRemoteAddr(), PlayerUtils.getAnonymizedURLForRequest(request), e);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
