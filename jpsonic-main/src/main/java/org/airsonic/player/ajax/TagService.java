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

package org.airsonic.player.ajax;

import java.util.concurrent.CompletionException;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for editing tags in music files. This class is used by the DWR framework
 * (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxTagService")
public class TagService {

    private static final Logger LOG = LoggerFactory.getLogger(TagService.class);

    private final MetaDataParserFactory metaDataParserFactory;
    private final MediaFileService mediaFileService;

    public TagService(MetaDataParserFactory metaDataParserFactory, MediaFileService mediaFileService) {
        super();
        this.metaDataParserFactory = metaDataParserFactory;
        this.mediaFileService = mediaFileService;
    }

    /**
     * Updated tags for a given music file.
     *
     * @param id
     *            The ID of the music file.
     * @param trackStr
     *            The track number.
     * @param artistStr
     *            The artist name.
     * @param albumStr
     *            The album name.
     * @param titleStr
     *            The song title.
     * @param yearStr
     *            The release year.
     * @param genreStr
     *            The musical genre.
     * 
     * @return "UPDATED" if the new tags were updated, "SKIPPED" if no update was necessary. Otherwise the error message
     *         is returned.
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI") // Because it's ajax API
    public String updateTags(int id, String trackStr, String artistStr, String albumStr, String titleStr,
            String yearStr, String genreStr) {

        MediaFile file = mediaFileService.getMediaFile(id);
        MetaDataParser parser = metaDataParserFactory.getParser(file.getFile());
        if (!parser.isEditingSupported()) {
            return "Tag editing of " + FilenameUtils.getExtension(file.getPath()) + " files is not supported.";
        }

        String artist = StringUtils.trimToNull(artistStr);
        String album = StringUtils.trimToNull(albumStr);
        String title = StringUtils.trimToNull(titleStr);
        String genre = StringUtils.trimToNull(genreStr);
        String track = StringUtils.trimToNull(trackStr);
        Integer trackNumber = getTrackNumber(track);
        String year = StringUtils.trimToNull(yearStr);
        Integer yearNumber = getYearNumber(year);
        if (StringUtils.equals(artist, file.getArtist()) && StringUtils.equals(album, file.getAlbumName())
                && StringUtils.equals(title, file.getTitle()) && ObjectUtils.equals(yearNumber, file.getYear())
                && StringUtils.equals(genre, file.getGenre())
                && ObjectUtils.equals(trackNumber, file.getTrackNumber())) {
            return "SKIPPED";
        }

        MetaData newMetaData = parser.getMetaData(file.getFile());

        // Note: album artist is intentionally set, as it is not user-changeable.
        newMetaData.setArtist(artist);
        newMetaData.setAlbumName(album);
        newMetaData.setTitle(title);
        newMetaData.setYear(yearNumber);
        newMetaData.setGenre(genre);
        newMetaData.setTrackNumber(trackNumber);

        try {
            parser.setMetaData(file, newMetaData);
        } catch (CompletionException | IllegalArgumentException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to update tags for " + id, e);
            }
            return e.getMessage();
        }
        mediaFileService.refreshMediaFile(file);
        file = mediaFileService.getMediaFile(file.getId());
        mediaFileService.refreshMediaFile(mediaFileService.getParentOf(file));
        return "UPDATED";
    }

    private Integer getTrackNumber(String track) {
        Integer trackNumber = null;
        if (track != null) {
            try {
                trackNumber = Integer.valueOf(track);
            } catch (NumberFormatException x) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Illegal track number: " + track, x);
                }
            }
        }
        return trackNumber;
    }

    private Integer getYearNumber(String year) {
        Integer yearNumber = null;
        if (year != null) {
            try {
                yearNumber = Integer.valueOf(year);
            } catch (NumberFormatException x) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Illegal year: " + year, x);
                }
            }
        }
        return yearNumber;
    }
}
