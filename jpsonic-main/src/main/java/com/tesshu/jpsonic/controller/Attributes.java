package com.tesshu.jpsonic.controller;

/*
 * Naming conventions of this class are exceptional so that we can code in property styles.
 */
@SuppressWarnings({ "PMD.ClassNamingConventions", "PMD.FieldNamingConventions" })
public class Attributes {

    public class model {
        public class command {
            public class keys {
            }

            public static final String name = "command";
        }

        public class keys {
            public static final String error = "error";

            private keys() {
            }
        }

        public static final String name = "model";

        private model() {
        }

    }

    public class requestParam {
        public class names {
            public static final String count = "count";
            public static final String genre = "genre";
            public static final String musicFolderId = "musicFolderId";
            public static final String offset = "offset";
            public static final String password = "password";
            public static final String size = "size";
            public static final String username = "username";

            private names() {
            }
        }
    }

    public class view {
        public class names {
            public static final String podcastChannels = "podcastChannels.view";

            private names() {
            }
        }
    }
}