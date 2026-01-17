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

import java.nio.file.Path;
import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Parses meta data from media files.
 *
 * @author Sindre Mehus
 */
public abstract class MetaDataParser {

    /**
     * Parses meta data for the given file.
     *
     * @param path The file to parse.
     *
     * @return Meta data for the file, never null.
     */
    public MetaData getMetaData(Path path) {

        MetaData metaData = getRawMetaData(path);

        String artist = metaData.getArtist();
        if (artist == null) {
            artist = guessArtist(path);
        }
        String albumArtist = metaData.getAlbumArtist();
        if (albumArtist == null) {
            albumArtist = guessArtist(path);
        }
        String album = metaData.getAlbumName();
        if (album == null) {
            album = guessAlbum(path, artist);
        }
        String title = metaData.getTitle();
        if (title == null) {
            title = removeTrackNumberFromTitle(guessTitle(path), metaData.getTrackNumber());
        }

        metaData.setArtist(artist);
        metaData.setAlbumArtist(albumArtist);
        metaData.setAlbumName(album);
        metaData.setTitle(title);

        return metaData;
    }

    /**
     * Parses meta data for the given file. No guessing or reformatting is done.
     *
     * @param path The file to parse.
     *
     * @return Meta data for the file.
     */
    public abstract MetaData getRawMetaData(Path path);

    /**
     * Updates the given file with the given meta data.
     *
     * @param file     The file to update.
     * @param metaData The new meta data.
     */
    public abstract void setMetaData(MediaFile file, MetaData metaData);

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param path The file in question.
     *
     * @return Whether this parser is applicable to the given file.
     */
    public abstract boolean isApplicable(@NonNull Path path);

    /**
     * Returns whether this parser supports tag editing (using the
     * {@link #setMetaData} method).
     *
     * @return Whether tag editing is supported.
     */
    public abstract boolean isEditingSupported(Path path);

    /**
     * Guesses the artist for the given file.
     */
    protected final String guessArtist(@NonNull Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        if (isRoot(parent)) {
            return null;
        }
        Path grandParent = parent.getParent();
        if (grandParent == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        Path grandParentFilename = grandParent.getFileName();
        if (grandParentFilename == null) {
            return "";
        }
        return isRoot(grandParent) ? null : grandParentFilename.toString();
    }

    /**
     * Guesses the album for the given file.
     */
    protected final String guessAlbum(@NonNull Path path, String artist) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        Path parentFileName = parent.getFileName();
        if (parentFileName == null) {
            return "";
        }

        String album = isRoot(parent) ? null : parentFileName.toString();
        if (artist != null && album != null) {
            album = album.replace(artist + " - ", "");
        }
        return album;
    }

    /**
     * Guesses the title for the given file.
     */
    public String guessTitle(@NonNull Path path) {
        return StringUtils.trim(FilenameUtils.getBaseName(path.toString()));
    }

    protected boolean isRoot(Path path) {
        List<MusicFolder> folders = getMusicFolderService().getAllMusicFolders(false, true);
        for (MusicFolder folder : folders) {
            if (path.equals(folder.toPath())) {
                return true;
            }
        }
        return false;
    }

    protected abstract MusicFolderService getMusicFolderService();

    /**
     * Removes any prefixed track number from the given title string.
     *
     * @param title       The title with or without a prefixed track number, e.g.,
     *                    "02 - Back In Black".
     * @param trackNumber If specified, this is the "true" track number.
     *
     * @return The title with the track number removed, e.g., "Back In Black".
     */
    protected String removeTrackNumberFromTitle(final String title, Integer trackNumber) {
        String result = title.trim();

        // Don't remove numbers if true track number is missing, or if title does not
        // start with it.
        if (trackNumber == null || !result.matches("0?" + trackNumber + "[. -].*")) {
            return result;
        }
        result = result.replaceFirst("^\\d{2}[. -]+", "");
        return result.isEmpty() ? title : result;
    }
}
