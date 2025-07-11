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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.logging.LogManager;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.wav.WavTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(0)
public class MusicParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(MusicParser.class);

    // @see AudioFileIO#prepareReadersAndWriters
    // MP4: with FFmpegParser
    // M4P: Not supported by ffmpeg
    // RA: Not published in document
    // RM: Not published in document
    private static final List<String> APPLICABLES = Arrays
        .asList(SupportedFileFormat.OGG.getFilesuffix(), //
                SupportedFileFormat.OGA.getFilesuffix(), //
                SupportedFileFormat.FLAC.getFilesuffix(), //
                SupportedFileFormat.MP3.getFilesuffix(), //
                SupportedFileFormat.M4A.getFilesuffix(), //
                SupportedFileFormat.M4B.getFilesuffix(), //
                SupportedFileFormat.WAV.getFilesuffix(), //
                SupportedFileFormat.WMA.getFilesuffix(), //
                SupportedFileFormat.AIF.getFilesuffix(), //
                SupportedFileFormat.AIFC.getFilesuffix(), //
                SupportedFileFormat.AIFF.getFilesuffix(), //
                SupportedFileFormat.DSF.getFilesuffix(), //
                SupportedFileFormat.DFF.getFilesuffix()); //

    private static final List<String> NOT_EDITABLES = Arrays
        .asList(SupportedFileFormat.DFF.getFilesuffix());

    private final MusicFolderService musicFolderService;

    public MusicParser(MusicFolderService musicFolderService) {
        super();
        this.musicFolderService = musicFolderService;
        try {
            LogManager.getLogManager().reset();
        } catch (SecurityException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to turn off logging from Jaudiotagger.", e);
            }
        }
    }

    private Optional<String> getField(AudioFile audioFile, Tag tag, FieldKey fieldKey) {
        try {
            return Optional.ofNullable(trimToNull(tag.getFirst(fieldKey)));
        } catch (KeyNotFoundException e) {
            if (LOG.isWarnEnabled()) {
                LOG
                    .warn("The tag could not be read due to an unexpected format. Please let the developer know: {}({}) in [{}]",
                            audioFile.getFile().getName(), tag.getClass(), fieldKey);
            }
            return Optional.empty();
        }
    }

    @Override
    public @NonNull MetaData getRawMetaData(Path path) {

        MetaData metaData = new MetaData();

        if (!isApplicable(path)) {
            return metaData;
        }

        AudioFile af;
        try {
            af = AudioFileIO.read(path.toFile());
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                | InvalidAudioFrameException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to read file: " + FileUtil.getShortPath(path), e);
            } else if (LOG.isWarnEnabled()) {
                LOG
                    .warn("Unable to read " + FileUtil.getShortPath(path) + ": [{}]",
                            e.getMessage().trim());
            }
            return metaData;
        }

        AudioHeader audioHeader = af.getAudioHeader();
        metaData.setVariableBitRate(audioHeader.isVariableBitRate());
        metaData.setBitRate((int) audioHeader.getBitRateAsNumber());
        metaData.setDurationSeconds(audioHeader.getTrackLength());

        Tag tag = af.getTag();
        if (isEmpty(tag)) {
            return metaData;
        } else if (tag instanceof WavTag wavTag && !wavTag.isExistingId3Tag()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Only ID3 chunk is supported: {}", FileUtil.getShortPath(path));
            }
            return metaData;
        }

        getField(af, tag, FieldKey.ALBUM_ARTIST).ifPresent(metaData::setAlbumArtist);
        getField(af, tag, FieldKey.ALBUM).ifPresent(metaData::setAlbumName);
        getField(af, tag, FieldKey.ARTIST).ifPresent(metaData::setArtist);
        getField(af, tag, FieldKey.DISC_NO)
            .ifPresent(s -> metaData.setDiscNumber(ParserUtils.parseInt(s)));
        getField(af, tag, FieldKey.GENRE)
            .ifPresent(s -> metaData.setGenre(ParserUtils.mapGenre(s)));
        getField(af, tag, FieldKey.MUSICBRAINZ_TRACK_ID)
            .ifPresent(metaData::setMusicBrainzRecordingId);
        getField(af, tag, FieldKey.MUSICBRAINZ_RELEASEID)
            .ifPresent(metaData::setMusicBrainzReleaseId);
        getField(af, tag, FieldKey.TITLE).ifPresent(metaData::setTitle);
        getField(af, tag, FieldKey.TRACK)
            .ifPresent(s -> metaData.setTrackNumber(ParserUtils.parseTrackNumber(s)));
        getField(af, tag, FieldKey.YEAR).ifPresent(s -> metaData.setYear(ParserUtils.parseYear(s)));
        getField(af, tag, FieldKey.ARTIST_SORT).ifPresent(metaData::setArtistSort);
        getField(af, tag, FieldKey.ALBUM_SORT).ifPresent(metaData::setAlbumSort);
        getField(af, tag, FieldKey.TITLE_SORT).ifPresent(metaData::setTitleSort);
        getField(af, tag, FieldKey.ALBUM_ARTIST_SORT).ifPresent(metaData::setAlbumArtistSort);
        getField(af, tag, FieldKey.COMPOSER).ifPresent(metaData::setComposer);
        getField(af, tag, FieldKey.COMPOSER_SORT).ifPresent(metaData::setComposerSort);

        if (isBlank(metaData.getArtist())) {
            metaData.setArtist(metaData.getAlbumArtist());
            metaData.setArtistSort(metaData.getAlbumArtistSort());
        }
        if (isBlank(metaData.getAlbumArtist())) {
            metaData.setAlbumArtist(metaData.getArtist());
            metaData.setAlbumArtistSort(metaData.getArtistSort());
        }

        return metaData;
    }

    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {

        try {
            AudioFile audioFile = AudioFileIO.read(file.toPath().toFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            tag.setField(FieldKey.ARTIST, trimToEmpty(metaData.getArtist()));
            tag.setField(FieldKey.ALBUM, trimToEmpty(metaData.getAlbumName()));
            tag.setField(FieldKey.TITLE, trimToEmpty(metaData.getTitle()));
            tag.setField(FieldKey.GENRE, trimToEmpty(metaData.getGenre()));

            String albumArtist = trimToEmpty(metaData.getAlbumArtist());
            if (!isEmpty(albumArtist)) {
                // Silently ignored. ID3v1 doesn't support album artist.
                tag.setField(FieldKey.ALBUM_ARTIST, albumArtist);
            }

            Integer track = metaData.getTrackNumber();
            if (track == null) {
                tag.deleteField(FieldKey.TRACK);
            } else {
                tag.setField(FieldKey.TRACK, String.valueOf(track));
            }

            Integer year = metaData.getYear();
            if (year == null) {
                tag.deleteField(FieldKey.YEAR);
            } else {
                tag.setField(FieldKey.YEAR, String.valueOf(year));
            }

            audioFile.commit();

        } catch (IOException | CannotWriteException | KeyNotFoundException | TagException
                | CannotReadException | ReadOnlyFileException | InvalidAudioFrameException e) {
            throw new CompletionException("Failed to update tags for file: " + file.getPathString(),
                    e);
        }
    }

    @Override
    public boolean isApplicable(Path path) {
        if (Files.isDirectory(path)) {
            return false;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String extension = FilenameUtils.getExtension(fileName.toString());
        return extension != null && APPLICABLES.contains(extension.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean isEditingSupported(Path path) {
        if (Files.isDirectory(path)) {
            return false;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String extension = FilenameUtils.getExtension(fileName.toString());
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase(Locale.ENGLISH);
        if (NOT_EDITABLES.contains(extension)) {
            return false;
        } else if (APPLICABLES.contains(extension)) {
            return true;
        }
        return false;
    }

    @Override
    protected MusicFolderService getMusicFolderService() {
        return musicFolderService;
    }
}
