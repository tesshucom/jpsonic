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

import static com.tesshu.jpsonic.infrastructure.filesystem.PathInspector.toIdentityName;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class FFprobe {

    private static final Logger LOG = LoggerFactory.getLogger(FFprobe.class);
    private static final String[] FFPROBE_OPTIONS = { "-v", "quiet", "-show_format",
            "-show_streams", "-print_format", "json" };
    private static final String CODEC_TYPE_VIDEO = "video";

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

    private final ObjectMapper objectMapper;

    public FFprobe(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    private Optional<String> getField(JsonNode tags, FFmpegFieldKey fieldKey) {
        JsonNode node = tags.get(fieldKey.value);
        if (isEmpty(node)) {
            return Optional.empty();
        }
        return Optional.ofNullable(trimToNull(tags.get(fieldKey.value).asString()));
    }

    private MetaData parse(@NonNull JsonNode node, @NonNull MetaData result) {

        // ### streams
        for (JsonNode stream : node.at("/streams")) {
            String codec = stream.get("codec_type").asString();
            if (CODEC_TYPE_VIDEO.equals(codec) && stream.has("width") && stream.has("height")) {
                result.setWidth(ParserUtils.parseInt(stream.get("width").asString()));
                result.setHeight(ParserUtils.parseInt(stream.get("height").asString()));
                break;
            }
        }

        // ### format
        JsonNode format = node.at("/format");
        if (isEmpty(format)) {
            return result;
        }

        JsonNode durationNode = format.at("/duration");
        if (!isEmpty(durationNode) && durationNode.isValueNode()) {
            double value = durationNode.asDouble();
            int rounded = (int) Math.floor(value);
            if (rounded != 0) {
                result.setDurationSeconds(rounded);
            }
        }

        JsonNode bitRateNode = format.at("/bit_rate");
        if (!isEmpty(bitRateNode) && bitRateNode.isValueNode()) {
            double value = bitRateNode.asDouble();
            int rounded = (int) Math.floor(value / 1000.0);
            if (rounded != 0) {
                result.setBitRate(rounded);
            }
        }

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

        final MetaData result = new MetaData();

        ProcessBuilder pb = new ProcessBuilder();
        pb
            .command()
            .add(EnvironmentProvider.getInstance().getFfprobePath().toAbsolutePath().toString());
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
                node = objectMapper.readTree(bis);
                os.close();
                es.close();
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            // Exceptions to this class are self-explanatory, avoiding redundant trace
            // output
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to execute ffprobe({}): {}", toIdentityName(path), e.getMessage());
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
