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

import static com.tesshu.jpsonic.service.metadata.ParserUtils.createSimplePath;
import static com.tesshu.jpsonic.service.metadata.ParserUtils.getFolder;
import static com.tesshu.jpsonic.service.metadata.ParserUtils.parseDoubleToInt;
import static com.tesshu.jpsonic.service.metadata.ParserUtils.parseInt;
import static com.tesshu.jpsonic.service.metadata.ParserUtils.parseTrackNumber;
import static com.tesshu.jpsonic.service.metadata.ParserUtils.parseYear;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import com.tesshu.jpsonic.domain.MediaFile;
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

@SuppressWarnings("PMD.TooManyStaticImports")
public class MP4Parser {

    private static final Logger LOG = LoggerFactory.getLogger(MP4Parser.class);

    private final FFProbe ffProbe;

    private final org.apache.tika.parser.mp4.MP4Parser tikaParser;

    public MP4Parser(FFProbe ffProbe) {
        super();
        this.ffProbe = ffProbe;
        tikaParser = new org.apache.tika.parser.mp4.MP4Parser();
    }

    long getThreshold(@NonNull MediaFile mediaFile, Map<String, @NonNull MP4ParseStatistics> statistics) {
        String folder = getFolder(mediaFile);
        if (!statistics.containsKey(folder)) {
            statistics.putIfAbsent(folder, new MP4ParseStatistics());
        }
        return statistics.get(folder).getThreshold();
    }

    private Optional<String> getField(Metadata metadata, String fieldKey) {
        return Optional.ofNullable(trimToNull(metadata.get(fieldKey)));
    }

    MetaData parseWithFFProbe(@NonNull MediaFile mediaFile, @Nullable Map<String, MP4ParseStatistics> statistics) {
        return ffProbe.parse(mediaFile, statistics);
    }

    MetaData parseWithTika(MediaFile mediaFile, Map<String, MP4ParseStatistics> statistics) {

        MetaData result = new MetaData();

        long current;
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext ps = new ParseContext();
        try (InputStream is = Files.newInputStream(mediaFile.getFile().toPath());
                BufferedInputStream bid = new BufferedInputStream(is, 1_000_000);) {
            current = System.currentTimeMillis();
            tikaParser.parse(bid, handler, metadata, ps);
        } catch (IOException | SAXException | TikaException e) {
            String simplePath = createSimplePath(mediaFile.getFile());
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to parse the tag({}): {}", simplePath, e.getMessage());
            }
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
