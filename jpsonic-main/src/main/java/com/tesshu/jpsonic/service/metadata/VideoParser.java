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
     * @param file
     *            The music file to parse.
     *
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(File file) {
        return ffprobe.parse(file);
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
    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    @Override
    public boolean isApplicable(File file) {
        String format = FilenameUtils.getExtension(file.getName()).toLowerCase();

        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (format.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
