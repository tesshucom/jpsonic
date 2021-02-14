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

package org.airsonic.player.domain;

import java.util.Date;
import java.util.Locale;

/**
 * Represent user-specific settings.
 *
 * @author Sindre Mehus
 */
public class UserSettings {

    private String username;
    private Locale locale;
    private String themeId;
    private boolean showNowPlayingEnabled;
    private boolean showArtistInfoEnabled;
    private boolean finalVersionNotificationEnabled;
    private boolean betaVersionNotificationEnabled;
    private boolean songNotificationEnabled;
    private boolean keyboardShortcutsEnabled;
    private boolean autoHidePlayQueue;
    private boolean viewAsList;
    private boolean queueFollowingSongs;
    private AlbumListType defaultAlbumList;
    private Visibility mainVisibility;
    private Visibility playlistVisibility;
    private boolean lastFmEnabled;
    private boolean listenBrainzEnabled;
    private String lastFmUsername;
    private String lastFmPassword;
    private String listenBrainzToken;
    private TranscodeScheme transcodeScheme;
    private int selectedMusicFolderId;
    private boolean partyModeEnabled;
    private boolean nowPlayingAllowed;
    private AvatarScheme avatarScheme;
    private Integer systemAvatarId;
    private Date changed = new Date();
    private int paginationSize;

    // JP >>>>
    private boolean closeDrawer;
    private boolean closePlayQueue;
    private boolean alternativeDrawer;
    private boolean showIndex;
    private boolean assignAccesskeyToNumber;
    private boolean openDetailIndex;
    private boolean openDetailSetting;
    private boolean openDetailStar;
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
    private boolean showAlbumActions;
    private boolean breadcrumbIndex;
    private boolean putMenuInDrawer;
    private String fontSchemeName;
    private boolean showOutlineHelp;
    private boolean forceBio2Eng;
    private boolean voiceInputEnabled;
    private boolean showCurrentSongInfo;
    private String speechLangSchemeName;
    private String ietf;
    private String fontFamily;
    private int fontSize;
    // <<<< JP

    public UserSettings() {
        defaultAlbumList = AlbumListType.RANDOM;
        mainVisibility = new Visibility();
        playlistVisibility = new Visibility();
        transcodeScheme = TranscodeScheme.OFF;
        selectedMusicFolderId = -1;
        avatarScheme = AvatarScheme.NONE;
        systemAvatarId = 101;
    }

