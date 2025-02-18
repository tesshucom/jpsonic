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

import com.tesshu.jpsonic.taglib.util.StringW;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * A more intelligent substring. It attempts to cut off a string after a space, following predefined or user-supplied
 * lower and upper limits, useful for making short descriptions from long text. Can also strip HTML, or if not,
 * intelligently close any tags that were left open. It adds on a user-defined ending.
 * <dl>
 * <dt>lower</dt>
 * <dd>Minimum length to truncate at. Required.</dd>
 * <dt>upper</dt>
 * <dd>Maximum length to truncate at. Required.</dd>
 * <dt>upper</dt>
 * <dd>String to append to end of truncated string.</dd>
 * </dl>
 *
 * @author timster@mac.com
 */
@SuppressWarnings("serial")
public class TruncateNicelyTag extends StringTagSupport {

    private String lower;
    private String upper;
    private String appendToEnd;

    public TruncateNicelyTag() {
        super();
    }

    /**
     * Get the lower property
     *
     * @return String lower property
     */
    public String getLower() {
        return this.lower;
    }

    /**
     * Set the upper property
     *
     * @param lower
     *            String property
     */
    public void setLower(String l) {
        this.lower = l;
    }

    /**
     * Get the upper property
     *
     * @return String upper property
     */
    public String getUpper() {
        return this.upper;
    }

    /**
     * Set the upper property
     *
     * @param upper
     *            String property
     */
    public void setUpper(String u) {
        this.upper = u;
    }

    public String getAppendToEnd() {
        return this.appendToEnd;
    }

    public void setAppendToEnd(String s) {
        this.appendToEnd = s;
    }

    public String changeString(String text) {
        int l = NumberUtils.toInt(lower);
        int u = NumberUtils.toInt(upper);
        return StringW.truncateNicely(text, l, u, this.appendToEnd);
    }

    public void initAttributes() {
        this.lower = "10";
        this.upper = "-1";
        this.appendToEnd = "...";
    }
}
