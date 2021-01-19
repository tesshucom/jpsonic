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
 * Hierarchical enumeration of Attributes used by the controller.
 */
/*
 * The immediate purpose is to port the legacy. It is useful for preventing
 * spelling mistakes, understanding the hierarchical structure, identifying
 * where to use it, and considering better management methods.
 */
public class Attributes {

    public enum Model {

        ERROR("error");

        public static final String VALUE = "model";// Used from annotation

        public enum Command {
            ID("id");

            public static final String VALUE = "command";// Used from annotation

            private String v;

            private Command(String value) {
                this.v = value;
            }

            public String value() {
                return v;
            }
        }

        private String v;

        private Model(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }

    public enum RequestParam {
        COUNT(NameConstants.COUNT),
        GENRE(NameConstants.GENRE),
        MUSIC_FOLDER_ID(NameConstants.MUSIC_FOLDER_ID),
        OFFSET(NameConstants.OFFSET),
        PASSWORD(NameConstants.PASSWORD),
        SIZE(NameConstants.SIZE),
        USERNAME(NameConstants.USERNAME);
        public class NameConstants { // Used from annotation
            public static final String COUNT = "count";
            public static final String GENRE = "genre";
            public static final String MUSIC_FOLDER_ID = "musicFolderId";
            public static final String OFFSET = "offset";
            public static final String PASSWORD = "password";
            public static final String SIZE = "size";
            public static final String USERNAME = "username";
            private NameConstants() {}
        }

        private String v;

        private RequestParam(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }
}