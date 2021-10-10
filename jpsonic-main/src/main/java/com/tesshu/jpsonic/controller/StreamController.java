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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.util.StringUtil.getMimeType;
import static org.springframework.web.bind.ServletRequestUtils.getBooleanParameter;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.VideoTranscodingSettings;
import com.tesshu.jpsonic.io.RangeOutputStream;
import com.tesshu.jpsonic.io.ShoutCastOutputStream;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.service.StreamService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.spring.LoggingExceptionResolver;
import com.tesshu.jpsonic.util.HttpRange;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A controller which streams the content of a {@link PlayQueue} to a remote {@link Player}.
 *
 * @author Sindre Mehus
 */
@Controller
@DependsOn("shortExecutor")
@RequestMapping({ "/stream/**", "/ext/stream/**" })
public class StreamController {

    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final PlayerService playerService;
    private final TranscodingService transcodingService;
    private final StatusService statusService;
    private final StreamService streamService;
    private final AtomicBoolean destroy;

    public StreamController(SettingsService settingsService, SecurityService securityService,
            PlayerService playerService, TranscodingService transcodingService, StatusService statusService,
            StreamService streamService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.transcodingService = transcodingService;
        this.statusService = statusService;
        this.streamService = streamService;
        destroy = new AtomicBoolean();
    }

    @PostConstruct
    public void init() {
        destroy.set(false);
    }

