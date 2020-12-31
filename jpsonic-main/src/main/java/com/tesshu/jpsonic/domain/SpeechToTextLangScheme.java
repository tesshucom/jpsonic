/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */
package com.tesshu.jpsonic.domain;

/**
 * Enumeration of language methods of speech recognition engine used on web pages.
 */
public enum SpeechToTextLangScheme {

    /**
     * Default.
     * Same as the language specified in your personal settings.
     */
    DEFAULT,

    /**
     * Unspecified explicitly.
     * (For the browser's Javascript speech recognition
     * engine, unspecified means the browser agent's language.)
     */
    //UNSPECIFIED,

    /**
     * Explicit specification by code.
     * https://cloud.google.com/speech-to-text/docs/languages
     */
    BCP47

}
