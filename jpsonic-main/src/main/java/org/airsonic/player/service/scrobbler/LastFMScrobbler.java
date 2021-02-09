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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.tesshu.jpsonic.SuppressFBWarnings;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides services for "audioscrobbling" at www.last.fm. <br/>
 * See https://www.last.fm/api/submissions
 */
public class LastFMScrobbler {

    private static final Logger LOG = LoggerFactory.getLogger(LastFMScrobbler.class);
    private static final int MAX_PENDING_REGISTRATION = 2000;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(15_000)
            .setSocketTimeout(15_000).build();
    private static final Object REGISTRATION_LOCK = new Object();
    private static final String MSG_PREF_ON_FAIL = "Failed to scrobble song '";

    private final LinkedBlockingQueue<RegistrationData> queue;

    private RegistrationThread thread;

    public LastFMScrobbler() {
        queue = new LinkedBlockingQueue<>();
    }

    /**
     * Registers the given media file at www.last.fm. This method returns immediately, the actual registration is done
     * by a separate thread.
     *
     * @param mediaFile
     *            The media file to register.
     * @param username
     *            last.fm username.
     * @param password
     *            last.fm password.
     * @param submission
     *            Whether this is a submission or a now playing notification.
     * @param time
     *            Event time, or {@code null} to use current time.
     */
    public void register(MediaFile mediaFile, String username, String password, boolean submission, Date time) {

        synchronized (REGISTRATION_LOCK) {

            if (thread == null) {
                thread = new RegistrationThread(queue);
                thread.start();
            }

            if (queue.size() >= MAX_PENDING_REGISTRATION) {
                writeWarn("Last.fm scrobbler queue is full. Ignoring " + mediaFile);
                return;
            }

            RegistrationData registrationData = new RegistrationData(mediaFile, username, password, submission, time);
            try {
                queue.put(registrationData);
            } catch (InterruptedException x) {
                writeWarn("Interrupted while queuing Last.fm scrobble: " + x.toString());
            }
        }
    }

    /**
     * Scrobbles the given song data at last.fm, using the protocol defined at http://www.last.fm/api/submissions.
     *
     * @param registrationData
     *            Registration data for the song.
     */
    protected static final void scrobble(RegistrationData registrationData)
            throws URISyntaxException, ClientProtocolException, IOException {
        if (registrationData == null) {
            return;
        }

        String[] lines = authenticate(registrationData);
        if (lines == null) {
            return;
        }

        String sessionId = lines[1];
        String nowPlayingUrl = lines[2];
        String submissionUrl = lines[3];

        if (registrationData.isSubmission()) {
            lines = registerSubmission(registrationData, sessionId, submissionUrl);
        } else {
            lines = registerNowPlaying(registrationData, sessionId, nowPlayingUrl);
        }

        if (lines[0].startsWith("FAILED")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm: " + lines[0]);
        } else if (lines[0].startsWith("BADSESSION")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm.  Invalid session.");
        } else if (LOG.isInfoEnabled() && lines[0].startsWith("OK")) {
            LOG.info("Successfully registered " + (registrationData.isSubmission() ? "submission" : "now playing")
                    + " for song '" + registrationData.getTitle() + "' for user " + registrationData.getUsername()
                    + " at Last.fm: " + registrationData.getTime());
        }
    }

    /**
     * Returns the following lines if authentication succeeds:
     * <p/>
     * Line 0: Always "OK" Line 1: Session ID, e.g., "17E61E13454CDD8B68E8D7DEEEDF6170" Line 2: URL to use for now
     * playing, e.g., "http://post.audioscrobbler.com:80/np_1.2" Line 3: URL to use for submissions, e.g.,
     * "http://post2.audioscrobbler.com:80/protocol_1.2"
     * <p/>
     * If authentication fails, <code>null</code> is returned.
     */
    private static String[] authenticate(RegistrationData registrationData)
            throws URISyntaxException, ClientProtocolException, IOException {
        String clientId = "sub";
        String clientVersion = "0.1";
        long timestamp = System.currentTimeMillis() / 1000L;
        String authToken = calculateAuthenticationToken(registrationData.getPassword(), timestamp);
        // NOTE: HTTPS support DOES NOT WORK on the AudioScrobbler v1 API.
        URI uri = new URI("http", /* userInfo= */ null, "post.audioscrobbler.com", -1, "/",
                String.format("hs=true&p=1.2.1&c=%s&v=%s&u=%s&t=%s&a=%s", clientId, clientVersion,
                        registrationData.getUsername(), timestamp, authToken),
                /* fragment= */ null);

        String[] lines = executeGetRequest(uri);

        if (lines[0].startsWith("BANNED")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm. Client version is banned.");
            return null;
        }

        if (lines[0].startsWith("BADAUTH")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm. Wrong username or password.");
            return null;
        }

        if (lines[0].startsWith("BADTIME")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle()
                    + "' at Last.fm. Bad timestamp, please check local clock.");
            return null;
        }

