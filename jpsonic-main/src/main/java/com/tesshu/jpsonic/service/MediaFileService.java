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

import static com.tesshu.jpsonic.dao.MediaFileDao.ZERO_DATE;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MetaDataParser;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaFileService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileService.class);

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final MediaFileCache mediaFileCache;
    private final MediaFileDao mediaFileDao;
    private final AlbumDao albumDao;
    private final MetaDataParserFactory metaDataParserFactory;
    private final MediaFileServiceUtils utils;

    private String nio2DirErrorMsg = "Unable to get file list.";

    public MediaFileService(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, MediaFileCache mediaFileCache, MediaFileDao mediaFileDao,
            AlbumDao albumDao, MetaDataParserFactory metaDataParserFactory, MediaFileServiceUtils utils) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaFileCache = mediaFileCache;
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
        this.metaDataParserFactory = metaDataParserFactory;
        this.utils = utils;
    }

    public @Nullable MediaFile getMediaFile(Path path) {
        return getMediaFile(path, true);
    }

    public @Nullable MediaFile getMediaFile(Path path, boolean useFastCache, MediaLibraryStatistics... statistics) {

        // Look in fast memory cache first.
        MediaFile result = mediaFileCache.get(path);
        if (result != null) {
            return result;
        }

        if (!securityService.isReadAllowed(path)) {
            throw new SecurityException("Access denied to file " + path);
        }

        // Secondly, look in database.
        result = mediaFileDao.getMediaFile(path.toString());
        if (result != null) {
            result = checkLastModified(result, useFastCache);
            mediaFileCache.put(path, result);
            return result;
        }

        if (!Files.exists(path)) {
            return null;
        }
        // Not found in database, must read from disk.
        result = createMediaFile(path, statistics);

        // Put in cache and database.
        mediaFileCache.put(path, result);
        mediaFileDao.createOrUpdateMediaFile(result);

        return result;
    }

    public MediaFile getMediaFile(String path) {
        if (!securityService.isNoTraversal(path)) {
            throw new SecurityException("Access denied to file : " + path);
        }
        return getMediaFile(Path.of(path));
    }

    // TODO: Optimize with memory caching.
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        if (!securityService.isReadAllowed(mediaFile.toPath())) {
            throw new SecurityException("Access denied to file " + mediaFile);
        }

        return mediaFile;
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPathString() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPathString());
    }

    boolean isSchemeLastModified() {
        return FileModifiedCheckScheme.LAST_MODIFIED == FileModifiedCheckScheme
                .valueOf(settingsService.getFileModifiedCheckSchemeName());
    }

    private boolean isSchemeLastScaned() {
        return FileModifiedCheckScheme.LAST_SCANNED == FileModifiedCheckScheme
                .valueOf(settingsService.getFileModifiedCheckSchemeName());
    }

    long getLastModified(@NonNull Path path, MediaLibraryStatistics... statistics) {
        if (statistics.length == 0 || isSchemeLastModified()) {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                LOG.error("Unable to get last modified.", e);
            }
        }
        return statistics[0].getScanDate().getTime();
    }

    MediaFile checkLastModified(final MediaFile mediaFile, boolean useFastCache, MediaLibraryStatistics... statistics) {

        // Determine if the file has not changed
        if (useFastCache) {
            return mediaFile;
        } else if (mediaFile.getVersion() >= MediaFileDao.VERSION) {
            FileModifiedCheckScheme scheme = FileModifiedCheckScheme
                    .valueOf(settingsService.getFileModifiedCheckSchemeName());
            switch (scheme) {
            case LAST_MODIFIED:
                if (!settingsService.isIgnoreFileTimestamps()
                        && mediaFile.getChanged().getTime() >= getLastModified(mediaFile.toPath(), statistics)
                        && !ZERO_DATE.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                } else if (settingsService.isIgnoreFileTimestamps() && !ZERO_DATE.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            case LAST_SCANNED:
                if (!ZERO_DATE.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            default:
                break;
            }
        }

        // Updating database file from disk
        MediaFile mf = createMediaFile(mediaFile.toPath(), statistics);
        mediaFileDao.createOrUpdateMediaFile(mf);
        return mf;
    }

    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            boolean sort) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, true);
    }

    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            boolean sort, boolean useFastCache, MediaLibraryStatistics... statistics) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        // Make sure children are stored and up-to-date in the database.
        if (!useFastCache) {
            updateChildren(parent, statistics);
        }

        List<MediaFile> result = new ArrayList<>();
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPathString())) {
            MediaFile checked = checkLastModified(child, useFastCache, statistics);
            if (checked.isDirectory() && includeDirectories && includeMediaFile(checked)) {
                result.add(checked);
            }
            if (checked.isFile() && includeFiles && includeMediaFile(checked)) {
                result.add(checked);
            }
        }

        if (sort) {
            result.sort(utils.mediaFileOrder(parent));
        }

        return result;
    }

    public boolean isRoot(MediaFile mediaFile) {
        for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders(false, true)) {
            if (mediaFile.toPath().equals(musicFolder.getPath().toPath())) {
                return true;
            }
        }
        return false;
    }

    void updateChildren(MediaFile parent, MediaLibraryStatistics... statistics) {

        if (isSchemeLastModified() //
                && parent.getChildrenLastUpdated().getTime() >= parent.getChanged().getTime()) {
            return;
        } else if (isSchemeLastScaned() //
                && parent.getMediaType() == MediaType.ALBUM && !ZERO_DATE.equals(parent.getChildrenLastUpdated())) {
            return;
        }

        List<MediaFile> storedChildren = mediaFileDao.getChildrenOf(parent.getPathString());
        Map<String, MediaFile> storedChildrenMap = new ConcurrentHashMap<>();
        for (MediaFile child : storedChildren) {
            storedChildrenMap.put(child.getPathString(), child);
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of(parent.getPathString()))) {
            for (Path child : ds) {
                if (includeMediaFile(child) && storedChildrenMap.remove(child.toString()) == null) {
                    // Add children that are not already stored.
                    mediaFileDao.createOrUpdateMediaFile(createMediaFile(child, statistics));
                }
            }
        } catch (IOException e) {
            LOG.error(nio2DirErrorMsg, e);
        }

        // Delete children that no longer exist on disk.
        for (String path : storedChildrenMap.keySet()) {
            mediaFileDao.deleteMediaFile(path);
        }

        // Update timestamp in parent.
        parent.setChildrenLastUpdated(parent.getChanged());
        parent.setPresent(true);
        mediaFileDao.createOrUpdateMediaFile(parent);
    }

    private boolean includeMediaFile(MediaFile candidate) {
        return includeMediaFile(candidate.toPath());
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    public boolean includeMediaFile(Path candidate) {
        String suffix = FilenameUtils.getExtension(candidate.getFileName().toString()).toLowerCase();
        return !isExcluded(candidate) && (Files.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix));
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    private boolean isAudioFile(String suffix) {
        for (String type : settingsService.getMusicFileTypesAsArray()) {
            if (type.equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    private boolean isVideoFile(String suffix) {
        for (String type : settingsService.getVideoFileTypesAsArray()) {
            if (type.equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(Path path) {
        if (settingsService.isIgnoreSymLinks() && Files.isSymbolicLink(path)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("excluding symbolic link " + path.toString());
            }
            return true;
        }
        String name = path.getFileName().toString();
        if (settingsService.getExcludePattern() != null && settingsService.getExcludePattern().matcher(name).find()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("excluding file which matches exclude pattern " + settingsService.getExcludePatternString()
                        + ": " + path.toString());
            }
            return true;
        }

        // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
        return !name.isEmpty() && name.charAt(0) == '.' && !name.startsWith("..") || name.startsWith("@eaDir")
                || "Thumbs.db".equals(name);
    }

    MediaFile createMediaFile(Path path, MediaLibraryStatistics... statistics) {

        MediaFile existingFile = mediaFileDao.getMediaFile(path.toString());

        MediaFile mediaFile = new MediaFile();

        // Variable initial value
        Date lastModified = new Date(getLastModified(path, statistics));
        mediaFile.setChanged(lastModified);
        mediaFile.setCreated(lastModified);

        mediaFile.setLastScanned(existingFile == null ? ZERO_DATE : existingFile.getLastScanned());
        mediaFile.setChildrenLastUpdated(ZERO_DATE);

        mediaFile.setPathString(path.toString());
        mediaFile.setFolder(securityService.getRootFolderForFile(path));
        mediaFile.setParentPathString(path.getParent().toString());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (Files.isDirectory(path)) {
            applyDirectory(path, mediaFile, statistics);
        } else {
            applyFile(path, mediaFile, statistics);
        }
        return mediaFile;
    }

    private void applyFile(Path path, MediaFile to, MediaLibraryStatistics... statistics) {
        MetaDataParser parser = metaDataParserFactory.getParser(path);
        if (parser != null) {
            MetaData metaData = parser.getMetaData(path);
            to.setArtist(metaData.getArtist());
            to.setAlbumArtist(metaData.getAlbumArtist());
            to.setAlbumName(metaData.getAlbumName());
            to.setTitle(metaData.getTitle());
            to.setDiscNumber(metaData.getDiscNumber());
            to.setTrackNumber(metaData.getTrackNumber());
            to.setGenre(metaData.getGenre());
            to.setYear(metaData.getYear());
            to.setDurationSeconds(metaData.getDurationSeconds());
            to.setBitRate(metaData.getBitRate());
            to.setVariableBitRate(metaData.isVariableBitRate());
            to.setHeight(metaData.getHeight());
            to.setWidth(metaData.getWidth());
            to.setTitleSort(metaData.getTitleSort());
            to.setAlbumSort(metaData.getAlbumSort());
            to.setAlbumSortRaw(metaData.getAlbumSort());
            to.setArtistSort(metaData.getArtistSort());
            to.setArtistSortRaw(metaData.getArtistSort());
            to.setAlbumArtistSort(metaData.getAlbumArtistSort());
            to.setAlbumArtistSortRaw(metaData.getAlbumArtistSort());
            to.setMusicBrainzReleaseId(metaData.getMusicBrainzReleaseId());
            to.setMusicBrainzRecordingId(metaData.getMusicBrainzRecordingId());
            to.setComposer(metaData.getComposer());
            to.setComposerSort(metaData.getComposerSort());
            to.setComposerSortRaw(metaData.getComposerSort());
            utils.analyze(to);
            to.setLastScanned(statistics.length == 0 ? new Date() : statistics[0].getScanDate());
        }
        String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(to.getPathString())));
        to.setFormat(format);
        try {
            to.setFileSize(Files.size(path));
        } catch (IOException e) {
            LOG.error("Unable to get last file size.", e);
        }
        to.setMediaType(getMediaType(to));
    }

    private void applyDirectory(Path path, MediaFile to, MediaLibraryStatistics... statistics) {
        // Is this an album?
        if (!isRoot(to)) {

            Optional<Path> firstChildMediaFile = getFirstChildMediaFile(path);

            firstChildMediaFile.ifPresentOrElse(firstChildPath -> {
                to.setMediaType(MediaFile.MediaType.ALBUM);

                // Guess artist/album name, year and genre.
                MediaFile firstChild = getMediaFile(firstChildPath, false, statistics);
                if (firstChild != null) {
                    to.setArtist(firstChild.getAlbumArtist());
                    to.setArtistSort(firstChild.getAlbumArtistSort());
                    to.setArtistSortRaw(firstChild.getAlbumArtistSort());
                    to.setAlbumName(firstChild.getAlbumName());
                    to.setAlbumSort(firstChild.getAlbumSort());
                    to.setAlbumSortRaw(firstChild.getAlbumSort());
                    to.setYear(firstChild.getYear());
                    to.setGenre(firstChild.getGenre());
                }

                // Look for cover art.
                findCoverArt(path)
                        .ifPresent(coverArtOEmbeddedPath -> to.setCoverArtPathString(coverArtOEmbeddedPath.toString()));

            }, () -> to.setArtist(path.getFileName().toString()));

            utils.analyze(to);
        }
    }

    private Optional<Path> getFirstChildMediaFile(Path parent) {
        try {
            return Files.list(parent).filter(child -> Files.isRegularFile(child))
                    .filter(child -> includeMediaFile(child)).findAny();
        } catch (IOException e) {
            LOG.error(nio2DirErrorMsg, e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
        if (isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPathString().toLowerCase();
        String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase();
        if (path.contains("podcast") || genre.contains("podcast")) {
            return MediaFile.MediaType.PODCAST;
        }
        if (path.contains("audiobook") || genre.contains("audiobook") || path.contains("audio book")
                || genre.contains("audio book")) {
            return MediaFile.MediaType.AUDIOBOOK;
        }
        return MediaFile.MediaType.MUSIC;
    }

    public void refreshMediaFile(final MediaFile mediaFile) {
        Path path = mediaFile.toPath();
        MediaFile mf = createMediaFile(path);
        mediaFileDao.createOrUpdateMediaFile(mf);
        mediaFileCache.remove(path);
    }

    public Path getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getCoverArtPathString() != null) {
            return Path.of(mediaFile.getCoverArtPathString());
        }
        if (!securityService.isReadAllowed(mediaFile.getParent())) {
            return null;
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null || parent.getCoverArtPathString() == null ? null
                : Path.of(parent.getCoverArtPathString());
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    Optional<Path> findCoverArt(Path parent) {

        BiPredicate<Path, BasicFileAttributes> coverArtNameRule = (childPath, attrs) -> Files.isRegularFile(childPath)
                && childPath.getFileName().toString().charAt(0) != '.'
                && Stream.of(settingsService.getCoverArtFileTypesAsArray())
                        .anyMatch(t -> childPath.getFileName().toString().toUpperCase().endsWith(t.toUpperCase()));

        try (Stream<Path> stream = Files.find(parent, 1, coverArtNameRule)) {
            Optional<Path> coverArt = stream.findFirst();
            if (!coverArt.isEmpty()) {
                return coverArt;
            }
        } catch (IOException e) {
            LOG.error(nio2DirErrorMsg, e);
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent)) {
            for (Path childPath : ds) {
                if (ParserUtils.isEmbeddedArtworkApplicable(childPath)) {
                    return Optional.ofNullable(childPath);
                }
            }
        } catch (IOException e) {
            LOG.error(nio2DirErrorMsg, e);
        }

        return Optional.empty();
    }

    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<>();

        for (MediaFile child : getChildrenOf(ancestor, true, true, sort)) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public void incrementPlayCount(MediaFile file) {
        Date now = new Date();
        file.setLastPlayed(now);
        file.setPlayCount(file.getPlayCount() + 1);
        updateMediaFile(file);

        MediaFile parent = getParentOf(file);
        if (!isRoot(parent)) {
            parent.setLastPlayed(now);
            parent.setPlayCount(parent.getPlayCount() + 1);
            updateMediaFile(parent);
        }

        Album album = albumDao.getAlbum(file.getAlbumArtist(), file.getAlbumName());
        if (album != null) {
            album.setLastPlayed(now);
            album.setPlayCount(album.getPlayCount() + 1);
            albumDao.createOrUpdateAlbum(album);
        }
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

    public Date getMediaFileStarredDate(int id, String username) {
        return mediaFileDao.getMediaFileStarredDate(id, username);
    }

    public void populateStarredDate(List<MediaFile> mediaFiles, String username) {
        for (MediaFile mediaFile : mediaFiles) {
            populateStarredDate(mediaFile, username);
        }
    }

    public void populateStarredDate(MediaFile mediaFile, String username) {
        Date starredDate = mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username);
        mediaFile.setStarredDate(starredDate);
    }

    public void updateMediaFile(MediaFile mediaFile) {
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
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

    public void resetLastScanned(MediaFile album) {
        mediaFileDao.resetLastScanned(album.getId());
        for (MediaFile child : mediaFileDao.getChildrenOf(album.getPathString())) {
            mediaFileDao.resetLastScanned(child.getId());
        }
    }

    @Deprecated
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

    @Deprecated
    public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByGenre(offset, count, genre, musicFolders);
    }
}
