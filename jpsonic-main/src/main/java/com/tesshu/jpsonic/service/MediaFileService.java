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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import com.tesshu.jpsonic.util.PathValidator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaFileService {

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final MediaFileCache mediaFileCache;
    private final MediaFileDao mediaFileDao;
    private final JpsonicComparators comparators;

    public MediaFileService(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, MediaFileCache mediaFileCache, MediaFileDao mediaFileDao,
            JpsonicComparators comparators) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaFileCache = mediaFileCache;
        this.mediaFileDao = mediaFileDao;
        this.comparators = comparators;
    }

    public @Nullable MediaFile getMediaFile(Path path) {
        MediaFile result = mediaFileCache.get(path);
        if (result != null) {
            return result;
        }

        if (!securityService.isReadAllowed(path)) {
            throw new SecurityException("Access denied to file " + path);
        }

        result = mediaFileDao.getMediaFile(path.toString());
        if (result == null) {
            return null;
        }

        mediaFileCache.put(path, result);
        return result;
    }

    public @Nullable MediaFile getMediaFile(String path) {
        if (!PathValidator.isNoTraversal(path)) {
            throw new SecurityException("Access denied to file : " + path);
        }
        return getMediaFile(Path.of(path)); // lgtm [java/path-injection]
    }

    public @Nullable MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        if (!securityService.isReadAllowed(mediaFile.toPath())) {
            throw new SecurityException("Access denied to file " + mediaFile);
        }

        return mediaFile;
    }

    public @NonNull MediaFile getMediaFileStrict(String path) {
        MediaFile mediaFile = getMediaFile(path);
        if (mediaFile == null) {
            throw new IllegalArgumentException("The specified MediaFile cannot be found.");
        }
        return mediaFile;
    }

    public @NonNull MediaFile getMediaFileStrict(int id) {
        MediaFile mediaFile = getMediaFile(id);
        if (mediaFile == null) {
            throw new IllegalArgumentException("The specified MediaFile cannot be found.");
        }
        return mediaFile;
    }

    public @Nullable MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPathString() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPathString());
    }

    public Optional<MediaFile> getParent(MediaFile mediaFile) {
        return Optional.ofNullable(getParentOf(mediaFile));
    }

    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDir) {
        List<MediaFile> result = getChildrenWithoutSortOf(parent, includeFiles, includeDir);
        result.sort(comparators.mediaFileOrder(parent));
        return result;
    }

    public List<MediaFile> getChildrenOf(MediaFile parent, long offset, long count, boolean byYear) {
        return mediaFileDao.getChildrenOf(offset, count, parent.getPathString(), byYear);
    }

    public List<MediaFile> getChildrenWithoutSortOf(MediaFile parent, boolean includeFiles, boolean includeDir) {
        List<MediaFile> result = new ArrayList<>();
        if (!parent.isDirectory()) {
            return result;
        }

        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPathString())) {
            if (child.isDirectory() && includeDir && includeMediaFile(child.toPath())) {
                result.add(child);
            }
            if (child.isFile() && includeFiles && includeMediaFile(child.toPath())) {
                result.add(child);
            }
        }
        return result;
    }

    public boolean isRoot(@Nullable MediaFile mediaFile) {
        if (mediaFile == null) {
            return false;
        }
        for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders(false, true)) {
            if (mediaFile.toPath().equals(musicFolder.toPath())) {
                return true;
            }
        }
        return false;
    }

    public boolean includeMediaFile(Path candidate) {
        Path fileName = candidate.getFileName();
        if (fileName == null) {
            return false;
        }
        String suffix = FilenameUtils.getExtension(fileName.toString()).toLowerCase(Locale.ENGLISH);
        return !securityService.isExcluded(candidate)
                && (Files.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix));
    }

    public boolean isAudioFile(String suffix) {
        for (String type : settingsService.getMusicFileTypesAsArray()) {
            if (type.equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVideoFile(String suffix) {
        for (String type : settingsService.getVideoFileTypesAsArray()) {
            if (type.equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Path getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getCoverArtPathString() != null) {
            return Path.of(mediaFile.getCoverArtPathString());
        }
        Path parentPath = mediaFile.getParent();
        if (parentPath == null || !securityService.isReadAllowed(parentPath)) {
            return null;
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null || parent.getCoverArtPathString() == null ? null
                : Path.of(parent.getCoverArtPathString());
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "False positive.")
    public boolean isAvailableCoverArtPath(Path path, BasicFileAttributes attrs) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String fileName = path.getFileName().toString();
        return attrs.isRegularFile() && fileName.charAt(0) != '.'
                && settingsService.getExcludedCoverArtsAsArray().stream()
                        .noneMatch(excluded -> StringUtils.endsWithIgnoreCase(fileName, excluded))
                && settingsService.getCoverArtFileTypesAsArray().stream()
                        .anyMatch(type -> StringUtils.endsWithIgnoreCase(fileName, type));
    }

    public Optional<Path> findCoverArt(Path parent) {
        try (Stream<Path> results = Files.find(parent, 1, this::isAvailableCoverArtPath)) {
            Optional<Path> coverArt = results.findFirst();
            if (coverArt.isPresent()) {
                return coverArt;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        try (Stream<Path> children = Files.list(parent)) {
            return children.filter(p -> ParserUtils.isEmbeddedArtworkApplicable(p)).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<>();

        List<MediaFile> children = sort ? getChildrenOf(ancestor, true, true)
                : getChildrenWithoutSortOf(ancestor, true, true);
        for (MediaFile child : children) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
    }

    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
    }

    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getNewestAlbums(offset, count, musicFolders);
    }

    public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbums(offset, count, username, musicFolders);
    }

    public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist,
            List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
    }

    public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear,
            List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
    }

    public List<MediaFile> getRandomSongsForParent(MediaFile parent, int count) {
        List<MediaFile> children = getDescendantsOf(parent, false);
        removeVideoFiles(children);

        if (children.isEmpty()) {
            return children;
        }
        Collections.shuffle(children);
        return children.subList(0, Math.min(count, children.size()));
    }

    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, String username) {
        return mediaFileDao.getRandomSongs(criteria, username);
    }

    public void removeVideoFiles(List<MediaFile> files) {
        files.removeIf(MediaFile::isVideo);
    }

    public @Nullable Instant getMediaFileStarredDate(int id, String username) {
        return mediaFileDao.getMediaFileStarredDate(id, username);
    }

    public void populateStarredDate(List<MediaFile> mediaFiles, String username) {
        for (MediaFile mediaFile : mediaFiles) {
            populateStarredDate(mediaFile, username);
        }
    }

    public void populateStarredDate(MediaFile mediaFile, String username) {
        Instant starredDate = mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username);
        mediaFile.setStarredDate(starredDate);
    }

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumCount(musicFolders);
    }

    public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getPlayedAlbumCount(musicFolders);
    }

    public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbumCount(username, musicFolders);
    }

    public int getChildSizeOf(MediaFile mediaFile) {
        return mediaFileDao.getChildSizeOf(mediaFile.getPathString());
    }

    public int getChildSizeOf(MusicFolder musicFolder) {
        return mediaFileDao.getChildSizeOf(musicFolder.getPathString());
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

    public int getSongsCountForAlbum(String albumArtist, String album) {
        return mediaFileDao.getSongsCountForAlbum(albumArtist, album);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) {
        return mediaFileDao.getSongsForAlbum(offset, count, album);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) {
        return mediaFileDao.getSongsForAlbum(offset, count, albumArtist, album);
    }

    public List<MediaFile> getIndexedArtists(List<MusicFolder> folders) {
        return mediaFileDao.getIndexedArtists(folders, settingsService.getShortcutsAsArray());
    }

    public List<MediaFile> getSingleSongs(List<MusicFolder> folders) {
        return mediaFileDao.getSingleSongs(folders);
    }
}
