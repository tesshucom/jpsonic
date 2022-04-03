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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
import com.tesshu.jpsonic.util.FileUtil;
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

    public @Nullable MediaFile getMediaFile(File file) {
        return getMediaFile(file, settingsService.isFastCacheEnabled());
    }

    public @Nullable MediaFile getMediaFile(File file, boolean useFastCache, MediaLibraryStatistics... statistics) {

        // Look in fast memory cache first.
        MediaFile result = mediaFileCache.get(file);
        if (result != null) {
            return result;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        // Secondly, look in database.
        result = mediaFileDao.getMediaFile(file.getPath());
        if (result != null) {
            result = checkLastModified(result, useFastCache);
            mediaFileCache.put(file, result);
            return result;
        }

        if (!FileUtil.exists(file)) {
            return null;
        }
        // Not found in database, must read from disk.
        result = createMediaFile(file, statistics);

        // Put in cache and database.
        mediaFileCache.put(file, result);
        mediaFileDao.createOrUpdateMediaFile(result);

        return result;
    }

    public MediaFile getMediaFile(String pathName) {
        if (!securityService.isNoTraversal(pathName)) {
            throw new SecurityException("Access denied to file : " + pathName);
        }
        return getMediaFile(new File(pathName));
    }

    // TODO: Optimize with memory caching.
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        if (!securityService.isReadAllowed(mediaFile.getFile())) {
            throw new SecurityException("Access denied to file " + mediaFile);
        }

        return checkLastModified(mediaFile, settingsService.isFastCacheEnabled());
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath());
    }

    boolean isSchemeLastModified() {
        return FileModifiedCheckScheme.LAST_MODIFIED == FileModifiedCheckScheme
                .valueOf(settingsService.getFileModifiedCheckSchemeName());
    }

    private boolean isSchemeLastScaned() {
        return FileModifiedCheckScheme.LAST_SCANNED == FileModifiedCheckScheme
                .valueOf(settingsService.getFileModifiedCheckSchemeName());
    }

    long getLastModified(@NonNull File file, MediaLibraryStatistics... statistics) {
        if (statistics.length == 0 || isSchemeLastModified()) {
            return FileUtil.lastModified(file);
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
                        && mediaFile.getChanged().getTime() >= getLastModified(mediaFile.getFile(), statistics)
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
        MediaFile mf = createMediaFile(mediaFile.getFile(), statistics);
        mediaFileDao.createOrUpdateMediaFile(mf);
        return mf;
    }

    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            boolean sort, MediaLibraryStatistics... statistics) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, settingsService.isFastCacheEnabled(),
                statistics);
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
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPath())) {
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
            if (mediaFile.getPath().equals(musicFolder.getPath().getPath())) {
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

        List<MediaFile> storedChildren = mediaFileDao.getChildrenOf(parent.getPath());
        Map<String, MediaFile> storedChildrenMap = new ConcurrentHashMap<>();
        for (MediaFile child : storedChildren) {
            storedChildrenMap.put(child.getPath(), child);
        }

        List<File> children = filterMediaFiles(FileUtil.listFiles(parent.getFile()));
        for (File child : children) {
            if (storedChildrenMap.remove(child.getPath()) == null) {
                // Add children that are not already stored.
                mediaFileDao.createOrUpdateMediaFile(createMediaFile(child, statistics));
            }
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
        return includeMediaFile(candidate.getFile());
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    public boolean includeMediaFile(File candidate) {
        String suffix = FilenameUtils.getExtension(candidate.getName()).toLowerCase();
        return !isExcluded(candidate)
                && (FileUtil.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix));
    }

    private List<File> filterMediaFiles(File... candidates) {
        List<File> result = new ArrayList<>();
        for (File candidate : candidates) {
            if (includeMediaFile(candidate)) {
                result.add(candidate);
            }
        }
        return result;
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

    private boolean isExcluded(File file) {
        if (settingsService.isIgnoreSymLinks() && Files.isSymbolicLink(file.toPath())) {
            if (LOG.isInfoEnabled()) {
                LOG.info("excluding symbolic link " + file.toPath());
            }
            return true;
        }
        String name = file.getName();
        if (settingsService.getExcludePattern() != null && settingsService.getExcludePattern().matcher(name).find()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("excluding file which matches exclude pattern " + settingsService.getExcludePatternString()
                        + ": " + file.toPath());
            }
            return true;
        }

        // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
        return !name.isEmpty() && name.charAt(0) == '.' && !name.startsWith("..") || name.startsWith("@eaDir")
                || "Thumbs.db".equals(name);
    }

    MediaFile createMediaFile(File file, MediaLibraryStatistics... statistics) {

        MediaFile existingFile = mediaFileDao.getMediaFile(file.getPath());

        MediaFile mediaFile = new MediaFile();

        // Variable initial value
        Date lastModified = new Date(getLastModified(file, statistics));
        mediaFile.setChanged(lastModified);
        mediaFile.setCreated(lastModified);

        mediaFile.setLastScanned(existingFile == null ? ZERO_DATE : existingFile.getLastScanned());
        mediaFile.setChildrenLastUpdated(ZERO_DATE);

        mediaFile.setPath(file.getPath());
        mediaFile.setFolder(securityService.getRootFolderForFile(file));
        mediaFile.setParentPath(file.getParent());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (file.isFile()) {
            applyFile(file, mediaFile, statistics);
        } else {
            applyDirectory(file, mediaFile, statistics);
        }
        return mediaFile;
    }

    private void applyFile(File file, MediaFile to, MediaLibraryStatistics... statistics) {
        MetaDataParser parser = metaDataParserFactory.getParser(file);
        if (parser != null) {
            MetaData metaData = parser.getMetaData(file);
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
        String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(to.getPath())));
        to.setFormat(format);
        to.setFileSize(FileUtil.length(file));
        to.setMediaType(getMediaType(to));
    }

    private void applyDirectory(File file, MediaFile to, MediaLibraryStatistics... statistics) {
        // Is this an album?
        if (!isRoot(to)) {
            File[] children = FileUtil.listFiles(file);
            File firstChildMediaFile = getFirstChildMediaFile(children);

            if (firstChildMediaFile == null) {
                to.setArtist(file.getName());
            } else {
                to.setMediaType(MediaFile.MediaType.ALBUM);

                // Guess artist/album name, year and genre.
                MediaFile firstChild = getMediaFile(firstChildMediaFile, mediaFileCache.isEnabled(), statistics);
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
                findCoverArt(children).ifPresent(coverArtOEmbedded -> to.setCoverArtPath(coverArtOEmbedded.getPath()));
            }
            utils.analyze(to);
        }
    }

    private File getFirstChildMediaFile(File... children) {
        for (File child : filterMediaFiles(children)) {
            if (FileUtil.isFile(child)) {
                return child;
            }
        }
        return null;
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
        if (isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPath().toLowerCase();
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
        MediaFile mf = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mf);
        mediaFileCache.remove(mf.getFile());
    }

    public File getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getCoverArtFile() != null) {
            return mediaFile.getCoverArtFile();
        }
        if (!securityService.isReadAllowed(mediaFile.getParentPath())) {
            return null;
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null ? null : parent.getCoverArtFile();
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    @Nullable
    Optional<File> findCoverArt(File... candidates) {

        for (String mask : settingsService.getCoverArtFileTypesAsArray()) {
            for (File candidate : candidates) {
                if (candidate.isFile() && candidate.getName().toUpperCase().endsWith(mask.toUpperCase())
                        && candidate.getName().charAt(0) != '.') {
                    return Optional.ofNullable(candidate);
                }
            }
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        for (File candidate : candidates) {
            if (ParserUtils.isEmbeddedArtworkApplicable(candidate)) {
                return ParserUtils.getEmbeddedArtwork(candidate).isEmpty() ? Optional.empty() : Optional.of(candidate);
            }
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
        for (MediaFile child : mediaFileDao.getChildrenOf(album.getPath())) {
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
