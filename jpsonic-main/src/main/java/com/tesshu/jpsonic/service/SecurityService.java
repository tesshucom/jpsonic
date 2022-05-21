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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.WebFontUtils;
import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import net.sf.ehcache.Ehcache;
import org.apache.commons.lang3.RandomStringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Provides security-related services for authentication and authorization.
 *
 * @author Sindre Mehus
 */
@Service
public class SecurityService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityService.class);
    private static final Pattern NO_TRAVERSAL = Pattern.compile("^(?!.*(\\.\\./|\\.\\.\\\\)).*$");

    private final UserDao userDao;
    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final Ehcache userCache;

    public SecurityService(UserDao userDao, SettingsService settingsService, MusicFolderService musicFolderService,
            Ehcache userCache) {
        super();
        this.userDao = userDao;
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.userCache = userCache;
    }

    /**
     * Returns the selected music folder for a given user, or {@code null} if all music folders should be displayed.
     */
    public MusicFolder getSelectedMusicFolder(String username) {
        UserSettings settings = getUserSettings(username);
        int musicFolderId = settings.getSelectedMusicFolderId();

        MusicFolder musicFolder = musicFolderService.getMusicFolderById(musicFolderId);
        List<MusicFolder> allowedMusicFolders = musicFolderService.getMusicFoldersForUser(username);
        return allowedMusicFolders.contains(musicFolder) ? musicFolder : null;
    }

    /**
     * Returns settings for the given user.
     *
     * @param username
     *            The username.
     *
     * @return User-specific settings. Never <code>null</code>.
     */
    public UserSettings getUserSettings(String username) {
        UserSettings settings = userDao.getUserSettings(username);
        return settings == null ? createDefaultUserSettings(username) : settings;
    }

    private UserSettings createDefaultUserSettings(String username) {

        UserSettings settings = new UserSettings(username);
        settings.setChanged(new Date());
        settings.setFinalVersionNotificationEnabled(true);

        // settings for desktop PC
        settings.setKeyboardShortcutsEnabled(true);
        settings.setDefaultAlbumList(AlbumListType.RANDOM);
        settings.setShowIndex(true);
        settings.setClosePlayQueue(true);
        settings.setAlternativeDrawer(true);
        settings.setAutoHidePlayQueue(true);
        settings.setBreadcrumbIndex(true);
        settings.setAssignAccesskeyToNumber(true);
        settings.setSimpleDisplay(true);
        settings.setQueueFollowingSongs(true);
        settings.setShowCurrentSongInfo(true);
        settings.setSongNotificationEnabled(false);
        settings.setSpeechLangSchemeName(SpeechToTextLangScheme.DEFAULT.name());
        settings.setFontSchemeName(FontScheme.DEFAULT.name());
        settings.setFontFamily(WebFontUtils.DEFAULT_FONT_FAMILY);
        settings.setFontSize(WebFontUtils.DEFAULT_FONT_SIZE);

        // display
        UserSettings.Visibility main = settings.getMainVisibility();
        main.setTrackNumberVisible(true);
        main.setArtistVisible(true);
        main.setComposerVisible(true);
        main.setGenreVisible(true);
        main.setDurationVisible(true);
        UserSettings.Visibility playlist = settings.getPlaylistVisibility();
        playlist.setArtistVisible(true);
        playlist.setAlbumVisible(true);
        playlist.setComposerVisible(true);
        playlist.setGenreVisible(true);
        playlist.setYearVisible(true);
        playlist.setDurationVisible(true);
        playlist.setBitRateVisible(true);
        playlist.setFormatVisible(true);
        playlist.setFileSizeVisible(true);

        // additional display
        settings.setPaginationSize(40);

        return settings;
    }

    public UserSettings createDefaultTabletUserSettings(String username) {
        UserSettings settings = createDefaultUserSettings(username);
        settings.setKeyboardShortcutsEnabled(false);
        settings.setCloseDrawer(true);
        settings.setVoiceInputEnabled(true);
        return settings;
    }

    public UserSettings createDefaultSmartphoneUserSettings(String username) {
        UserSettings settings = createDefaultUserSettings(username);
        settings.setKeyboardShortcutsEnabled(false);
        settings.setDefaultAlbumList(AlbumListType.INDEX);
        settings.setPutMenuInDrawer(true);
        settings.setShowIndex(false);
        settings.setCloseDrawer(true);
        settings.setVoiceInputEnabled(true);
        return settings;
    }

    /**
     * Updates settings for the given username.
     *
     * @param settings
     *            The user-specific settings.
     */
    public void updateUserSettings(UserSettings settings) {
        userDao.updateUserSettings(settings);
    }

    @Override
    public @Nullable UserDetails loadUserByUsername(String username) {
        boolean caseSensitive = true;
        User user = getUserByName(username, caseSensitive);
        if (user == null) {
            throw new UsernameNotFoundException("User \"" + username + "\" was not found.");
        }
        List<GrantedAuthority> authorities = getGrantedAuthorities(username);
        return new org.springframework.security.core.userdetails.User(username, user.getPassword(),
                !user.isLdapAuthenticated(), true, true, true, authorities);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // [AvoidInstantiatingObjectsInLoops] (SimpleGrantedAuthority)
    public List<GrantedAuthority> getGrantedAuthorities(String username) {
        String[] roles = userDao.getRolesForUser(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("IS_AUTHENTICATED_ANONYMOUSLY"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ENGLISH)));
        }
        return authorities;
    }

    @Deprecated
    public @Nullable User getCurrentUser(@NonNull HttpServletRequest request) {
        String username = getCurrentUsername(request);
        return username == null ? null : getUserByName(username);
    }

    public @NonNull User getCurrentUserStrict(@NonNull HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            throw new IllegalArgumentException("User not found");
        }
        User user = getUserByName(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found:" + username);
        }
        return user;
    }

    /**
     * Returns the name of the currently logged-in user.
     *
     * @param request
     *            The HTTP request.
     *
     * @return The name of the logged-in user, or <code>null</code>.
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return new SecurityContextHolderAwareRequestWrapper(request, null).getRemoteUser();
    }

    /**
     * Returns the user with the given username.
     *
     * @param username
     *            The username used when logging in.
     *
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByName(String username) {
        return getUserByName(username, true);
    }

    /**
     * Returns the user with the given username
     *
     * @param username
     *            The username to look for
     * @param caseSensitive
     *            If false, will do a case insensitive search
     *
     * @return The corresponding User
     */
    public User getUserByName(String username, boolean caseSensitive) {
        return userDao.getUserByName(username, caseSensitive);
    }

    public User getGuestUser() {
        // Create guest user if necessary.
        User user = getUserByName(User.USERNAME_GUEST);
        if (user == null) {
            user = new User(User.USERNAME_GUEST, RandomStringUtils.randomAlphanumeric(30), null);
            user.setStreamRole(true);
            createUser(user);
        }
        return user;
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email
     *            The email address.
     *
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    /**
     * Returns whether the given user has administrative rights.
     */
    public boolean isAdmin(String username) {
        User user = getUserByName(username);
        return user != null && user.isAdminRole();
    }

    /**
     * Returns username with admin rights.
     *
     * @return username of admin user
     */
    public String getAdminUsername() {
        for (User user : userDao.getAllUsers()) {
            if (user.isAdminRole()) {
                return user.getUsername();
            }
        }
        return null;
    }

    /**
     * Creates a new user.
     *
     * @param user
     *            The user to create.
     */
    public void createUser(User user) {
        userDao.createUser(user);
        musicFolderService.setMusicFoldersForUser(user.getUsername(),
                MusicFolder.toIdList(musicFolderService.getAllMusicFolders()));
        if (LOG.isInfoEnabled()) {
            LOG.info("Created user " + user.getUsername());
        }
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username
     *            The username.
     */
    public void deleteUser(String username) {
        userDao.deleteUser(username);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted user " + username);
        }
        userCache.remove(username);
    }

    /**
     * Updates the given user.
     *
     * @param user
     *            The user to update.
     */
    public void updateUser(User user) {
        userDao.updateUser(user);
        userCache.remove(user.getUsername());
    }

    /**
     * Updates the byte counts for given user.
     *
     * @param user
     *            The user to update, may be <code>null</code>.
     * @param bytesStreamedDelta
     *            Increment bytes streamed count with this value.
     * @param bytesDownloadedDelta
     *            Increment bytes downloaded count with this value.
     * @param bytesUploadedDelta
     *            Increment bytes uploaded count with this value.
     */
    public void updateUserByteCounts(User user, long bytesStreamedDelta, long bytesDownloadedDelta,
            long bytesUploadedDelta) {
        if (user == null) {
            return;
        }

        user.setBytesStreamed(user.getBytesStreamed() + bytesStreamedDelta);
        user.setBytesDownloaded(user.getBytesDownloaded() + bytesDownloadedDelta);
        user.setBytesUploaded(user.getBytesUploaded() + bytesUploadedDelta);

        userDao.updateUser(user);
    }

    /**
     * Returns whether the given file may be read.
     *
     * @return Whether the given file may be read.
     */
    public boolean isReadAllowed(Path path) {
        // Allowed to read from both music folder and podcast folder.
        return isInMusicFolder(path.toString()) || isInPodcastFolder(path);
    }

    public static boolean isNoTraversal(String path) {
        return NO_TRAVERSAL.matcher(path).matches();
    }

    /**
     * Returns whether the given file may be written, created or deleted.
     *
     * @return Whether the given file may be written, created or deleted.
     */
    public boolean isWriteAllowed(@NonNull Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Illegal path specified: " + path);
        }
        // Only allowed to write podcasts or cover art.
        boolean isPodcast = isInPodcastFolder(path);
        boolean isCoverArt = isInMusicFolder(path.toString()) && fileName.toString().startsWith("cover.");
        return isPodcast || isCoverArt;
    }

    /**
     * Returns whether the given file may be uploaded.
     *
     * @return Whether the given file may be uploaded.
     */
    public boolean isUploadAllowed(Path path) {
        return isInMusicFolder(path.toString()) && !Files.exists(path);
    }

    /**
     * Returns whether the given file is located in one of the music folders (or any of their sub-folders).
     *
     * @param path
     *            The file in question.
     *
     * @return Whether the given file is located in one of the music folders.
     */
    private boolean isInMusicFolder(String path) {
        return getMusicFolderForFile(path) != null;
    }

    private MusicFolder getMusicFolderForFile(String path) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, true);
        for (MusicFolder folder : folders) {
            if (isFileInFolder(path, folder.getPath().getPath())) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Returns whether the given file is located in the Podcast folder (or any of its sub-folders).
     *
     * @param path
     *            The file in question.
     *
     * @return Whether the given file is located in the Podcast folder.
     */
    public boolean isInPodcastFolder(Path path) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(path.toString(), podcastFolder);
    }

    private boolean isInPodcastFolder(String path) {
        String podcastFolder = settingsService.getPodcastFolder();
        return isFileInFolder(path, podcastFolder);
    }

    public String getRootFolderForFile(String path) {
        MusicFolder folder = getMusicFolderForFile(path);
        if (folder != null) {
            return folder.getPath().getPath();
        }

        if (isInPodcastFolder(path)) {
            return settingsService.getPodcastFolder();
        }
        return null;
    }

    public String getRootFolderForFile(Path path) {
        MusicFolder folder = getMusicFolderForFile(path.toString());
        if (folder != null) {
            return folder.getPath().getPath();
        }

        if (isInPodcastFolder(path)) {
            return settingsService.getPodcastFolder();
        }
        return null;
    }

    public boolean isFolderAccessAllowed(MediaFile file, String username) {
        if (isInPodcastFolder(file.toPath())) {
            return true;
        }

        for (MusicFolder musicFolder : musicFolderService.getMusicFoldersForUser(username)) {
            if (musicFolder.getPath().getPath().equals(file.getFolder())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is located in the given folder (or any of its sub-folders). If the given file
     * contains the expression ".." (indicating a reference to the parent directory), this method will return
     * <code>false</code>.
     *
     * @param file
     *            The file in question.
     * @param folder
     *            The folder in question.
     *
     * @return Whether the given file is located in the given folder.
     */
    protected boolean isFileInFolder(final String file, final String folder) {
        if (isEmpty(file)) {
            return false;
        }
        // Deny access if file contains ".." surrounded by slashes (or end of line).
        if (file.matches(".*(/|\\\\)\\.\\.(/|\\\\|$).*")) {
            return false;
        }

        // Convert slashes.
        return StringUtils.startsWithIgnoreCase(file.replace('\\', '/'), folder.replace('\\', '/'));
    }
}
