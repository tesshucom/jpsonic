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

package com.tesshu.jpsonic.service.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Parses meta data from video files using FFmpeg (http://ffmpeg.org/).
 * <p/>
 * Currently duration, bitrate and dimension are supported.
 *
 * @author Sindre Mehus
 */
@Service("ffmpegParser")
@Order(100)
public class FFmpegParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String[] FFPROBE_OPTIONS = { "-v", "quiet", "-print_format", "json", "-show_format",
            "-show_streams" };

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final TranscodingService transcodingService;

    public FFmpegParser(SettingsService settingsService, MusicFolderService musicFolderService,
            TranscodingService transcodingService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.transcodingService = transcodingService;
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     *
     * @param file
     *            The music file to parse.
     * 
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {

        MetaData metaData = new MetaData();

        // Use `ffprobe` in the transcode directory if it exists, otherwise let the system sort it out.
        String ffprobe;
        File inTranscodeDirectory = new File(transcodingService.getTranscodeDirectory(), "ffprobe");
        if (inTranscodeDirectory.exists()) {
            ffprobe = inTranscodeDirectory.getAbsolutePath();
        } else {
            File winffprobe = new File(transcodingService.getTranscodeDirectory(), "ffprobe.exe");
            if (winffprobe.exists()) {
                ffprobe = winffprobe.getAbsolutePath();
            } else {
                ffprobe = "ffprobe";
            }
        }

        List<String> command = new ArrayList<>();
        command.add(ffprobe);
        command.addAll(Arrays.asList(FFPROBE_OPTIONS));
        command.add(file.getAbsolutePath());

        JsonNode result;
        try {
            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            result = OBJECT_MAPPER.readTree(process.getInputStream());
        } catch (IOException e) {
            if (settingsService.isVerboseLogScanning() && LOG.isWarnEnabled()) {
                /*
                 * It is relatively easy to grasp the situation for user, and when it occurs, it tends to be a large
                 * amount of logs. 1 line log and do not stack trace.
                 */
                LOG.warn("'" + ffprobe + "' execution error in " + file.getPath() + ": ", e.getMessage());
            }
            return metaData;
        }

        metaData.setDurationSeconds(result.at("/format/duration").asInt());
        // Bitrate is in Kb/s
        metaData.setBitRate(result.at("/format/bit_rate").asInt() / 1000);

        // Find the first (if any) stream that has dimensions and use those.
        // 'width' and 'height' are display dimensions; compare to 'coded_width', 'coded_height'.
        for (JsonNode stream : result.at("/streams")) {
            if (stream.has("width") && stream.has("height")) {
                metaData.setWidth(stream.get("width").asInt());
                metaData.setHeight(stream.get("height").asInt());
                break;
            }
        }

        return metaData;
    }

    /**
     * Not supported.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {
        throw new IllegalArgumentException("setMetaData() not supported in " + getClass().getSimpleName());
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    @Override
    public boolean isEditingSupported() {
        return false;
    }

    @Override
    protected MusicFolderService getMusicFolderService() {
        return musicFolderService;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file
     *            The file in question.
     * 
     * @return Whether this parser is applicable to the given file.
     */
    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    @Override
    public boolean isApplicable(File file) {
        String format = FilenameUtils.getExtension(file.getName()).toLowerCase();

        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (format.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
