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

import static com.tesshu.jpsonic.util.FileUtil.getShortPath;
import static com.tesshu.jpsonic.util.PlayerUtils.OBJECT_MAPPER;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FFprobe {

    private static final Logger LOG = LoggerFactory.getLogger(FFprobe.class);

    private static final String[] FFPROBE_OPTIONS = { "-v", "quiet", "-show_format",
            "-show_streams", "-print_format", "json" };

    private static final String CODEC_TYPE_VIDEO = "video";

    private final TranscodingService transcodingService;

    public FFprobe(TranscodingService transcodingService) {
        super();
        this.transcodingService = transcodingService;
    }

    enum FFmpegFieldKey {

        ALBUM_ARTIST("album_artist"), ALBUM("album"), ARTIST("artist"), DISC_NO("disc"),
        GENRE("genre"), MUSICBRAINZ_TRACK_ID("musicbrainz_trackid"),
        MUSICBRAINZ_RELEASEID("MusicBrainz Album Id"), TITLE("title"), TRACK("track"), YEAR("date"),
        ARTIST_SORT("sort_artist"), ALBUM_SORT("sort_album"), TITLE_SORT("sort_name"),
        ALBUM_ARTIST_SORT("sort_album_artist"), COMPOSER("composer"),
        COMPOSER_SORT("sort_composer");

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

    private @Nullable String getCommandPath() {
        Path cmdFile = Path
            .of(transcodingService.getTranscodeDirectory().toString(),
                    PlayerUtils.isWindows() ? "ffprobe.exe" : "ffprobe");
        if (Files.exists(cmdFile)) {
            return cmdFile.toString();
        }
        return null;
    }

    private MetaData parse(@NonNull JsonNode node, @NonNull MetaData result) {

        // ### streams
        for (JsonNode stream : node.at("/streams")) {
            String codec = stream.get("codec_type").asText();
            if (CODEC_TYPE_VIDEO.equals(codec) && stream.has("width") && stream.has("height")) {
                result.setWidth(ParserUtils.parseInt(stream.get("width").asText()));
                result.setHeight(ParserUtils.parseInt(stream.get("height").asText()));
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
        getField(tags, FFmpegFieldKey.ALBUM_ARTIST).ifPresent(result::setAlbumArtist);
        getField(tags, FFmpegFieldKey.ALBUM).ifPresent(result::setAlbumName);
        getField(tags, FFmpegFieldKey.ARTIST).ifPresent(result::setArtist);
        getField(tags, FFmpegFieldKey.DISC_NO)
            .ifPresent(s -> result.setDiscNumber(ParserUtils.parseInt(s)));
        getField(tags, FFmpegFieldKey.GENRE).ifPresent(result::setGenre);
        getField(tags, FFmpegFieldKey.TITLE).ifPresent(result::setTitle);
        getField(tags, FFmpegFieldKey.TRACK)
            .ifPresent(s -> result.setTrackNumber(ParserUtils.parseTrackNumber(s)));
        getField(tags, FFmpegFieldKey.YEAR)
            .ifPresent(s -> result.setYear(ParserUtils.parseYear(s)));
        getField(tags, FFmpegFieldKey.COMPOSER).ifPresent(result::setComposer);

        return result;
    }

    @SafeVarargs
    final MetaData parse(@NonNull Path path, Consumer<Long>... startTimeCallback) {

        MetaData result = new MetaData();
        String cmdPath = getCommandPath();
        if (isEmpty(cmdPath)) {
            return result;
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(cmdPath);
        Stream.of(FFPROBE_OPTIONS).forEach(op -> pb.command().add(op));
        pb.command().add(path.toString());

        long start;
        JsonNode node;
        try {
            start = Instant.now().toEpochMilli();
            Process process = pb.start();
            try (InputStream is = process.getInputStream();
                    OutputStream os = process.getOutputStream();
                    InputStream es = process.getErrorStream();
                    BufferedInputStream bis = new BufferedInputStream(is);) {
                node = OBJECT_MAPPER.readTree(bis);
                os.close();
                es.close();
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            // Exceptions to this class are self-explanatory, avoiding redundant trace
            // output
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to execute ffprobe({}): {}", getShortPath(path), e.getMessage());
            }
            return result;
        }

        Stream.of(startTimeCallback).forEach(c -> c.accept(start));

        return parse(node, result);
    }

    MetaData parse(@NonNull MediaFile mediaFile,
            @Nullable Map<String, MP4ParseStatistics> statistics) {
        return parse(mediaFile.toPath(), (start) -> {
            if (!isEmpty(statistics) && statistics.containsKey(ParserUtils.getFolder(mediaFile))) {
                long readtime = Instant.now().toEpochMilli() - start;
                statistics.get(ParserUtils.getFolder(mediaFile)).addCmdLeadTime(readtime);
            }
        });
    }
}
