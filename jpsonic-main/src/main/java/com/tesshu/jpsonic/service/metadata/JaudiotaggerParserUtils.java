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

import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.domain.MediaFile;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.GenreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses meta data from audio files using the Jaudiotagger library (http://www.jthink.net/jaudiotagger/)
 *
 * @author Sindre Mehus
 */
public final class JaudiotaggerParserUtils {

    private static final Pattern GENRE_PATTERN = Pattern.compile("\\((\\d+)\\).*");
    private static final Pattern TRACK_NO_PATTERN = Pattern.compile("(\\d+)/\\d+");
    private static final Pattern YEAR_NO_PATTERN = Pattern.compile("(\\d{4}).*");
    private static final Logger LOG = LoggerFactory.getLogger(JaudiotaggerParserUtils.class);

    static {
        try {
            LogManager.getLogManager().reset();
        } catch (SecurityException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to turn off logging from Jaudiotagger.", e);
            }
        }
    }

    private JaudiotaggerParserUtils() {
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
    static String mapGenre(String genre) {
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

    private static Integer parseInt(String str, Pattern pattern) {
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

    static Integer parseInt(String s) {
        return parseInt(trimToNull(s), null);
    }

    static Integer parseTrackNumber(String s) {
        return parseInt(trimToNull(s), TRACK_NO_PATTERN);
    }

    static Integer parseYear(String s) {
        return parseInt(trimToNull(s), YEAR_NO_PATTERN);
    }

    static String createSimplePath(File file) {
        return file.getParentFile().getName().concat("/").concat(file.getName());
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
