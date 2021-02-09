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

package org.airsonic.player.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import org.airsonic.player.controller.PersonalSettingsController;
import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.Avatar;
import org.airsonic.player.domain.Theme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;

/**
 * Command used in {@link PersonalSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PersonalSettingsCommand {

    private User user;
    private UserSettings defaultSettings;
    private UserSettings tabletSettings;
    private UserSettings smartphoneSettings;
    private String fontFamilyDefault;
    private String fontFamilyJpEmbedDefault;
    private int fontSizeDefault;
    private int fontSizeJpEmbedDefault;
    private String ietfDefault;
    private String ietfDisplayDefault;
    private String localeIndex;
    private String[] locales;
    private String themeIndex;
    private Theme[] themes;
    private String albumListId;
    private AlbumListType[] albumLists;
    private int avatarId;
    private List<Avatar> avatars;
    private Avatar customAvatar;
    private UserSettings.Visibility mainVisibility;
    private UserSettings.Visibility playlistVisibility;
    private boolean partyModeEnabled;
    private boolean showNowPlayingEnabled;
    private boolean closeDrawer;
    private boolean closePlayQueue;
    private boolean alternativeDrawer;
    private boolean showIndex;
    private boolean assignAccesskeyToNumber;
    private boolean openDetailIndex;
    private boolean openDetailSetting;
    private boolean openDetailStar;
    private boolean showArtistInfoEnabled;
    private boolean nowPlayingAllowed;
    private boolean autoHidePlayQueue;
    private boolean keyboardShortcutsEnabled;
    private boolean finalVersionNotificationEnabled;
    private boolean betaVersionNotificationEnabled;
    private boolean songNotificationEnabled;
    private boolean queueFollowingSongs;
    private boolean lastFmEnabled;
    private boolean listenBrainzEnabled;
    private int paginationSize;
    private String lastFmUsername;
    private String lastFmPassword;
    private String listenBrainzToken;
    private boolean useRadio;
    private boolean useSonos;
    private boolean simpleDisplay;
    private boolean showSibling;
    private boolean showRate;
    private boolean showAlbumSearch;
    private boolean showLastPlay;
    private boolean showDownload;
    private boolean showTag;
    private boolean showComment;
    private boolean showShare;
    private boolean showChangeCoverArt;
    private boolean showTopSongs;
    private boolean showSimilar;
    private boolean showToast;
    private boolean showAlbumActions;
    private boolean breadcrumbIndex;
    private boolean putMenuInDrawer;
    private boolean forceBio2Eng;
    private EnumHolder[] fontSchemeHolders;
    private String fontSchemeName;
    private boolean showOutlineHelp;
    private boolean voiceInputEnabled;
    private boolean othersPlayingEnabled;
    private boolean showCurrentSongInfo;
    private EnumHolder[] speechLangSchemeHolders;
    private String speechLangSchemeName;
    private String ietf;
    private String fontFamily;
    private int fontSize;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public List<Avatar> getAvatars() {
        return avatars;
    }

    public void setAvatars(List<Avatar> avatars) {
        this.avatars = avatars;
    }

    public Avatar getCustomAvatar() {
        return customAvatar;
    }

    public void setCustomAvatar(Avatar customAvatar) {
        this.customAvatar = customAvatar;
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

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public boolean isShowNowPlayingEnabled() {
        return showNowPlayingEnabled;
    }

    public void setShowNowPlayingEnabled(boolean showNowPlayingEnabled) {
        this.showNowPlayingEnabled = showNowPlayingEnabled;
    }

    public boolean isCloseDrawer() {
        return closeDrawer;
    }

    public void setCloseDrawer(boolean closeDrawer) {
        this.closeDrawer = closeDrawer;
    }

    public boolean isClosePlayQueue() {
        return closePlayQueue;
    }

    public void setClosePlayQueue(boolean closePlayqueue) {
        this.closePlayQueue = closePlayqueue;
    }

    public boolean isAlternativeDrawer() {
        return alternativeDrawer;
    }

    public void setAlternativeDrawer(boolean alternativeDrawer) {
        this.alternativeDrawer = alternativeDrawer;
    }

    public boolean isShowIndex() {
        return showIndex;
    }

    public void setShowIndex(boolean showIndex) {
        this.showIndex = showIndex;
    }

    public boolean isAssignAccesskeyToNumber() {
        return assignAccesskeyToNumber;
    }

    public void setAssignAccesskeyToNumber(boolean assignAccesskeyToNumber) {
        this.assignAccesskeyToNumber = assignAccesskeyToNumber;
    }

    public boolean isOpenDetailIndex() {
        return openDetailIndex;
    }

    public void setOpenDetailIndex(boolean openDetailIndex) {
        this.openDetailIndex = openDetailIndex;
    }

    public boolean isOpenDetailSetting() {
        return openDetailSetting;
    }

    public void setOpenDetailSetting(boolean openDetailSetting) {
        this.openDetailSetting = openDetailSetting;
    }

    public boolean isOpenDetailStar() {
        return openDetailStar;
    }

    public void setOpenDetailStar(boolean openDetailStar) {
        this.openDetailStar = openDetailStar;
    }

    public boolean isShowArtistInfoEnabled() {
        return showArtistInfoEnabled;
    }

    public void setShowArtistInfoEnabled(boolean showArtistInfoEnabled) {
        this.showArtistInfoEnabled = showArtistInfoEnabled;
    }

    public boolean isNowPlayingAllowed() {
        return nowPlayingAllowed;
    }

    public void setNowPlayingAllowed(boolean nowPlayingAllowed) {
        this.nowPlayingAllowed = nowPlayingAllowed;
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

    public void setSongNotificationEnabled(boolean songNotificationEnabled) {
        this.songNotificationEnabled = songNotificationEnabled;
    }

    public boolean isSongNotificationEnabled() {
        return songNotificationEnabled;
    }

    public boolean isAutoHidePlayQueue() {
        return autoHidePlayQueue;
    }

    public void setAutoHidePlayQueue(boolean autoHidePlayQueue) {
        this.autoHidePlayQueue = autoHidePlayQueue;
    }

    public boolean isKeyboardShortcutsEnabled() {
        return keyboardShortcutsEnabled;
    }

    public void setKeyboardShortcutsEnabled(boolean keyboardShortcutsEnabled) {
        this.keyboardShortcutsEnabled = keyboardShortcutsEnabled;
    }

    public boolean isLastFmEnabled() {
        return lastFmEnabled;
    }

    public void setLastFmEnabled(boolean lastFmEnabled) {
        this.lastFmEnabled = lastFmEnabled;
    }

    public boolean isListenBrainzEnabled() {
        return listenBrainzEnabled;
    }

    public void setListenBrainzEnabled(boolean listenBrainzEnabled) {
        this.listenBrainzEnabled = listenBrainzEnabled;
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

    public String getListenBrainzToken() {
        return listenBrainzToken;
    }

    public void setListenBrainzToken(String listenBrainzToken) {
        this.listenBrainzToken = listenBrainzToken;
    }

    public boolean isQueueFollowingSongs() {
        return queueFollowingSongs;
    }

    public void setQueueFollowingSongs(boolean queueFollowingSongs) {
        this.queueFollowingSongs = queueFollowingSongs;
    }

    public int getPaginationSize() {
        return paginationSize;
    }

    public void setPaginationSize(int paginationSize) {
        this.paginationSize = paginationSize;
    }

    public boolean isUseRadio() {
        return useRadio;
    }

    public void setUseRadio(boolean useRadio) {
        this.useRadio = useRadio;
    }

    public boolean isUseSonos() {
        return useSonos;
    }

    public void setUseSonos(boolean useSonos) {
        this.useSonos = useSonos;
    }

    public boolean isSimpleDisplay() {
        return simpleDisplay;
    }

    public void setSimpleDisplay(boolean simpleDisplay) {
        this.simpleDisplay = simpleDisplay;
    }

    public boolean isShowSibling() {
        return showSibling;
    }

    public void setShowSibling(boolean siblingVisible) {
        this.showSibling = siblingVisible;
    }

    public boolean isShowRate() {
        return showRate;
    }

    public void setShowRate(boolean rateVisible) {
        this.showRate = rateVisible;
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

    public boolean isShowDownload() {
        return showDownload;
    }

    public void setShowDownload(boolean downloadVisible) {
        this.showDownload = downloadVisible;
    }

    public boolean isShowTag() {
        return showTag;
    }

    public void setShowTag(boolean tagVisible) {
        this.showTag = tagVisible;
    }

    public boolean isShowComment() {
        return showComment;
    }

    public void setShowComment(boolean commentVisible) {
        this.showComment = commentVisible;
    }

    public boolean isShowShare() {
        return showShare;
    }

    public void setShowShare(boolean shareVisible) {
        this.showShare = shareVisible;
    }

    public boolean isShowChangeCoverArt() {
        return showChangeCoverArt;
    }

    public void setShowChangeCoverArt(boolean showChangeCoverArt) {
        this.showChangeCoverArt = showChangeCoverArt;
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

    public boolean isShowToast() {
        return showToast;
    }

    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    public boolean isShowAlbumActions() {
        return showAlbumActions;
    }

    public void setShowAlbumActions(boolean showAlbumActions) {
        this.showAlbumActions = showAlbumActions;
    }

    public boolean isBreadcrumbIndex() {
        return breadcrumbIndex;
    }

    public void setBreadcrumbIndex(boolean breadcrumbIndex) {
        this.breadcrumbIndex = breadcrumbIndex;
    }

    public boolean isPutMenuInDrawer() {
        return putMenuInDrawer;
    }

    public void setPutMenuInDrawer(boolean putMenuInDrawer) {
        this.putMenuInDrawer = putMenuInDrawer;
    }

    public boolean isForceBio2Eng() {
        return forceBio2Eng;
    }

    public void setForceBio2Eng(boolean forceBio2Eng) {
        this.forceBio2Eng = forceBio2Eng;
    }

    public EnumHolder[] getFontSchemeHolders() {
        return fontSchemeHolders;
    }

    public void setFontSchemes(FontScheme... fontSchemes) {
        fontSchemeHolders = Arrays.stream(fontSchemes).map(s -> new EnumHolder(s.name(), s.toString()))
                .collect(Collectors.toList()).toArray(new EnumHolder[fontSchemes.length]);
    }

    public String getFontSchemeName() {
        return fontSchemeName;
    }

    public void setFontSchemeName(String fontSchemeName) {
        this.fontSchemeName = fontSchemeName;
    }

    public boolean isShowOutlineHelp() {
        return showOutlineHelp;
    }

    public void setShowOutlineHelp(boolean showOutlineHelp) {
        this.showOutlineHelp = showOutlineHelp;
    }

    public boolean isVoiceInputEnabled() {
        return voiceInputEnabled;
    }

    public void setVoiceInputEnabled(boolean voiceInputEnabled) {
        this.voiceInputEnabled = voiceInputEnabled;
    }

    public boolean isOthersPlayingEnabled() {
        return othersPlayingEnabled;
    }

    public void setOthersPlayingEnabled(boolean othersPlayingEnabled) {
        this.othersPlayingEnabled = othersPlayingEnabled;
    }

    public boolean isShowCurrentSongInfo() {
        return showCurrentSongInfo;
    }

    public void setShowCurrentSongInfo(boolean showCurrentSongInfo) {
        this.showCurrentSongInfo = showCurrentSongInfo;
    }

    public EnumHolder[] getSpeechLangSchemeHolders() {
        return speechLangSchemeHolders;
    }

    public void setSpeechLangSchemes(SpeechToTextLangScheme... speechLangSchemes) {
        speechLangSchemeHolders = Arrays.stream(speechLangSchemes).map(s -> new EnumHolder(s.name(), s.toString()))
                .collect(Collectors.toList()).toArray(new EnumHolder[speechLangSchemes.length]);
    }

    public String getSpeechLangSchemeName() {
        return speechLangSchemeName;
    }

    public void setSpeechLangSchemeName(String speechLangSchemeName) {
        this.speechLangSchemeName = speechLangSchemeName;
    }

    public String getIetf() {
        return ietf;
    }

    public void setIetf(String ietf) {
        this.ietf = ietf;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String font) {
        this.fontFamily = font;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
}
