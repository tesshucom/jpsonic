package com.tesshu.jpsonic.domain;

/**
 * A method of determining the default value of the preferred format used for transcoding.
 */
public enum PreferredFormatSheme {

    /**
     * For anonymous access, the preferred format defaults are applied uniformly. In other words, access from UPnP and
     * Share is targeted.
     */
    ANNOYMOUS,

    /**
     * For access other than access via API, the default value of the preferred format is applied uniformly. Access from
     * a browser is also included.
     */
    OTHER_THAN_REQUEST,

    /**
     * Exactly the same policy as the legacy server. Preferred format can be specified only by request parameter. In
     * other words, the priority format is applied only to access via API.
     */
    REQUEST_ONLY;

    public static PreferredFormatSheme of(String s) {
        if (REQUEST_ONLY.name().equals(s)) {
            return REQUEST_ONLY;
        } else if (OTHER_THAN_REQUEST.name().equals(s)) {
            return OTHER_THAN_REQUEST;
        }
        return ANNOYMOUS;
    }
}
