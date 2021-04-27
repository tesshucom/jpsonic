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

package org.airsonic.player.service.scrobbler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.SuppressFBWarnings;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.LegacyMap;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides services for "audioscrobbling" at listenbrainz.org. <br/>
 * See https://listenbrainz.readthedocs.io/
 */
public class ListenBrainzScrobbler {

    private static final Logger LOG = LoggerFactory.getLogger(ListenBrainzScrobbler.class);
    private static final int MAX_PENDING_REGISTRATION = 2000;
    private static final Object REGISTRATION_LOCK = new Object();

    private final LinkedBlockingQueue<RegistrationData> queue;

    private RegistrationTask task;

    // private final RequestConfig requestConfig = RequestConfig.custom()
    // .setConnectTimeout(15000)
    // .setSocketTimeout(15000)
    // .build();

    public ListenBrainzScrobbler() {
        queue = new LinkedBlockingQueue<>();
    }

    /**
     * Registers the given media file at listenbrainz.org. This method returns immediately, the actual registration is
     * done by a separate task.
     *
     * @param mediaFile
     *            The media file to register.
     * @param token
     *            The token to authentication user on ListenBrainz.
     * @param submission
     *            Whether this is a submission or a now playing notification.
     * @param time
     *            Event time, or {@code null} to use current time.
     */
    public void register(MediaFile mediaFile, String token, boolean submission, Date time, Executor executor) {

        synchronized (REGISTRATION_LOCK) {

            if (task == null) {
                task = new RegistrationTask(queue);
                executor.execute(task);
            }

            if (queue.size() >= MAX_PENDING_REGISTRATION) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("ListenBrainz scrobbler queue is full. Ignoring '" + mediaFile.getTitle() + "'");
                }
                return;
            }

            RegistrationData registrationData = new RegistrationData(mediaFile, token, submission, time);
            try {
                queue.put(registrationData);
            } catch (InterruptedException x) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Interrupted while queuing ListenBrainz scrobble: " + x.toString());
                }
            }
        }

    }

    /**
     * Scrobbles the given song data at listenbrainz.org, using the protocol defined at
     * https://listenbrainz.readthedocs.io/en/latest/dev/api.html.
     *
     * @param registrationData
     *            Registration data for the song.
     */
    protected static void scrobble(RegistrationData registrationData) throws ClientProtocolException, IOException {
        if (registrationData == null || registrationData.getToken() == null) {
            return;
        }

        if (submit(registrationData)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Successfully registered " + (registrationData.isSubmission() ? "submission" : "now playing")
                        + " for song '" + registrationData.getTitle() + "'" + " at ListenBrainz: "
                        + registrationData.getTime());
            }
        } else {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to scrobble song '" + registrationData.getTitle() + "' at ListenBrainz.");
            }
        }
    }

    /**
     * Returns if submission succeeds.
     */
    private static boolean submit(RegistrationData registrationData) throws ClientProtocolException, IOException {
        Map<String, Object> additionalInfo = LegacyMap.of();
        additionalInfo.computeIfAbsent("release_mbid", k -> registrationData.getMusicBrainzReleaseId());
        additionalInfo.computeIfAbsent("recording_mbid", k -> registrationData.getMusicBrainzRecordingId());
        additionalInfo.computeIfAbsent("tracknumber", k -> registrationData.getTrackNumber());

        Map<String, Object> trackMetadata = LegacyMap.of();
        if (additionalInfo.size() > 0) {
            trackMetadata.put("additional_info", additionalInfo);
        }
        trackMetadata.computeIfAbsent("artist_name", k -> registrationData.getArtist());
        trackMetadata.computeIfAbsent("track_name", k -> registrationData.getTitle());
        trackMetadata.computeIfAbsent("release_name", k -> registrationData.getAlbum());

        Map<String, Object> payload = LegacyMap.of();
        if (trackMetadata.size() > 0) {
            payload.put("track_metadata", trackMetadata);
        }

        Map<String, Object> content = LegacyMap.of();

        if (registrationData.submission) {
            payload.put("listened_at", registrationData.getTime().getTime() / 1000L);
            content.put("listen_type", "single");
        } else {
            content.put("listen_type", "playing_now");
        }

        List<Map<String, Object>> payloads = new ArrayList<>();
        payloads.add(payload);
        content.put("payload", payloads);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(content);

        executeJsonPostRequest("https://api.listenbrainz.org/1/submit-listens", registrationData.getToken(), json);

        return true;
    }

    private static boolean executeJsonPostRequest(String url, String token, String json)
            throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(json, "UTF-8"));
        request.setHeader("Authorization", "token " + token);
        request.setHeader("Content-type", "application/json; charset=utf-8");

        executeRequest(request);
        return true;
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private static void executeRequest(HttpUriRequest request) throws ClientProtocolException, IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            client.execute(request);
        }
    }

    /*
     * httpClient can be reused #833
     */
    private static class RegistrationTask implements Runnable {

        private static final Logger LOG = LoggerFactory.getLogger(ListenBrainzScrobbler.class);

        private final LinkedBlockingQueue<RegistrationData> queue;

        RegistrationTask(LinkedBlockingQueue<RegistrationData> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                RegistrationData registrationData = null;
                try {
                    registrationData = queue.take();
                    scrobble(registrationData);
                } catch (ClientProtocolException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Error in ListenBrainz registration.", e);
                    }
                    break;
                } catch (IOException e) {
                    handleNetworkError(registrationData, e.toString());
                    break;
                } catch (Exception e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Error in ListenBrainz registration.", e);
                    }
                    break;
                }
            }
        }

        private void handleNetworkError(RegistrationData registrationData, String errorMessage) {
            try {
                queue.put(registrationData);
                if (LOG.isInfoEnabled()) {
                    LOG.info("ListenBrainz registration for '" + registrationData.getTitle()
                            + "' encountered network error: " + errorMessage + ".  Will try again later. In queue: "
                            + queue.size());
                }
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to reschedule ListenBrainz registration for '" + registrationData.getTitle()
                            + "': " + x.toString());
                }
            }
            try {
                Thread.sleep(60L * 1000L); // Wait 60 seconds.
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to sleep after ListenBrainz registration failure for '"
                            + registrationData.getTitle() + "': " + x.toString());
                }
            }
        }
    }

    private static class RegistrationData {
        private final String token;
        private final String artist;
        private final String album;
        private final String title;
        private final String musicBrainzReleaseId;
        private final String musicBrainzRecordingId;
        private final Integer trackNumber;
        // private int duration;
        private final Date time;
        public boolean submission;

        public RegistrationData(MediaFile mediaFile, String token, boolean submission, Date time) {
            super();
            this.token = token;
            this.artist = mediaFile.getArtist();
            this.album = mediaFile.getAlbumName();
            this.title = mediaFile.getTitle();
            this.musicBrainzReleaseId = mediaFile.getMusicBrainzReleaseId();
            this.musicBrainzRecordingId = mediaFile.getMusicBrainzRecordingId();
            this.trackNumber = mediaFile.getTrackNumber();
            // reg.duration = mediaFile.getDurationSeconds() == null ? 0 : mediaFile.getDurationSeconds();
            this.time = time == null ? new Date() : time;
            this.submission = submission;
        }

        public String getTitle() {
            return title;
        }

        public String getToken() {
            return token;
        }

        public Date getTime() {
            return time;
        }

        public boolean isSubmission() {
            return submission;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public String getMusicBrainzReleaseId() {
            return musicBrainzReleaseId;
        }

        public String getMusicBrainzRecordingId() {
            return musicBrainzRecordingId;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }
    }
}