    public UserSettings(String username) {
        this();
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
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

    public boolean isSongNotificationEnabled() {
        return songNotificationEnabled;
    }

    public void setSongNotificationEnabled(boolean songNotificationEnabled) {
        this.songNotificationEnabled = songNotificationEnabled;
    }

    public Visibility getMainVisibility() {
        return mainVisibility;
    }

    public void setMainVisibility(Visibility mainVisibility) {
        this.mainVisibility = mainVisibility;
    }

    public Visibility getPlaylistVisibility() {
        return playlistVisibility;
    }

    public void setPlaylistVisibility(Visibility playlistVisibility) {
        this.playlistVisibility = playlistVisibility;
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

    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    public int getSelectedMusicFolderId() {
        return selectedMusicFolderId;
    }

    public void setSelectedMusicFolderId(int selectedMusicFolderId) {
        this.selectedMusicFolderId = selectedMusicFolderId;
    }

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public boolean isNowPlayingAllowed() {
        return nowPlayingAllowed;
    }

    public void setNowPlayingAllowed(boolean nowPlayingAllowed) {
        this.nowPlayingAllowed = nowPlayingAllowed;
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

    public boolean isViewAsList() {
        return viewAsList;
    }

    public void setViewAsList(boolean viewAsList) {
        this.viewAsList = viewAsList;
    }

    public AlbumListType getDefaultAlbumList() {
        return defaultAlbumList;
    }

    public void setDefaultAlbumList(AlbumListType defaultAlbumList) {
        this.defaultAlbumList = defaultAlbumList;
    }

    public AvatarScheme getAvatarScheme() {
        return avatarScheme;
    }

    public void setAvatarScheme(AvatarScheme avatarScheme) {
        this.avatarScheme = avatarScheme;
    }

    public Integer getSystemAvatarId() {
        return systemAvatarId;
    }

    public void setSystemAvatarId(Integer systemAvatarId) {
        this.systemAvatarId = systemAvatarId;
    }

    /**
     * Returns when the corresponding database entry was last changed.
     *
     * @return When the corresponding database entry was last changed.
     */
    public Date getChanged() {
        return changed;
    }

    /**
     * Sets when the corresponding database entry was last changed.
     *
     * @param changed
     *            When the corresponding database entry was last changed.
     */
    public void setChanged(Date changed) {
        this.changed = changed;
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

    public boolean isSimpleDisplay() {
        return simpleDisplay;
    }

    public void setSimpleDisplay(boolean isSimpleDisplay) {
        this.simpleDisplay = isSimpleDisplay;
    }

    public boolean isShowSibling() {
        return showSibling;
    }

    public void setShowSibling(boolean isSiblingVisible) {
        this.showSibling = isSiblingVisible;
    }

    public boolean isShowRate() {
        return showRate;
    }

    public void setShowRate(boolean isRateVisible) {
        this.showRate = isRateVisible;
    }

    public boolean isShowAlbumSearch() {
        return showAlbumSearch;
    }

    public void setShowAlbumSearch(boolean isAlbumSearchVisible) {
        this.showAlbumSearch = isAlbumSearchVisible;
    }

    public boolean isShowLastPlay() {
        return showLastPlay;
    }

    public void setShowLastPlay(boolean isLastPlayVisible) {
        this.showLastPlay = isLastPlayVisible;
    }

    public boolean isShowDownload() {
        return showDownload;
    }

    public void setShowDownload(boolean isDownloadVisible) {
        this.showDownload = isDownloadVisible;
    }

    public boolean isShowTag() {
        return showTag;
    }

    public void setShowTag(boolean isTagVisible) {
        this.showTag = isTagVisible;
    }

    public boolean isShowComment() {
        return showComment;
    }

    public void setShowComment(boolean isCommentVisible) {
        this.showComment = isCommentVisible;
    }

    public boolean isShowShare() {
        return showShare;
    }

    public void setShowShare(boolean isShareVisible) {
        this.showShare = isShareVisible;
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

    public boolean isForceBio2Eng() {
        return forceBio2Eng;
    }

    public void setForceBio2Eng(boolean forceBio2Eng) {
        this.forceBio2Eng = forceBio2Eng;
    }

    public boolean isVoiceInputEnabled() {
        return voiceInputEnabled;
    }

    public void setVoiceInputEnabled(boolean voiceInputEnabled) {
        this.voiceInputEnabled = voiceInputEnabled;
    }

    public boolean isShowCurrentSongInfo() {
        return showCurrentSongInfo;
    }

    public void setShowCurrentSongInfo(boolean showCurrentSongInfo) {
        this.showCurrentSongInfo = showCurrentSongInfo;
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

    /**
     * Configuration of what information to display about a song.
     */
    public static class Visibility {
        private boolean trackNumberVisible;
        private boolean artistVisible;
        private boolean albumVisible;
        private boolean genreVisible;
        private boolean yearVisible;
        private boolean bitRateVisible;
        private boolean durationVisible;
        private boolean formatVisible;
        private boolean fileSizeVisible;
        // JP >>>>
        private boolean composerVisible;
        // <<<< JP

        public Visibility() {
        }

        public Visibility(boolean trackNumberVisible, boolean artistVisible, boolean albumVisible, boolean genreVisible,
                boolean yearVisible, boolean bitRateVisible, boolean durationVisible, boolean formatVisible,
                boolean fileSizeVisible,
                // JP >>>>
                boolean composerVisible // <<<< JP
        ) {
            this.trackNumberVisible = trackNumberVisible;
            this.artistVisible = artistVisible;
            this.albumVisible = albumVisible;
            this.genreVisible = genreVisible;
            this.yearVisible = yearVisible;
            this.bitRateVisible = bitRateVisible;
            this.durationVisible = durationVisible;
            this.formatVisible = formatVisible;
            this.fileSizeVisible = fileSizeVisible;
            // JP >>>>
            this.composerVisible = composerVisible;
            // <<<< JP
        }

        public boolean isTrackNumberVisible() {
            return trackNumberVisible;
        }

        public void setTrackNumberVisible(boolean trackNumberVisible) {
            this.trackNumberVisible = trackNumberVisible;
        }

        public boolean isArtistVisible() {
            return artistVisible;
        }

        public void setArtistVisible(boolean artistVisible) {
            this.artistVisible = artistVisible;
        }

        public boolean isAlbumVisible() {
            return albumVisible;
        }

        public void setAlbumVisible(boolean albumVisible) {
            this.albumVisible = albumVisible;
        }

        public boolean isGenreVisible() {
            return genreVisible;
        }

        public void setGenreVisible(boolean genreVisible) {
            this.genreVisible = genreVisible;
        }

        public boolean isYearVisible() {
            return yearVisible;
        }

        public void setYearVisible(boolean yearVisible) {
            this.yearVisible = yearVisible;
        }

        public boolean isBitRateVisible() {
            return bitRateVisible;
        }

        public void setBitRateVisible(boolean bitRateVisible) {
            this.bitRateVisible = bitRateVisible;
        }

        public boolean isDurationVisible() {
            return durationVisible;
        }

        public void setDurationVisible(boolean durationVisible) {
            this.durationVisible = durationVisible;
        }

        public boolean isFormatVisible() {
            return formatVisible;
        }

        public void setFormatVisible(boolean formatVisible) {
            this.formatVisible = formatVisible;
        }

        public boolean isFileSizeVisible() {
            return fileSizeVisible;
        }

        public void setFileSizeVisible(boolean fileSizeVisible) {
            this.fileSizeVisible = fileSizeVisible;
        }

        public boolean isComposerVisible() {
            return composerVisible;
        }

        public void setComposerVisible(boolean isComposerVisible) {
            this.composerVisible = isComposerVisible;
        }

    }
}
