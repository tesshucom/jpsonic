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

package com.tesshu.jpsonic.command;

import java.util.List;

import com.tesshu.jpsonic.controller.PersonalSettingsController;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.Avatar;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.Theme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;

/**
 * Command used in {@link PersonalSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PersonalSettingsCommand extends SettingsPageCommons {

    private User user;

    // Language and theme
    // - Default language
    private String localeIndex;
    private String[] locales;
    // - Theme
    private String themeIndex;
    private Theme[] themes;
    // - Font
    private FontScheme fontScheme;
    private String fontFamily;
    private String fontFamilyDefault;
    private String fontFamilyJpEmbedDefault;
    private int fontSize;
    private int fontSizeDefault;
    private int fontSizeJpEmbedDefault;

    // Options related to display and playing control
    private UserSettings defaultSettings;
    private UserSettings tabletSettings;
    private UserSettings smartphoneSettings;
    private boolean keyboardShortcutsEnabled;
    private String albumListId;
    private AlbumListType[] albumLists;
    private boolean putMenuInDrawer;
    private boolean showIndex;
    private boolean closeDrawer;
    private boolean alternativeDrawer;
    private boolean closePlayQueue;
    private boolean autoHidePlayQueue;
    private boolean breadcrumbIndex;
    private boolean assignAccesskeyToNumber;
    private boolean simpleDisplay;
    private boolean queueFollowingSongs;
    private boolean showCurrentSongInfo;
    private boolean songNotificationEnabled;
    private boolean voiceInputEnabled;
    private SpeechToTextLangScheme speechToTextLangScheme;
    private String ietf;
    private String ietfDefault;
    private String ietfDisplayDefault;
    private boolean openDetailStar;
    private boolean openDetailIndex;

    // Column to be displayed
    private UserSettings.Visibility mainVisibility;
    private UserSettings.Visibility playlistVisibility;

    // Additional display features
    private boolean nowPlayingAllowed;
    private boolean othersPlayingEnabled;
    private boolean showNowPlayingEnabled;
    private boolean showArtistInfoEnabled;
    private boolean forceBio2Eng;
    private boolean showTopSongs;
    private boolean showSimilar;
    private boolean showComment;
    private boolean showSibling;
    private int paginationSize;
    private boolean showTag;
    private boolean showChangeCoverArt;
    private boolean showAlbumSearch;
    private boolean showLastPlay;
    private boolean showRate;
    private boolean showAlbumActions;
    private boolean showDownload;
    private boolean showShare;
    private boolean partyModeEnabled;

    // Personal image
    private int avatarId;
    private Avatar customAvatar;
    private List<Avatar> avatars;

    // Cooperation with Music SNS
    private boolean listenBrainzEnabled;
    private String listenBrainzToken;
    private boolean lastFmEnabled;
    private String lastFmUsername;
    private String lastFmPassword;

    // Update notification
    private boolean finalVersionNotificationEnabled;
    private boolean betaVersionNotificationEnabled;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLocaleIndex() {
        return localeIndex;
    }

    public void setLocaleIndex(String localeIndex) {
        this.localeIndex = localeIndex;
    }

    public String[] getLocales() {
        return locales;
    }

    public void setLocales(String... locales) {
        this.locales = locales.clone();
    }

    public String getThemeIndex() {
        return themeIndex;
    }

    public void setThemeIndex(String themeIndex) {
        this.themeIndex = themeIndex;
    }

    public Theme[] getThemes() {
        return themes;
    }

    public void setThemes(Theme... themes) {
        if (themes != null) {
            this.themes = themes.clone();
        }
    }

    public FontScheme getFontScheme() {
        return fontScheme;
    }

    public void setFontScheme(FontScheme fontScheme) {
        this.fontScheme = fontScheme;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String font) {
        this.fontFamily = font;
    }

    public String getFontFamilyDefault() {
        return fontFamilyDefault;
    }

    public void setFontFamilyDefault(String fontFamilyDefault) {
        this.fontFamilyDefault = fontFamilyDefault;
    }

    public String getFontFamilyJpEmbedDefault() {
        return fontFamilyJpEmbedDefault;
    }

    public void setFontFamilyJpEmbedDefault(String fontFamilyJpEmbed) {
        this.fontFamilyJpEmbedDefault = fontFamilyJpEmbed;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getFontSizeDefault() {
        return fontSizeDefault;
    }

    public void setFontSizeDefault(int fontSizeDefault) {
        this.fontSizeDefault = fontSizeDefault;
    }

    public int getFontSizeJpEmbedDefault() {
        return fontSizeJpEmbedDefault;
    }

    public void setFontSizeJpEmbedDefault(int fontSizeJpEmbedDefault) {
        this.fontSizeJpEmbedDefault = fontSizeJpEmbedDefault;
    }

    public UserSettings getDefaultSettings() {
        return defaultSettings;
    }

    public void setDefaultSettings(UserSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    public UserSettings getTabletSettings() {
        return tabletSettings;
    }

    public void setTabletSettings(UserSettings tabletSettings) {
        this.tabletSettings = tabletSettings;
    }

    public UserSettings getSmartphoneSettings() {
        return smartphoneSettings;
    }

    public void setSmartphoneSettings(UserSettings smartphoneSettings) {
        this.smartphoneSettings = smartphoneSettings;
    }

    public boolean isKeyboardShortcutsEnabled() {
        return keyboardShortcutsEnabled;
    }

    public void setKeyboardShortcutsEnabled(boolean keyboardShortcutsEnabled) {
        this.keyboardShortcutsEnabled = keyboardShortcutsEnabled;
    }

    public String getAlbumListId() {
        return albumListId;
    }

    public void setAlbumListId(String albumListId) {
        this.albumListId = albumListId;
    }

    public AlbumListType[] getAlbumLists() {
        return albumLists;
    }

    public void setAlbumLists(AlbumListType... albumLists) {
        this.albumLists = albumLists.clone();
    }

    public boolean isPutMenuInDrawer() {
        return putMenuInDrawer;
    }

    public void setPutMenuInDrawer(boolean putMenuInDrawer) {
        this.putMenuInDrawer = putMenuInDrawer;
    }

    public boolean isShowIndex() {
        return showIndex;
    }

    public void setShowIndex(boolean showIndex) {
        this.showIndex = showIndex;
    }

    public boolean isCloseDrawer() {
        return closeDrawer;
    }

    public void setCloseDrawer(boolean closeDrawer) {
        this.closeDrawer = closeDrawer;
    }

    public boolean isAlternativeDrawer() {
        return alternativeDrawer;
    }

    public void setAlternativeDrawer(boolean alternativeDrawer) {
        this.alternativeDrawer = alternativeDrawer;
    }

    public boolean isClosePlayQueue() {
        return closePlayQueue;
    }

    public void setClosePlayQueue(boolean closePlayqueue) {
        this.closePlayQueue = closePlayqueue;
    }

    public boolean isAutoHidePlayQueue() {
        return autoHidePlayQueue;
    }

    public void setAutoHidePlayQueue(boolean autoHidePlayQueue) {
        this.autoHidePlayQueue = autoHidePlayQueue;
    }

    public boolean isBreadcrumbIndex() {
        return breadcrumbIndex;
    }

    public void setBreadcrumbIndex(boolean breadcrumbIndex) {
        this.breadcrumbIndex = breadcrumbIndex;
    }

    public boolean isAssignAccesskeyToNumber() {
        return assignAccesskeyToNumber;
    }

    public void setAssignAccesskeyToNumber(boolean assignAccesskeyToNumber) {
        this.assignAccesskeyToNumber = assignAccesskeyToNumber;
    }

    public boolean isSimpleDisplay() {
        return simpleDisplay;
    }

    public void setSimpleDisplay(boolean simpleDisplay) {
        this.simpleDisplay = simpleDisplay;
    }

    public boolean isQueueFollowingSongs() {
        return queueFollowingSongs;
    }

    public void setQueueFollowingSongs(boolean queueFollowingSongs) {
        this.queueFollowingSongs = queueFollowingSongs;
    }

    public boolean isShowCurrentSongInfo() {
        return showCurrentSongInfo;
    }

    public void setShowCurrentSongInfo(boolean showCurrentSongInfo) {
        this.showCurrentSongInfo = showCurrentSongInfo;
    }

    public void setSongNotificationEnabled(boolean songNotificationEnabled) {
        this.songNotificationEnabled = songNotificationEnabled;
    }

    public boolean isSongNotificationEnabled() {
        return songNotificationEnabled;
    }

    public boolean isVoiceInputEnabled() {
        return voiceInputEnabled;
    }

    public void setVoiceInputEnabled(boolean voiceInputEnabled) {
        this.voiceInputEnabled = voiceInputEnabled;
    }

    public SpeechToTextLangScheme getSpeechToTextLangScheme() {
        return speechToTextLangScheme;
    }

    public void setSpeechToTextLangScheme(SpeechToTextLangScheme speechToTextLangScheme) {
        this.speechToTextLangScheme = speechToTextLangScheme;
    }

    public String getIetf() {
        return ietf;
    }

    public void setIetf(String ietf) {
        this.ietf = ietf;
    }

    public String getIetfDefault() {
        return ietfDefault;
    }

    public void setIetfDefault(String ietfDefault) {
        this.ietfDefault = ietfDefault;
    }

    public String getIetfDisplayDefault() {
        return ietfDisplayDefault;
    }

    public void setIetfDisplayDefault(String ietfDisplayDefault) {
        this.ietfDisplayDefault = ietfDisplayDefault;
    }

    public boolean isOpenDetailStar() {
        return openDetailStar;
    }

    public void setOpenDetailStar(boolean openDetailStar) {
        this.openDetailStar = openDetailStar;
    }

    public boolean isOpenDetailIndex() {
        return openDetailIndex;
    }

    public void setOpenDetailIndex(boolean openDetailIndex) {
        this.openDetailIndex = openDetailIndex;
    }

    public UserSettings.Visibility getMainVisibility() {
        return mainVisibility;
    }

    public void setMainVisibility(UserSettings.Visibility mainVisibility) {
        this.mainVisibility = mainVisibility;
    }

    public UserSettings.Visibility getPlaylistVisibility() {
        return playlistVisibility;
    }

    public void setPlaylistVisibility(UserSettings.Visibility playlistVisibility) {
        this.playlistVisibility = playlistVisibility;
    }

    public boolean isNowPlayingAllowed() {
        return nowPlayingAllowed;
    }

    public void setNowPlayingAllowed(boolean nowPlayingAllowed) {
        this.nowPlayingAllowed = nowPlayingAllowed;
    }

    public boolean isOthersPlayingEnabled() {
        return othersPlayingEnabled;
    }

    public void setOthersPlayingEnabled(boolean othersPlayingEnabled) {
        this.othersPlayingEnabled = othersPlayingEnabled;
    }

    public boolean isShowNowPlayingEnabled() {
        return showNowPlayingEnabled;
    }

    public void setShowNowPlayingEnabled(boolean showNowPlayingEnabled) {
        this.showNowPlayingEnabled = showNowPlayingEnabled;
    }

    public boolean isShowArtistInfoEnabled() {
        return showArtistInfoEnabled;
    }

    public void setShowArtistInfoEnabled(boolean showArtistInfoEnabled) {
        this.showArtistInfoEnabled = showArtistInfoEnabled;
    }

    public boolean isForceBio2Eng() {
        return forceBio2Eng;
    }

    public void setForceBio2Eng(boolean forceBio2Eng) {
        this.forceBio2Eng = forceBio2Eng;
    }

    public boolean isShowTopSongs() {
        return showTopSongs;
    }

    public void setShowTopSongs(boolean showtopSongs) {
        this.showTopSongs = showtopSongs;
    }

    public boolean isShowSimilar() {
        return showSimilar;
    }

    public void setShowSimilar(boolean showSimilar) {
        this.showSimilar = showSimilar;
    }

    public boolean isShowComment() {
        return showComment;
    }

    public void setShowComment(boolean commentVisible) {
        this.showComment = commentVisible;
    }

    public boolean isShowSibling() {
        return showSibling;
    }

    public void setShowSibling(boolean siblingVisible) {
        this.showSibling = siblingVisible;
    }

    public int getPaginationSize() {
        return paginationSize;
    }

    public void setPaginationSize(int paginationSize) {
        this.paginationSize = paginationSize;
    }

    public boolean isShowTag() {
        return showTag;
    }

    public void setShowTag(boolean tagVisible) {
        this.showTag = tagVisible;
    }

    public boolean isShowChangeCoverArt() {
        return showChangeCoverArt;
    }

    public void setShowChangeCoverArt(boolean showChangeCoverArt) {
        this.showChangeCoverArt = showChangeCoverArt;
    }

    public boolean isShowAlbumSearch() {
        return showAlbumSearch;
    }

    public void setShowAlbumSearch(boolean albumSearchVisible) {
        this.showAlbumSearch = albumSearchVisible;
    }

    public boolean isShowLastPlay() {
        return showLastPlay;
    }

    public void setShowLastPlay(boolean lastPlayVisible) {
        this.showLastPlay = lastPlayVisible;
    }

    public boolean isShowRate() {
        return showRate;
    }

    public void setShowRate(boolean rateVisible) {
        this.showRate = rateVisible;
    }

    public boolean isShowAlbumActions() {
        return showAlbumActions;
    }

    public void setShowAlbumActions(boolean showAlbumActions) {
        this.showAlbumActions = showAlbumActions;
    }

    public boolean isShowDownload() {
        return showDownload;
    }

    public void setShowDownload(boolean downloadVisible) {
        this.showDownload = downloadVisible;
    }

    public boolean isShowShare() {
        return showShare;
    }

    public void setShowShare(boolean shareVisible) {
        this.showShare = shareVisible;
    }

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public Avatar getCustomAvatar() {
        return customAvatar;
    }

    public void setCustomAvatar(Avatar customAvatar) {
        this.customAvatar = customAvatar;
    }

    public List<Avatar> getAvatars() {
        return avatars;
    }

    public void setAvatars(List<Avatar> avatars) {
        this.avatars = avatars;
    }

    public boolean isListenBrainzEnabled() {
        return listenBrainzEnabled;
    }

    public void setListenBrainzEnabled(boolean listenBrainzEnabled) {
        this.listenBrainzEnabled = listenBrainzEnabled;
    }

    public String getListenBrainzToken() {
        return listenBrainzToken;
    }

    public void setListenBrainzToken(String listenBrainzToken) {
        this.listenBrainzToken = listenBrainzToken;
    }

    public boolean isLastFmEnabled() {
        return lastFmEnabled;
    }

    public void setLastFmEnabled(boolean lastFmEnabled) {
        this.lastFmEnabled = lastFmEnabled;
    }

    public String getLastFmUsername() {
        return lastFmUsername;
    }

    public void setLastFmUsername(String lastFmUsername) {
        this.lastFmUsername = lastFmUsername;
    }

    public String getLastFmPassword() {
        return lastFmPassword;
    }

    public void setLastFmPassword(String lastFmPassword) {
        this.lastFmPassword = lastFmPassword;
    }

    public boolean isFinalVersionNotificationEnabled() {
        return finalVersionNotificationEnabled;
    }

    public void setFinalVersionNotificationEnabled(boolean finalVersionNotificationEnabled) {
        this.finalVersionNotificationEnabled = finalVersionNotificationEnabled;
    }

    public boolean isBetaVersionNotificationEnabled() {
        return betaVersionNotificationEnabled;
    }

    public void setBetaVersionNotificationEnabled(boolean betaVersionNotificationEnabled) {
        this.betaVersionNotificationEnabled = betaVersionNotificationEnabled;
    }

}
