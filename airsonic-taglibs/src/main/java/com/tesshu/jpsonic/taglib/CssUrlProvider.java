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

import jakarta.servlet.jsp.JspContext;

/**
 * Interface for providing the CSS URL for the current request or user settings.
 *
 * Implementations are responsible for determining the correct CSS URL based on
 * the current user, request, or application settings.
 *
 * This interface is designed to be used by JSP custom tags such as CssUrlTag.
 */
public interface CssUrlProvider {

    /**
     * Returns the CSS URL for the current request or user settings.
     *
     * @param jspContext the current JSP context
     * @return the resolved CSS URL
     */
    String getCssUrl(JspContext jspContext);
}
