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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.util.PlayerUtils.OBJECT_MAPPER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.tesshu.jpsonic.domain.Version;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    private static final Logger LOG = LoggerFactory.getLogger(VersionService.class);
    private static final ThreadLocal<DateTimeFormatter> DATE_FORMAT = ThreadLocal
            .withInitial(() -> DateTimeFormatter.ofPattern("yyyyMMdd"));
    private static final Pattern VERSION_REGEX = Pattern.compile("^v(.*)");
    private static final String VERSION_URL = "https://api.github.com/repos/jpsonic/jpsonic/releases";

    /**
     * Only fetch last version this often (in milliseconds.).
     */
    private static final long LAST_VERSION_FETCH_INTERVAL = 7L * 24L * 3600L * 1000L; // One week

    private final Object latestLock = new Object();
    private final Object localVersionLock = new Object();
    private final Object localBuildDateLock = new Object();
    private final Object localBuildNumberLock = new Object();

    private Version latestFinalVersion;
    private Version latestBetaVersion;
    private Version localVersion;
    private LocalDate localBuildDate;
    private String localBuildNumber;

    /**
     * Time when latest version was fetched (in milliseconds).
     */
    private long lastVersionFetched;

    @PostConstruct
    public void init() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting Jpsonic " + getLocalVersion() + " (" + getLocalBuildNumber() + "), Java: "
                    + System.getProperty("java.version") + ", OS: " + System.getProperty("os.name"));
        }
    }

    /**
     * Returns the version number for the locally installed Jpsonic version.
     *
     * @return The version number for the locally installed Jpsonic version.
     */
    public Version getLocalVersion() {
        synchronized (localVersionLock) {
            if (localVersion == null) {
                localVersion = new Version(readLineFromResource("/version.txt"));
            }
            return localVersion;
        }
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
    public LocalDate getLocalBuildDate() {
        synchronized (localBuildDateLock) {
            if (localBuildDate == null) {
                try {
                    String date = readLineFromResource("/build_date.txt");
                    synchronized (DATE_FORMAT) {
                        localBuildDate = parseLocalBuildDate(date);
                    }
                } catch (DateTimeParseException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to resolve local Jpsonic build date.", e);
                    }
                }
            }
            return localBuildDate;
        }
    }

    @Nullable
    LocalDate parseLocalBuildDate(String date) {
        synchronized (DATE_FORMAT) {
            return LocalDate.parse(date, DATE_FORMAT.get());
        }
    }

    /**
     * Returns the build number for the locally installed Jpsonic version.
     *
     * @return The build number for the locally installed Jpsonic version, or <code>null</code> if the build number
     *         can't be resolved.
     */
    public String getLocalBuildNumber() {
        synchronized (localBuildNumberLock) {
            if (localBuildNumber == null) {
                localBuildNumber = readLineFromResource("/build_number.txt");
            }
            return localBuildNumber;
        }
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
    private void refreshLatestVersion() {
        long now = Instant.now().toEpochMilli();
        boolean isOutdated = now - lastVersionFetched > LAST_VERSION_FETCH_INTERVAL;

        if (isOutdated) {
            try {
                lastVersionFetched = now;
                readLatestVersion();
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to resolve latest Jpsonic version.", e);
                }
            }
        }
    }

    /**
     * Resolves the latest available Jpsonic version by inspecting github.
     */
    private void readLatestVersion() throws IOException {

        synchronized (latestLock) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Starting to read latest version");
            }
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(10))
                    .setResponseTimeout(Timeout.ofSeconds(10)).build();
            HttpGet method = new HttpGet(URI.create(VERSION_URL + "?v=" + getLocalVersion()));
            method.setConfig(requestConfig);
            String content;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(method)) {
                    content = response.toString();
                }
            } catch (ConnectTimeoutException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Got a timeout when trying to reach {}", VERSION_URL);
                }
                return;
            }

            List<String> unsortedTags = new ArrayList<>();
            for (JsonNode item : OBJECT_MAPPER.readTree(content)) {
                String tagName = item.path("tag_name").asText();
                if (!StringUtils.isEmpty(tagName)) {
                    unsortedTags.add(tagName);
                }
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
