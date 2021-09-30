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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.controller.WebFontUtils;
import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link SecurityService}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class SecurityServiceTest {

    private SecurityService service;

    @BeforeEach
    public void setup() {
        service = new SecurityService(mock(UserDao.class), mock(SettingsService.class), mock(MusicFolderService.class),
                null);
    }

    @Test
    void testIsFileInFolder() {

        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "/"));

        assertTrue(service.isFileInFolder("/music/foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("\\music\\foo.mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music"));
        assertTrue(service.isFileInFolder("/music/foo.mp3", "\\music\\"));

        assertFalse(service.isFileInFolder("", "/tmp"));
        assertFalse(service.isFileInFolder("foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp"));
        assertFalse(service.isFileInFolder("/music/foo.mp3", "/tmp/music"));

        // Test that references to the parent directory (..) is not allowed.
        assertTrue(service.isFileInFolder("/music/foo..mp3", "/music"));
        assertTrue(service.isFileInFolder("/music/foo..", "/music"));
        assertTrue(service.isFileInFolder("/music/foo.../", "/music"));
        assertFalse(service.isFileInFolder("/music/foo/..", "/music"));
        assertFalse(service.isFileInFolder("../music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/../bar/../foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\foo\\..", "/music"));
        assertFalse(service.isFileInFolder("..\\music/foo", "/music"));
        assertFalse(service.isFileInFolder("/music\\../foo", "/music"));
        assertFalse(service.isFileInFolder("/music/..\\bar/../foo", "/music"));
    }

    @Test
    void testLanguageAndTheme() throws ExecutionException {
        assertEquals("DEFAULT", service.getUserSettings("").getFontSchemeName());
    }

    @Test
    void testSettings4DesktopPC() throws ExecutionException {
        UserSettings userSettings = service.getUserSettings("");
        assertTrue(userSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.RANDOM, userSettings.getDefaultAlbumList());
        assertFalse(userSettings.isPutMenuInDrawer());
        assertTrue(userSettings.isShowIndex());
        assertFalse(userSettings.isCloseDrawer());
        assertTrue(userSettings.isClosePlayQueue());
        assertTrue(userSettings.isAlternativeDrawer());
        assertTrue(userSettings.isAutoHidePlayQueue());
        assertTrue(userSettings.isBreadcrumbIndex());
        assertTrue(userSettings.isAssignAccesskeyToNumber());
        assertTrue(userSettings.isSimpleDisplay());
        assertTrue(userSettings.isQueueFollowingSongs());
        assertFalse(userSettings.isOpenDetailSetting());
        assertFalse(userSettings.isOpenDetailStar());
        assertFalse(userSettings.isOpenDetailIndex());
        assertFalse(userSettings.isSongNotificationEnabled());
        assertFalse(userSettings.isVoiceInputEnabled());
        assertTrue(userSettings.isShowCurrentSongInfo());
        assertEquals(SpeechToTextLangScheme.DEFAULT.name(), userSettings.getSpeechLangSchemeName());
        Assertions.assertNull(userSettings.getIetf());
        assertEquals(FontScheme.DEFAULT.name(), userSettings.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, userSettings.getFontFamily());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, userSettings.getFontSize());
        assertEquals(Integer.valueOf(101), userSettings.getSystemAvatarId());
    }

    @Test
    void testSettings4Tablet() {
        UserSettings tabletSettings = service.createDefaultTabletUserSettings("");
        assertFalse(tabletSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.RANDOM, tabletSettings.getDefaultAlbumList());
        assertFalse(tabletSettings.isPutMenuInDrawer());
        assertTrue(tabletSettings.isShowIndex());
        assertTrue(tabletSettings.isCloseDrawer());
        assertTrue(tabletSettings.isClosePlayQueue());
        assertTrue(tabletSettings.isAlternativeDrawer());
        assertTrue(tabletSettings.isAutoHidePlayQueue());
        assertTrue(tabletSettings.isBreadcrumbIndex());
        assertTrue(tabletSettings.isAssignAccesskeyToNumber());
        assertTrue(tabletSettings.isSimpleDisplay());
        assertTrue(tabletSettings.isQueueFollowingSongs());
        assertFalse(tabletSettings.isOpenDetailSetting());
        assertFalse(tabletSettings.isOpenDetailStar());
        assertFalse(tabletSettings.isOpenDetailIndex());
        assertFalse(tabletSettings.isSongNotificationEnabled());
        assertTrue(tabletSettings.isVoiceInputEnabled());
        assertTrue(tabletSettings.isShowCurrentSongInfo());
        assertEquals(SpeechToTextLangScheme.DEFAULT.name(), tabletSettings.getSpeechLangSchemeName());
        Assertions.assertNull(tabletSettings.getIetf());
        assertEquals(FontScheme.DEFAULT.name(), tabletSettings.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, tabletSettings.getFontFamily());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, tabletSettings.getFontSize());
        assertEquals(Integer.valueOf(101), tabletSettings.getSystemAvatarId());
    }

    @Test
    void testSettings4Smartphone() {
        UserSettings smartphoneSettings = service.createDefaultSmartphoneUserSettings("");
        assertFalse(smartphoneSettings.isKeyboardShortcutsEnabled());
        assertEquals(AlbumListType.INDEX, smartphoneSettings.getDefaultAlbumList());
        assertTrue(smartphoneSettings.isPutMenuInDrawer());
        assertFalse(smartphoneSettings.isShowIndex());
        assertTrue(smartphoneSettings.isCloseDrawer());
        assertTrue(smartphoneSettings.isClosePlayQueue());
        assertTrue(smartphoneSettings.isAlternativeDrawer());
        assertTrue(smartphoneSettings.isAutoHidePlayQueue());
        assertTrue(smartphoneSettings.isBreadcrumbIndex());
        assertTrue(smartphoneSettings.isAssignAccesskeyToNumber());
        assertTrue(smartphoneSettings.isSimpleDisplay());
        assertTrue(smartphoneSettings.isQueueFollowingSongs());
        assertFalse(smartphoneSettings.isOpenDetailSetting());
        assertFalse(smartphoneSettings.isOpenDetailStar());
        assertFalse(smartphoneSettings.isOpenDetailIndex());
        assertFalse(smartphoneSettings.isSongNotificationEnabled());
        assertTrue(smartphoneSettings.isVoiceInputEnabled());
        assertTrue(smartphoneSettings.isShowCurrentSongInfo());
        assertEquals(SpeechToTextLangScheme.DEFAULT.name(), smartphoneSettings.getSpeechLangSchemeName());
        Assertions.assertNull(smartphoneSettings.getIetf());
        assertEquals(FontScheme.DEFAULT.name(), smartphoneSettings.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, smartphoneSettings.getFontFamily());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, smartphoneSettings.getFontSize());
        assertEquals(Integer.valueOf(101), smartphoneSettings.getSystemAvatarId());
    }

    @Test
    void testDisplay() throws Exception {
        UserSettings userSettings = service.getUserSettings("");
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
    void testAdditionalDisplay() throws ExecutionException {
        UserSettings userSettings = service.getUserSettings("");
        assertFalse(userSettings.isShowNowPlayingEnabled());
        assertFalse(userSettings.isNowPlayingAllowed());
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
