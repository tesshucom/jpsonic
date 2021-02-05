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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.domain;

/**
 * Enumeration of font specification method used on web pages.
 */
public enum FontScheme {

    /**
     * A method that uses fonts that are commonly used in Japan and can display a mixture of Japanese and English
     * characters.
     */
    DEFAULT,

    /**
     * A method that uses the server's built-in Japanese fonts in addition to the default fonts. It is intended to
     * support users who watch Japanese music on clients that do not have Japanese fonts.
     */
    JP_EMBED,

    /**
     * A method in which the user specifies an arbitrary font.
     */
    CUSTOM

}
