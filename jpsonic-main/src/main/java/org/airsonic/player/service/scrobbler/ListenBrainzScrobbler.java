/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service.scrobbler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.LegacyMap;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides services for "audioscrobbling" at listenbrainz.org.
 * <br/>
 * See https://listenbrainz.readthedocs.io/
 */
public class ListenBrainzScrobbler {

    private static final Logger LOG = LoggerFactory.getLogger(ListenBrainzScrobbler.class);
    private static final int MAX_PENDING_REGISTRATION = 2000;

    private RegistrationThread thread;
    private final LinkedBlockingQueue<RegistrationData> queue = new LinkedBlockingQueue<>();
    private final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(15000)
            .setSocketTimeout(15000)
            .build();

    /**
     * Registers the given media file at listenbrainz.org. This method returns immediately, the actual registration is done
     * by a separate thread.
     *
     * @param mediaFile  The media file to register.
     * @param token      The token to authentication user on ListenBrainz.
     * @param submission Whether this is a submission or a now playing notification.
     * @param time       Event time, or {@code null} to use current time.
     */
    public synchronized void register(MediaFile mediaFile, String token, boolean submission, Date time) {
        if (thread == null) {
            thread = new RegistrationThread();
            thread.start();
        }

        if (queue.size() >= MAX_PENDING_REGISTRATION) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("ListenBrainz scrobbler queue is full. Ignoring '" + mediaFile.getTitle() + "'");
            }
            return;
        }

        RegistrationData registrationData = createRegistrationData(mediaFile, token, submission, time);
        if (registrationData == null) {
            return;
        }

        try {
            queue.put(registrationData);
        } catch (InterruptedException x) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Interrupted while queuing ListenBrainz scrobble: " + x.toString());
            }
        }
    }

    private RegistrationData createRegistrationData(MediaFile mediaFile, String token, boolean submission, Date time) {
        RegistrationData reg = new RegistrationData();
        reg.token = token;
        reg.artist = mediaFile.getArtist();
        reg.album = mediaFile.getAlbumName();
        reg.title = mediaFile.getTitle();
        reg.musicBrainzReleaseId = mediaFile.getMusicBrainzReleaseId();
        reg.musicBrainzRecordingId = mediaFile.getMusicBrainzRecordingId();
        reg.trackNumber = mediaFile.getTrackNumber();
        reg.duration = mediaFile.getDurationSeconds() == null ? 0 : mediaFile.getDurationSeconds();
        reg.time = time == null ? new Date() : time;
        reg.submission = submission;

        return reg;
    }

    /**
     * Scrobbles the given song data at listenbrainz.org, using the protocol defined at https://listenbrainz.readthedocs.io/en/latest/dev/api.html.
     *
     * @param registrationData Registration data for the song.
     */
    private void scrobble(RegistrationData registrationData) throws ClientProtocolException, IOException {
        if (registrationData == null || registrationData.token == null) {
            return;
        }

        if (!submit(registrationData)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to scrobble song '" + registrationData.title + "' at ListenBrainz.");
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("Successfully registered " +
                        (registrationData.submission ? "submission" : "now playing") +
                         " for song '" + registrationData.title + "'" +
                         " at ListenBrainz: " + registrationData.time);
            }
        }
    }
    /**
     * Returns if submission succeeds.
     */
    private boolean submit(RegistrationData registrationData) throws ClientProtocolException, IOException {
        Map<String, Object> additionalInfo = LegacyMap.of();
        additionalInfo.computeIfAbsent("release_mbid", k -> registrationData.musicBrainzReleaseId);
        additionalInfo.computeIfAbsent("recording_mbid", k -> registrationData.musicBrainzRecordingId);
        additionalInfo.computeIfAbsent("tracknumber", k -> registrationData.trackNumber);

        Map<String, Object> trackMetadata = LegacyMap.of();
        if (additionalInfo.size() > 0) {
            trackMetadata.put("additional_info", additionalInfo);
        }
        trackMetadata.computeIfAbsent("artist_name", k -> registrationData.artist);
        trackMetadata.computeIfAbsent("track_name", k -> registrationData.title);
        trackMetadata.computeIfAbsent("release_name", k -> registrationData.album);

        Map<String, Object> payload = LegacyMap.of();
        if (trackMetadata.size() > 0) {
            payload.put("track_metadata", trackMetadata);
        }

        Map<String, Object> content = LegacyMap.of();

        if (registrationData.submission) {
            payload.put("listened_at", Long.valueOf(registrationData.time.getTime() / 1000L));
            content.put("listen_type", "single");
        } else {
            content.put("listen_type", "playing_now");
        }

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);
        content.put("payload", payloads);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(content);

        executeJsonPostRequest("https://api.listenbrainz.org/1/submit-listens", registrationData.token, json);

        return true;
    }

    private boolean executeJsonPostRequest(String url, String token, String json) throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(json, "UTF-8"));
        request.setHeader("Authorization", "token " + token);
        request.setHeader("Content-type", "application/json; charset=utf-8");

        executeRequest(request);
        return true;
    }

    private void executeRequest(HttpUriRequest request) throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        client.execute(request);
    }

    private class RegistrationThread extends Thread {
        private RegistrationThread() {
            super("ListenBrainzScrobbler Registration");
        }

        @Override
        public void run() {
            while (true) {
                RegistrationData registrationData = null;
                try {
                    registrationData = queue.take();
                    scrobble(registrationData);
                } catch (ClientProtocolException x) {
                } catch (IOException x) {
                    handleNetworkError(registrationData, x.toString());
                } catch (Exception x) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Error in ListenBrainz registration: " + x.toString());
                    }
                }
            }
        }

        private void handleNetworkError(RegistrationData registrationData, String errorMessage) {
            try {
                queue.put(registrationData);
                if (LOG.isInfoEnabled()) {
                    LOG.info("ListenBrainz registration for '" + registrationData.title +
                             "' encountered network error: " + errorMessage + ".  Will try again later. In queue: " + queue.size());
                }
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to reschedule ListenBrainz registration for '" + registrationData.title + "': " + x.toString());
                }
            }
            try {
                sleep(60L * 1000L);  // Wait 60 seconds.
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to sleep after ListenBrainz registration failure for '" + registrationData.title + "': " + x.toString());
                }
            }
        }
    }

    private static class RegistrationData {
        private String token;
        private String artist;
        private String album;
        private String title;
        private String musicBrainzReleaseId;
        private String musicBrainzRecordingId;
        private Integer trackNumber;
        private int duration;
        private Date time;
        public boolean submission;
    }

}
