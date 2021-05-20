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

package org.airsonic.player.controller;

import static org.airsonic.player.util.StringUtil.getMimeType;
import static org.springframework.web.bind.ServletRequestUtils.getBooleanParameter;
import static org.springframework.web.bind.ServletRequestUtils.getIntParameter;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.VideoTranscodingSettings;
import org.airsonic.player.io.PlayQueueInputStream;
import org.airsonic.player.io.RangeOutputStream;
import org.airsonic.player.io.ShoutCastOutputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.AudioScrobblerService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.sonos.SonosHelper;
import org.airsonic.player.spring.LoggingExceptionResolver;
import org.airsonic.player.util.HttpRange;
import org.airsonic.player.util.PlayerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    private static final int MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL1 = 400;
    private static final int MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL2 = 600;
    private static final int MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL3 = 1800;

    private final StatusService statusService;
    private final PlayerService playerService;
    private final PlaylistService playlistService;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final TranscodingService transcodingService;
    private final AudioScrobblerService audioScrobblerService;
    private final MediaFileService mediaFileService;
    private final SearchService searchService;

    // Used to perform transcoding in subthreads (Priority changes)
    private final ThreadPoolTaskExecutor shortExecutor;
    private final AtomicBoolean destroy = new AtomicBoolean();

    public StreamController(StatusService statusService, PlayerService playerService, PlaylistService playlistService,
            SecurityService securityService, SettingsService settingsService, TranscodingService transcodingService,
            AudioScrobblerService audioScrobblerService, MediaFileService mediaFileService, SearchService searchService,
            ThreadPoolTaskExecutor shortExecutor) {
        super();
        this.statusService = statusService;
        this.playerService = playerService;
        this.playlistService = playlistService;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.transcodingService = transcodingService;
        this.audioScrobblerService = audioScrobblerService;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
        this.shortExecutor = shortExecutor;
    }

    @PostConstruct
    public void init() {
        destroy.set(false);
    }

    @PreDestroy
    public void onDestroy() {
        destroy.set(true);
    }

    private boolean isAliveStream(Player player) {
        return !destroy.get() && statusService.getStreamStatusesForPlayer(player).stream().filter(ts -> ts.isActive())
                .allMatch(ts -> !ts.isTerminated());
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
            setUpPlayQueue(req, res, player, playlistId);
        }

        res.setHeader("Access-Control-Allow-Origin", "*");
        String contentType = getMimeType(req.getParameter(Attributes.Request.SUFFIX.value()));
        res.setContentType(contentType);

        // Is this a request for a single file (typically from the embedded Flash player)?
        // In that case, create a separate playlist (in order to support multiple parallel streams).
        // Also, enable partial download (HTTP byte range).
        MediaFile file = getSingleFile(req);
        boolean isSingleFile = file != null;
        PrepareResponseResult result = null;
        String format = req.getParameter(Attributes.Request.FORMAT.value());
        Integer maxBitRate = getMaxBitRate(req);
        if (isSingleFile) {
            result = prepareResponse(req, res, auth, user, player, file, format, maxBitRate);
        }
        if (result == null || result.isFolderAccessNotAllowed() || HttpMethod.HEAD.name().equals(req.getMethod())) {
            // All headers are set, stop if that's all the client requested.
            return;
        }

        Long fileLength = result.getFileLengthExpected();
        writeLog(res, file);

        // Terminate any other streams to this player.
        closeAllStreamFor(player, isPodcast, isSingleFile);

        TransferStatus status = statusService.createStreamStatus(player);
        try (InputStream in = createInputStream(player, status, maxBitRate, format,
                result.getVideoTranscodingSettings());
                OutputStream out = createOutputStream(req, res, result.getRange(), isSingleFile, player)) {
            writeStream(player, in, out, fileLength, isPodcast, isSingleFile);
        } catch (IOException e) {
            writeStraemLog(e, req);
        } finally {
            removeStreamStatus(user, status);
        }
    }

    private void sendForbidden(HttpServletResponse res, String m) {
        try {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, m);
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error writing error :", e);
            }
        }
    }

    private InputStream createInputStream(Player player, TransferStatus status, Integer maxBitRate, String format,
            VideoTranscodingSettings videoTranscodingSettings) {
        return new PlayQueueInputStream(player, status, maxBitRate, format, videoTranscodingSettings,
                transcodingService, audioScrobblerService, mediaFileService, searchService, settingsService,
                shortExecutor);
    }

    private void closeAllStreamFor(Player player, boolean isPodcast, boolean isSingleFile) {
        if (!isPodcast && !isSingleFile) {
            statusService.getStreamStatusesForPlayer(player).stream().filter(TransferStatus::isActive)
                    .forEach(TransferStatus::terminate);
        }
    }

    private void removeStreamStatus(User user, TransferStatus status) {
        if (status != null) {
            securityService.updateUserByteCounts(user, status.getBytesTransfered(), 0L, 0L);
            statusService.removeStreamStatus(status);
        }
    }

    private void writeStraemLog(IOException e, HttpServletRequest req) {
        if (LoggingExceptionResolver.isSuppressedException(e)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(req.getRemoteAddr() + ": Client unexpectedly closed connection while loading "
                        + req.getRemoteAddr() + " (" + PlayerUtils.getAnonymizedURLForRequest(req) + ")", e);
            }
        } else {
            LOG.error("Error while writing the playing Stream.", e);
        }
    }

    private void writeLog(HttpServletResponse response, MediaFile file) {
        if (settingsService.isVerboseLogPlaying() && LOG.isInfoEnabled()) {
            LOG.info("Streaming request for [{}] with range [{}]", file.getPath(), response.getHeader("Content-Range"));
        }
    }

    @SuppressWarnings("PMD.NullAssignment") // false positive
    private Integer getMaxBitRate(HttpServletRequest request) throws ServletRequestBindingException {
        Integer maxBitRate = getIntParameter(request, Attributes.Request.MAX_BIT_RATE.value());
        if (Integer.valueOf(0).equals(maxBitRate)) {
            maxBitRate = null;
        }
        return maxBitRate;
    }

    /**
     * create a separate play queue (in order to support multiple parallel Podcast streams).
     */
    private void setUpPlayQueue(HttpServletRequest request, HttpServletResponse response, Player player,
            Integer playlistId) {
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(false, playlistService.getFilesInPlaylist(playlistId));
        player.setPlayQueue(playQueue);
        PlayerUtils.setContentLength(response, playQueue.length());
        if (LOG.isInfoEnabled()) {
            LOG.info("{}: Incoming Podcast request for playlist {}", request.getRemoteAddr(), playlistId);
        }
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

    private PrepareResponseResult prepareResponse(final HttpServletRequest request, final HttpServletResponse response,
            final Authentication authentication, final User user, final Player player, final MediaFile file,
            final String preferredTargetFormat, final Integer maxBitRate) throws ServletRequestBindingException {

        HttpRange range = null;
        Long fileLengthExpected = null;
        VideoTranscodingSettings videoTranscodingSettings = null;

        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(file, user.getUsername())) {
            sendForbidden(response, "Access to file " + file.getId() + " is forbidden for user " + user.getUsername());
            return new PrepareResponseResult(true, range, fileLengthExpected, videoTranscodingSettings);
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

        TranscodingService.Parameters parameters = transcodingService.getParameters(file, player, maxBitRate,
                preferredTargetFormat, null);
        boolean isHls = getBooleanParameter(request, Attributes.Request.HLS.value(), false);
        fileLengthExpected = parameters.getExpectedLength();

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

        // Set content type of response
        if (isHls) {
            response.setContentType(getMimeType("ts")); // HLS is always MPEG TS.
        } else {
            String transcodedSuffix = transcodingService.getSuffix(player, file, preferredTargetFormat);
            boolean sonos = SonosHelper.JPSONIC_CLIENT_ID.equals(player.getClientId());
            response.setContentType(getMimeType(transcodedSuffix, sonos));
            setContentDuration(response, file);
        }

        if (file.isVideo() || isHls) {
            videoTranscodingSettings = createVideoTranscodingSettings(file, request);
        }
        return new PrepareResponseResult(false, range, fileLengthExpected, videoTranscodingSettings);
    }

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
                // The significance of the existence of this block is doubtful
                if (!alive) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("PlayQueue stopped playing.(Player :name={}, ip={}, user={})", player.getName(),
                                player.getIpAddress(), player.getUsername());
                    }
                    if (isPodcast || isSingleFile) {
                        break;
                    } else {
                        sendDummyDelayed(buf, out);
                    }
                }
            } else {
                int n = in.read(buf);
                if (n == -1) {
                    if (isPodcast || isSingleFile) {
                        // Pad the output if needed to avoid content length errors on transcodes
                        if (fileLengthExpected != null && bytesWritten < fileLengthExpected) {
                            sendDummy(buf, out, fileLengthExpected - bytesWritten);
                        }
                        break;
                    } else {
                        sendDummyDelayed(buf, out);
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
            return new ShoutCastOutputStream(out, player.getPlayQueue(), settingsService);
        }
        return RangeOutputStream.wrap(response.getOutputStream(), range);
    }

    private void setContentDuration(HttpServletResponse response, MediaFile file) {
        if (file.getDurationSeconds() != null) {
            response.setHeader("X-Content-Duration", String.format("%.1f", file.getDurationSeconds().doubleValue()));
        }
    }

    @SuppressFBWarnings(value = {
            "PT_ABSOLUTE_PATH_TRAVERSAL" }, justification = "Has been verified in subsequent processing.")
    private MediaFile getSingleFile(HttpServletRequest request) throws ServletRequestBindingException {
        String path = request.getParameter(Attributes.Request.PATH.value());
        if (path != null) {
            return mediaFileService.getMediaFile(path);
        }
        Integer id = getIntParameter(request, Attributes.Request.ID.value());
        if (id != null) {
            return mediaFileService.getMediaFile(id);
        }
        return null;
    }

    @Nullable
    private HttpRange getRange(HttpServletRequest request, Integer fileDuration, Long fileSize) {

        // First, look for "Range" HTTP header.
        HttpRange range = HttpRange.valueOf(request.getHeader("Range"));
        if (range != null) {
            return range;
        }

        // Second, look for "offsetSeconds" request parameter.
        String offsetSeconds = request.getParameter(Attributes.Request.OFFSET_SECONDS.value());
        range = parseAndConvertOffsetSeconds(offsetSeconds, fileDuration, fileSize);
        return range;

    }

    @Nullable
    private HttpRange parseAndConvertOffsetSeconds(String offsetSeconds, Integer fileDuration, Long fileSize) {
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

    private VideoTranscodingSettings createVideoTranscodingSettings(MediaFile file, HttpServletRequest request)
            throws ServletRequestBindingException {
        Integer existingWidth = file.getWidth();
        Integer existingHeight = file.getHeight();
        Integer maxBitRate = getIntParameter(request, Attributes.Request.MAX_BIT_RATE.value());
        int timeOffset = getIntParameter(request, Attributes.Request.TIME_OFFSET.value(), 0);
        int defaultDuration = file.getDurationSeconds() == null ? Integer.MAX_VALUE
                : file.getDurationSeconds() - timeOffset;
        int duration = getIntParameter(request, Attributes.Request.DURATION.value(), defaultDuration);
        boolean hls = getBooleanParameter(request, Attributes.Request.HLS.value(), false);

        Dimension dim = getRequestedVideoSize(request.getParameter(Attributes.Request.SIZE.value()));
        if (dim == null) {
            dim = getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        }

        return new VideoTranscodingSettings(dim.width, dim.height, timeOffset, duration, hls);
    }

    protected Dimension getRequestedVideoSize(String sizeSpec) {
        if (sizeSpec == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("^(\\d+)x(\\d+)$");
        Matcher matcher = pattern.matcher(sizeSpec);
        if (matcher.find()) {
            int w = Integer.parseInt(matcher.group(1));
            int h = Integer.parseInt(matcher.group(2));
            if (w >= 0 && h >= 0 && w <= 2000 && h <= 2000) {
                return new Dimension(w, h);
            }
        }
        return null;
    }

    protected Dimension getSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate) {
        if (maxBitRate == null) {
            return new Dimension(400, 224);
        }

        int w;
        if (maxBitRate < MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL1) {
            w = 400;
        } else if (maxBitRate < MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL2) {
            w = 480;
        } else if (maxBitRate < MAXBITRATE_THRESHOLD_FOR_VIDEO_SIZE_LEVEL3) {
            w = 640;
        } else {
            w = 960;
        }
        int h = even(w * 9 / 16);

        if (existingWidth == null || existingHeight == null) {
            return new Dimension(w, h);
        }

        if (existingWidth < w || existingHeight < h) {
            return new Dimension(even(existingWidth), even(existingHeight));
        }

        double aspectRate = existingWidth.doubleValue() / existingHeight.doubleValue();
        h = (int) Math.round(w / aspectRate);

        return new Dimension(even(w), even(h));
    }

    // Make sure width and height are multiples of two, as some versions of ffmpeg require it.
    private int even(int size) {
        return size + (size % 2);
    }

    private void sendDummy(byte[] buf, OutputStream out, long len) throws IOException {
        long bytesWritten = 0;
        int n;

        Arrays.fill(buf, (byte) 0xFF);
        while (bytesWritten < len) {
            n = (int) Math.min(buf.length, len - bytesWritten);
            out.write(buf, 0, n);
            bytesWritten += n;
        }
    }

    /**
     * Feed the other end with some dummy data to keep it from reconnecting.
     */
    private void sendDummyDelayed(byte[] buf, OutputStream out) throws IOException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            LOG.info("Interrupted in sleep.", e);
        }
        sendDummy(buf, out, buf.length);
        out.flush();
    }
}
