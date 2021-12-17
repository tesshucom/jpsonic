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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.io.File;
import java.util.List;

import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Extended service of MediaFileService. The purpose is a paging extension, mainly used in UpnP.
 */
@Service
@DependsOn({ "mediaFileDao", "mediaFileService" })
public class JMediaFileService {

    private final JMediaFileDao mediaFileDao;
    private final MediaFileService deligate;

    public JMediaFileService(JMediaFileDao mediaFileDao, MediaFileService deligate) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.deligate = deligate;
    }

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        return deligate.getAlbumCount(musicFolders);

    }

    public List<MediaFile> getChildrenOf(MediaFile parent, long offset, long count, boolean byYear) {
        return mediaFileDao.getChildrenOf(offset, count, parent.getPath(), byYear);
    }

    /**
     * Returns the number of child elements of the specified mediaFile.
     * 
     * @return the number of child elements
     */
    public int getChildSizeOf(MediaFile mediaFile) {
        return mediaFileDao.getChildSizeOf(mediaFile.getPath());
    }

    /**
     * Returns the number of child elements of the specified musicFolder.
     * 
     * @return the number of child elements
     */
    public int getChildSizeOf(MusicFolder musicFolder) {
        return mediaFileDao.getChildSizeOf(musicFolder.getPath().getPath());
    }

    public MediaFile getMediaFile(File file) {
        return deligate.getMediaFile(file);

    }

    public MediaFile getMediaFile(int id) {
        return deligate.getMediaFile(id);
    }

    public MediaFile getMediaFile(String pathName) {
        return deligate.getMediaFile(pathName);
    }

    public long countSongs(List<MusicFolder> folders) {
        return mediaFileDao.countMediaFile(MediaType.MUSIC, folders);
    }

    public List<MediaFile> getSongs(long count, long offset, List<MusicFolder> folders) {
        return mediaFileDao.getMediaFile(MediaType.MUSIC, count, offset, folders);
    }

    public long countVideos(List<MusicFolder> folders) {
        return mediaFileDao.countMediaFile(MediaType.VIDEO, folders);
    }

    public List<MediaFile> getVideos(long count, long offset, List<MusicFolder> folders) {
        return mediaFileDao.getMediaFile(MediaType.VIDEO, count, offset, folders);
    }

    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return deligate.getNewestAlbums(offset, count, musicFolders);
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        return deligate.getParentOf(mediaFile);
    }

    /**
     * Returns the song count of the specified album name and album-artist.
     * 
     * @param albumArtist
     *            album-artist
     * @param album
     *            name of album
     * 
     * @return song count
     */
    public int getSongsCountForAlbum(String albumArtist, String album) {
        return mediaFileDao.getSongsCountForAlbum(albumArtist, album);
    }

    /**
     * Returns the song of the specified album.
     * 
     * @param offset
     *            Number of songs to skip.
     * @param count
     *            Maximum number of songs to return.
     * @param album
     *            album
     * 
     * @return Enumerating songs considering paging
     */
    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) {
        return mediaFileDao.getSongsForAlbum(offset, count, album);
    }

    /**
     * Returns the song of the specified album name and album-artist.
     * 
     * @param offset
     *            Number of songs to skip.
     * @param count
     *            Maximum number of songs to return.
     * @param albumArtist
     *            album-artist
     * @param album
     *            name of album
     * 
     * @return Enumerating songs considering paging
     */
    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) {
        return mediaFileDao.getSongsForAlbum(offset, count, albumArtist, album);
    }

    public boolean isRoot(MediaFile mediaFile) {
        return deligate.isRoot(mediaFile);
    }

}