    private static void sendForbidden(HttpServletResponse res, String m) {
        try {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, m);
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error writing error :", e);
            }
        }
    }

    private static Integer getMaxBitRate(HttpServletRequest request) throws ServletRequestBindingException {
        Integer maxBitRate = getIntParameter(request, Attributes.Request.MAX_BIT_RATE.value());
        if (Integer.valueOf(0).equals(maxBitRate)) {
            return null;
        }
        return maxBitRate;
    }

    private PrepareResponseResult prepareResponse(final HttpServletRequest request, final HttpServletResponse response,
            final Authentication authentication, final User user, final Player player, final MediaFile file,
            final String preferredTargetFormat, final Integer maxBitRate) throws ServletRequestBindingException {

        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(file, user.getUsername())) {
            sendForbidden(response, "Access to file " + file.getId() + " is forbidden for user " + user.getUsername());
            return new PrepareResponseResult(true, null, null, null);
        }

        // Update the index of the currently playing media file. At
        // this point we haven't yet modified the play queue to support
        // multiple streams, so the current play queue is the real one.
        int currentIndex = player.getPlayQueue().getFiles().indexOf(file);
        player.getPlayQueue().setIndex(currentIndex);

        // Create a new, fake play queue that only contains the
        // currently playing media file, in case multiple streams want
        // to use the same player.
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, file);
        player.setPlayQueue(playQueue);

        final TranscodingService.Parameters parameters = transcodingService.getParameters(file, player, maxBitRate,
                preferredTargetFormat, null);
        HttpRange range = applyHeader(request, response, file, parameters);

        // Set content type of response
        final boolean isHls = getBooleanParameter(request, Attributes.Request.HLS.value(), false);
        applyContentType(isHls, response, player, file, preferredTargetFormat);

        final VideoTranscodingSettings videoTranscodingSettings = file.isVideo() || isHls
                ? streamService.createVideoTranscodingSettings(file, request) : null;

        return new PrepareResponseResult(false, range, parameters.getExpectedLength(), videoTranscodingSettings);
    }

    private static class PrepareResponseResult {
        private final boolean folderAccessNotAllowed;
        private final HttpRange range;
        private final Long fileLengthExpected;
        private final VideoTranscodingSettings videoTranscodingSettings;

        public PrepareResponseResult(boolean authenticationFailed, HttpRange range, Long fileLengthExpected,
                VideoTranscodingSettings videoTranscodingSettings) {
            super();
            this.folderAccessNotAllowed = authenticationFailed;
            this.range = range;
            this.fileLengthExpected = fileLengthExpected;
            this.videoTranscodingSettings = videoTranscodingSettings;
        }

        public boolean isFolderAccessNotAllowed() {
            return folderAccessNotAllowed;
        }

        public HttpRange getRange() {
            return range;
        }

        public Long getFileLengthExpected() {
            return fileLengthExpected;
        }

        public VideoTranscodingSettings getVideoTranscodingSettings() {
            return videoTranscodingSettings;
        }
    }

    private static @Nullable HttpRange applyHeader(HttpServletRequest request, HttpServletResponse response,
            MediaFile file, TranscodingService.Parameters parameters) {
        HttpRange range = null;
        Long fileLengthExpected = parameters.getExpectedLength();
        // Wrangle response length and ranges.
        //
        // Support ranges as long as we're not transcoding blindly; video is always assumed to
        // transcode
        if (file.isVideo() || !parameters.isRangeAllowed()) {
            // Use chunked transfer; do not accept range requests
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Accept-Ranges", "none");
        } else {
            // Partial content permitted because either know or expect to be able to predict the
            // final size
            long contentLength;
            // If range was requested, respond in kind
            range = getRange(request, file.getDurationSeconds(), fileLengthExpected);
            if (range == null) {
                // No range was requested, give back the whole file
                response.setStatus(HttpServletResponse.SC_OK);
                contentLength = fileLengthExpected;
            } else {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Accept-Ranges", "bytes");

                // Both ends are inclusive
                long startByte = range.getFirstBytePos();
                long endByte = range.isClosed() ? range.getLastBytePos() : fileLengthExpected - 1;

                response.setHeader("Content-Range",
                        String.format("bytes %d-%d/%d", startByte, endByte, fileLengthExpected));
                contentLength = endByte + 1 - startByte;
            }

            response.setIntHeader("ETag", file.getId());
            PlayerUtils.setContentLength(response, contentLength);
        }
        return range;
    }

    private static @Nullable HttpRange getRange(HttpServletRequest request, Integer fileDuration, Long fileSize) {

        // First, look for "Range" HTTP header.
        HttpRange range = HttpRange.valueOf(request.getHeader("Range"));
        if (range != null) {
            return range;
        }

        // Second, look for "offsetSeconds" request parameter.
        String offsetSeconds = request.getParameter(Attributes.Request.OFFSET_SECONDS.value());
        if (offsetSeconds == null) {
            return null;
        }

        if (fileDuration == null || fileSize == null) {
            return null;
        }
        float offset;
        try {
            offset = Float.parseFloat(offsetSeconds);
        } catch (NumberFormatException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to parse and convert time offset: " + offsetSeconds, e);
            }
            return null;
        }
        // Convert from time offset to byte offset.
        long byteOffset = (long) (fileSize * (offset / fileDuration));
        return new HttpRange(byteOffset, null);
    }

    private void applyContentType(boolean isHls, HttpServletResponse response, Player player, MediaFile file,
            String preferredTargetFormat) {
        if (isHls) {
            response.setContentType(getMimeType("ts")); // HLS is always MPEG TS.
        } else {
            String transcodedSuffix = transcodingService.getSuffix(player, file, preferredTargetFormat);
            response.setContentType(getMimeType(transcodedSuffix));
            applyContentDuration(response, file);
        }
    }

    private static void applyContentDuration(HttpServletResponse response, MediaFile file) {
        if (file.getDurationSeconds() != null) {
            response.setHeader("X-Content-Duration", String.format("%.1f", file.getDurationSeconds().doubleValue()));
        }
    }

    private static void writeVerboseLog(boolean verbose, boolean isHeaderRequest, MediaFile file) {
        if (verbose && isHeaderRequest && LOG.isInfoEnabled()) {
            LOG.info("Header request for [{}]", file.getPath());
        }
    }

    private static void writeVerboseLog(boolean verbose, HttpServletResponse response, MediaFile file) {
        if (verbose && LOG.isInfoEnabled()) {
            LOG.info("Streaming request for [{}] with range [{}]", file.getPath(), response.getHeader("Content-Range"));
        }
    }

    /**
     * Construct an appropriate output stream based on the request.
     * <p>
     * This is responsible for limiting the output to the given range (if not null) and injecting Shoutcast metadata
     * into the stream if requested.
     */
    private OutputStream createOutputStream(HttpServletRequest request, HttpServletResponse response, HttpRange range,
            boolean isSingleFile, Player player) throws IOException {
        // Enabled SHOUTcast, if requested.
        boolean isShoutCastRequested = "1".equals(request.getHeader("icy-metadata"));
        if (isShoutCastRequested && !isSingleFile) {
            response.setHeader("icy-metaint", Integer.toString(ShoutCastOutputStream.META_DATA_INTERVAL));
            response.setHeader("icy-notice1", "This stream is served using Airsonic");
            response.setHeader("icy-notice2", "Airsonic - Free media streamer");
            response.setHeader("icy-name", "Airsonic");
            response.setHeader("icy-genre", "Mixed");
            response.setHeader("icy-url", "https://airsonic.github.io/");
            OutputStream out = RangeOutputStream.wrap(response.getOutputStream(), range);
            return new ShoutCastOutputStream(out, player.getPlayQueue(), settingsService.getWelcomeTitle());
        }
        return RangeOutputStream.wrap(response.getOutputStream(), range);
    }

    @SuppressWarnings("PMD.CognitiveComplexity") // #1020
    @SuppressFBWarnings(value = "UC_USELESS_CONDITION", justification = "False positive. #1078")
    private void writeStream(Player player, InputStream in, OutputStream out, Long fileLengthExpected,
            boolean isPodcast, boolean isSingleFile) throws IOException {
        byte[] buf = new byte[settingsService.getBufferSize()];
        long bytesWritten = 0;

        boolean alive = isAliveStream(player);

        while (alive) {

            if (!alive && LOG.isInfoEnabled()) {
                LOG.info("Transfer was interrupted.(Player :name={}, ip={}, user={})", player.getName(),
                        player.getIpAddress(), player.getUsername());
            }

            // To reduce the frequency. If size is too small, it become bottleneck.
            boolean checkRequired = bytesWritten % 4096 * 200 == 0; // 0.8192Mb

            if (checkRequired && player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("PlayQueue stopped playing.(Player :name={}, ip={}, user={})", player.getName(),
                            player.getIpAddress(), player.getUsername());
                }
                if (isPodcast || isSingleFile) {
                    break;
                } else {
                    streamService.sendDummyDelayed(buf, out);
                }
            } else {
                int n = in.read(buf);
                if (n == -1) {
                    if (isPodcast || isSingleFile) {
                        // Pad the output if needed to avoid content length errors on transcodes
                        if (fileLengthExpected != null && bytesWritten < fileLengthExpected) {
                            streamService.sendDummy(buf, out, fileLengthExpected - bytesWritten);
                        }
                        break;
                    } else {
                        streamService.sendDummyDelayed(buf, out);
                    }
                } else {
                    if (LOG.isWarnEnabled() && fileLengthExpected != null && bytesWritten <= fileLengthExpected
                            && bytesWritten + n > fileLengthExpected) {
                        LOG.warn("Stream output exceeded expected length of {}. It is likely that "
                                + "the transcoder is not adhering to the bitrate limit or the media "
                                + "source is corrupted or has grown larger", fileLengthExpected);
                    }
                    out.write(buf, 0, n);
                    bytesWritten += n;
                }
            }

            if (checkRequired) {
                alive = isAliveStream(player);
            }
        }
    }

    private boolean isAliveStream(Player player) {
        return !destroy.get() && statusService.getStreamStatusesForPlayer(player).stream().filter(ts -> ts.isActive())
                .allMatch(ts -> !ts.isTerminated());
    }

    private static void writeErrorLog(IOException e, HttpServletRequest req) {
        if (LoggingExceptionResolver.isSuppressedException(e)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(req.getRemoteAddr() + ": Client unexpectedly closed connection while loading "
                        + req.getRemoteAddr() + " (" + PlayerUtils.getAnonymizedURLForRequest(req) + ")", e);
            }
        } else {
            LOG.error("Error while writing the playing Stream.", e);
        }
    }

    @GetMapping
    public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws ServletRequestBindingException {

        final Player player = playerService.getPlayer(req, res, false, true);
        final User user = securityService.getUserByName(player.getUsername());
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JWTAuthenticationToken) && !user.isStreamRole()) {
            sendForbidden(res, "Streaming is forbidden for user " + user.getUsername());
            return;
        }

        // Podcast-specific processing
        Integer playlistId = getIntParameter(req, Attributes.Request.PLAYLIST.value());
        final boolean isPodcast = playlistId != null;
        if (isPodcast) {
            // If "playlist" request parameter is set, this is a Podcast request.
            streamService.setUpPlayQueue(req, res, player, playlistId);
        }

        res.setHeader("Access-Control-Allow-Origin", "*");
        String contentType = getMimeType(req.getParameter(Attributes.Request.SUFFIX.value()));
        res.setContentType(contentType);

        // Is this a request for a single file (typically from the embedded Flash player)?
        // In that case, create a separate playlist (in order to support multiple parallel streams).
        // Also, enable partial download (HTTP byte range).
        MediaFile file = streamService.getSingleFile(req);
        boolean isSingleFile = file != null;
        PrepareResponseResult result = null;
        String format = req.getParameter(Attributes.Request.FORMAT.value());
        Integer maxBitRate = getMaxBitRate(req);
        if (isSingleFile) {
            result = prepareResponse(req, res, auth, user, player, file, format, maxBitRate);
        }

        boolean isHeaderRequest = HttpMethod.HEAD.name().equals(req.getMethod());
        if (result == null || result.isFolderAccessNotAllowed() || isHeaderRequest) {
            // All headers are set, stop if that's all the client requested.
            writeVerboseLog(settingsService.isVerboseLogPlaying(), isHeaderRequest, file);
            return;
        }

        Long fileLength = result.getFileLengthExpected();
        writeVerboseLog(settingsService.isVerboseLogPlaying(), res, file);

        // Terminate any other streams to this player.
        streamService.closeAllStreamFor(player, isPodcast, isSingleFile);

        TransferStatus status = statusService.createStreamStatus(player);
        try (InputStream in = streamService.createInputStream(player, status, maxBitRate, format,
                result.getVideoTranscodingSettings());
                OutputStream out = createOutputStream(req, res, result.getRange(), isSingleFile, player)) {
            writeStream(player, in, out, fileLength, isPodcast, isSingleFile);
        } catch (IOException e) {
            writeErrorLog(e, req);
        } finally {
            streamService.removeStreamStatus(user, status);
        }
    }

    @PreDestroy
    public void onDestroy() {
        destroy.set(true);
    }
}
