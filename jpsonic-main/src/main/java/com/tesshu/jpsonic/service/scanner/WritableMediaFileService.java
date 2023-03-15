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

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_FUTURE;
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.SuppressLint;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MusicParser;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import com.tesshu.jpsonic.service.metadata.VideoParser;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
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

    private final ScannerStateService scannerState;
    private final MediaFileDao mediaFileDao;
    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;
    private final MediaFileCache mediaFileCache;
    private final MusicParser musicParser;
    private final VideoParser videoParser;

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final JapaneseReadingUtils readingUtils;
    private final IndexManager indexManager;

    public WritableMediaFileService(MediaFileDao mediaFileDao, ScannerStateService scannerStateService,
            MediaFileService mediaFileService, AlbumDao albumDao, MediaFileCache mediaFileCache,
            MusicParser musicParser, VideoParser videoParser, SettingsService settingsService,
            SecurityService securityService, JapaneseReadingUtils readingUtils, IndexManager indexManager) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.scannerState = scannerStateService;
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
        this.mediaFileCache = mediaFileCache;
        this.musicParser = musicParser;
        this.videoParser = videoParser;
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.readingUtils = readingUtils;
        this.indexManager = indexManager;
    }

    /**
     * Use of this method suggests imperfect workflow design.
     *
     * @deprecated Spec subject to change
     */
    @Deprecated
    Instant newScanDate() {
        return now();
    }

    /**
     * Will be removed in v111.7.0. Use of this method suggests imperfect workflow design.
     *
     * @deprecated Use MediaFileService#getMediaFile if use the cache, otherwise use
     *             WritableMediaFileService#getMediaFile(Instant, Path)
     */
    @Deprecated
    @Nullable
    MediaFile getMediaFile(@Nullable Path path) {
        return getMediaFile(newScanDate(), path);
    }

    /**
     * Will be removed in v111.7.0.
     *
     * @deprecated Only used on top nodes in scans. So creating a top-level node is necessary and there should be no
     *             need to provide a multipurpose method.
     */
    @Deprecated
    @Nullable
    MediaFile getMediaFile(@NonNull Instant scanDate, @Nullable Path path) {

        if (path == null || !Files.exists(path)) {
            return null;
        } else if (!securityService.isReadAllowed(path)) {
            throw new SecurityException("Access denied to file " + path);
        }

        MediaFile registered = mediaFileDao.getMediaFile(path.toString());
        if (registered != null) {
            Optional<MediaFile> op = checkLastModified(scanDate, registered);
            op.ifPresent(m -> mediaFileCache.put(path, m));
            return op.orElse(registered);
        }

        return createMediaFile(scanDate, path).get();
    }

    boolean isSchemeLastModified() {
        return FileModifiedCheckScheme.LAST_MODIFIED == settingsService.getFileModifiedCheckScheme();
    }

    boolean isSchemeLastScaned() {
        return FileModifiedCheckScheme.LAST_SCANNED == settingsService.getFileModifiedCheckScheme();
    }

    boolean isSkipUpdateChildren(@NonNull MediaFile parent) {
        return isSchemeLastScaned() //
                && parent.getMediaType() == MediaType.ALBUM && !FAR_PAST.equals(parent.getChildrenLastUpdated());
    }

    Optional<Path> updateChildren(@NonNull Instant scanDate, @NonNull MediaFile parent) {

        if (isSkipUpdateChildren(parent)) {
            return Optional.empty();
        }

        Map<String, MediaFile> stored = mediaFileDao.getChildrenOf(parent.getPathString()).stream()
                .collect(Collectors.toMap(mf -> mf.getPathString(), mf -> mf));

        LongAdder updateCount = new LongAdder();
        CoverArtDetector coverArtDetector = new CoverArtDetector(mediaFileService);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent.toPath())) {
            for (Path childPath : ds) {

                coverArtDetector.setChildFilePath(childPath);

                if (!mediaFileService.includeMediaFile(childPath)) {
                    continue;
                }

                coverArtDetector.setMediaFilePath(childPath);

                MediaFile child = stored.get(childPath.toString());
                stored.remove(childPath.toString());
                createOrUpdateChild(child, childPath, scanDate).ifPresentOrElse(result -> updateCount.increment(),
                        () -> {
                            if (child != null && !scanDate.equals(child.getLastScanned())
                                    && !FAR_FUTURE.equals(child.getLastScanned())) {
                                mediaFileDao.updateLastScanned(child.getId(), scanDate);
                            }
                        });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        stored.values().stream().filter(m -> mediaFileDao.deleteMediaFile(m.getId()) > 0).forEach(m -> {
            deleteMediafileIndex(m);
            updateCount.increment();
        });

        if (updateCount.intValue() > 0) {
            mediaFileDao.updateChildrenLastUpdated(parent.getPathString(),
                    parent.getMediaType() == MediaType.ALBUM ? FAR_FUTURE : parent.getChanged());
        }
        return coverArtDetector.getCoverArtAvailable();
    }

    private void deleteMediafileIndex(MediaFile mediaFile) {
        switch (mediaFile.getMediaType()) {
        case DIRECTORY:
            indexManager.expungeArtist(mediaFile.getId());
            break;
        case ALBUM:
            indexManager.expungeAlbum(mediaFile.getId());
            break;
        case MUSIC:
            indexManager.expungeSong(mediaFile.getId());
            break;
        default:
            break;
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "False positive. getMediaFile is pre-checked and thread safe here.")
    @SuppressLint(value = "NULL_DEREFERENCE", justification = "False positive. getMediaFile is pre-checked and thread safe here.")
    Optional<MediaFile> createOrUpdateChild(@Nullable MediaFile child, @NonNull Path childPath,
            @NonNull Instant scanDate) {
        if (child == null) {
            if (settingsService.isUseCleanUp()) {
                if (mediaFileDao.exists(childPath)) {
                    return refreshMediaFile(scanDate, mediaFileDao.getMediaFile(childPath));
                } else {
                    return createMediaFile(scanDate, childPath);
                }
            } else {
                // @see ScannerProcedureService#beforeScan
                return createMediaFile(scanDate, childPath);
            }
        } else {
            return checkLastModified(scanDate, child);
        }
    }

    List<MediaFile> getChildrenOf(@NonNull Instant scanDate, @NonNull MediaFile parent, boolean fileOnly) {

        List<MediaFile> result = new ArrayList<>();
        if (!parent.isDirectory()) {
            return result;
        }

        updateChildren(scanDate, parent).ifPresentOrElse(covrerArtPath -> {
            if (!Objects.equals(parent.getCoverArtPathString(), covrerArtPath.toString())) {
                mediaFileDao.updateCoverArtPath(parent.getPathString(), covrerArtPath.toString());
            }
        }, () -> {
            if (parent.getPathString() != null) {
                mediaFileDao.updateCoverArtPath(parent.getPathString(), null);
            }
        });

        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPathString())) {
            if (child.isDirectory() && !fileOnly && mediaFileService.includeMediaFile(child.toPath())) {
                result.add(child);
            }
            if (child.isFile() && fileOnly && mediaFileService.includeMediaFile(child.toPath())) {
                result.add(child);
            }
        }

        return result;
    }

    Optional<MediaFile> checkLastModified(@NonNull Instant scanDate, @NonNull final MediaFile mediaFile) {
        if (scanDate.equals(mediaFile.getLastScanned()) || FAR_FUTURE.equals(mediaFile.getLastScanned())) {
            return Optional.empty();
        } else if (mediaFile.getVersion() >= MediaFileDao.VERSION) {
            switch (settingsService.getFileModifiedCheckScheme()) {
            case LAST_MODIFIED:
                if (settingsService.isIgnoreFileTimestamps() && !FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return Optional.empty();
                } else if (!settingsService.isIgnoreFileTimestamps()
                        && !mediaFile.getChanged().isBefore(getLastModified(scanDate, mediaFile.toPath()))
                        && !FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return Optional.empty();
                }
                break;
            case LAST_SCANNED:
                if (!FAR_PAST.equals(mediaFile.getLastScanned())) {
                    return Optional.empty();
                }
                break;
            default:
                break;
            }
        }
        return refreshMediaFile(scanDate, mediaFile);
    }

    Instant getLastModified(@NonNull Instant scanDate, @NonNull Path path) {
        if (isSchemeLastModified()) {
            try {
                return Files.getLastModifiedTime(path).toInstant().truncatedTo(ChronoUnit.MILLIS);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return scanDate;
    }

    @SuppressLint(value = "NULL_DEREFERENCE", justification = "False positive. parseMediaFile is NonNull")
    Optional<MediaFile> createMediaFile(@NonNull Instant scanDate, @NonNull Path path) {
        MediaFile created = mediaFileDao.createMediaFile(parseMediaFile(scanDate, path, null));
        if (created != null && created.getMediaType() != MediaType.ALBUM) {
            indexManager.index(created);
        }
        return Optional.ofNullable(created);
    }

    @NonNull
    MediaFile parseMediaFile(@NonNull Instant scanDate, @NonNull Path path, MediaFile registered) {
        MediaFile mediaFile = instanceOf(scanDate, path, registered);
        if (Files.isDirectory(path)) {
            applyDirectory(path, mediaFile, scanDate);
        } else {
            applyFile(path, mediaFile, scanDate);
        }
        return mediaFile;
    }

    private @NonNull MediaFile instanceOf(@NonNull Instant scanDate, @NonNull Path path,
            @Nullable MediaFile registered) {
        MediaFile mediaFile = new MediaFile();

        mediaFile.setId(registered == null ? 0 : registered.getId());

        Instant lastModified = getLastModified(scanDate, path);
        mediaFile.setChanged(lastModified);
        mediaFile.setCreated(lastModified);

        mediaFile.setLastScanned(registered == null ? FAR_PAST : registered.getLastScanned());
        mediaFile.setChildrenLastUpdated(FAR_PAST);

        mediaFile.setPathString(path.toString());
        mediaFile.setFolder(securityService.getRootFolderForFile(path));

        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        mediaFile.setParentPathString(parent.toString());
        mediaFile.setPlayCount(registered == null ? 0 : registered.getPlayCount());
        mediaFile.setLastPlayed(registered == null ? null : registered.getLastPlayed());
        mediaFile.setComment(registered == null ? null : registered.getComment());
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);
        mediaFile.setOrder(registered == null ? -1 : registered.getOrder());
        return mediaFile;
    }

    private void applyFile(@NonNull Path path, @NonNull MediaFile to, @NonNull Instant scanDate) {

        if (musicParser.isApplicable(path)) {
            MetaData metaData = musicParser.getMetaData(path);
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
            to.setLastScanned(scanDate);
        } else if (videoParser.isApplicable(path)) {
            to.setLastScanned(FAR_FUTURE);
        } else {
            readingUtils.analyze(to);
            to.setLastScanned(scanDate);
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

    private void applyDirectory(@NonNull Path dirPath, @NonNull MediaFile to, @NonNull Instant scanDate) {
        if (!mediaFileService.isRoot(to)) {
            getFirstChildMediaFile(dirPath).ifPresentOrElse(firstChildPath -> {
                to.setMediaType(MediaFile.MediaType.ALBUM);
                to.setLastScanned(FAR_FUTURE);
            }, () -> {
                to.setArtist(dirPath.getFileName().toString());
                to.setLastScanned(scanDate);
            });
        }
    }

    private Optional<Path> getFirstChildMediaFile(@NonNull Path parent) {
        try (Stream<Path> children = Files.list(parent)) {
            return children.filter(child -> Files.isRegularFile(child))
                    .filter(child -> mediaFileService.includeMediaFile(child)).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MediaFile.MediaType getMediaType(@NonNull MediaFile mediaFile) {
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

    @NonNull
    MediaFile parseVideo(@NonNull Instant scanDate, MediaFile registered) {
        Path path = registered.toPath();
        /*
         * See #1368.If we limit yourself to ffprobe, we can handle Sort and MusicBrainz. Currently, MP4 ... old
         * QuickTime format specification range is used considering versatility. That is the lowest common multiple.
         */
        if (videoParser.isApplicable(path)) {
            MetaData metaData = videoParser.getMetaData(path);
            registered.setArtist(metaData.getArtist());
            registered.setAlbumArtist(metaData.getAlbumArtist());
            registered.setAlbumName(metaData.getAlbumName());
            registered.setTitle(metaData.getTitle());
            registered.setDiscNumber(metaData.getDiscNumber());
            registered.setTrackNumber(metaData.getTrackNumber());
            registered.setGenre(metaData.getGenre());
            registered.setYear(metaData.getYear());
            registered.setDurationSeconds(metaData.getDurationSeconds());
            registered.setBitRate(metaData.getBitRate()); // ffprobe Only!
            registered.setHeight(metaData.getHeight());
            registered.setWidth(metaData.getWidth());
            // registered.setTitleSort(metaData.getTitleSort());
            // registered.setAlbumSort(metaData.getAlbumSort());
            // registered.setAlbumSortRaw(metaData.getAlbumSort());
            // registered.setArtistSort(metaData.getArtistSort());
            // registered.setArtistSortRaw(metaData.getArtistSort());
            // registered.setAlbumArtistSort(metaData.getAlbumArtistSort());
            // registered.setAlbumArtistSortRaw(metaData.getAlbumArtistSort());
            // registered.setMusicBrainzReleaseId(metaData.getMusicBrainzReleaseId());
            // registered.setMusicBrainzRecordingId(metaData.getMusicBrainzRecordingId());
            registered.setComposer(metaData.getComposer());
            // registered.setComposerSort(metaData.getComposerSort());
            // registered.setComposerSortRaw(metaData.getComposerSort());
        }
        readingUtils.analyze(registered);
        registered.setLastScanned(scanDate);
        return registered;
    }

    void updateOrder(@NonNull final MediaFile file) {
        mediaFileDao.updateOrder(file.getPathString(), file.getOrder());
    }

    /*
     * TODO To be fixed in v111.7.0 later #1925. Used for some tag updates. Note that it only updates the tags and does
     * not take into account the completeness of the scan. Strictly speaking, processing equivalent to partial scan is
     * required.
     */
    @Deprecated
    void refreshMediaFile(@NonNull MediaFile mediaFile) {
        refreshMediaFile(newScanDate(), mediaFile);
    }

    @SuppressLint(value = "NULL_DEREFERENCE", justification = "False positive. parseMediaFile is NonNull")
    Optional<MediaFile> refreshMediaFile(@NonNull Instant scanDate, @NonNull MediaFile registered) {
        MediaFile parsed = parseMediaFile(scanDate, registered.toPath(), registered);
        MediaFile updated = mediaFileDao.updateMediaFile(parsed);
        if (updated != null && updated.getMediaType() != MediaType.ALBUM) {
            indexManager.index(updated);
        }
        mediaFileCache.remove(parsed.toPath());
        return Optional.ofNullable(updated);
    }

    // Updateable even during scanning
    public void refreshCoverArt(@NonNull final MediaFile dir) {
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
    public void updateTags(@NonNull final MediaFile file) {
        if (scannerState.isScanning()) {
            // It will be skipped during scanning. No rigor required. Do not acquire locks.
            return;
        }
        refreshMediaFile(file);
        MediaFile refreshed = mediaFileService.getMediaFileStrict(file.getId());
        mediaFileService.getParent(refreshed).ifPresent(parent -> refreshMediaFile(parent));
    }

    // Updateable even during scanning
    public void incrementPlayCount(@NonNull MediaFile file) {
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
    public void updateComment(@NonNull MediaFile mediaFile) {
        mediaFileDao.updateComment(mediaFile.getPathString(), mediaFile.getComment());
    }

    // Cannot be updated while scanning
    public void resetLastScanned(@NonNull MediaFile album) {
        if (scannerState.isScanning()) {
            // It will be skipped during scanning. No rigor required. Do not acquire locks.
            return;
        }
        mediaFileDao.resetLastScanned(album.getId());
        for (MediaFile child : mediaFileDao.getChildrenOf(album.getPathString())) {
            mediaFileDao.resetLastScanned(child.getId());
        }
    }

    private static class CoverArtDetector {

        private final MediaFileService mediaFileService;
        private Path coverArtAvailable;
        private Path firstCoverArtEmbeddable;

        public CoverArtDetector(MediaFileService mediaFileService) {
            this.mediaFileService = mediaFileService;
        }

        void setChildFilePath(Path childPath) {
            try {
                if (coverArtAvailable == null && mediaFileService.isAvailableCoverArtPath(childPath,
                        Files.readAttributes(childPath, BasicFileAttributes.class))) {
                    coverArtAvailable = childPath;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        void setMediaFilePath(Path childPath) {
            if (firstCoverArtEmbeddable == null && ParserUtils.isEmbeddedArtworkApplicable(childPath)) {
                firstCoverArtEmbeddable = childPath;
            }
        }

        Optional<Path> getCoverArtAvailable() {
            return Optional.ofNullable(ObjectUtils.defaultIfNull(coverArtAvailable, firstCoverArtEmbeddable));
        }
    }
}
