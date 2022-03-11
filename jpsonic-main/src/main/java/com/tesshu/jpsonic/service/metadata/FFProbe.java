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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.metadata;

import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.createSimplePath;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.getFolder;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseInt;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseTrackNumber;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseYear;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.TranscodingService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("PMD.TooManyStaticImports")
@Component
public class FFProbe {

    private static final Logger LOG = LoggerFactory.getLogger(FFProbe.class);

    private static final String[] FFPROBE_OPTIONS = { "-v", "quiet", "-show_format", "-show_streams", "-print_format",
            "json" };

    private static final String CODEC_TYPE_VIDEO = "video";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TranscodingService transcodingService;

    public FFProbe(TranscodingService transcodingService) {
        super();
        this.transcodingService = transcodingService;
    }

    enum FFmpegFieldKey {

        ALBUM_ARTIST("album_artist"), ALBUM("album"), ARTIST("artist"), DISC_NO("disc"), GENRE("genre"),
        MUSICBRAINZ_TRACK_ID("musicbrainz_trackid"), MUSICBRAINZ_RELEASEID("MusicBrainz Album Id"), TITLE("title"),
        TRACK("track"), YEAR("date"), ARTIST_SORT("sort_artist"), ALBUM_SORT("sort_album"), TITLE_SORT("sort_name"),
        ALBUM_ARTIST_SORT("sort_album_artist"), COMPOSER("composer"), COMPOSER_SORT("sort_composer");

        private final String value;

        FFmpegFieldKey(String n) {
            this.value = n;
        }
    }

    private Optional<String> getField(JsonNode tags, FFmpegFieldKey fieldKey) {
        JsonNode node = tags.get(fieldKey.value);
        if (isEmpty(node)) {
            return Optional.empty();
        }
        return Optional.ofNullable(trimToNull(tags.get(fieldKey.value).asText()));
    }

    private String getCommandPath() {
        String cmdPath = null;
        File cmdFile = new File(transcodingService.getTranscodeDirectory(), "ffprobe");
        if (cmdFile.exists()) {
            cmdPath = cmdFile.getAbsolutePath();
        } else {
            File winffprobe = new File(transcodingService.getTranscodeDirectory(), "ffprobe.exe");
            if (winffprobe.exists()) {
                cmdPath = winffprobe.getAbsolutePath();
            }
        }
        return cmdPath;
    }

    private MetaData parse(@NonNull JsonNode node, @NonNull MetaData result) {

        // ### streams
        for (JsonNode stream : node.at("/streams")) {
            String codec = stream.get("codec_type").asText();
            if (CODEC_TYPE_VIDEO.equals(codec) && stream.has("width") && stream.has("height")) {
                result.setWidth(parseInt(stream.get("width").asText()));
                result.setHeight(parseInt(stream.get("height").asText()));
                break;
            }
        }

        // ### format
        JsonNode format = node.at("/format");
        if (isEmpty(format)) {
            return result;
        }
        Optional.ofNullable(format.at("/duration")).ifPresent(duration -> {
            int value = duration.asInt();
            if (value != 0) {
                result.setDurationSeconds(value);
            }
        });
        Optional.ofNullable(format.at("/bit_rate")).ifPresent(bitRate -> {
            int value = bitRate.asInt();
            if (value != 0) {
                result.setBitRate(value / 1000);
            }
        });

        // ### format/tags
        JsonNode tags = format.at("/tags");
        if (isEmpty(tags)) {
            return result;
        }
        getField(tags, FFmpegFieldKey.ALBUM_ARTIST).ifPresent(s -> result.setAlbumArtist(s));
        getField(tags, FFmpegFieldKey.ALBUM).ifPresent(s -> result.setAlbumName(s));
        getField(tags, FFmpegFieldKey.ARTIST).ifPresent(s -> result.setArtist(s));
        getField(tags, FFmpegFieldKey.DISC_NO).ifPresent(s -> result.setDiscNumber(parseInt(s)));
        getField(tags, FFmpegFieldKey.GENRE).ifPresent(s -> result.setGenre(s));
        getField(tags, FFmpegFieldKey.TITLE).ifPresent(s -> result.setTitle(s));
        getField(tags, FFmpegFieldKey.TRACK).ifPresent(s -> result.setTrackNumber(parseTrackNumber(s)));
        getField(tags, FFmpegFieldKey.YEAR).ifPresent(s -> result.setYear(parseYear(s)));
        getField(tags, FFmpegFieldKey.COMPOSER).ifPresent(s -> result.setComposer(s));

        return result;
    }

    @SafeVarargs
    final MetaData parse(@NonNull File file, Consumer<Long>... startTimeCallback) {

        MetaData result = new MetaData();
        String cmdPath = getCommandPath();
        if (isEmpty(cmdPath)) {
            return result;
        }

        List<String> command = new ArrayList<>();
        command.add(cmdPath);
        command.addAll(Arrays.asList(FFPROBE_OPTIONS));
        command.add(file.getAbsolutePath());

        long start;
        JsonNode node;
        try {
            start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command).start();
            try (InputStream is = process.getInputStream(); BufferedInputStream bis = new BufferedInputStream(is);) {
                node = MAPPER.readTree(bis);
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            // Exceptions to this class are self-explanatory, avoiding redundant trace output
            String simplePath = createSimplePath(file);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to execute ffprobe({}): {}", simplePath, e.getMessage());
            }
            return result;
        }

        Stream.of(startTimeCallback).forEach(c -> c.accept(start));

        return parse(node, result);
    }

    MetaData parse(@NonNull MediaFile mediaFile, @Nullable Map<String, MP4ParseStatistics> statistics) {
        return parse(mediaFile.getFile(), (start) -> {
            if (!isEmpty(statistics) && statistics.containsKey(getFolder(mediaFile))) {
                long readtime = System.currentTimeMillis() - start;
                statistics.get(getFolder(mediaFile)).addCmdLeadTime(readtime);
            }
        });
    }
}
