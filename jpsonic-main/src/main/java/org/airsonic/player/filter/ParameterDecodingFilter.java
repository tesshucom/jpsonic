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

package org.airsonic.player.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter which decodes HTTP request parameters. If a parameter name ends with "Utf8Hex" ({@link #PARAM_SUFFIX})
 * , the corresponding parameter value is assumed to be the hexadecimal representation of the UTF-8 bytes of the value.
 * <p/>
 * Used to support request parameter values of any character encoding.
 *
 * @author Sindre Mehus
 */
public class ParameterDecodingFilter implements Filter {

    public static final String PARAM_SUFFIX = "Utf8Hex";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Wrap request in decoder.
        ServletRequest decodedRequest = new DecodingServletRequestWrapper((HttpServletRequest) request);

        // Pass the request/response on
        chain.doFilter(decodedRequest, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Don't remove this method.
    }

    @Override
    public void destroy() {
        // Don't remove this method.
    }

    private static class DecodingServletRequestWrapper extends HttpServletRequestWrapper {

        private static final Logger LOG = LoggerFactory.getLogger(DecodingServletRequestWrapper.class);

        public DecodingServletRequestWrapper(HttpServletRequest servletRequest) {
            super(servletRequest);
        }

        @Override
        public String getParameter(String name) {
            String[] values = getParameterValues(name);
            if (values == null || values.length == 0) {
                return null;
            }
            return values[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = super.getParameterMap();
            Map<String, String[]> result = LegacyMap.of();

            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String name = entry.getKey();
                String[] values = entry.getValue();

                if (name.endsWith(PARAM_SUFFIX)) {
                    result.put(name.replace(PARAM_SUFFIX, ""), decode(values));
                } else {
                    result.put(name, values);
                }
            }
            return result;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            Enumeration<String> e = super.getParameterNames();
            List<String> v = new ArrayList<>();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                if (name.endsWith(PARAM_SUFFIX)) {
                    name = name.replace(PARAM_SUFFIX, "");
                }
                v.add(name);
            }
            return Collections.enumeration(v);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values != null) {
                return values;
            }

            values = super.getParameterValues(name + PARAM_SUFFIX);
            if (values != null) {
                return decode(values);
            }

            return new String[0];
        }

        private String[] decode(String... values) {
            if (values == null) {
                return null;
            }

            String[] result = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    result[i] = StringUtil.utf8HexDecode(values[i]);
                } catch (DecoderException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to decode parameter value '" + values[i] + "'", e);
                    }
                    result[i] = values[i];
                }
            }

            return result;
        }

    }

}
