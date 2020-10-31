/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package com.tesshu.jpsonic.service;

import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the initial value of user data in the display order of Web pages.
 */
public class SettingsServiceUserSettingsTestCase {

    private UserSettings userSettings;
    private UserSettings tabletSettings;
    private UserSettings smartphoneSettings;

    @Before
    public void before() throws ReflectiveOperationException, IllegalArgumentException {
        SettingsService settingsService = new SettingsService();
        Method method = settingsService.getClass().getDeclaredMethod("createDefaultUserSettings", String.class);
        method.setAccessible(true);
        userSettings = (UserSettings) method.invoke(settingsService, "");
        tabletSettings = settingsService.createDefaultTabletUserSettings("");
        smartphoneSettings = settingsService.createDefaultSmartphoneUserSettings("");
    }

    @Test
    public void testLanguageAndTheme() {
        assertEquals("DEFAULT", userSettings.getFontSchemeName());
    }

    @Test
    public void testSettings4DesktopPC() {
        assertTrue(userSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.RANDOM, userSettings.getDefaultAlbumList());
        assertFalse(userSettings.isPutMenuInDrawer());
        assertTrue(userSettings.isShowIndex());
        assertFalse(userSettings.isCloseDrawer());
        assertTrue(userSettings.isClosePlayQueue());
        assertTrue(userSettings.isAlternativeDrawer());
        assertTrue(userSettings.isBreadcrumbIndex());
        assertTrue(userSettings.isAssignAccesskeyToNumber());
        assertTrue(userSettings.isSimpleDisplay());
        assertTrue(userSettings.isQueueFollowingSongs());
        assertFalse(userSettings.isOpenDetailSetting());
        assertFalse(userSettings.isOpenDetailStar());
        assertFalse(userSettings.isOpenDetailIndex());
        assertTrue(userSettings.isSongNotificationEnabled());
        assertFalse(userSettings.isVoiceInputEnabled());
    }

    @Test
    public void testSettings4Tablet() {
        assertFalse(tabletSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.RANDOM, tabletSettings.getDefaultAlbumList());
        assertFalse(tabletSettings.isPutMenuInDrawer());
        assertTrue(tabletSettings.isShowIndex());
        assertTrue(tabletSettings.isCloseDrawer());
        assertTrue(tabletSettings.isClosePlayQueue());
        assertTrue(tabletSettings.isAlternativeDrawer());
        assertTrue(tabletSettings.isBreadcrumbIndex());
        assertTrue(tabletSettings.isAssignAccesskeyToNumber());
        assertTrue(tabletSettings.isSimpleDisplay());
        assertTrue(tabletSettings.isQueueFollowingSongs());
        assertFalse(tabletSettings.isOpenDetailSetting());
        assertFalse(tabletSettings.isOpenDetailStar());
        assertFalse(tabletSettings.isOpenDetailIndex());
        assertTrue(tabletSettings.isSongNotificationEnabled());
        assertFalse(tabletSettings.isVoiceInputEnabled());
    }

    @Test
    public void testSettings4Smartphone() {
        assertFalse(smartphoneSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.INDEX, smartphoneSettings.getDefaultAlbumList());
        assertTrue(smartphoneSettings.isPutMenuInDrawer());
        assertFalse(smartphoneSettings.isShowIndex());
        assertTrue(smartphoneSettings.isCloseDrawer());
        assertTrue(smartphoneSettings.isClosePlayQueue());
        assertTrue(smartphoneSettings.isAlternativeDrawer());
        assertTrue(smartphoneSettings.isBreadcrumbIndex());
        assertTrue(smartphoneSettings.isAssignAccesskeyToNumber());
        assertTrue(smartphoneSettings.isSimpleDisplay());
        assertTrue(smartphoneSettings.isQueueFollowingSongs());
        assertFalse(smartphoneSettings.isOpenDetailSetting());
        assertFalse(smartphoneSettings.isOpenDetailStar());
        assertFalse(smartphoneSettings.isOpenDetailIndex());
        assertTrue(smartphoneSettings.isSongNotificationEnabled());
        assertFalse(smartphoneSettings.isVoiceInputEnabled());
    }

    @Test
    public void testDisplay() throws Exception {
        assertTrue(userSettings.getMainVisibility().isTrackNumberVisible());
        assertTrue(userSettings.getMainVisibility().isArtistVisible());
        assertFalse(userSettings.getMainVisibility().isAlbumVisible());
        assertTrue(userSettings.getMainVisibility().isComposerVisible());
        assertTrue(userSettings.getMainVisibility().isGenreVisible());
        assertFalse(userSettings.getMainVisibility().isYearVisible());
        assertFalse(userSettings.getMainVisibility().isBitRateVisible());
        assertTrue(userSettings.getMainVisibility().isDurationVisible());
        assertFalse(userSettings.getMainVisibility().isFormatVisible());
        assertFalse(userSettings.getMainVisibility().isFileSizeVisible());

        assertFalse(userSettings.getPlaylistVisibility().isTrackNumberVisible());
        assertTrue(userSettings.getPlaylistVisibility().isArtistVisible());
        assertTrue(userSettings.getPlaylistVisibility().isAlbumVisible());
        assertTrue(userSettings.getPlaylistVisibility().isComposerVisible());
        assertTrue(userSettings.getPlaylistVisibility().isGenreVisible());
        assertTrue(userSettings.getPlaylistVisibility().isYearVisible());
        assertTrue(userSettings.getPlaylistVisibility().isBitRateVisible());
        assertTrue(userSettings.getPlaylistVisibility().isDurationVisible());
        assertTrue(userSettings.getPlaylistVisibility().isFormatVisible());
        assertTrue(userSettings.getPlaylistVisibility().isFileSizeVisible());
    }

    @Test
    public void testAdditionalDisplay() {
        assertFalse(userSettings.isShowNowPlayingEnabled());
        assertFalse(userSettings.isNowPlayingAllowed());
        assertFalse(userSettings.isAutoHidePlayQueue());
        assertFalse(userSettings.isShowArtistInfoEnabled());
        assertFalse(userSettings.isForceBio2Eng());
        assertFalse(userSettings.isShowTopSongs());
        assertFalse(userSettings.isShowSimilar());
        assertFalse(userSettings.isShowSibling());
        assertEquals(40, userSettings.getPaginationSize());
        assertFalse(userSettings.isShowDownload());
        assertFalse(userSettings.isShowTag());
        assertFalse(userSettings.isShowChangeCoverArt());
        assertFalse(userSettings.isShowComment());
        assertFalse(userSettings.isShowShare());
        assertFalse(userSettings.isShowRate());
        assertFalse(userSettings.isShowAlbumSearch());
        assertFalse(userSettings.isShowLastPlay());
        assertFalse(userSettings.isShowAlbumActions());
        assertFalse(userSettings.isPartyModeEnabled());
    }

}
