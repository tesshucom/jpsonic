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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is executed very early in the filter chain. It verifies that the Airsonic home directory (c:\airsonic or
 * /var/airsonic) exists and is writable. If not, a proper error message is given to the user.
 * <p/>
 * (The Airsonic home directory is usually created automatically, but a common problem on Linux is that the Tomcat user
 * does not have the necessary privileges).
 *
 * @author Sindre Mehus
 */
public class BootstrapVerificationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapVerificationFilter.class);

    private boolean jpsonicHomeVerified;
    private final AtomicBoolean serverInfoLogged;

    public BootstrapVerificationFilter() {
        serverInfoLogged = new AtomicBoolean();
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Already verified?
        if (jpsonicHomeVerified) {
            chain.doFilter(req, res);
            return;
        }

        File home = SettingsService.getJpsonicHome();
        if (!directoryExists(home)) {
            writeError(res,
                    "<p>The directory <b>" + home + "</b> does not exist. Please create it and make it writable, "
                            + "then restart the servlet container.</p>"
                            + "<p>(You can override the directory location by specifying -Dairsonic.home=... when "
                            + "starting the servlet container.)</p>");

        } else if (!directoryWritable(home)) {
            writeError(res,
                    "<p>The directory <b>" + home + "</b> is not writable. Please change file permissions, "
                            + "then restart the servlet container.</p>"
                            + "<p>(You can override the directory location by specifying -Dairsonic.home=... when "
                            + "starting the servlet container.)</p>");

        } else {
            jpsonicHomeVerified = true;
            logServerInfo(req);
            chain.doFilter(req, res);
        }
    }

    private void logServerInfo(ServletRequest req) {
        if (!serverInfoLogged.getAndSet(true) && req instanceof HttpServletRequest) {
            String serverInfo = ((HttpServletRequest) req).getSession().getServletContext().getServerInfo();
            if (LOG.isInfoEnabled()) {
                LOG.info("Servlet container: " + serverInfo);
            }
        }
    }

    private boolean directoryExists(File dir) {
        return dir.exists() && dir.isDirectory();
    }

    private boolean directoryWritable(File dir) {
        try {
            File tempFile = File.createTempFile("test", null, dir);
            return tempFile.delete();
        } catch (IOException x) {
            return false;
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    /*
     * False positive. The container will close at the end of service.
     */
    private void writeError(ServletResponse res, String error) throws IOException {
        ServletOutputStream out = res.getOutputStream();
        out.println("<html>" + "<head><title>Airsonic Error</title></head>" + "<body>" + "<h2>Airsonic Error</h2>"
                + error + "</body>" + "</html>");
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
