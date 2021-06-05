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

package com.tesshu.jpsonic.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.util.FileUtil;
import org.springframework.stereotype.Service;

/**
 * Provides services for user ratings.
 *
 * @author Sindre Mehus
 */
@Service
public class RatingService {

    private final RatingDao ratingDao;
    private final SecurityService securityService;
    private final MediaFileService mediaFileService;

    public RatingService(RatingDao ratingDao, SecurityService securityService, MediaFileService mediaFileService) {
        super();
        this.ratingDao = ratingDao;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
    }

    /**
     * Returns the highest rated albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     * 
     * @return The highest rated albums.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File) Not reusable
    public List<MediaFile> getHighestRatedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        List<String> highestRated = ratingDao.getHighestRatedAlbums(offset, count, musicFolders);
        List<MediaFile> result = new ArrayList<>();
        for (String path : highestRated) {
            File file = new File(path);
            if (FileUtil.exists(file) && securityService.isReadAllowed(file)) {
                result.add(mediaFileService.getMediaFile(path));
            }
        }
        return result;
    }

    /**
     * Sets the rating for a music file and a given user.
     *
     * @param username
     *            The user name.
     * @param mediaFile
     *            The music file.
     * @param rating
     *            The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        ratingDao.setRatingForUser(username, mediaFile, rating);
    }

    /**
     * Returns the average rating for the given music file.
     *
     * @param mediaFile
     *            The music file.
     * 
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        return ratingDao.getAverageRating(mediaFile);
    }

    /**
     * Returns the rating for the given user and music file.
     *
     * @param username
     *            The user name.
     * @param mediaFile
     *            The music file.
     * 
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        return ratingDao.getRatingForUser(username, mediaFile);
    }

    public int getRatedAlbumCount(String username, List<MusicFolder> musicFolders) {
        return ratingDao.getRatedAlbumCount(username, musicFolders);
    }
}
