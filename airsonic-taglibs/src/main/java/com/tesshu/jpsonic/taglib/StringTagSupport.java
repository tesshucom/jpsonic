/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.tesshu.jpsonic.taglib;

import java.io.IOException;
import java.io.StringWriter;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

/**
 * Abstract support class for the String Taglib. It handles the JSP taglib side
 * of things and calls abstract protected methods to delegate the String
 * functionality.
 * <dl>
 * <dt>var</dt>
 * <dd>PageContext variable to put the return result in instead of pushing out
 * to the html page.</dd>
 * </dl>
 *
 * @author bayard@generationjava.com
 */
@SuppressWarnings("serial")
public abstract class StringTagSupport extends BodyTagSupport {

    /**
     * PageContext attribute to store the result in.
     */
    private String var;

    /**
     * Empty constructor. Initialises the attributes.
     */
    public StringTagSupport() {
        initAttributes();
    }

    /**
     * Get the PageContext attribute to store the result in.
     */
    public String getVar() {
        return this.var;
    }

    /**
     * Set the PageContext attribute to store the result in.
     */
    public void setVar(String var) {
        this.var = var;
    }

    /**
     * Handles the manipulation of the String tag, evaluating the body of the tag.
     * The evaluation is delegated to the changeString(String) method
     */
    public int doEndTag() throws JspException {

        if ((bodyContent == null)) {
            return EVAL_PAGE;
        }
        String text = "";
        if (bodyContent != null) {
            StringWriter body = new StringWriter();
            try {
                bodyContent.writeOut(body);
                text = body.toString();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        text = changeString(text);

        if (this.var == null) {
            JspWriter writer = pageContext.getOut();
            try {
                writer.print(text);
            } catch (IOException e) {
                throw new JspException(e.toString());
            }
        } else {
            pageContext.setAttribute(this.var, text);
        }

        return (EVAL_PAGE);
    }

    /**
     * Perform an operation on the passed in String.
     *
     * @param str String to be manipulated
     *
     * @return String result of operation upon passed in String
     */
    public abstract String changeString(String str);

    /**
     * Initialise any properties to default values. This method is called upon
     * construction, and after changeString(String) is called. This is a default
     * empty implementation.
     */
    public void initAttributes() {
        this.var = null;
    }
}
