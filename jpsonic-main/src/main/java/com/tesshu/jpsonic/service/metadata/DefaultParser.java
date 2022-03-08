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

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Parses meta data by guessing artist, album and song title based on the path of the file.
 *
 * @author Sindre Mehus
 */
@Service
@Order(200)
public class DefaultParser extends MetaDataParser {

    private final MusicFolderService musicFolderService;

    public DefaultParser(MusicFolderService musicFolderService) {
        super();
        this.musicFolderService = musicFolderService;
    }

    /**
     * Parses meta data for the given file.
     *
     * @param file
     *            The file to parse.
     *
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {
        MetaData metaData = new MetaData();
        String artist = guessArtist(file);
        metaData.setArtist(artist);
        metaData.setAlbumArtist(artist);
        metaData.setAlbumName(guessAlbum(file, artist));
        metaData.setTitle(guessTitle(file));
        return metaData;
    }

    /**
     * Updates the given file with the given meta data. This method has no effect.
     *
     * @param file
     *            The file to update.
     * @param metaData
     *            The new meta data.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {
        // Nothing is currently done. It seems that it is only implemented in JaudiotaggerParser...
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    @Override
    public boolean isEditingSupported(File file) {
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
    @Override
    public boolean isApplicable(File file) {
        return file.isFile();
    }
}
