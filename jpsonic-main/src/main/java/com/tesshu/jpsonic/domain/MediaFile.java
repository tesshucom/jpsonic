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

package com.tesshu.jpsonic.domain;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FilenameUtils;

/**
 * A media file (audio, video or directory) with an assortment of its meta data.
 *
 * @author Sindre Mehus
 */
public class MediaFile {

    private int id;
    private String path;
    private String folder;
    private MediaType mediaType;
    private String format;
    private String title;
    private String albumName;
    private String artist;
    private String albumArtist;
    private Integer discNumber;
    private Integer trackNumber;
    private Integer year;
    private String genre;
    private Integer bitRate;
    private boolean variableBitRate;
    private Integer durationSeconds;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String coverArtPath;
    private String parentPath;
    private int playCount;
    private Date lastPlayed;
    private String comment;
    private Date created;
    private Date changed;
    private Date lastScanned;
    private Date starredDate;
    private Date childrenLastUpdated;
    private boolean present;
    private int version;
    private String musicBrainzReleaseId;
    private String musicBrainzRecordingId;
    private String composer;
    private String artistSort;
    private String albumSort;
    private String titleSort;
    private String albumArtistSort;
    private String composerSort;
    private String artistReading;
    private String albumReading;
    private String albumArtistReading;
    private int order;
    private String artistSortRaw;
    private String albumSortRaw;
    private String albumArtistSortRaw;
    private String composerSortRaw;
    private transient int rownum;

    public MediaFile(int id, String path, String folder, MediaType mediaType, String format, String title,
            String albumName, String artist, String albumArtist, Integer discNumber, Integer trackNumber, Integer year,
            String genre, Integer bitRate, boolean variableBitRate, Integer durationSeconds, Long fileSize,
            Integer width, Integer height, String coverArtPath, String parentPath, int playCount, Date lastPlayed,
            String comment, Date created, Date changed, Date lastScanned, Date childrenLastUpdated, boolean present,
            int version, String musicBrainzReleaseId, String musicBrainzRecordingId, String composer, String artistSort,
            String albumSort, String titleSort, String albumArtistSort, String composerSort, String artistReading,
            String albumReading, String albumArtistReading, String artistSortRaw, String albumSortRaw,
            String albumArtistSortRaw, String composerSortRaw, int order) {
        this.id = id;
        this.path = path;
        this.folder = folder;
        this.mediaType = mediaType;
        this.format = format;
        this.title = title;
        this.albumName = albumName;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.year = year;
        this.genre = genre;
        this.bitRate = bitRate;
        this.variableBitRate = variableBitRate;
        this.durationSeconds = durationSeconds;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.coverArtPath = coverArtPath;
        this.parentPath = parentPath;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.changed = changed;
        this.lastScanned = lastScanned;
        this.childrenLastUpdated = childrenLastUpdated;
        this.present = present;
        this.version = version;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
        this.musicBrainzRecordingId = musicBrainzRecordingId;
        this.composer = composer;
        this.artistSort = artistSort;
        this.albumSort = albumSort;
        this.titleSort = titleSort;
        this.albumArtistSort = albumArtistSort;
        this.composerSort = composerSort;
        this.artistReading = artistReading;
        this.albumReading = albumReading;
        this.albumArtistReading = albumArtistReading;
        this.artistSortRaw = artistSortRaw;
        this.albumSortRaw = albumSortRaw;
        this.albumArtistSortRaw = albumArtistSortRaw;
        this.composerSortRaw = composerSortRaw;
        this.order = order;
    }

    public MediaFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPathString() {
        return path;
    }

