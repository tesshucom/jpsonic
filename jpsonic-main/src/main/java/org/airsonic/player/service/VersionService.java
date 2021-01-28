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

package org.airsonic.player.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.SuppressFBWarnings;
import org.airsonic.player.domain.Version;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides version-related services, including functionality for determining whether a newer version of Jpsonic is
 * available.
 *
 * @author Sindre Mehus
 */
@Service
public class VersionService {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final Logger LOG = LoggerFactory.getLogger(VersionService.class);

    private Version latestFinalVersion;
    private Version latestBetaVersion;
    private static final Object LATEST_LOCK = new Object();

    private Version localVersion;
    private static final Object LOCAL_VERSION_LOCK = new Object();

    private Date localBuildDate;
    private static final Object LOCAL_BUILD_DATE = new Object();

    private String localBuildNumber;
    private static final Object LOCAL_BUILD_NUMBER_LOCK = new Object();

    /**
     * Time when latest version was fetched (in milliseconds).
     */
    private long lastVersionFetched;

    /**
     * Only fetch last version this often (in milliseconds.).
     */
    private static final long LAST_VERSION_FETCH_INTERVAL = 7L * 24L * 3600L * 1000L; // One week

    /**
     * Returns the version number for the locally installed Jpsonic version.
     *
     * @return The version number for the locally installed Jpsonic version.
     */
    public Version getLocalVersion() {
        synchronized (LOCAL_VERSION_LOCK) {
            if (localVersion == null) {
                try {
                    localVersion = new Version(readLineFromResource("/version.txt"));
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Resolved local Jpsonic version to: " + localVersion);
                    }
                } catch (Exception x) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to resolve local Jpsonic version.", x);
                    }
                }
            }
        }
        return localVersion;
    }

    /**
     * Returns the version number for the latest available Jpsonic final version.
     *
     * @return The version number for the latest available Jpsonic final version, or <code>null</code> if the version
     *         number can't be resolved.
     */
    public Version getLatestFinalVersion() {
        refreshLatestVersion();
        return latestFinalVersion;
    }

    /**
     * Returns the version number for the latest available Jpsonic beta version.
     *
     * @return The version number for the latest available Jpsonic beta version, or <code>null</code> if the version
     *         number can't be resolved.
     */
    public Version getLatestBetaVersion() {
        refreshLatestVersion();
        return latestBetaVersion;
    }

    /**
     * Returns the build date for the locally installed Jpsonic version.
     *
     * @return The build date for the locally installed Jpsonic version, or <code>null</code> if the build date can't be
     *         resolved.
     */
    public Date getLocalBuildDate() {
        synchronized (LOCAL_BUILD_DATE) {
            if (localBuildDate == null) {
                try {
                    String date = readLineFromResource("/build_date.txt");
                    synchronized (DATE_FORMAT) {
                        localBuildDate = DATE_FORMAT.parse(date);
                    }
                } catch (Exception x) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to resolve local Jpsonic build date.", x);
                    }
                }
            }
        }
        return localBuildDate;
    }

    /**
     * Returns the build number for the locally installed Jpsonic version.
     *
     * @return The build number for the locally installed Jpsonic version, or <code>null</code> if the build number
     *         can't be resolved.
     */
    public String getLocalBuildNumber() {
        synchronized (LOCAL_BUILD_NUMBER_LOCK) {
            if (localBuildNumber == null) {
                try {
                    localBuildNumber = readLineFromResource("/build_number.txt");
                } catch (Exception x) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to resolve local Jpsonic build number.", x);
                    }
                }
            }
        }
        return localBuildNumber;
    }

    /**
     * Returns whether a new final version of Jpsonic is available.
     *
     * @return Whether a new final version of Jpsonic is available.
     */
    public boolean isNewFinalVersionAvailable() {
        Version latest = getLatestFinalVersion();
        Version local = getLocalVersion();

        if (latest == null || local == null) {
            return false;
        }

        return local.compareTo(latest) < 0;
    }

    /**
     * Returns whether a new beta version of Jpsonic is available.
     *
     * @return Whether a new beta version of Jpsonic is available.
     */
    public boolean isNewBetaVersionAvailable() {
        Version latest = getLatestBetaVersion();
        Version local = getLocalVersion();

        if (latest == null || local == null) {
            return false;
        }

        return local.compareTo(latest) < 0;
    }

    /**
     * Reads the first line from the resource with the given name.
     *
     * @param resourceName
     *            The resource name.
     * 
     * @return The first line of the resource.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private String readLineFromResource(@NonNull String resourceName) {
        try (InputStream in = VersionService.class.getResourceAsStream(resourceName)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.readLine();
            } catch (IOException x) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Refreshes the latest final and beta versions.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private void refreshLatestVersion() {
        long now = System.currentTimeMillis();
        boolean isOutdated = now - lastVersionFetched > LAST_VERSION_FETCH_INTERVAL;

        if (isOutdated) {
            try {
                lastVersionFetched = now;
                readLatestVersion();
            } catch (Exception x) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve latest Jpsonic version.", x);
                }
            }
        }
    }

    private static final Pattern VERSION_REGEX = Pattern.compile("^v(.*)");
    private static final String VERSION_URL = "https://api.github.com/repos/jpsonic/jpsonic/releases";

    /**
     * Resolves the latest available Jpsonic version by inspecting github.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private void readLatestVersion() throws IOException {

        synchronized (LATEST_LOCK) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Starting to read latest version");
            }
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10_000).setSocketTimeout(10_000)
                    .build();
            HttpGet method = new HttpGet(VERSION_URL + "?v=" + getLocalVersion());
            method.setConfig(requestConfig);
            String content;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = client.execute(method, responseHandler);
            } catch (ConnectTimeoutException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Got a timeout when trying to reach {}", VERSION_URL);
                }
                return;
            }

            List<String> unsortedTags = new LinkedList<>();
            for (JsonNode item : new ObjectMapper().readTree(content)) {
                unsortedTags.add(item.path("tag_name").asText());
            }

            Function<String, Version> convertToVersion = s -> {
                Matcher match = VERSION_REGEX.matcher(s);
                if (!match.matches()) {
                    throw new IllegalArgumentException("Unexpected tag format " + s);
                }
                return new Version(match.group(1));
            };

            Predicate<Version> finalVersionPredicate = version -> !version.isPreview();

            Optional<Version> betaV = unsortedTags.stream().map(convertToVersion).max(Comparator.naturalOrder());
            Optional<Version> finalV = unsortedTags.stream().map(convertToVersion).sorted(Comparator.reverseOrder())
                    .filter(finalVersionPredicate).findFirst();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got {} for beta version", betaV);
                LOG.debug("Got {} for final version", finalV);
            }

            latestBetaVersion = betaV.get();
            latestFinalVersion = finalV.get();
        }
    }
}
