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

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.tesshu.jpsonic.controller.VideoPlayerController;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.domain.VideoTranscodingSettings;
import com.tesshu.jpsonic.io.TranscodeInputStream;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.util.PlayerUtils;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Provides services for transcoding media. Transcoding is the process of converting a media file/stream to a different
 * format and/or bit rate. The latter is also called downsampling.
 *
 * @author Sindre Mehus
 * 
 * @see TranscodeInputStream
 */
@Service
@DependsOn("shortExecutor")
public class TranscodingService {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodingService.class);
    public static final String FORMAT_RAW = "raw";

    private final TranscodingDao transcodingDao;
    private final SettingsService settingsService;
    private final PlayerService playerService;
    private final Executor shortExecutor;
    private final String transcodePath = Optional.ofNullable(System.getProperty("transcodePath") == null ? null
            : System.getProperty("transcodePath").replaceAll("\\\\", "\\\\\\\\")).orElse(null);
    private File transcodeDirectory;

    public TranscodingService(TranscodingDao transcodingDao, SettingsService settingsService,
            @Lazy PlayerService playerService, Executor shortExecutor) {
        super();
        this.transcodingDao = transcodingDao;
        this.settingsService = settingsService;
        this.playerService = playerService;
        this.shortExecutor = shortExecutor;
    }

    /**
     * Returns the directory in which all transcoders are installed.
     */
    public @NonNull File getTranscodeDirectory() {
        if (!isEmpty(transcodeDirectory)) {
            return transcodeDirectory;
        }
        if (isEmpty(transcodePath)) {
            transcodeDirectory = new File(SettingsService.getJpsonicHome(), "transcode");
            if (!transcodeDirectory.exists()) {
                boolean ok = transcodeDirectory.mkdir();
                if (ok && LOG.isInfoEnabled()) {
                    LOG.info("Created directory " + transcodeDirectory);
                } else if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to create directory " + transcodeDirectory);
                }
            }
        } else {
            transcodeDirectory = new File(transcodePath);
        }
        return transcodeDirectory;
    }

    protected void setTranscodeDirectory(@Nullable File transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
    }

    /**
     * Returns all transcodings.
     *
     * @return Possibly empty list of all transcodings.
     */
    public List<Transcoding> getAllTranscodings() {
        return transcodingDao.getAllTranscodings();
    }

    /**
     * Returns all active transcodings for the given player. Only enabled transcodings are returned.
     *
     * @param player
     *            The player.
     * 
     * @return All active transcodings for the player.
     */
    public List<Transcoding> getTranscodingsForPlayer(@NonNull Player player) {
        // FIXME - This should probably check isTranscodingInstalled()
        return transcodingDao.getTranscodingsForPlayer(player.getId());
    }

    /**
     * Sets the list of active transcodings for the given player.
     *
     * @param player
     *            The player.
     * @param transcodingIds
     *            ID's of the active transcodings.
     */
    public void setTranscodingsForPlayer(@NonNull Player player, int... transcodingIds) {
        transcodingDao.setTranscodingsForPlayer(player.getId(), transcodingIds);
    }

    /**
     * Sets the list of active transcodings for the given player.
     *
     * @param player
     *            The player.
     * @param transcodings
     *            The active transcodings.
     */
    public void setTranscodingsForPlayer(@NonNull Player player, @NonNull List<Transcoding> transcodings) {
        int[] transcodingIds = new int[transcodings.size()];
        for (int i = 0; i < transcodingIds.length; i++) {
            transcodingIds[i] = transcodings.get(i).getId();
        }
        setTranscodingsForPlayer(player, transcodingIds);
    }

    /**
     * Creates a new transcoding.
     *
     * @param transcoding
     *            The transcoding to create.
     */
    public void createTranscoding(@NonNull Transcoding transcoding) {
        transcodingDao.createTranscoding(transcoding);

        // Activate this transcoding for all players?
        if (transcoding.isDefaultActive()) {
            playerService.getAllPlayers().forEach(player -> {
                List<Transcoding> transcodings = getTranscodingsForPlayer(player);
                transcodings.add(transcoding);
                setTranscodingsForPlayer(player, transcodings);
            });
        }
    }

    /**
     * Deletes the transcoding with the given ID.
     *
     * @param id
     *            The transcoding ID.
     */
    public void deleteTranscoding(Integer id) {
        transcodingDao.deleteTranscoding(id);
    }

    /**
     * Updates the given transcoding.
     *
     * @param transcoding
     *            The transcoding to update.
     */
    public void updateTranscoding(Transcoding transcoding) {
        transcodingDao.updateTranscoding(transcoding);
    }

    /**
     * Returns whether transcoding is required for the given media file and player combination.
     *
     * @param mediaFile
     *            The media file.
     * @param player
     *            The player.
     * 
     * @return Whether transcoding will be performed if invoking the {@link #getTranscodedInputStream} method with the
     *         same arguments.
     */
    public boolean isTranscodingRequired(@NonNull MediaFile mediaFile, @NonNull Player player) {
        return getTranscoding(mediaFile, player, null, false) != null;
    }

    /**
     * Returns the suffix for the given player and media file, taking transcodings into account.
     *
     * @param player
     *            The player in question.
     * @param file
     *            The media file.
     * @param preferredTargetFormat
     *            Used to select among multiple applicable transcodings. May be {@code null}.
     * 
     * @return The file suffix, e.g., "mp3".
     */
    public String getSuffix(@NonNull Player player, @NonNull MediaFile file, @Nullable String preferredTargetFormat) {
        Transcoding transcoding = getTranscoding(file, player, preferredTargetFormat, false);
        return transcoding == null ? file.getFormat() : transcoding.getTargetFormat();
    }

    /**
     * Returns an applicable transcoding for the given file and player, or <code>null</code> if no transcoding should be
     * done.
     */
    private @Nullable Transcoding getTranscoding(@NonNull MediaFile mediaFile, @NonNull Player player,
            @Nullable String preferredTargetFormat, boolean hls) {

        if (FORMAT_RAW.equals(preferredTargetFormat)) {
            return null;
        }

        if (hls) {
            return new Transcoding(null, "hls", mediaFile.getFormat(), "ts", settingsService.getHlsCommand(), null,
                    null, true);
        }

        List<Transcoding> applicableTranscodings = new LinkedList<>();
        String suffix = mediaFile.getFormat();

        // This is what I'd like todo, but this will most likely break video transcoding as video transcoding is
        // never expected to be null
        // if(StringUtils.equalsIgnoreCase(preferredTargetFormat, suffix)) {
        // LOG.debug("Target formats are the same, returning no transcoding");
        // return null;
        // }

        List<Transcoding> transcodingsForPlayer = getTranscodingsForPlayer(player);
        for (Transcoding transcoding : transcodingsForPlayer) {
            // special case for now as video must have a transcoding
            if (mediaFile.isVideo()
                    && StringUtils.equalsIgnoreCase(preferredTargetFormat, transcoding.getTargetFormat())) {
                // Detected source to target format match for video
                return transcoding;
            }
            Arrays.stream(transcoding.getSourceFormatsAsArray())
                    .filter(sourceFormat -> sourceFormat.equalsIgnoreCase(suffix))
                    .filter(sourceFormat -> isTranscoderInstalled(transcoding))
                    .forEach(s -> applicableTranscodings.add(transcoding));
        }

        if (applicableTranscodings.isEmpty()) {
            return null;
        }

        for (Transcoding transcoding : applicableTranscodings) {
            if (transcoding.getTargetFormat().equalsIgnoreCase(preferredTargetFormat)) {
                return transcoding;
            }
        }

        return applicableTranscodings.get(0);
    }

    /**
     * Returns a possibly transcoded or downsampled input stream for the given music file and player combination.
     * <p/>
     * A transcoding is applied if it is applicable for the format of the given file, and is activated for the given
     * player.
     * <p/>
     * If no transcoding is applicable, the file may still be downsampled, given that the player is configured with a
     * bit rate limit which is higher than the actual bit rate of the file.
     * <p/>
     * Otherwise, a normal input stream to the original file is returned.
     *
     * @param parameters
     *            As returned by {@link #getParameters}.
     * 
     * @return A possible transcoded or downsampled input stream.
     * 
     * @throws IOException
     *             If an I/O error occurs.
     */
    public @NonNull InputStream getTranscodedInputStream(@Nullable Parameters parameters) throws IOException {
        try {
            if (parameters.getTranscoding() != null) {
                return createTranscodedInputStream(parameters);
            }
        } catch (IOException e) {
            // IOException : Process failure or Windows limited process(createTempFile, File copy)
            throw new IOException("Transcoder failed: " + parameters.getMediaFile().getFile().getAbsolutePath(), e);
        }
        // IOException : InvalidPathException
        return Files.newInputStream(Paths.get(parameters.getMediaFile().getFile().toURI()));
    }

    /**
     * Returns an input stream by applying the given transcoding to the given music file.
     *
     * @param parameters
     *            Transcoding parameters.
     * 
     * @return The transcoded input stream.
     * 
     * @throws IOException
     *             If an I/O error occurs.
     */
    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    private InputStream createTranscodedInputStream(@NonNull Parameters parameters) throws IOException {

        Transcoding transcoding = parameters.getTranscoding();
        Integer maxBitRate = parameters.getMaxBitRate();
        VideoTranscodingSettings videoTranscodingSettings = parameters.getVideoTranscodingSettings();
        MediaFile mediaFile = parameters.getMediaFile();

        if (!isEmpty(transcoding.getStep2()) && !isEmpty(transcoding.getStep3())) {
            return createTranscodeInputStream(transcoding.getStep3(), maxBitRate, videoTranscodingSettings, mediaFile,
                    createTranscodeInputStream(transcoding.getStep2(), maxBitRate, videoTranscodingSettings, mediaFile,
                            createTranscodeInputStream(transcoding.getStep1(), maxBitRate, videoTranscodingSettings,
                                    mediaFile, null)));
        } else if (!isEmpty(transcoding.getStep2())) {
            return createTranscodeInputStream(transcoding.getStep2(), maxBitRate, videoTranscodingSettings, mediaFile,
                    createTranscodeInputStream(transcoding.getStep1(), maxBitRate, videoTranscodingSettings, mediaFile,
                            null));
        } else if (!isEmpty(transcoding.getStep3())) {
            return createTranscodeInputStream(transcoding.getStep3(), maxBitRate, videoTranscodingSettings, mediaFile,
                    createTranscodeInputStream(transcoding.getStep1(), maxBitRate, videoTranscodingSettings, mediaFile,
                            null));
        }

        return createTranscodeInputStream(transcoding.getStep1(), maxBitRate, videoTranscodingSettings, mediaFile,
                null);
    }

    /**
     * Creates a transcoded input stream by interpreting the given command line string. This includes the following:
     * <ul>
     * <li>Splitting the command line string to an array.</li>
     * <li>Replacing occurrences of "%s" with the path of the given music file.</li>
     * <li>Replacing occurrences of "%t" with the title of the given music file.</li>
     * <li>Replacing occurrences of "%l" with the album name of the given music file.</li>
     * <li>Replacing occurrences of "%a" with the artist name of the given music file.</li>
     * <li>Replacing occurrcences of "%b" with the max bitrate.</li>
     * <li>Replacing occurrcences of "%o" with the video time offset (used for scrubbing).</li>
     * <li>Replacing occurrcences of "%d" with the video duration (used for HLS).</li>
     * <li>Replacing occurrcences of "%w" with the video image width.</li>
     * <li>Replacing occurrcences of "%h" with the video image height.</li>
     * <li>Prepending the path of the transcoder directory if the transcoder is found there.</li>
     * </ul>
     *
     * @param command
     *            The command line string.
     * @param maxBitRate
     *            The maximum bitrate to use. May not be {@code null}.
     * @param videoTranscodingSettings
     *            Parameters used when transcoding video. May be {@code null}.
     * @param mediaFile
     *            The media file.
     * @param in
     *            Data to feed to the process. May be {@code null}. @return The newly created input stream.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File) Not reusable
    private TranscodeInputStream createTranscodeInputStream(@NonNull String command, Integer maxBitRate,
            VideoTranscodingSettings videoTranscodingSettings, @NonNull MediaFile mediaFile, InputStream in)
            throws IOException {

        String title = mediaFile.getTitle();
        String album = mediaFile.getAlbumName();
        String artist = mediaFile.getArtist();

        if (title == null) {
            title = "Unknown Song";
        }
        if (album == null) {
            album = "Unknown Album";
        }
        if (artist == null) {
            artist = "Unknown Artist";
        }

        List<String> result = new LinkedList<>(Arrays.asList(StringUtil.split(command)));
        result.set(0, getTranscodeDirectory().getPath() + File.separatorChar + result.get(0));

        File tmpFile = null;

        for (int i = 1; i < result.size(); i++) {
            String cmd = result.get(i);
            cmd = replaceIfcontains(cmd, "%b", String.valueOf(maxBitRate));
            cmd = replaceIfcontains(cmd, "%t", title);
            cmd = replaceIfcontains(cmd, "%l", album);
            cmd = replaceIfcontains(cmd, "%a", artist);
            if (videoTranscodingSettings != null) {
                cmd = replaceIfcontains(cmd, "%o", String.valueOf(videoTranscodingSettings.getTimeOffset()));
                cmd = replaceIfcontains(cmd, "%d", String.valueOf(videoTranscodingSettings.getDuration()));
                cmd = replaceIfcontains(cmd, "%w", String.valueOf(videoTranscodingSettings.getWidth()));
                cmd = replaceIfcontains(cmd, "%h", String.valueOf(videoTranscodingSettings.getHeight()));
            }
            if (cmd.contains("%s")) {

                // Work-around for filename character encoding problem on Windows.
                // Create temporary file, and feed this to the transcoder.
                String path = mediaFile.getFile().getAbsolutePath();
                if (PlayerUtils.isWindows() && !mediaFile.isVideo() && !StringUtils.isAsciiPrintable(path)) {
                    tmpFile = File.createTempFile("jpsonic", "." + FilenameUtils.getExtension(path));
                    tmpFile.deleteOnExit();
                    FileUtils.copyFile(new File(path), tmpFile);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Created tmp file: " + tmpFile);
                    }
                    cmd = cmd.replace("%s", tmpFile.getPath());
                } else {
                    cmd = cmd.replace("%s", path);
                }
            }

            result.set(i, cmd);
        }
        return new TranscodeInputStream(new ProcessBuilder(result), in, tmpFile, shortExecutor,
                settingsService.isVerboseLogPlaying());
    }

    private String replaceIfcontains(@NonNull String line, @NonNull String target, @NonNull String value) {
        if (line.contains(target)) {
            return line.replace(target, value);
        }
        return line;
    }

    /**
     * Returns whether transcoding is supported (i.e. whether ffmpeg is installed or not).
     *
     * @param mediaFile
     *            If not null, returns whether transcoding is supported for this file.
     * 
     * @return Whether transcoding is supported.
     */
    public boolean isTranscodingSupported(@Nullable MediaFile mediaFile) {
        List<Transcoding> transcodings = getAllTranscodings();
        for (Transcoding transcoding : transcodings) {
            if (mediaFile == null) {
                return true;
            }
            for (String sourceFormat : transcoding.getSourceFormatsAsArray()) {
                if (sourceFormat.equalsIgnoreCase(mediaFile.getFormat())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTranscoderInstalled(@NonNull Transcoding transcoding) {
        return isTranscoderInstalled(transcoding.getStep1()) && isTranscoderInstalled(transcoding.getStep2())
                && isTranscoderInstalled(transcoding.getStep3());
    }

    private boolean isTranscoderInstalled(String step) {
        if (StringUtils.isEmpty(step)) {
            return true;
        }
        String executable = StringUtil.split(step)[0];
        PrefixFileFilter filter = new PrefixFileFilter(executable);
        String[] matches = getTranscodeDirectory().list(filter);
        return matches != null && matches.length > 0;
    }

    /**
     * Creates parameters for a possibly transcoded input stream for the given media file and player combination.
     * <p/>
     * A transcoding is applied if it is applicable for the format of the given file, and is activated for the given
     * player, and either the desired format or bitrate needs changing.
     * <p/>
     * Otherwise, a normal input stream to the original file is returned.
     *
     * @param mediaFile
     *            The media file.
     * @param p
     *            The player.
     * @param maxBitRate
     *            Overrides the per-player and per-user bitrate limit. May be {@code null}.
     * @param preferredTargetFormat
     *            Used to select among multiple applicable transcodings. May be {@code null}.
     * @param videoTranscodingSettings
     *            Parameters used when transcoding video. May be {@code null}.
     * 
     * @return Parameters to be used in the {@link #getTranscodedInputStream} method.
     */
    public Parameters getParameters(@NonNull MediaFile mediaFile, @NonNull Player p, @Nullable final Integer maxBitRate,
            @Nullable String preferredTargetFormat, @Nullable VideoTranscodingSettings videoTranscodingSettings) {

        boolean useGuestPlayer = JWTAuthenticationToken.USERNAME_ANONYMOUS.equals(p.getUsername())
                && !settingsService.isAnonymousTranscoding();
        final Player player = useGuestPlayer ? playerService.getGuestPlayer(null) : p;
        final TranscodeScheme transcodeScheme = getTranscodeScheme(player).strictest(TranscodeScheme.fromMaxBitRate(
                maxBitRate == null ? Integer.valueOf(TranscodeScheme.OFF.getMaxBitRate()) : maxBitRate));
        final int bitRate = createBitrate(mediaFile);
        final int mb = createMaxBitrate(transcodeScheme, mediaFile, bitRate);
        final boolean hls = videoTranscodingSettings != null && videoTranscodingSettings.isHls();
        final Transcoding transcoding = getTranscoding(mediaFile, player, preferredTargetFormat, hls);

        Parameters parameters = new Parameters(mediaFile, videoTranscodingSettings);
        if (isNeedTranscoding(transcoding, mb, bitRate, preferredTargetFormat, mediaFile)) {
            parameters.setTranscoding(transcoding);
        }

        parameters.setMaxBitRate(mb == 0 ? null : mb);
        parameters.setRangeAllowed(isRangeAllowed(parameters));
        parameters.setExpectedLength(getExpectedLength(parameters));
        return parameters;
    }

    /**
     * Returns the strictest transcoding scheme defined for the player and the user.
     */
    private TranscodeScheme getTranscodeScheme(@Nullable Player player) {
        String username = player.getUsername();
        if (username != null) {
            UserSettings userSettings = settingsService.getUserSettings(username);
            return player.getTranscodeScheme().strictest(userSettings.getTranscodeScheme());
        }

        return player.getTranscodeScheme();
    }

    private int createBitrate(@NonNull MediaFile mediaFile) {
        // If null assume unlimited bitrate
        int bitRate = mediaFile.getBitRate() == null ? Integer.valueOf(TranscodeScheme.OFF.getMaxBitRate())
                : mediaFile.getBitRate();
        if (!mediaFile.isVideo()) {
            if (mediaFile.isVariableBitRate()) {
                // Assume VBR needs approx 20% more bandwidth to maintain equivalent quality in CBR
                bitRate = bitRate * 6 / 5;
            }
            // Make sure bitrate is quantized to valid values for CBR
            if (TranscodeScheme.fromMaxBitRate(bitRate) != null) {
                bitRate = TranscodeScheme.fromMaxBitRate(bitRate).getMaxBitRate();
            }
        }
        return bitRate;
    }

    private int createMaxBitrate(@NonNull TranscodeScheme transcodeScheme, @NonNull MediaFile mediaFile, int bitRate) {
        final int mb = mediaFile.isVideo() ? VideoPlayerController.DEFAULT_BIT_RATE : transcodeScheme.getMaxBitRate();
        if (mb == 0 || bitRate != 0 && bitRate < mb) {
            return bitRate;
        }
        return mb;
    }

    private boolean isNeedTranscoding(@Nullable Transcoding transcoding, int mb, int bitRate,
            @Nullable String preferredTargetFormat, @NonNull MediaFile mediaFile) {
        boolean isNeedTranscoding = false;
        if (transcoding != null && (mb != 0 && (bitRate == 0 || bitRate > mb)
                || preferredTargetFormat != null && !mediaFile.getFormat().equalsIgnoreCase(preferredTargetFormat))) {
            isNeedTranscoding = true;
        }
        return isNeedTranscoding;
    }

    private boolean isRangeAllowed(@NonNull Parameters parameters) {
        Transcoding transcoding = parameters.getTranscoding();
        List<String> steps;
        if (transcoding == null) {
            return true; // not transcoding
        } else {
            steps = Arrays.asList(transcoding.getStep3(), transcoding.getStep2(), transcoding.getStep1());
        }

        // Verify that were able to predict the length
        if (parameters.getExpectedLength() == null) {
            return false;
        }

        // Check if last configured step uses the bitrate, if so, range should be pretty safe
        for (String step : steps) {
            if (step != null) {
                return step.contains("%b");
            }
        }
        return false;
    }

    /**
     * Returns the length (or predicted/expected length) of a (possibly padded) media stream
     */
    private @Nullable Long getExpectedLength(@NonNull Parameters parameters) {

        MediaFile file = parameters.getMediaFile();
        if (!parameters.isTranscode()) {
            return file.getFileSize();
        }

        Integer duration = file.getDurationSeconds();
        if (duration == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Unknown duration for " + file + ". Unable to estimate transcoded size.");
            }
            return null;
        }

        Integer maxBitRate = parameters.getMaxBitRate();
        if (maxBitRate == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unknown bit rate for " + file + ". Unable to estimate transcoded size.");
            }
            return null;
        }

        // Over-estimate size a bit (2 seconds) so don't cut off early in case of small calculation differences
        return (duration + 2) * (long) maxBitRate * 1000L / 8L;
    }

    public static class Parameters {
        private Long expectedLength;
        private boolean rangeAllowed;
        private final MediaFile mediaFile;
        private final VideoTranscodingSettings videoTranscodingSettings;
        private Integer maxBitRate;
        private Transcoding transcoding;

        public Parameters(MediaFile mediaFile, VideoTranscodingSettings videoTranscodingSettings) {
            this.mediaFile = mediaFile;
            this.videoTranscodingSettings = videoTranscodingSettings;
        }

        public void setMaxBitRate(Integer maxBitRate) {
            this.maxBitRate = maxBitRate;
        }

        public boolean isTranscode() {
            return transcoding != null;
        }

        public boolean isRangeAllowed() {
            return this.rangeAllowed;
        }

        public void setRangeAllowed(boolean rangeAllowed) {
            this.rangeAllowed = rangeAllowed;
        }

        public Long getExpectedLength() {
            return this.expectedLength;
        }

        public void setExpectedLength(Long expectedLength) {
            this.expectedLength = expectedLength;
        }

        public void setTranscoding(Transcoding transcoding) {
            this.transcoding = transcoding;
        }

        public Transcoding getTranscoding() {
            return transcoding;
        }

        public MediaFile getMediaFile() {
            return mediaFile;
        }

        public Integer getMaxBitRate() {
            return maxBitRate;
        }

        public VideoTranscodingSettings getVideoTranscodingSettings() {
            return videoTranscodingSettings;
        }
    }
}
