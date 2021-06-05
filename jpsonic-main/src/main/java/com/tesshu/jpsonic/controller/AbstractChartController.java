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

package com.tesshu.jpsonic.controller;

import java.awt.Color;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.context.Theme;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Abstract super class for controllers which generate charts.
 *
 * @author Sindre Mehus
 */
public abstract class AbstractChartController {

    private static final String KEY_BACKGROUND_COLOR = "backgroundColor";
    private static final String KEY_STROKE_COLOR = "strokeColor";
    private static final String KEY_TEXT_COLOR = "textColor";

    public abstract ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    /**
     * Returns the chart background color for the current theme.
     * 
     * @param request
     *            The servlet request.
     * 
     * @return The chart background color.
     */
    protected Color getBackground(HttpServletRequest request) {
        return getColor(KEY_BACKGROUND_COLOR, request);
    }

    /**
     * Returns the chart foreground color for the current theme.
     * 
     * @param request
     *            The servlet request.
     * 
     * @return The chart foreground color.
     */
    protected Color getForeground(HttpServletRequest request) {
        return getColor(KEY_TEXT_COLOR, request);
    }

    /**
     * Returns the chart stroke color for the current theme.
     * 
     * @param request
     *            The servlet request.
     * 
     * @return The chart stroke color.
     */
    protected Color getStroke(HttpServletRequest request) {
        return getColor(KEY_STROKE_COLOR, request);
    }

    private Color getColor(String key, HttpServletRequest request) {
        Theme theme = RequestContextUtils.getTheme(request);
        if (theme != null) {
            Locale locale = RequestContextUtils.getLocale(request);
            String colorHex = theme.getMessageSource().getMessage(key, new Object[0], locale);
            return new Color(Integer.parseInt(colorHex, 16));
        }
        return Color.RED;
    }
}
