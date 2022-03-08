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
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseDoubleToInt;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseInt;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseTrackNumber;
import static com.tesshu.jpsonic.service.metadata.JaudiotaggerParserUtils.parseYear;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.TranscodingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.GuardLogStatement" })
public class MP4Parser {

    private static final Logger LOG = LoggerFactory.getLogger(MP4Parser.class);

    private static final String[] FFPROBE_OPTIONS = { "-v", "quiet", "-show_format", "-show_streams", "-print_format",
            "json" };

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TranscodingService transcodingService;

    public MP4Parser(TranscodingService transcodingService) {
        super();
        this.transcodingService = transcodingService;
    }

    private String getFolder(MediaFile mediaFile) {
        return StringUtils.defaultIfBlank(mediaFile.getFolder(), "root");
    }

    long getThreshold(@NonNull MediaFile mediaFile, Map<String, @NonNull MP4ParseStatistics> statistics) {
        String folder = getFolder(mediaFile);
        if (!statistics.containsKey(folder)) {
            statistics.putIfAbsent(folder, new MP4ParseStatistics());
        }
        return statistics.get(folder).getThreshold();
    }

    private Optional<String> getField(JsonNode tags, FFProbeFieldKey fieldKey) {
        JsonNode node = tags.get(fieldKey.value);
        if (isEmpty(node)) {
            return Optional.empty();
        }
        return Optional.ofNullable(trimToNull(tags.get(fieldKey.value).asText()));
    }

    private Optional<String> getField(Metadata metadata, String fieldKey) {
        return Optional.ofNullable(trimToNull(metadata.get(fieldKey)));
    }

    public enum FFProbeFieldKey {

        ALBUM_ARTIST("album_artist"), ALBUM("album"), ARTIST("artist"), DISC_NO("disc"), GENRE("genre"),
        MUSICBRAINZ_TRACK_ID("musicbrainz_trackid"), MUSICBRAINZ_RELEASEID("MusicBrainz Album Id"), TITLE("title"),
        TRACK("track"), YEAR("date"), ARTIST_SORT("sort_artist"), ALBUM_SORT("sort_album"), TITLE_SORT("sort_name"),
        ALBUM_ARTIST_SORT("sort_album_artist"), COMPOSER("composer"), COMPOSER_SORT("sort_composer");

        private final String value;

        FFProbeFieldKey(String n) {
            this.value = n;
        }
    }

    // Will be fixed to be later transferred to an outer class
    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
    MetaData parseWithFFProbe(@NonNull MediaFile mediaFile, @Nullable Map<String, MP4ParseStatistics> statistics) {

        MetaData result = new MetaData();

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
        if (isEmpty(cmdPath)) {
            return result;
        }

        List<String> command = new ArrayList<>();
        command.add(cmdPath);
        command.addAll(Arrays.asList(FFPROBE_OPTIONS));
        command.add(mediaFile.getFile().getAbsolutePath());

        long current;
        Process process;
        try {
            current = System.currentTimeMillis();
            process = Runtime.getRuntime().exec(command.toArray(new String[0]));
        } catch (IOException e) {
            // Exceptions to this class are self-explanatory, avoiding redundant trace output
            String simplePath = createSimplePath(mediaFile.getFile());
            LOG.warn("Failed to execute ffprobe({}): {}", simplePath, e.getMessage());
            return result;
        }

        JsonNode node;
        try (InputStream is = process.getInputStream(); BufferedInputStream bis = new BufferedInputStream(is);) {
            node = MAPPER.readTree(bis);
        } catch (IOException e) {
            String simplePath = createSimplePath(mediaFile.getFile());
            LOG.warn("Failed to parse the tag({}): {}", simplePath, e.getMessage());
            return result;
        }

        if (!isEmpty(statistics)) {
            long readtime = System.currentTimeMillis() - current;
            statistics.get(getFolder(mediaFile)).addCmdLeadTime(readtime);
        }

        // ### streams
        for (JsonNode stream : node.at("/streams")) {
            String codec = stream.get("codec_type").asText();
            if ("video".equals(codec)) {
                if (stream.has("width") && stream.has("height")) {
                    result.setWidth(stream.get("width").asInt());
                    result.setHeight(stream.get("height").asInt());
                }
                break;
            }
        }

        // ### format
        JsonNode format = node.at("/format");
        if (isEmpty(format)) {
            return result;
        }
        Optional.ofNullable(format.at("/duration")).ifPresent(duration -> result.setDurationSeconds(duration.asInt()));
        Optional.ofNullable(format.at("/bit_rate")).ifPresent(bitRate -> result.setBitRate(bitRate.asInt() / 1000));

        // ### format/tags
        JsonNode tags = format.at("/tags");
        if (isEmpty(tags)) {
            return result;
        }
        getField(tags, FFProbeFieldKey.ALBUM_ARTIST).ifPresent(s -> result.setAlbumArtist(s));
        getField(tags, FFProbeFieldKey.ALBUM).ifPresent(s -> result.setAlbumName(s));
        getField(tags, FFProbeFieldKey.ARTIST).ifPresent(s -> result.setArtist(s));
        getField(tags, FFProbeFieldKey.DISC_NO).ifPresent(s -> result.setDiscNumber(parseInt(s)));
        getField(tags, FFProbeFieldKey.GENRE).ifPresent(s -> result.setGenre(s));
        getField(tags, FFProbeFieldKey.TITLE).ifPresent(s -> result.setTitle(s));
        getField(tags, FFProbeFieldKey.TRACK).ifPresent(s -> result.setTrackNumber(parseTrackNumber(s)));
        getField(tags, FFProbeFieldKey.YEAR).ifPresent(s -> result.setYear(parseYear(s)));
        getField(tags, FFProbeFieldKey.COMPOSER).ifPresent(s -> result.setComposer(s));

        return result;
    }

