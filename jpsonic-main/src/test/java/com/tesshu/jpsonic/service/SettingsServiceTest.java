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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link SettingsService}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
class SettingsServiceTest {

    @Autowired
    private SettingsService settingsService;

    @Test
    void testJpsonicHome() {
        String homePath = System.getProperty("jpsonic.home");
        assertEquals(homePath, SettingsService.getJpsonicHome().getAbsolutePath(), "Wrong Jpsonic home.");
    }

    @Test
    void testDefaultValues() {
        assertEquals("ja", settingsService.getLocale().getLanguage(), "Wrong default language.");
        assertEquals(1, settingsService.getIndexCreationInterval(), "Wrong default index creation interval.");
        assertEquals(3, settingsService.getIndexCreationHour(), "Wrong default index creation hour.");
        assertTrue(settingsService.getPlaylistFolder().endsWith("playlists"), "Wrong default playlist folder.");
        assertEquals("jpsonic", settingsService.getThemeId(), "Wrong default theme.");
        assertEquals(10, settingsService.getPodcastEpisodeRetentionCount(),
                "Wrong default Podcast episode retention count.");
        assertEquals(1, settingsService.getPodcastEpisodeDownloadCount(),
                "Wrong default Podcast episode download count.");
        assertTrue(settingsService.getPodcastFolder().endsWith("Podcast"), "Wrong default Podcast folder.");
        assertEquals(24, settingsService.getPodcastUpdateInterval(), "Wrong default Podcast update interval.");
        assertFalse(settingsService.isLdapEnabled(), "Wrong default LDAP enabled.");
        assertEquals("ldap://host.domain.com:389/cn=Users,dc=domain,dc=com", settingsService.getLdapUrl(),
                "Wrong default LDAP URL.");
        assertNull(settingsService.getLdapManagerDn(), "Wrong default LDAP manager DN.");
        assertNull(settingsService.getLdapManagerPassword(), "Wrong default LDAP manager password.");
        assertEquals("(sAMAccountName={0})", settingsService.getLdapSearchFilter(),
                "Wrong default LDAP search filter.");
        assertFalse(settingsService.isLdapAutoShadowing(), "Wrong default LDAP auto-shadowing.");
    }

    @Test
    void testChangeSettings() {
        settingsService.setIndexString("indexString");
        settingsService.setIgnoredArticles("a the foo bar");
        settingsService.setShortcuts("new incoming \"rock 'n' roll\"");
        settingsService.setPlaylistFolder("playlistFolder");
        settingsService.setMusicFileTypes("mp3 ogg  aac");
        settingsService.setCoverArtFileTypes("jpeg gif  png");
        settingsService.setWelcomeMessage("welcomeMessage");
        settingsService.setLoginMessage("loginMessage");
        settingsService.setLocale(Locale.CANADA_FRENCH);
        settingsService.setThemeId("dark");
        settingsService.setIndexCreationInterval(4);
        settingsService.setIndexCreationHour(9);
        settingsService.setPodcastEpisodeRetentionCount(5);
        settingsService.setPodcastEpisodeDownloadCount(-1);
        settingsService.setPodcastFolder("d:/podcasts");
        settingsService.setPodcastUpdateInterval(-1);
        settingsService.setLdapEnabled(true);
        settingsService.setLdapUrl("newLdapUrl");
        settingsService.setLdapManagerDn("admin");
        settingsService.setLdapManagerPassword("secret");
        settingsService.setLdapSearchFilter("newLdapSearchFilter");
        settingsService.setLdapAutoShadowing(true);

        verifySettings(settingsService);

        settingsService.save();
        verifySettings(settingsService);
    }

    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // containsExactly
    private void verifySettings(SettingsService ss) {
        assertEquals("indexString", ss.getIndexString(), "Wrong index string.");
        assertEquals("a the foo bar", ss.getIgnoredArticles(), "Wrong ignored articles.");
        assertEquals("new incoming \"rock 'n' roll\"", ss.getShortcuts(), "Wrong shortcuts.");
        Assertions.assertArrayEquals(new String[] { "a", "the", "foo", "bar" }, ss.getIgnoredArticlesAsArray(),
                "Wrong ignored articles array.");
        Assertions.assertArrayEquals(new String[] { "new", "incoming", "rock 'n' roll" }, ss.getShortcutsAsArray(),
                "Wrong shortcut array.");
        assertEquals("playlistFolder", ss.getPlaylistFolder(), "Wrong playlist folder.");
        assertEquals("mp3 ogg  aac", ss.getMusicFileTypes(), "Wrong music mask.");
        Assertions.assertArrayEquals(new String[] { "mp3", "ogg", "aac" }, ss.getMusicFileTypesAsArray(),
                "Wrong music mask array.");
        assertEquals("jpeg gif  png", ss.getCoverArtFileTypes(), "Wrong cover art mask.");
        Assertions.assertArrayEquals(new String[] { "jpeg", "gif", "png" }, ss.getCoverArtFileTypesAsArray(),
                "Wrong cover art mask array.");
        assertEquals("welcomeMessage", ss.getWelcomeMessage(), "Wrong welcome message.");
        assertEquals("loginMessage", ss.getLoginMessage(), "Wrong login message.");
        assertEquals(Locale.CANADA_FRENCH, ss.getLocale(), "Wrong locale.");
        assertEquals("dark", ss.getThemeId(), "Wrong theme.");
        assertEquals(4, ss.getIndexCreationInterval(), "Wrong index creation interval.");
        assertEquals(9, ss.getIndexCreationHour(), "Wrong index creation hour.");
        assertEquals(5, settingsService.getPodcastEpisodeRetentionCount(), "Wrong Podcast episode retention count.");
        assertEquals(-1, settingsService.getPodcastEpisodeDownloadCount(), "Wrong Podcast episode download count.");
        assertEquals("d:/podcasts", settingsService.getPodcastFolder(), "Wrong Podcast folder.");
        assertEquals(-1, settingsService.getPodcastUpdateInterval(), "Wrong Podcast update interval.");
        assertTrue(settingsService.isLdapEnabled(), "Wrong LDAP enabled.");
        assertEquals("newLdapUrl", settingsService.getLdapUrl(), "Wrong LDAP URL.");
        assertEquals("admin", settingsService.getLdapManagerDn(), "Wrong LDAP manager DN.");
        assertEquals("secret", settingsService.getLdapManagerPassword(), "Wrong LDAP manager password.");
        assertEquals("newLdapSearchFilter", settingsService.getLdapSearchFilter(), "Wrong LDAP search filter.");
        assertTrue(settingsService.isLdapAutoShadowing(), "Wrong LDAP auto-shadowing.");
    }
}
