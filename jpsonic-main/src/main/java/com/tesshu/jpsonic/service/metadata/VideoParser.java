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
import java.util.Locale;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Parses meta data from video files using FFProve.
 *
 * @author Sindre Mehus
 */
@Service("videoParser")
@Order(100)
public class VideoParser extends MetaDataParser {

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final FFprobe ffprobe;

    public VideoParser(SettingsService settingsService, MusicFolderService musicFolderService, FFprobe ffprobe) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.ffprobe = ffprobe;
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     *
     * @param path
     *            The music file to parse.
     *
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(Path path) {
        return ffprobe.parse(path);
    }

    /**
     * Not supported.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {
        throw new IllegalArgumentException("setMetaData() not supported in " + getClass().getSimpleName());
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    @Override
    public boolean isEditingSupported(Path path) {
        return false;
    }

    @Override
    protected MusicFolderService getMusicFolderService() {
        return musicFolderService;
    }

    @Override
    public boolean isApplicable(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Illegal path specified.");
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        String extension = FilenameUtils.getExtension(fileName.toString());
        if (extension.isEmpty()) {
            return false;
        }
        String format = extension.toLowerCase(Locale.ENGLISH);
        return settingsService.getVideoFileTypesAsArray().stream()
                .anyMatch(type -> format.equals(type.toLowerCase(Locale.ENGLISH)));
    }
}
