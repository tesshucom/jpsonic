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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.controller.WebFontUtils;
import com.tesshu.jpsonic.domain.system.AlbumListType;
import com.tesshu.jpsonic.domain.system.FontScheme;
import com.tesshu.jpsonic.domain.system.SpeechToTextLangScheme;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.persistence.core.entity.UserSettings;
import com.tesshu.jpsonic.persistence.core.repository.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

/**
 * Service for managing user accounts, authentication context, and personal
 * settings.
 * <p>
 * This class serves as the primary authority for user-centric data and
 * lifecycle management, including profile updates, security roles, and
 * session-specific state such as the selected music folder cursor.
 * </p>
 */
@Service
@DependsOn("musicFolderService")
public class UserService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;
    private final MusicFolderService musicFolderService;

    public UserService(UserDao userDao, MusicFolderService musicFolderService) {
        super();
        this.userDao = userDao;
        this.musicFolderService = musicFolderService;
    }

    /**
     * Returns settings for the given user.
     *
     * @param username The username.
     *
     * @return User-specific settings. Never <code>null</code>.
     */
    public @NonNull UserSettings getUserSettings(String username) {
        UserSettings settings = userDao.getUserSettings(username);
        return settings == null ? createDefaultUserSettings(username) : settings;
    }

    private @NonNull UserSettings createDefaultUserSettings(String username) {

        UserSettings settings = new UserSettings(username);
        settings.setChanged(now());

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

    public @NonNull UserSettings createDefaultTabletUserSettings(String username) {
        UserSettings settings = createDefaultUserSettings(username);
        settings.setKeyboardShortcutsEnabled(false);
        settings.setCloseDrawer(true);
        settings.setVoiceInputEnabled(true);
        return settings;
    }

    public @NonNull UserSettings createDefaultSmartphoneUserSettings(String username) {
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
     * @param settings The user-specific settings.
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

    public List<GrantedAuthority> getGrantedAuthorities(String username) {
        List<String> roles = userDao.getRolesForUser(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("IS_AUTHENTICATED_ANONYMOUSLY"));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ENGLISH)));
        }
        return authorities;
    }

    public @Nullable User getCurrentUser(@NonNull HttpServletRequest request) {
        String username = getCurrentUsername(request);
        return username == null ? null : getUserByName(username);
    }

    // TODO For REST, fix to return response instead of exception.
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

    public @Nullable String getCurrentUsername(HttpServletRequest request) {
        return new SecurityContextHolderAwareRequestWrapper(request, null).getRemoteUser();
    }

    public @NonNull String getCurrentUsernameStrict(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            throw new IllegalArgumentException("User not found");
        }
        return username;
    }

    public @Nullable User getUserByName(String username) {
        return getUserByName(username, true);
    }

    public @Nullable User getUserByName(String username, boolean caseSensitive) {
        return userDao.getUserByName(username, caseSensitive);
    }

    public @NonNull User getUserByNameStrict(String username) {
        User user = getUserByName(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found:" + username);
        }
        return user;
    }

    public User getGuestUser() {
        // Create guest user if necessary.
        User user = getUserByName(User.USERNAME_GUEST);
        if (user == null) {
            user = new User(User.USERNAME_GUEST, RandomStringUtils.secure().nextAlphanumeric(30),
                    null);
            user.setStreamRole(true);
            createUser(user);
        }
        return user;
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email The email address.
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
     * @param user The user to create.
     */
    public void createUser(User user) {
        userDao.createUser(user);
        musicFolderService
            .setMusicFoldersForUser(user.getUsername(),
                    MusicFolder.toIdList(musicFolderService.getAllMusicFolders()));
        if (LOG.isInfoEnabled()) {
            LOG.info("Created user " + user.getUsername());
        }
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username The username.
     */
    public void deleteUser(String username) {
        userDao.deleteUser(username);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted user " + username);
        }
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    public void updateUser(User user) {
        userDao.updateUser(user);
    }

    public void updatePassword(User user, String newPass, boolean ldapAuthenticated) {
        userDao.updatePassword(user, newPass, ldapAuthenticated);
    }

    /**
     * Updates the byte counts for given user.
     *
     * @param user                 The user to update, may be <code>null</code>.
     * @param bytesStreamedDelta   Increment bytes streamed count with this value.
     * @param bytesDownloadedDelta Increment bytes downloaded count with this value.
     * @param bytesUploadedDelta   Increment bytes uploaded count with this value.
     */
    public void updateUserByteCounts(User user, long bytesStreamedDelta, long bytesDownloadedDelta,
            long bytesUploadedDelta) {
        if (user == null) {
            return;
        }
        userDao
            .updateUserByteCounts(user.getBytesStreamed() + bytesStreamedDelta,
                    user.getBytesDownloaded() + bytesDownloadedDelta,
                    user.getBytesUploaded() + bytesUploadedDelta, user.getUsername());
    }

    /**
     * Although this functions as a folder cursor, it explicitly re-verifies access
     * to handle cases where an admin revokes permissions during a user's session.
     */
    @Nullable
    MusicFolder resolveAllowedMusicFolder(MusicFolder musicFolder, String username) {
        List<MusicFolder> allowedMusicFolders = musicFolderService.getMusicFoldersForUser(username);
        return allowedMusicFolders.contains(musicFolder) ? musicFolder : null;
    }

    /**
     * Returns the selected music folder for a given user, or {@code null} if all
     * music folders should be displayed.
     */
    public @Nullable MusicFolder getSelectedMusicFolder(String username) {
        UserSettings settings = getUserSettings(username);
        MusicFolder musicFolder = musicFolderService
            .getMusicFolderById(settings.getSelectedMusicFolderId());
        return resolveAllowedMusicFolder(musicFolder, username);
    }
}
