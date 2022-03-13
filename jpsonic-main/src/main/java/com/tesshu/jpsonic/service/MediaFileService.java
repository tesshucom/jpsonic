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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.metadata.MetaData;
import com.tesshu.jpsonic.service.metadata.MetaDataParser;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import com.tesshu.jpsonic.util.FileUtil;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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
    private final Ehcache mediaFileMemoryCache;
    private final MediaFileDao mediaFileDao;
    private final AlbumDao albumDao;
    private final MetaDataParserFactory metaDataParserFactory;
    private final MediaFileServiceUtils utils;

    private boolean memoryCacheEnabled;

    public MediaFileService(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, Ehcache mediaFileMemoryCache, MediaFileDao mediaFileDao, AlbumDao albumDao,
            MetaDataParserFactory metaDataParserFactory, MediaFileServiceUtils utils) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaFileMemoryCache = mediaFileMemoryCache;
        this.mediaFileDao = mediaFileDao;
        this.albumDao = albumDao;
        this.metaDataParserFactory = metaDataParserFactory;
        this.utils = utils;
        memoryCacheEnabled = true;
    }

    /**
     * Returns a media file instance for the given file. If possible, a cached value is returned.
     *
     * @param file
     *            A file on the local file system.
     *
     * @return A media file instance, or null if not found.
     *
     * @throws SecurityException
     *             If access is denied to the given file.
     */
    public MediaFile getMediaFile(File file) {
        return getMediaFile(file, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns a media file instance for the given file. If possible, a cached value is returned.
     *
     * @param file
     *            A file on the local file system.
     *
     * @return A media file instance, or null if not found.
     *
     * @throws SecurityException
     *             If access is denied to the given file.
     */
    public MediaFile getMediaFile(File file, boolean useFastCache) {

        // Look in fast memory cache first.
        MediaFile result = getFromMemoryCache(file);
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
            putInMemoryCache(file, result);
            return result;
        }

        if (!FileUtil.exists(file)) {
            return null;
        }
        // Not found in database, must read from disk.
        result = createMediaFile(file);

        // Put in cache and database.
        putInMemoryCache(file, result);
        mediaFileDao.createOrUpdateMediaFile(result);

        return result;
    }

    /**
     * Returns a media file instance for the given path name. If possible, a cached value is returned.
     *
     * @param pathName
     *            A path name for a file on the local file system.
     *
     * @return A media file instance.
     *
     * @throws SecurityException
     *             If access is denied to the given file.
     */
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

    MediaFile checkLastModified(final MediaFile mediaFile, boolean useFastCache) {

        // Determine if the file has not changed
        if (useFastCache) {
            return mediaFile;
        } else if (mediaFile.getVersion() >= MediaFileDao.VERSION) {
            FileModifiedCheckScheme scheme = FileModifiedCheckScheme
                    .valueOf(settingsService.getFileModifiedCheckSchemeName());
            switch (scheme) {
            case LAST_MODIFIED:
                if (!settingsService.isIgnoreFileTimestamps()
                        && mediaFile.getChanged().getTime() >= FileUtil.lastModified(mediaFile.getFile())
                        && !MediaFileDao.ZERO_DATE.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            case LAST_SCANNED:
                if (!MediaFileDao.ZERO_DATE.equals(mediaFile.getLastScanned())) {
                    return mediaFile;
                }
                break;
            default:
                break;
            }
        }

        // Updating database file from disk
        MediaFile mf = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mf);
        return mf;
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles
     *            Whether files should be included in the result.
     * @param includeDirectories
     *            Whether directories should be included in the result.
     * @param sort
     *            Whether to sort files in the same directory.
     *
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            boolean sort) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles
     *            Whether files should be included in the result.
     * @param includeDirectories
     *            Whether directories should be included in the result.
     * @param sort
     *            Whether to sort files in the same directory.
     *
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories,
            boolean sort, boolean useFastCache) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        // Make sure children are stored and up-to-date in the database.
        if (!useFastCache) {
            updateChildren(parent);
        }

        List<MediaFile> result = new ArrayList<>();
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPath())) {
            MediaFile checked = checkLastModified(child, useFastCache);
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

    /**
     * Returns whether the given file is the root of a media folder.
     *
     * @see MusicFolder
     */
    public boolean isRoot(MediaFile mediaFile) {
        for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders(false, true)) {
            if (mediaFile.getPath().equals(musicFolder.getPath().getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all genres in the music collection.
     *
     * @param sortByAlbum
     *            Whether to sort by album count, rather than song count.
     *
     * @return Sorted list of genres.
     *
     * @Deprecated Use {@link SearchService} {@link #getGenres(boolean)}.
     */
    @Deprecated
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getNewestAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param username
     *            Returns albums starred by this user.
     * @param musicFolders
     *            Only return albums from these folders.
     *
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbums(offset, count, username, musicFolders);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param byArtist
     *            Whether to sort by artist name
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist,
            List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param fromYear
     *            The first year in the range.
     * @param toYear
     *            The last year in the range.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear,
            List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param genre
     *            The genre name.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return Albums in the genre.
     *
     * @Deprecated Use {@link SearchService}{@link #getAlbumsByGenre(int, int, String, List)}
     */
    @Deprecated
    public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByGenre(offset, count, genre, musicFolders);
    }

    /**
     * Returns random songs for the given parent.
     *
     * @param parent
     *            The parent.
     * @param count
     *            Max number of songs to return.
     *
     * @return Random songs.
     */
    public List<MediaFile> getRandomSongsForParent(MediaFile parent, int count) {
        List<MediaFile> children = getDescendantsOf(parent, false);
        removeVideoFiles(children);

        if (children.isEmpty()) {
            return children;
        }
        Collections.shuffle(children);
        return children.subList(0, Math.min(count, children.size()));
    }

    /**
     * Returns random songs matching search criteria.
     *
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, String username) {
        return mediaFileDao.getRandomSongs(criteria, username);
    }

    /**
     * Removes video files from the given list.
     */
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

    private void updateChildren(MediaFile parent) {

        FileModifiedCheckScheme checkScheme = FileModifiedCheckScheme
                .valueOf(settingsService.getFileModifiedCheckSchemeName());

        /*
         * LAST_MODIFIED : Check timestamps. LAST_SCANNED : Albums other than those specified by the user or newly added
         * are considered unchanged and skipped. Others (DIRECTORY) do not access the update date and are all subject to
         * update check.
         */
        if (FileModifiedCheckScheme.LAST_MODIFIED == checkScheme
                && parent.getChildrenLastUpdated().getTime() >= parent.getChanged().getTime()) {
            return;
        } else if (FileModifiedCheckScheme.LAST_SCANNED == checkScheme && parent.getMediaType() == MediaType.ALBUM
                && !MediaFileDao.ZERO_DATE.equals(parent.getLastScanned())) {
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
                mediaFileDao.createOrUpdateMediaFile(createMediaFile(child));
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
        for (String s : settingsService.getMusicFileTypesAsArray()) {
            if (suffix.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions")
    private boolean isVideoFile(String suffix) {
        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (suffix.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is excluded.
     *
     * @param file
     *            The child file in question.
     *
     * @return Whether the child file is excluded.
     */
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

    private MediaFile createMediaFile(File file) {

        MediaFile existingFile = mediaFileDao.getMediaFile(file.getPath());

        MediaFile mediaFile = new MediaFile();
        Date lastModified = new Date(FileUtil.lastModified(file));
        mediaFile.setPath(file.getPath());
        mediaFile.setFolder(securityService.getRootFolderForFile(file));
        mediaFile.setParentPath(file.getParent());
        mediaFile.setChanged(lastModified);
        mediaFile.setLastScanned(existingFile == null ? MediaFileDao.ZERO_DATE : existingFile.getLastScanned());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setChildrenLastUpdated(new Date(0));
        mediaFile.setCreated(lastModified);
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (file.isFile()) {
            applyFile(file, mediaFile);
        } else {
            applyDirectory(file, mediaFile);
        }
        return mediaFile;
    }

    private void applyFile(File file, MediaFile to) {
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
        }
        String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(to.getPath())));
        to.setFormat(format);
        to.setFileSize(FileUtil.length(file));
        to.setMediaType(getMediaType(to));
    }

    private void applyDirectory(File file, MediaFile to) {
        // Is this an album?
        if (!isRoot(to)) {
            File[] children = FileUtil.listFiles(file);
            File firstChildMediaFile = getFirstChildMediaFile(children);

            if (firstChildMediaFile == null) {
                to.setArtist(file.getName());
            } else {
                to.setMediaType(MediaFile.MediaType.ALBUM);

                // Guess artist/album name, year and genre.
                MetaDataParser parser = metaDataParserFactory.getParser(firstChildMediaFile);
                if (parser != null) {
                    MetaData metaData = parser.getMetaData(firstChildMediaFile);
                    to.setArtist(metaData.getAlbumArtist());
                    to.setArtistSort(metaData.getAlbumArtistSort());
                    to.setArtistSortRaw(metaData.getAlbumArtistSort());
                    to.setAlbumName(metaData.getAlbumName());
                    to.setAlbumSort(metaData.getAlbumSort());
                    to.setAlbumSortRaw(metaData.getAlbumSort());
                    to.setYear(metaData.getYear());
                    to.setGenre(metaData.getGenre());
                }

                // Look for cover art.
                File coverArtOEmbedded = findCoverArt(children);
                if (coverArtOEmbedded != null) {
                    to.setCoverArtPath(coverArtOEmbedded.getPath());
                }
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
        mediaFileMemoryCache.remove(mf.getFile());
    }

    private void putInMemoryCache(File file, MediaFile mediaFile) {
        if (memoryCacheEnabled) {
            mediaFileMemoryCache.put(new Element(file, mediaFile));
        }
    }

    private MediaFile getFromMemoryCache(File file) {
        if (!memoryCacheEnabled) {
            return null;
        }
        Element element = mediaFileMemoryCache.get(file);
        return element == null ? null : (MediaFile) element.getObjectValue();
    }

    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
        if (!memoryCacheEnabled) {
            mediaFileMemoryCache.removeAll();
        }
    }

    /**
     * Returns a cover art image for the given media file.
     */
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
    File findCoverArt(File... candidates) {

        for (String mask : settingsService.getCoverArtFileTypesAsArray()) {
            for (File candidate : candidates) {
                if (candidate.isFile() && candidate.getName().toUpperCase().endsWith(mask.toUpperCase())
                        && candidate.getName().charAt(0) != '.') {
                    return candidate;
                }
            }
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        for (File candidate : candidates) {
            if (ParserUtils.isArtworkApplicable(candidate)) {
                return ParserUtils.getArtwork(getMediaFile(candidate)) == null ? null : candidate;
            }
        }
        return null;
    }

    /**
     * Returns all media files that are children, grand-children etc of a given media file. Directories are not included
     * in the result.
     *
     * @param sort
     *            Whether to sort files in the same directory.
     *
     * @return All descendant music files.
     */
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

    public void updateMediaFile(MediaFile mediaFile) {
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
    }

    /**
     * Increments the play count and last played date for the given media file and its directory and album.
     */
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

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumCount(musicFolders);
    }

    public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getPlayedAlbumCount(musicFolders);
    }

    public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbumCount(username, musicFolders);
    }

    public void clearMemoryCache() {
        mediaFileMemoryCache.removeAll();
    }

    public void resetLastScanned(MediaFile album) {
        mediaFileDao.resetLastScanned(album.getId());
        for (MediaFile child : mediaFileDao.getChildrenOf(album.getPath())) {
            mediaFileDao.resetLastScanned(child.getId());
        }
    }
}
