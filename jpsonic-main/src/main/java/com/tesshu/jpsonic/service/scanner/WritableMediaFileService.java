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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MetaDataParser;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

/**
 * Class for writing MediaFile. Package-private methods will be used from the scanning thread. Public methods may be
 * used from outside the scanning thread, so concurrency will need to be taken care of.
 */
@Service
public class WritableMediaFileService {

    private final ScannerStateService scannerStateService;
    private final MediaFileDao mediaFileDao;
    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;
    private final MediaFileCache mediaFileCache;
    private final MetaDataParserFactory metaDataParserFactory;
    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final JapaneseReadingUtils readingUtils;

    public WritableMediaFileService(MediaFileDao mediaFileDao, ScannerStateService scannerStateService,
            MediaFileService mediaFileService, AlbumDao albumDao, MediaFileCache mediaFileCache,
            MetaDataParserFactory metaDataParserFactory, SettingsService settingsService,
            SecurityService securityService, JapaneseReadingUtils readingUtils) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.scannerStateService = scannerStateService;
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
        this.mediaFileCache = mediaFileCache;
        this.metaDataParserFactory = metaDataParserFactory;
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.readingUtils = readingUtils;
    }

    /**
     * Where this method is currently used, Statistics is undesigned.
     *
     * @deprecated Spec subject to change
     */
    @Deprecated
    MediaLibraryStatistics newStatistics() {
        return new MediaLibraryStatistics(now());
    }

    /**
     * Where this method is currently used, Statistics is undesigned.
     *
     * @deprecated Use MediaFileService#getMediaFile if use the cache, otherwise use
     *             WritableMediaFileService#getMediaFile
     */
    @Deprecated
    @Nullable
    MediaFile getMediaFile(Path path) {
        return getMediaFile(path, newStatistics());
    }

    @Nullable
    MediaFile getMediaFile(Path path, @NonNull MediaLibraryStatistics stats) {

        if (path == null || !Files.exists(path)) {
            return null;
        } else if (!securityService.isReadAllowed(path)) {
            throw new SecurityException("Access denied to file " + path);
        }

        // Look in database.
        MediaFile mediaFile = mediaFileDao.getMediaFile(path.toString());
        if (mediaFile != null) {
            mediaFile = checkLastModified(mediaFile, stats);
            mediaFileCache.put(path, mediaFile);
            return mediaFile;
        }

        // Not found in database, must read from disk.
        mediaFile = createMediaFile(path, stats);
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
        return mediaFile;
    }

    boolean isSchemeLastModified() {
        return FileModifiedCheckScheme.LAST_MODIFIED == settingsService.getFileModifiedCheckScheme();
    }

    boolean isSchemeLastScaned() {
        return FileModifiedCheckScheme.LAST_SCANNED == settingsService.getFileModifiedCheckScheme();
    }

    void updateChildren(MediaFile parent, @NonNull MediaLibraryStatistics stats) {

        if (isSchemeLastModified() //
                && parent.getChildrenLastUpdated().toEpochMilli() >= parent.getChanged().toEpochMilli()) {
            return;
        } else if (isSchemeLastScaned() //
                && parent.getMediaType() == MediaType.ALBUM && !FAR_PAST.equals(parent.getChildrenLastUpdated())) {
            return;
        }

        List<MediaFile> storedChildren = mediaFileDao.getChildrenOf(parent.getPathString());
        Map<String, MediaFile> storedChildrenMap = new ConcurrentHashMap<>();
        for (MediaFile child : storedChildren) {
            storedChildrenMap.put(child.getPathString(), child);
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent.toPath())) {
            for (Path child : ds) {
                if (mediaFileService.includeMediaFile(child) && storedChildrenMap.remove(child.toString()) == null) {
                    // Add children that are not already stored.
                    mediaFileDao.createOrUpdateMediaFile(createMediaFile(child, stats));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            @NonNull MediaLibraryStatistics stats) {

        List<MediaFile> result = new ArrayList<>();
        if (!parent.isDirectory()) {
            return result;
        }

        // Make sure children are stored and up-to-date in the database.
        updateChildren(parent, stats);

        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPathString())) {
            MediaFile checked = checkLastModified(child, stats);
            if (checked.isDirectory() && includeDirectories && mediaFileService.includeMediaFile(checked.toPath())) {
                result.add(checked);
            }
            if (checked.isFile() && includeFiles && mediaFileService.includeMediaFile(checked.toPath())) {
                result.add(checked);
            }
        }

        return result;
    }

    MediaFile checkLastModified(final MediaFile mediaFile, @NonNull MediaLibraryStatistics stats) {

        // Determine if the file has not changed
        if (mediaFile.getVersion() >= MediaFileDao.VERSION) {
            switch (settingsService.getFileModifiedCheckScheme()) {
            case LAST_MODIFIED:
                if (!settingsService.isIgnoreFileTimestamps()
                        && mediaFile.getChanged().toEpochMilli() >= getLastModified(mediaFile.toPath(), stats)
                        && !FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                } else if (settingsService.isIgnoreFileTimestamps() && !FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            case LAST_SCANNED:
                if (!FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            default:
                break;
            }
        }

        // Updating database file from disk
        MediaFile mf = createMediaFile(mediaFile.toPath(), stats);
        mediaFileDao.createOrUpdateMediaFile(mf);
        return mf;
    }

    long getLastModified(@NonNull Path path, @NonNull MediaLibraryStatistics stats) {
        if (isSchemeLastModified()) {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return stats.getScanDate().toEpochMilli();
    }

    MediaFile createMediaFile(Path path, @NonNull MediaLibraryStatistics stats) {

        MediaFile existingFile = mediaFileDao.getMediaFile(path.toString());

        MediaFile mediaFile = new MediaFile();

        // Variable initial value
        Instant lastModified = Instant.ofEpochMilli(getLastModified(path, stats));
        mediaFile.setChanged(lastModified);
        mediaFile.setCreated(lastModified);

        mediaFile.setLastScanned(existingFile == null ? FAR_PAST : existingFile.getLastScanned());
        mediaFile.setChildrenLastUpdated(FAR_PAST);

        mediaFile.setPathString(path.toString());
        mediaFile.setFolder(securityService.getRootFolderForFile(path));

        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        mediaFile.setParentPathString(parent.toString());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (Files.isDirectory(path)) {
            applyDirectory(path, mediaFile, stats);
        } else {
            applyFile(path, mediaFile, stats);
        }
        return mediaFile;
    }

    private void applyFile(Path path, MediaFile to, @NonNull MediaLibraryStatistics stats) {
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
            readingUtils.analyze(to);
            to.setLastScanned(stats.getScanDate());
        }
        String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(to.getPathString())));
        to.setFormat(format);
        try {
            to.setFileSize(Files.size(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        to.setMediaType(getMediaType(to));
    }

    private void applyDirectory(Path dirPath, MediaFile to, @NonNull MediaLibraryStatistics stats) {
        // Is this an album?
        if (!mediaFileService.isRoot(to)) {

            getFirstChildMediaFile(dirPath).ifPresentOrElse(firstChildPath -> {
                to.setMediaType(MediaFile.MediaType.ALBUM);

                // Guess artist/album name, year and genre.
                MediaFile firstChild = getMediaFile(firstChildPath, stats);
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
                mediaFileService.findCoverArt(dirPath)
                        .ifPresent(coverArtPath -> to.setCoverArtPathString(coverArtPath.toString()));

            }, () -> to.setArtist(dirPath.getFileName().toString()));

            readingUtils.analyze(to);
        }
    }

    private Optional<Path> getFirstChildMediaFile(Path parent) {
        try (Stream<Path> children = Files.list(parent)) {
            return children.filter(child -> Files.isRegularFile(child))
                    .filter(child -> mediaFileService.includeMediaFile(child)).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
        if (mediaFileService.isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPathString().toLowerCase(Locale.ENGLISH);
        String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase(Locale.ENGLISH);
        if (path.contains("podcast") || genre.contains("podcast")) {
            return MediaFile.MediaType.PODCAST;
        }
        if (path.contains("audiobook") || genre.contains("audiobook") || path.contains("audio book")
                || genre.contains("audio book")) {
            return MediaFile.MediaType.AUDIOBOOK;
        }
        return MediaFile.MediaType.MUSIC;
    }

    void updateFolder(final MediaFile file) {
        mediaFileDao.updateFolder(file.getPathString(), file.getFolder());
    }

    void updateOrder(final MediaFile file) {
        mediaFileDao.updateOrder(file.getPathString(), file.getOrder());
    }

    /*
     * Used for some tag updates. Note that it only updates the tags and does not take into account the completeness of
     * the scan.
     */
    void refreshMediaFile(final MediaFile mediaFile) {
        Path path = mediaFile.toPath();
        MediaFile mf = createMediaFile(path, newStatistics());
        mediaFileDao.createOrUpdateMediaFile(mf);
        mediaFileCache.remove(path);
    }

    // Updateable even during scanning
    public void refreshCoverArt(final MediaFile dir) {
        if (!(dir.getMediaType() == MediaType.ALBUM || dir.getMediaType() == MediaType.DIRECTORY)) {
            return;
        }
        Path dirPath = dir.toPath();
        mediaFileService.findCoverArt(dirPath).ifPresent(coverArtPath -> {
            mediaFileDao.updateCoverArtPath(dirPath.toString(), coverArtPath.toString());
            albumDao.updateCoverArtPath(dir.getAlbumArtist(), dir.getAlbumName(), coverArtPath.toString());
            mediaFileCache.remove(dirPath);
        });
    }

    // Cannot be updated while scanning
    public void updateTags(final MediaFile file) {
        if (scannerStateService.isScanning()) {
            // It will be skipped during scanning. No rigor required. Do not acquire locks.
            return;
        }
        refreshMediaFile(file);
        MediaFile refreshed = mediaFileService.getMediaFileStrict(file.getId());
        mediaFileService.getParent(refreshed).ifPresent(parent -> refreshMediaFile(parent));
    }

    // Updateable even during scanning
    public void incrementPlayCount(MediaFile file) {
        Instant now = now();
        mediaFileDao.updatePlayCount(file.getPathString(), now, file.getPlayCount() + 1);
        MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null && !mediaFileService.isRoot(parent)) {
            mediaFileDao.updatePlayCount(parent.getPathString(), now, parent.getPlayCount() + 1);
        }
        Album album = albumDao.getAlbum(file.getAlbumArtist(), file.getAlbumName());
        if (album != null) {
            albumDao.updatePlayCount(album.getArtist(), album.getName(), now, album.getPlayCount() + 1);
        }
    }

    // Updateable even during scanning
    public void updateComment(MediaFile mediaFile) {
        mediaFileDao.updateComment(mediaFile.getPathString(), mediaFile.getComment());
    }

    // Cannot be updated while scanning
    public void resetLastScanned(MediaFile album) {
        if (scannerStateService.isScanning()) {
            // It will be skipped during scanning. No rigor required. Do not acquire locks.
            return;
        }
        mediaFileDao.resetLastScanned(album.getId());
        for (MediaFile child : mediaFileDao.getChildrenOf(album.getPathString())) {
            mediaFileDao.resetLastScanned(child.getId());
        }
    }
}
