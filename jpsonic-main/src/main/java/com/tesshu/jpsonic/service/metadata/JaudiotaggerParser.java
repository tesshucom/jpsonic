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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.GenreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Parses meta data from audio files using the Jaudiotagger library (http://www.jthink.net/jaudiotagger/)
 *
 * @author Sindre Mehus
 */
@Service
@Order(0)
public class JaudiotaggerParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(JaudiotaggerParser.class);
    private static final Pattern GENRE_PATTERN = Pattern.compile("\\((\\d+)\\).*");
    private static final Pattern TRACK_NUMBER_PATTERN = Pattern.compile("(\\d+)/\\d+");
    private static final Pattern YEAR_NUMBER_PATTERN = Pattern.compile("(\\d{4}).*");

    private final MusicFolderService musicFolderService;

    public JaudiotaggerParser(MusicFolderService musicFolderService) {
        super();
        this.musicFolderService = musicFolderService;
    }

    static {
        try {
            LogManager.getLogManager().reset();
        } catch (SecurityException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to turn off logging from Jaudiotagger.", e);
            }
        }
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     * @param file
     *            The music file to parse.
     * 
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {

        MetaData metaData = new MetaData();

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                metaData.setAlbumArtist(getTagField(tag, FieldKey.ALBUM_ARTIST));
                metaData.setAlbumName(getTagField(tag, FieldKey.ALBUM));
                metaData.setArtist(getTagField(tag, FieldKey.ARTIST));
                metaData.setDiscNumber(parseInteger(getTagField(tag, FieldKey.DISC_NO)));
                metaData.setGenre(mapGenre(getTagField(tag, FieldKey.GENRE)));
                metaData.setMusicBrainzRecordingId(getTagField(tag, FieldKey.MUSICBRAINZ_TRACK_ID));
                metaData.setMusicBrainzReleaseId(getTagField(tag, FieldKey.MUSICBRAINZ_RELEASEID));
                metaData.setTitle(getTagField(tag, FieldKey.TITLE));
                metaData.setTrackNumber(parseIntegerPattern(getTagField(tag, FieldKey.TRACK), TRACK_NUMBER_PATTERN));
                metaData.setYear(parseIntegerPattern(getTagField(tag, FieldKey.YEAR), YEAR_NUMBER_PATTERN));
                // JP >>>>
                metaData.setArtistSort(getTagField(tag, FieldKey.ARTIST_SORT));
                metaData.setAlbumSort(getTagField(tag, FieldKey.ALBUM_SORT));
                metaData.setTitleSort(getTagField(tag, FieldKey.TITLE_SORT));
                metaData.setAlbumArtistSort(getTagField(tag, FieldKey.ALBUM_ARTIST_SORT));
                metaData.setComposer(getTagField(tag, FieldKey.COMPOSER));
                metaData.setComposerSort(getTagField(tag, FieldKey.COMPOSER_SORT));
                // <<<< JP

                if (isBlank(metaData.getArtist())) {
                    metaData.setArtist(metaData.getAlbumArtist());
                    // JP >>>>
                    metaData.setArtistSort(metaData.getAlbumArtistSort());
                    // <<<< JP
                }
                if (isBlank(metaData.getAlbumArtist())) {
                    metaData.setAlbumArtist(metaData.getArtist());
                    // JP >>>>
                    metaData.setAlbumArtistSort(metaData.getArtistSort());
                    // <<<< JP
                }

            }

            AudioHeader audioHeader = audioFile.getAudioHeader();
            if (audioHeader != null) {
                metaData.setVariableBitRate(audioHeader.isVariableBitRate());
                metaData.setBitRate((int) audioHeader.getBitRateAsNumber());
                metaData.setDurationSeconds(audioHeader.getTrackLength());
            }

        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                | InvalidAudioFrameException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Error when parsing tags in " + file, e);
            }
        }

        return metaData;
    }

    private static String getTagField(Tag tag, FieldKey fieldKey) {
        try {
            return trimToNull(tag.getFirst(fieldKey));
        } catch (KeyNotFoundException x) {
            // Ignored.
            return null;
        }
    }

    /**
     * Returns all tags supported by id3v1.
     */
    public static SortedSet<String> getID3V1Genres() {
        return new TreeSet<>(GenreTypes.getInstanceOf().getAlphabeticalValueList());
    }

    /**
     * Sometimes the genre is returned as "(17)" or "(17)Rock", instead of "Rock". This method maps the genre ID to the
     * corresponding text.
     */
    private static String mapGenre(String genre) {
        if (genre == null) {
            return null;
        }
        Matcher matcher = GENRE_PATTERN.matcher(genre);
        if (matcher.matches()) {
            int genreId = Integer.parseInt(matcher.group(1));
            if (genreId >= 0 && genreId < GenreTypes.getInstanceOf().getSize()) {
                return GenreTypes.getInstanceOf().getValueForId(genreId);
            }
        }
        return genre;
    }

    private static Integer parseIntegerPattern(String str, Pattern pattern) {
        if (str == null) {
            return null;
        }

        Integer result = null;

        try {
            result = Integer.valueOf(str);
        } catch (NumberFormatException x) {
            if (pattern == null) {
                return null;
            }
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    private static Integer parseInteger(String s) {
        return parseIntegerPattern(trimToNull(s), null);
    }

    /**
     * Updates the given file with the given meta data.
     *
     * @param file
     *            The music file to update.
     * @param metaData
     *            The new meta data.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {

        try {
            AudioFile audioFile = AudioFileIO.read(file.getFile());
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

        } catch (IOException | CannotWriteException | KeyNotFoundException | TagException | CannotReadException
                | ReadOnlyFileException | InvalidAudioFrameException e) {
            throw new CompletionException("Failed to update tags for file: " + file.getPath(), e);
        }
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always true.
     */
    @Override
    public boolean isEditingSupported() {
        return true;
    }

    @Override
    protected MusicFolderService getMusicFolderService() {
        return musicFolderService;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file
     *            The music file in question.
     * 
     * @return Whether this parser is applicable to the given file.
     */
    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    /*
     * [UseLocaleWithCaseConversions] The locale doesn't matter, as only comparing the extension literal.
     */
    @Override
    public boolean isApplicable(File file) {
        if (!file.isFile()) {
            return false;
        }

        String format = FilenameUtils.getExtension(file.getName()).toLowerCase();

        return "mp3".equals(format) || "m4a".equals(format) || "m4b".equals(format) || "aac".equals(format)
                || "ogg".equals(format) || "flac".equals(format) || "wav".equals(format) || "mpc".equals(format)
                || "mp+".equals(format) || "ape".equals(format) || "wma".equals(format);
    }

    public static @Nullable Artwork getArtwork(MediaFile file) {
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(file.getFile());
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                | InvalidAudioFrameException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to find cover art tag in " + file, e);
            }
            return null;
        }
        Tag tag = audioFile.getTag();
        return tag == null ? null : tag.getFirstArtwork();
    }
}
