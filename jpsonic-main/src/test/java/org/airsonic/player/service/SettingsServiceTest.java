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

package org.airsonic.player.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link SettingsService}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
public class SettingsServiceTest extends AbstractAirsonicHomeTest {

    @Autowired
    private SettingsService settingsService;

    @Before
    public void setUp() {
        String jpsonicHome = TestCaseUtils.jpsonicHomePathForTest();
        System.setProperty("jpsonic.home", jpsonicHome);
        new File(jpsonicHome, "jpsonic.properties").delete();
    }

    @Test
    public void testJpsonicHome() {
        assertEquals("Wrong Jpsonic home.", TestCaseUtils.jpsonicHomePathForTest(),
                SettingsService.getJpsonicHome().getAbsolutePath());
    }

    @Test
    public void testDefaultValues() {
        assertEquals("Wrong default language.", "ja", settingsService.getLocale().getLanguage());
        assertEquals("Wrong default index creation interval.", 1, settingsService.getIndexCreationInterval());
        assertEquals("Wrong default index creation hour.", 3, settingsService.getIndexCreationHour());
        assertTrue("Wrong default playlist folder.", settingsService.getPlaylistFolder().endsWith("playlists"));
        assertEquals("Wrong default theme.", "jpsonic", settingsService.getThemeId());
        assertEquals("Wrong default Podcast episode retention count.", 10,
                settingsService.getPodcastEpisodeRetentionCount());
        assertEquals("Wrong default Podcast episode download count.", 1,
                settingsService.getPodcastEpisodeDownloadCount());
        assertTrue("Wrong default Podcast folder.", settingsService.getPodcastFolder().endsWith("Podcast"));
        assertEquals("Wrong default Podcast update interval.", 24, settingsService.getPodcastUpdateInterval());
        assertFalse("Wrong default LDAP enabled.", settingsService.isLdapEnabled());
        assertEquals("Wrong default LDAP URL.", "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com",
                settingsService.getLdapUrl());
        Assert.assertNull("Wrong default LDAP manager DN.", settingsService.getLdapManagerDn());
        Assert.assertNull("Wrong default LDAP manager password.", settingsService.getLdapManagerPassword());
        assertEquals("Wrong default LDAP search filter.", "(sAMAccountName={0})",
                settingsService.getLdapSearchFilter());
        assertFalse("Wrong default LDAP auto-shadowing.", settingsService.isLdapAutoShadowing());
    }

    @Test
    public void testChangeSettings() {
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
        assertEquals("Wrong index string.", "indexString", ss.getIndexString());
        assertEquals("Wrong ignored articles.", "a the foo bar", ss.getIgnoredArticles());
        assertEquals("Wrong shortcuts.", "new incoming \"rock 'n' roll\"", ss.getShortcuts());
        assertArrayEquals("Wrong ignored articles array.", new String[] { "a", "the", "foo", "bar" },
                ss.getIgnoredArticlesAsArray());
        assertArrayEquals("Wrong shortcut array.", new String[] { "new", "incoming", "rock 'n' roll" },
                ss.getShortcutsAsArray());
        assertEquals("Wrong playlist folder.", "playlistFolder", ss.getPlaylistFolder());
        assertEquals("Wrong music mask.", "mp3 ogg  aac", ss.getMusicFileTypes());
        assertArrayEquals("Wrong music mask array.", new String[] { "mp3", "ogg", "aac" },
                ss.getMusicFileTypesAsArray());
        assertEquals("Wrong cover art mask.", "jpeg gif  png", ss.getCoverArtFileTypes());
        assertArrayEquals("Wrong cover art mask array.", new String[] { "jpeg", "gif", "png" },
                ss.getCoverArtFileTypesAsArray());
        assertEquals("Wrong welcome message.", "welcomeMessage", ss.getWelcomeMessage());
        assertEquals("Wrong login message.", "loginMessage", ss.getLoginMessage());
        assertEquals("Wrong locale.", Locale.CANADA_FRENCH, ss.getLocale());
        assertEquals("Wrong theme.", "dark", ss.getThemeId());
        assertEquals("Wrong index creation interval.", 4, ss.getIndexCreationInterval());
        assertEquals("Wrong index creation hour.", 9, ss.getIndexCreationHour());
        assertEquals("Wrong Podcast episode retention count.", 5, settingsService.getPodcastEpisodeRetentionCount());
        assertEquals("Wrong Podcast episode download count.", -1, settingsService.getPodcastEpisodeDownloadCount());
        assertEquals("Wrong Podcast folder.", "d:/podcasts", settingsService.getPodcastFolder());
        assertEquals("Wrong Podcast update interval.", -1, settingsService.getPodcastUpdateInterval());
        assertTrue("Wrong LDAP enabled.", settingsService.isLdapEnabled());
        assertEquals("Wrong LDAP URL.", "newLdapUrl", settingsService.getLdapUrl());
        assertEquals("Wrong LDAP manager DN.", "admin", settingsService.getLdapManagerDn());
        assertEquals("Wrong LDAP manager password.", "secret", settingsService.getLdapManagerPassword());
        assertEquals("Wrong LDAP search filter.", "newLdapSearchFilter", settingsService.getLdapSearchFilter());
        assertTrue("Wrong LDAP auto-shadowing.", settingsService.isLdapAutoShadowing());
    }
}
