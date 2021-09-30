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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit test of {@link UserDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class UserDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from user_role");
        daoHelper.getJdbcTemplate().execute("delete from user");
    }

    @Test
    void testCreateUser() {
        User user = new User("sindre", "secret", "sindre@activeobjects.no", false, 1000L, 2000L, 3000L);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        User newUser = userDao.getAllUsers().get(0);
        assertUserEquals(user, newUser);
    }

    @Test
    void testCreateUserTransactionalError() {
        User user = new User("muff1nman", "secret", "noemail") {
            @Override
            public boolean isPlaylistRole() {
                throw new IllegalStateException();
            }
        };

        user.setAdminRole(true);
        int beforeSize = userDao.getAllUsers().size();
        Assertions.assertThrows(RuntimeException.class, () -> userDao.createUser(user),
                "It was expected for createUser to throw an exception");
        assertEquals(beforeSize, userDao.getAllUsers().size());
    }

    @Test
    void testUpdateUser() {
        User user = new User("sindre", "secret", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        user.setPassword("foo");
        user.setEmail("sindre@foo.bar");
        user.setLdapAuthenticated(true);
        user.setBytesStreamed(1);
        user.setBytesDownloaded(2);
        user.setBytesUploaded(3);
        user.setAdminRole(false);
        user.setCommentRole(false);
        user.setCoverArtRole(false);
        user.setDownloadRole(true);
        user.setPlaylistRole(false);
        user.setUploadRole(true);
        user.setPodcastRole(false);
        user.setStreamRole(false);
        user.setSettingsRole(false);
        userDao.updateUser(user);

        User newUser = userDao.getAllUsers().get(0);
        assertUserEquals(user, newUser);
        assertEquals(1, newUser.getBytesStreamed(), "Wrong bytes streamed.");
        assertEquals(2, newUser.getBytesDownloaded(), "Wrong bytes downloaded.");
        assertEquals(3, newUser.getBytesUploaded(), "Wrong bytes uploaded.");
    }

    @Test
    void testGetUserByName() {
        User user = new User("sindre", "secret", null);
        userDao.createUser(user);

        User newUser = userDao.getUserByName("sindre", true);
        Assertions.assertNotNull(newUser, "Error in getUserByName().");
        assertUserEquals(user, newUser);

        assertNull(userDao.getUserByName("sindre2", true), "Error in getUserByName().");
        Assertions.assertNotNull(userDao.getUserByName("sindre ", true), "Error in getUserByName().");
        assertNull(userDao.getUserByName("bente", true), "Error in getUserByName().");
        assertNull(userDao.getUserByName("", true), "Error in getUserByName().");
        assertNull(userDao.getUserByName(null, true), "Error in getUserByName().");
    }

    @Test
    void testDeleteUser() {
        assertEquals(0, userDao.getAllUsers().size(), "Wrong number of users.");

        userDao.createUser(new User("sindre", "secret", null));
        assertEquals(1, userDao.getAllUsers().size(), "Wrong number of users.");

        userDao.createUser(new User("bente", "secret", null));
        assertEquals(2, userDao.getAllUsers().size(), "Wrong number of users.");

        userDao.deleteUser("sindre");
        assertEquals(1, userDao.getAllUsers().size(), "Wrong number of users.");

        userDao.deleteUser("bente");
        assertEquals(0, userDao.getAllUsers().size(), "Wrong number of users.");
    }

    @Test
    void testGetRolesForUser() {
        User user = new User("sindre", "secret", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user);

        String[] roles = userDao.getRolesForUser("sindre");
        assertEquals(5, roles.length, "Wrong number of roles.");
        assertEquals("admin", roles[0], "Wrong role.");
        assertEquals("comment", roles[1], "Wrong role.");
        assertEquals("podcast", roles[2], "Wrong role.");
        assertEquals("stream", roles[3], "Wrong role.");
        assertEquals("settings", roles[4], "Wrong role.");
    }

    @Test
    void testCreateDefaultUserSettingsWithNonExist() throws ExecutionException {
        assertNull(userDao.getUserSettings("sindre"), "Error in getUserSettings.");
        SecurityService mockService = new SecurityService(Mockito.mock(UserDao.class), null, null, null);
        UserSettings userSettings = mockService.getUserSettings("sindre");
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> userDao.updateUserSettings(userSettings));
    }

    @Test
    void testUserSettings() throws ExecutionException {
        userDao.createUser(new User("sindre", "secret", null));
        assertNull(userDao.getUserSettings("sindre"), "Error in getUserSettings.");

        SecurityService mockService = new SecurityService(Mockito.mock(UserDao.class), null, null, null);
        userDao.updateUserSettings(mockService.getUserSettings("sindre"));

        UserSettings userSettings = userDao.getUserSettings("sindre");
        Assertions.assertNotNull(userSettings, "Error in getUserSettings().");
        assertNull(userSettings.getLocale(), "Error in getUserSettings().");
        assertNull(userSettings.getThemeId(), "Error in getUserSettings().");
        assertTrue(userSettings.isFinalVersionNotificationEnabled(), "Error in getUserSettings().");
        assertFalse(userSettings.isBetaVersionNotificationEnabled(), "Error in getUserSettings().");
        assertFalse(userSettings.isSongNotificationEnabled(), "Error in getUserSettings().");
        assertFalse(userSettings.isCloseDrawer(), "Error in getUserSettings().");
        assertFalse(userSettings.isLastFmEnabled(), "Error in getUserSettings().");
        assertNull(userSettings.getLastFmUsername(), "Error in getUserSettings().");
        assertNull(userSettings.getLastFmPassword(), "Error in getUserSettings().");
        assertFalse(userSettings.isListenBrainzEnabled(), "Error in getUserSettings().");
        assertNull(userSettings.getListenBrainzToken(), "Error in getUserSettings().");
        Assertions.assertSame(TranscodeScheme.OFF, userSettings.getTranscodeScheme(), "Error in getUserSettings().");
        assertFalse(userSettings.isShowNowPlayingEnabled(), "Error in getUserSettings().");
        assertEquals(-1, userSettings.getSelectedMusicFolderId(), "Error in getUserSettings().");
        assertFalse(userSettings.isPartyModeEnabled(), "Error in getUserSettings().");
        assertFalse(userSettings.isNowPlayingAllowed(), "Error in getUserSettings().");
        Assertions.assertSame(AvatarScheme.NONE, userSettings.getAvatarScheme(), "Error in getUserSettings().");
        assertEquals(Integer.valueOf(101), userSettings.getSystemAvatarId(), "Error in getUserSettings().");
        assertTrue(userSettings.isKeyboardShortcutsEnabled(), "Error in getUserSettings().");
        assertEquals(40, userSettings.getPaginationSize(), "Error in getUserSettings().");

        UserSettings settings = mockService.getUserSettings("sindre");
        settings.setLocale(Locale.SIMPLIFIED_CHINESE);
        settings.setThemeId("midnight");
        settings.setBetaVersionNotificationEnabled(true);
        settings.setSongNotificationEnabled(false);
        settings.setCloseDrawer(true);
        settings.getMainVisibility().setBitRateVisible(true);
        settings.getPlaylistVisibility().setYearVisible(true);

        settings.getMainVisibility().setComposerVisible(true);
        settings.getMainVisibility().setGenreVisible(true);
        settings.getPlaylistVisibility().setComposerVisible(true);
        settings.getPlaylistVisibility().setGenreVisible(true);

        settings.setLastFmEnabled(true);
        settings.setLastFmUsername("last_user");
        settings.setLastFmPassword("last_pass");
        settings.setListenBrainzEnabled(true);
        settings.setListenBrainzToken("01234567-89ab-cdef-0123-456789abcdef");
        settings.setTranscodeScheme(TranscodeScheme.MAX_256);
        settings.setShowNowPlayingEnabled(false);
        settings.setSelectedMusicFolderId(3);
        settings.setPartyModeEnabled(true);
        settings.setNowPlayingAllowed(true);
        settings.setAvatarScheme(AvatarScheme.SYSTEM);
        settings.setSystemAvatarId(102);
        settings.setChanged(new Date(9412L));
        settings.setKeyboardShortcutsEnabled(true);
        settings.setPaginationSize(120);

        userDao.updateUserSettings(settings);
        userSettings = userDao.getUserSettings("sindre");
        Assertions.assertNotNull(userSettings, "Error in getUserSettings().");
        assertEquals(Locale.SIMPLIFIED_CHINESE, userSettings.getLocale(), "Error in getUserSettings().");
        assertTrue(userSettings.isFinalVersionNotificationEnabled(), "Error in getUserSettings().");
        assertTrue(userSettings.isBetaVersionNotificationEnabled(), "Error in getUserSettings().");
        assertFalse(userSettings.isSongNotificationEnabled(), "Error in getUserSettings().");
        assertTrue(userSettings.isCloseDrawer(), "Error in getUserSettings().");
        assertEquals("midnight", userSettings.getThemeId(), "Error in getUserSettings().");
        assertTrue(userSettings.getMainVisibility().isBitRateVisible(), "Error in getUserSettings().");
        assertTrue(userSettings.getPlaylistVisibility().isYearVisible(), "Error in getUserSettings().");

        assertTrue(userSettings.getMainVisibility().isComposerVisible(), "Error in getUserSettings().");
        assertTrue(userSettings.getMainVisibility().isGenreVisible(), "Error in getUserSettings().");
        assertTrue(userSettings.getPlaylistVisibility().isComposerVisible(), "Error in getUserSettings().");
        assertTrue(userSettings.getPlaylistVisibility().isGenreVisible(), "Error in getUserSettings().");

        assertTrue(userSettings.isLastFmEnabled(), "Error in getUserSettings().");
        assertEquals("last_user", userSettings.getLastFmUsername(), "Error in getUserSettings().");
        assertEquals("last_pass", userSettings.getLastFmPassword(), "Error in getUserSettings().");
        assertTrue(userSettings.isListenBrainzEnabled(), "Error in getUserSettings().");
        assertEquals("01234567-89ab-cdef-0123-456789abcdef", userSettings.getListenBrainzToken(),
                "Error in getUserSettings().");
        Assertions.assertSame(TranscodeScheme.MAX_256, userSettings.getTranscodeScheme(),
                "Error in getUserSettings().");
        assertFalse(userSettings.isShowNowPlayingEnabled(), "Error in getUserSettings().");
        assertEquals(3, userSettings.getSelectedMusicFolderId(), "Error in getUserSettings().");
        assertTrue(userSettings.isPartyModeEnabled(), "Error in getUserSettings().");
        assertTrue(userSettings.isNowPlayingAllowed(), "Error in getUserSettings().");
        Assertions.assertSame(AvatarScheme.SYSTEM, userSettings.getAvatarScheme(), "Error in getUserSettings().");
        assertEquals(102, userSettings.getSystemAvatarId().intValue(), "Error in getUserSettings().");
        assertEquals(new Date(9412L), userSettings.getChanged(), "Error in getUserSettings().");
        assertTrue(userSettings.isKeyboardShortcutsEnabled(), "Error in getUserSettings().");
        assertEquals(120, userSettings.getPaginationSize(), "Error in getUserSettings().");

        userDao.deleteUser("sindre");
        assertNull(userDao.getUserSettings("sindre"), "Error in cascading delete.");
    }

    private void assertUserEquals(User expected, User actual) {
        assertEquals(expected.getUsername(), actual.getUsername(), "Wrong name.");
        assertEquals(expected.getPassword(), actual.getPassword(), "Wrong password.");
        assertEquals(expected.getEmail(), actual.getEmail(), "Wrong email.");
        assertEquals(expected.isLdapAuthenticated(), actual.isLdapAuthenticated(), "Wrong LDAP auth.");
        assertEquals(expected.getBytesStreamed(), actual.getBytesStreamed(), "Wrong bytes streamed.");
        assertEquals(expected.getBytesDownloaded(), actual.getBytesDownloaded(), "Wrong bytes downloaded.");
        assertEquals(expected.getBytesUploaded(), actual.getBytesUploaded(), "Wrong bytes uploaded.");
        assertEquals(expected.isAdminRole(), actual.isAdminRole(), "Wrong admin role.");
        assertEquals(expected.isCommentRole(), actual.isCommentRole(), "Wrong comment role.");
        assertEquals(expected.isCoverArtRole(), actual.isCoverArtRole(), "Wrong cover art role.");
        assertEquals(expected.isDownloadRole(), actual.isDownloadRole(), "Wrong download role.");
        assertEquals(expected.isPlaylistRole(), actual.isPlaylistRole(), "Wrong playlist role.");
        assertEquals(expected.isUploadRole(), actual.isUploadRole(), "Wrong upload role.");
        assertEquals(expected.isDownloadRole(), actual.isDownloadRole(), "Wrong download role.");
        assertEquals(expected.isStreamRole(), actual.isStreamRole(), "Wrong stream role.");
        assertEquals(expected.isSettingsRole(), actual.isSettingsRole(), "Wrong settings role.");
    }
}