    MetaData parseWithTika(MediaFile mediaFile, Map<String, MP4ParseStatistics> statistics) {

        MetaData result = new MetaData();
        org.apache.tika.parser.mp4.MP4Parser parser = new org.apache.tika.parser.mp4.MP4Parser();

        long current;
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext ps = new ParseContext();
        try (InputStream is = Files.newInputStream(mediaFile.getFile().toPath());
                BufferedInputStream bid = new BufferedInputStream(is, 1_000_000);) {
            current = System.currentTimeMillis();
            parser.parse(bid, handler, metadata, ps);
        } catch (IOException | SAXException | TikaException e) {
            String simplePath = createSimplePath(mediaFile.getFile());
            LOG.warn("Failed to parse the tag({}): {}", simplePath, e.getMessage());
            return result;
        }

        long readtime = System.currentTimeMillis() - current;
        statistics.get(getFolder(mediaFile)).addTikaLeadTime(mediaFile.getFileSize(), readtime);

        getField(metadata, TIFF.IMAGE_LENGTH.getName()).ifPresent(s -> result.setHeight(parseInt(s)));
        getField(metadata, TIFF.IMAGE_WIDTH.getName()).ifPresent(s -> result.setWidth(parseInt(s)));
        getField(metadata, XMPDM.DURATION.getName()).ifPresent(s -> result.setDurationSeconds(parseDoubleToInt(s)));
        getField(metadata, XMPDM.ALBUM_ARTIST.getName()).ifPresent(s -> result.setAlbumArtist(s));
        getField(metadata, XMPDM.ALBUM.getName()).ifPresent(s -> result.setAlbumName(s));
        getField(metadata, XMPDM.ARTIST.getName()).ifPresent(s -> result.setArtist(s));
        if (isEmpty(result.getArtist())) {
            getField(metadata, DublinCore.CREATOR.getName()).ifPresent(s -> result.setArtist(s));
        }
        getField(metadata, XMPDM.DISC_NUMBER.getName()).ifPresent(s -> result.setDiscNumber(parseInt(s)));
        getField(metadata, XMPDM.GENRE.getName()).ifPresent(s -> result.setGenre(s));
        getField(metadata, DublinCore.TITLE.getName()).ifPresent(s -> result.setTitle(s));
        getField(metadata, XMPDM.TRACK_NUMBER.getName()).ifPresent(s -> result.setTrackNumber(parseTrackNumber(s)));
        getField(metadata, XMPDM.RELEASE_DATE.getName()).ifPresent(s -> result.setYear(parseYear(s)));
        getField(metadata, XMPDM.COMPOSER.getName()).ifPresent(s -> result.setComposer(s));

        return result;
    }

    public MetaData getRawMetaData(@NonNull MediaFile mediaFile) {
        return parseWithFFProbe(mediaFile, null);
    }

    public MetaData getRawMetaData(@NonNull MediaFile mediaFile, @NonNull Map<String, MP4ParseStatistics> statistics) {
        if (mediaFile.getFileSize() > getThreshold(mediaFile, statistics)) {
            return parseWithFFProbe(mediaFile, statistics);
        }
        return parseWithTika(mediaFile, statistics);
    }
}