    public void setPathString(String path) {
        this.path = path;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Deprecated
    public File getFile() {
        return new File(path);
    }

    public Path toPath() {
        return Path.of(path);
    }

    public boolean exists() {
        return Files.exists(toPath());
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isVideo() {
        return mediaType == MediaType.VIDEO;
    }

    public boolean isAudio() {
        return mediaType == MediaType.MUSIC || mediaType == MediaType.AUDIOBOOK || mediaType == MediaType.PODCAST;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDirectory() {
        return !isFile();
    }

    public boolean isFile() {
        return mediaType != MediaType.DIRECTORY && mediaType != MediaType.ALBUM;
    }

    public boolean isAlbum() {
        return mediaType == MediaType.ALBUM;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String album) {
        this.albumName = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getName() {
        if (isFile()) {
            return title == null ? FilenameUtils.getBaseName(path) : title;
        }
        return FilenameUtils.getName(path);
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isVariableBitRate() {
        return variableBitRate;
    }

    public void setVariableBitRate(boolean variableBitRate) {
        this.variableBitRate = variableBitRate;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDurationString() {
        if (durationSeconds == null) {
            return null;
        }
        // Return in M:SS or H:MM:SS
        return StringUtil.formatDuration(durationSeconds);
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getCoverArtPathString() {
        return coverArtPath;
    }

    public void setCoverArtPathString(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }

    public String getParentPathString() {
        return parentPath;
    }

    public void setParentPathString(String parentPath) {
        this.parentPath = parentPath;
    }

    @Deprecated
    public File getParentFile() {
        return getFile().getParentFile();
    }

    public Path getParent() {
        return toPath().getParent();
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    public Date getStarredDate() {
        return starredDate;
    }

    public void setStarredDate(Date starredDate) {
        this.starredDate = starredDate;
    }

    public String getMusicBrainzReleaseId() {
        return musicBrainzReleaseId;
    }

    public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public String getMusicBrainzRecordingId() {
        return musicBrainzRecordingId;
    }

    public void setMusicBrainzRecordingId(String musicBrainzRecordingId) {
        this.musicBrainzRecordingId = musicBrainzRecordingId;
    }

    /**
     * Returns when the children was last updated in the database.
     */
    public Date getChildrenLastUpdated() {
        return childrenLastUpdated;
    }

    public void setChildrenLastUpdated(Date childrenLastUpdated) {
        this.childrenLastUpdated = childrenLastUpdated;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MediaFile && ((MediaFile) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Deprecated
    public File getCoverArtFile() {
        return coverArtPath == null ? null : new File(coverArtPath);
    }

    public Optional<Path> getCoverArtPath() {
        return coverArtPath == null ? Optional.empty() : Optional.of(Path.of(coverArtPath));
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getArtistSort() {
        return artistSort;
    }

    public void setArtistSort(String artistSort) {
        this.artistSort = artistSort;
    }

    public String getAlbumSort() {
        return albumSort;
    }

    public void setAlbumSort(String albumSort) {
        this.albumSort = albumSort;
    }

    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public String getAlbumArtistSort() {
        return albumArtistSort;
    }

    public void setAlbumArtistSort(String albumArtistSort) {
        this.albumArtistSort = albumArtistSort;
    }

    public String getComposerSort() {
        return composerSort;
    }

    public void setComposerSort(String composerSort) {
        this.composerSort = composerSort;
    }

    public String getArtistReading() {
        return artistReading;
    }

    public void setArtistReading(String artistReading) {
        this.artistReading = artistReading;
    }

    public String getAlbumReading() {
        return albumReading;
    }

    public void setAlbumReading(String albumReading) {
        this.albumReading = albumReading;
    }

    public String getAlbumArtistReading() {
        return albumArtistReading;
    }

    public void setAlbumArtistReading(String albumArtistReading) {
        this.albumArtistReading = albumArtistReading;
    }

    public String getArtistSortRaw() {
        return artistSortRaw;
    }

    public void setArtistSortRaw(String artistSortRaw) {
        this.artistSortRaw = artistSortRaw;
    }

    public String getAlbumSortRaw() {
        return albumSortRaw;
    }

    public void setAlbumSortRaw(String albumSortRaw) {
        this.albumSortRaw = albumSortRaw;
    }

    public String getAlbumArtistSortRaw() {
        return albumArtistSortRaw;
    }

    public void setAlbumArtistSortRaw(String albumArtistSortRaw) {
        this.albumArtistSortRaw = albumArtistSortRaw;
    }

    public String getComposerSortRaw() {
        return composerSortRaw;
    }

    public void setComposerSortRaw(String composerSortRaw) {
        this.composerSortRaw = composerSortRaw;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getRownum() {
        return rownum;
    }

    public void setRownum(int rownum) {
        this.rownum = rownum;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static List<Integer> toIdList(List<MediaFile> from) {
        return from.stream().map(toId()).collect(Collectors.toList());
    }

    public static Function<MediaFile, Integer> toId() {
        return MediaFile::getId;
    }

    public enum MediaType {
        MUSIC, PODCAST, AUDIOBOOK, VIDEO, DIRECTORY, ALBUM
    }
}