        if (lines[0].startsWith("FAILED")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm: " + lines[0]);
            return null;
        }

        if (!lines[0].startsWith("OK")) {
            writeWarn(MSG_PREF_ON_FAIL + registrationData.getTitle() + "' at Last.fm.  Unknown response: " + lines[0]);
            return null;
        }

        return lines;
    }

    protected static void writeWarn(String msg) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(msg);
        }
    }

    private static String[] registerSubmission(RegistrationData registrationData, String sessionId, String url)
            throws UnsupportedEncodingException, ClientProtocolException, IOException {
        Map<String, String> params = LegacyMap.of();
        params.put("s", sessionId);
        params.put("a[0]", registrationData.getArtist());
        params.put("t[0]", registrationData.getTitle());
        params.put("i[0]", String.valueOf(registrationData.getTime().getTime() / 1000L));
        params.put("o[0]", "P");
        params.put("r[0]", "");
        params.put("l[0]", String.valueOf(registrationData.getDuration()));
        params.put("b[0]", registrationData.getAlbum());
        params.put("n[0]", "");
        params.put("m[0]", "");
        return executePostRequest(url, params);
    }

    private static String[] registerNowPlaying(RegistrationData registrationData, String sessionId, String url)
            throws UnsupportedEncodingException, ClientProtocolException, IOException {
        Map<String, String> params = LegacyMap.of();
        params.put("s", sessionId);
        params.put("a", registrationData.getArtist());
        params.put("t", registrationData.getTitle());
        params.put("b", registrationData.getAlbum());
        params.put("l", String.valueOf(registrationData.getDuration()));
        params.put("n", "");
        params.put("m", "");
        return executePostRequest(url, params);
    }

    private static String calculateAuthenticationToken(String password, long timestamp) {
        return DigestUtils.md5Hex(DigestUtils.md5Hex(password) + timestamp);
    }

    private static String[] executeGetRequest(URI url) throws IOException, ClientProtocolException {
        HttpGet method = new HttpGet(url);
        method.setConfig(REQUEST_CONFIG);
        return executeRequest(method);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (BasicNameValuePair) Not reusable
    private static String[] executePostRequest(String url, Map<String, String> parameters)
            throws UnsupportedEncodingException, ClientProtocolException, IOException {
        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(params, StringUtil.ENCODING_UTF8));
        request.setConfig(REQUEST_CONFIG);
        return executeRequest(request);
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private static String[] executeRequest(HttpUriRequest request) throws ClientProtocolException, IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = client.execute(request, responseHandler);
            return response.split("\\r?\\n");
        }
    }

    /*
     * httpClient can be reused #833
     */
    private static class RegistrationThread extends Thread {

        private final LinkedBlockingQueue<RegistrationData> queue;

        private static final Logger LOG = LoggerFactory.getLogger(RegistrationThread.class);

        RegistrationThread(LinkedBlockingQueue<RegistrationData> queue) {
            super("LastFMScrobbler Registration");
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                RegistrationData registrationData = null;
                try {
                    registrationData = queue.take();
                    scrobble(registrationData);
                } catch (IOException x) {
                    handleNetworkError(registrationData, x.toString());
                    break;
                } catch (Exception x) {
                    writeWarn("Error in Last.fm registration: " + x.toString());
                    break;
                }
            }
        }

        private void handleNetworkError(RegistrationData registrationData, String errorMessage) {
            try {
                queue.put(registrationData);
                if (LOG.isInfoEnabled()) {
                    LOG.info(
                            "Last.fm registration for '" + registrationData.getTitle() + "' encountered network error: "
                                    + errorMessage + ".  Will try again later. In queue: " + queue.size());
                }
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to reschedule Last.fm registration for '" + registrationData.getTitle() + "': "
                            + x.toString());
                }
            }
            try {
                sleep(60L * 1000L); // Wait 60 seconds.
            } catch (InterruptedException x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to sleep after Last.fm registration failure for '" + registrationData.getTitle()
                            + "': " + x.toString());
                }
            }
        }
    }

    private static class RegistrationData {

        private final String username;
        private final String password;
        private final String artist;
        private final String album;
        private final String title;
        private final int duration;
        private final Date time;
        public boolean submission;

        public RegistrationData(MediaFile mediaFile, String username, String password, boolean submission, Date time) {
            this.username = username;
            this.password = password;
            this.artist = mediaFile.getArtist();
            this.album = mediaFile.getAlbumName();
            this.title = mediaFile.getTitle();
            this.duration = mediaFile.getDurationSeconds() == null ? 0 : mediaFile.getDurationSeconds();
            this.time = time == null ? new Date() : time;
            this.submission = submission;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public String getTitle() {
            return title;
        }

        public int getDuration() {
            return duration;
        }

        public Date getTime() {
            return time;
        }

        public boolean isSubmission() {
            return submission;
        }
    }
}
