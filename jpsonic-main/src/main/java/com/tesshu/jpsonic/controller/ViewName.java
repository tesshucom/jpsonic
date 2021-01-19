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

 Copyright 2021 (C) tesshu.com
 */
package com.tesshu.jpsonic.controller;

/**
 * ViewName enumeration.
 */
/*
 * @see #826. Lists the names that currently use "view" in the suffix. (That is,
 * the url where the view is processed by pattern matching.) If we want to
 * eliminate pattern matching, we need to change to a new mapping implementation
 * that meets the current matching specifications.
 */
public enum ViewName {

    PODCAST_CHANNELS(ViewNameConstants.PODCAST_CHANNELS);

    public final class ViewNameConstants {

        public static final String PODCAST_CHANNELS = "podcastChannels.view";

        private ViewNameConstants() {
        }
    }

    private String v;

    private ViewName(String value) {
        this.v = value;
    }

    public String value() {
        return v;
    }
}
