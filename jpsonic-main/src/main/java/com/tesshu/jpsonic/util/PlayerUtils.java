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

package com.tesshu.jpsonic.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Miscellaneous general utility methods.
 *
 * @author Sindre Mehus
 */
public final class PlayerUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .findAndRegisterModules();

    private static final Logger LOG = LoggerFactory.getLogger(PlayerUtils.class);
    private static final String URL_SENSITIVE_REPLACEMENT_STRING = "<hidden>";

    /*
     * Represents a "far past timestamp". A flag value that may be used as an
     * initial value or when forcibly scanning. To avoid ArithmeticException,
     * Instant.MIN is not used. https://bugs.openjdk.org/browse/JDK-8169532
     */
    public static final Instant FAR_PAST = Instant.EPOCH;

    /*
     * Represents a "time stamp in the far future". May be used as a flag value if
     * parsing is not complete. Many databases use a maximum value of 9999–12–31
     * 23:59:59 (instead of Long.MAX). However HSQLDB doesn't seem to accept it.
     */
    public static final Instant FAR_FUTURE = ZonedDateTime
        .of(9999, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)
        .toInstant()
        .truncatedTo(ChronoUnit.MILLIS);

    /**
     * Disallow external instantiation.
     */
    private PlayerUtils() {
    }

    public static Instant now() {
        // Date precision uses milliseconds.
        // (hsqldb timestamp precision is milliseconds)
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String getDefaultFolder(String key, String winDefault, String linuxDefault) {
        String arg = System.getProperty(key);
        if (PathValidator.validateFolderPath(arg).isEmpty()) {
            return isWindows() ? winDefault : linuxDefault;
        }
        return arg;
    }

    public static String getDefaultMusicFolder() {
        return getDefaultFolder("jpsonic.defaultMusicFolder", "c:\\music", "/var/music");
    }

    public static String getDefaultPodcastFolder() {
        return getDefaultFolder("jpsonic.defaultPodcastFolder", "c:\\music\\Podcast",
                "/var/music/Podcast");
    }

    public static String getDefaultPlaylistFolder() {
        return getDefaultFolder("jpsonic.defaultPlaylistFolder", "c:\\playlists", "/var/playlists");
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    /**
     * Similar to {@link ServletResponse#setContentLength(int)}, but this method
     * supports lengths bigger than 2GB.
     * <p/>
     * See http://blogger.ziesemer.com/2008/03/suns-version-of-640k-2gb.html
     *
     * @param response The HTTP response.
     * @param length   The content length.
     */
    public static void setContentLength(HttpServletResponse response, long length) {
        if (length <= Integer.MAX_VALUE) {
            response.setContentLength((int) length);
        } else {
            response.setHeader("Content-Length", String.valueOf(length));
        }
    }

    public static List<Integer> toIntegerList(int... values) {
        List<Integer> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (int value : values) {
            result.add(value);
        }
        return result;
    }

    public static int[] toIntArray(List<Integer> values) {
        if (values == null) {
            return new int[0];
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public static String debugObject(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.warn("Cant output debug object", e);
            return "";
        }
    }

    /**
     * Return a complete URL for the given HTTP request, including the query string.
     *
     * @param request An HTTP request instance
     *
     * @return The associated URL
     */
    public static String getURLForRequest(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            url = new StringBuilder(url).append('?').append(queryString).toString();
        }
        return url;
    }

    /**
     * Return an URL for the given HTTP request, with anonymized sensitive
     * parameters.
     *
     * @param request An HTTP request instance
     *
     * @return The associated anonymized URL
     */
    public static String getAnonymizedURLForRequest(HttpServletRequest request) {

        String url = getURLForRequest(request);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        MultiValueMap<String, String> components = builder.build().getQueryParams();

        // Subsonic REST API authentication (see RESTRequestParameterProcessingFilter)
        if (components.containsKey("p")) {
            builder.replaceQueryParam("p", URL_SENSITIVE_REPLACEMENT_STRING); // Cleartext password
        }
        if (components.containsKey("t")) {
            builder.replaceQueryParam("t", URL_SENSITIVE_REPLACEMENT_STRING); // Token
        }
        if (components.containsKey("s")) {
            builder.replaceQueryParam("s", URL_SENSITIVE_REPLACEMENT_STRING); // Salt
        }
        if (components.containsKey("u")) {
            builder.replaceQueryParam("u", URL_SENSITIVE_REPLACEMENT_STRING); // Username
        }

        return builder.build().toUriString();
    }

    public static <T> T defaultIfNull(T object, T defaultValue) {
        return (object != null) ? object : defaultValue;
    }
}
