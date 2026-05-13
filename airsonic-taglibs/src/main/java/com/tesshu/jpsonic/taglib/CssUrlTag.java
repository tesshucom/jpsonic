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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.taglib;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * JSP tag that resolves and outputs the CSS URL for the current request. The
 * CSS URL is provided by a Spring-managed CssUrlProvider bean. Thread-safe
 * initialization ensures the provider is cached on first access.
 */
public class CssUrlTag extends SimpleTagSupport {

    private static volatile CssUrlProvider cachedProvider;
    private static final Lock initLock = new ReentrantLock();

    /**
     * Executes the tag. Retrieves the CSS URL from CssUrlProvider and writes it to
     * the JSP output.
     *
     * @throws IOException if an I/O error occurs while writing to the output
     */
    @Override
    public void doTag() throws IOException {
        CssUrlProvider provider = getProvider(getJspContext());
        String cssUrl = provider.getCssUrl(getJspContext());
        getJspContext().getOut().write(cssUrl);
    }

    /**
     * Returns the cached CssUrlProvider instance. If not yet initialized, retrieves
     * it from the Spring WebApplicationContext via JspContext and caches it.
     *
     * @param jspContext the current JSP context
     * @return the initialized CssUrlProvider instance
     */
    private CssUrlProvider getProvider(JspContext jspContext) {
        if (cachedProvider != null) {
            return cachedProvider;
        }

        initLock.lock();
        try {
            if (cachedProvider == null) {
                WebApplicationContext ctx = WebApplicationContextUtils
                    .getWebApplicationContext(
                            ((jakarta.servlet.jsp.PageContext) jspContext).getServletContext());

                if (ctx == null) {
                    throw new IllegalStateException(
                            "Spring WebApplicationContext is not available");
                }

                cachedProvider = ctx.getBean(CssUrlProvider.class);
            }
            return cachedProvider;
        } finally {
            initLock.unlock();
        }
    }
}
