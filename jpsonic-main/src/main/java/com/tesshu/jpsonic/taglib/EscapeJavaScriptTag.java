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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Escapes the characters in a <code>String</code> using JavaScript String rules.
 * <p/>
 * Escapes any values it finds into their JavaScript String form. Deals correctly with quotes and control-chars (tab,
 * backslash, cr, ff, etc.)
 * <p/>
 * So a tab becomes the characters <code>'\\'</code> and <code>'t'</code>.
 * <p/>
 * The only difference between Java strings and JavaScript strings is that in JavaScript, a single quote must be
 * escaped.
 * <p/>
 * Example:
 *
 * <pre>
 * input string: He didn't say, "Stop!"
 * output string: He didn\'t say, \"Stop!\"
 * </pre>
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("serial")
public class EscapeJavaScriptTag extends BodyTagSupport {

    private String string;

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().print(StringEscapeUtils.escapeJavaScript(string));
        } catch (IOException x) {
            throw new JspTagException(x);
        }
        return EVAL_PAGE;
    }

    @SuppressWarnings("PMD.NullAssignment") // (string) Intentional allocation to release
    @Override
    public void release() {
        string = null;
        super.release();
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
