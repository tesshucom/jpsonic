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
// Copied from GenerationJava Core Library.
//package com.generationjava.lang;

package com.tesshu.jpsonic.taglib.util;

import org.apache.commons.lang3.StringUtils;

/**
 * A set of String library static methods. While extending String or StringBuffer would have been the nicest solution,
 * that is not possible, so a simple set of static methods seems the most workable.
 *
 * Method ideas have so far been taken from the PHP4, Ruby and .NET languages.
 *
 * @author bayard@generationjava.com
 *
 * @version 0.4 20010812
 */
public final class StringW {

    /**
     * Quote a string so that it may be used in a regular expression without any parts of the string being considered as
     * a part of the regular expression's control characters.
     */
    public static String quoteRegularExpression(String str) {
        // replace ? + * / . ^ $ as long as they're not in character
        // class. so must be done by hand
        char[] chrs = str.toCharArray();
        int sz = chrs.length;
        StringBuffer buffer = new StringBuffer(2 * sz);
        for (int i = 0; i < sz; i++) {
            switch (chrs[i]) {
            case '[':
            case ']':
            case '?':
            case '+':
            case '*':
            case '/':
            case '.':
            case '^':
            case '$':
                buffer.append("\\");
                break;
            default:
                buffer.append(chrs[i]);
            }
        }
        return buffer.toString();
    }

    /**
     * Create a word-wrapped version of a String. Wrap at 80 characters and use newlines as the delimiter. If a word is
     * over 80 characters long use a - sign to split it.
     */
    public static String wordWrap(String str) {
        return wordWrap(str, 80, "\n", "-");
    }

    /**
     * Create a word-wrapped version of a String. Wrap at a specified width and use newlines as the delimiter. If a word
     * is over the width in lenght use a - sign to split it.
     */
    public static String wordWrap(String str, int width) {
        return wordWrap(str, width, "\n", "-");
    }

    /**
     * Word-wrap a string.
     *
     * @param str
     *            String to word-wrap
     * @param width
     *            int to wrap at
     * @param delim
     *            String to use to separate lines
     * @param split
     *            String to use to split a word greater than width long
     *
     * @return String that has been word wrapped
     */
    public static String wordWrap(String str, int width, String delim, String split) {
        int sz = str.length();

        /// shift width up one. mainly as it makes the logic easier
        width++;

        // our best guess as to an initial size
        StringBuffer buffer = new StringBuffer(sz / width * delim.length() + sz);

        // every line will include a delim on the end
        width = width - delim.length();

        int idx = -1;
        String substr = null;

        // beware: i is rolled-back inside the loop
        for (int i = 0; i < sz; i += width) {

            // on the last line
            if (i > sz - width) {
                buffer.append(str.substring(i));
                break;
            }

            // the current line
            substr = str.substring(i, i + width);

            // is the delim already on the line
            idx = substr.indexOf(delim);
            if (idx != -1) {
                buffer.append(substr.substring(0, idx));
                buffer.append(delim);
                i -= width - idx - delim.length();

                // Erase a space after a delim. Is this too obscure?
                if (substr.length() > idx + 1) {
                    if (substr.charAt(idx + 1) != '\n') {
                        if (Character.isWhitespace(substr.charAt(idx + 1))) {
                            i++;
                        }
                    }
                }
                // System.err.println("i -= "+width+"-"+idx);
                continue;
            }

            idx = -1;

            // figure out where the last space is
            char[] chrs = substr.toCharArray();
            for (int j = width; j > 0; j--) {
                if (Character.isWhitespace(chrs[j - 1])) {
                    idx = j;
                    break;
                }
            }

            // idx is the last whitespace on the line.
            // System.err.println("idx is "+idx);
            if (idx == -1) {
                for (int j = width; j > 0; j--) {
                    if (chrs[j - 1] == '-') {
                        idx = j;
                        break;
                    }
                }
                if (idx == -1) {
                    buffer.append(substr);
                    buffer.append(delim);
                } else {
                    if (idx != width) {
                        idx++;
                    }
                    buffer.append(substr.substring(0, idx));
                    buffer.append(delim);
                    i -= width - idx;
                }
            } else {
                // insert spaces
                buffer.append(substr.substring(0, idx));
                buffer.append(StringUtils.repeat(" ", width - idx));
                buffer.append(delim);
                i -= width - idx;
                // }
            }
        }
        return buffer.toString();
    }

    /**
     * Truncates a string nicely. It will search for the first space after the lower limit and truncate the string
     * there. It will also append any string passed as a parameter to the end of the string. The hard limit can be
     * specified to forcibily truncate a string (in the case of an extremely long word or such). All HTML/XML markup
     * will be stripped from the string prior to processing for truncation.
     *
     * @param str
     *            String the string to be truncated.
     * @param lower
     *            int value of the lower limit.
     * @param upper
     *            int value of the upper limit, -1 if no limit is desired. If the uppper limit is lower than the lower
     *            limit, it will be adjusted to be same as the lower limit.
     * @param appendToEnd
     *            String to be appended to the end of the truncated string. This is appended ONLY if the string was
     *            indeed truncated. The append is does not count towards any lower/upper limits.
     *
     * @author timster@mac.com
     */
    public static String truncateNicely(String str, int lower, int upper, String appendToEnd) {
        // strip markup from the string
        str = XmlW.removeXml(str);

        // quickly adjust the upper if it is set lower than 'lower'
        if (upper < lower) {
            upper = lower;
        }

        // now determine if the string fits within the upper limit
        // if it does, go straight to return, do not pass 'go' and collect $200
        if (str.length() > upper) {
            // the magic location int
            int loc;

            // first we determine where the next space appears after lower
            loc = str.lastIndexOf(' ', upper);

            // now we'll see if the location is greater than the lower limit
            if (loc >= lower) {
                // yes it was, so we'll cut it off here
                str = str.substring(0, loc);
            } else {
                // no it wasnt, so we'll cut it off at the upper limit
                str = str.substring(0, upper);
            }

            // the string was truncated, so we append the appendToEnd String
            str = str + appendToEnd;
        }

        return str;
    }
}