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

package com.tesshu.jpsonic.taglib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.tesshu.jpsonic.filter.ParameterDecodingFilter;
import com.tesshu.jpsonic.util.StringUtilBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.web.util.UrlUtils;

/**
 * Creates a URL with optional query parameters. Similar to 'c:url', but you may
 * specify which character encoding to use for the URL query parameters. If no
 * encoding is specified, the following steps are performed:
 * <ul>
 * <li>Parameter values are encoded as the hexadecimal representation of the
 * UTF-8 bytes of the original string.</li>
 * <li>Parameter names are prepended with the suffix "Utf8Hex"</li>
 * <li>Note: Nothing is done with the parameter name or value if the value only
 * contains ASCII alphanumeric characters.</li>
 * </ul>
 * <p/>
 * (The problem with c:url is that is uses the same encoding as the http
 * response, but most(?) servlet container assumes that ISO-8859-1 is used.)
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("serial")
public class UrlTag extends BodyTagSupport {

    private static final String DEFAULT_ENCODING = "Utf8Hex";

    private final List<Pair<String, String>> parameters;

    private String var;
    private String value;
    private String encoding = DEFAULT_ENCODING;

    public UrlTag() {
        super();
        parameters = new ArrayList<>();
    }

    @Override
    public int doStartTag() {
        parameters.clear();
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {

        // Rewrite and encode the url.
        String result = formatUrl();

        // Store or print the output
        if (var == null) {
            try {
                pageContext.getOut().print(result);
            } catch (IOException x) {
                throw new JspTagException(x);
            }
        } else {
            pageContext.setAttribute(var, result, PageContext.PAGE_SCOPE);
        }
        return EVAL_PAGE;
    }

    public static String resolveUrl(String url, String context, PageContext pageContext)
            throws JspException {
        // don't touch absolute URLs
        if (UrlUtils.isAbsoluteUrl(url)) {
            return url;
        }

        // normalize relative URLs against a context root
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        if (context == null) {
            if (url.startsWith("/")) {
                return request.getContextPath() + url;
            } else {
                return url;
            }
        } else {
            if (!context.startsWith("/") || !url.startsWith("/")) {
                throw new JspTagException("IMPORT_BAD_RELATIVE");
            }
            if (context.endsWith("/") && url.startsWith("/")) {
                // Don't produce string starting with '//', many
                // browsers interpret this as host name, not as
                // path on same host. Bug 22860
                // Also avoid // inside the url. Bug 34109
                return context.substring(0, context.length() - 1) + url;
            } else {
                return context + url;
            }
        }
    }

    String formatUrl() throws JspException {
        String baseUrl = resolveUrl(value, null, pageContext);

        StringBuilder result = new StringBuilder();
        result.append(baseUrl);
        if (parameters.isEmpty()) {
            return result.toString();
        }

        result.append('?');
        for (int i = 0; i < parameters.size(); i++) {
            Pair<String, String> parameter = parameters.get(i);
            result.append(parameter.getLeft());
            if (isUtf8Hex() && !isAsciiAlphaNumeric(parameter.getRight())) {
                result.append(ParameterDecodingFilter.PARAM_SUFFIX);
            }
            result.append('=');
            if (parameter.getRight() != null) {
                try {
                    result.append(encode(parameter.getRight()));
                } catch (UnsupportedEncodingException e) {
                    throw new JspTagException(e);
                }
            }
            if (i < parameters.size() - 1) {
                result.append('&');
            }
        }
        return result.toString();
    }

    private String encode(String s) throws UnsupportedEncodingException {
        if (isUtf8Hex()) {
            if (isAsciiAlphaNumeric(s)) {
                return s;
            }
            return StringUtilBase.utf8HexEncode(s);
        }
        return URLEncoder.encode(s, encoding);
    }

    private boolean isUtf8Hex() {
        return DEFAULT_ENCODING.equals(encoding);
    }

    private static boolean isAsciiAlphaNumeric(String s) {
        if (s == null) {
            return true;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!CharUtils.isAsciiAlphanumeric(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("PMD.NullAssignment") // (var, value) Intentional allocation to release
    @Override
    public void release() {
        var = null;
        value = null;
        encoding = DEFAULT_ENCODING;
        parameters.clear();
        super.release();
    }

    public void addParameter(String name, String value) {
        parameters.add(Pair.of(name, value));
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
