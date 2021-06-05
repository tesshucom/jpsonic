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

import com.tesshu.jpsonic.controller.GeneralSettingsController;
import com.tesshu.jpsonic.domain.Theme;

/**
 * Command used in {@link GeneralSettingsController}.
 *
 * @author Sindre Mehus
 */
public class GeneralSettingsCommand {

    private String playlistFolder;
    private String musicFileTypes;
    private String videoFileTypes;
    private String coverArtFileTypes;
    private String index;
    private String ignoredArticles;
    private String shortcuts;
    private boolean sortAlbumsByYear;
    private boolean sortGenresByAlphabet;
    private boolean prohibitSortVarious;
    private boolean sortAlphanum;
    private boolean sortStrict;
    private boolean searchComposer;
    private boolean outputSearchQuery;
    private boolean gettingStartedEnabled;
    private boolean searchMethodLegacy;
    private boolean searchMethodChanged;
    private boolean anonymousTranscoding;
    private boolean openDetailSetting;
    private boolean useRadio;
    private boolean useSonos;
    private boolean publishPodcast;
    private boolean showJavaJukebox;
    private boolean showServerLog;
    private boolean showStatus;
    private boolean othersPlayingEnabled;
    private boolean showRememberMe;
    private boolean showToast;
    private String welcomeTitle;
    private String welcomeSubtitle;
    private String welcomeMessage;
    private String loginMessage;
    private String localeIndex;
    private String[] locales;
    private String themeIndex;
    private Theme[] themes;
    private String simpleIndexString;
    private boolean showOutlineHelp;
    private int shareCount;

    private String defaultIndexString;
    private boolean defaultSortAlbumsByYear;
    private boolean defaultSortGenresByAlphabet;
    private boolean defaultProhibitSortVarious;
    private boolean defaultSortAlphanum;
    private boolean defaultSortStrict;

    public String getPlaylistFolder() {
        return playlistFolder;
    }

    public void setPlaylistFolder(String playlistFolder) {
        this.playlistFolder = playlistFolder;
    }

    public String getMusicFileTypes() {
        return musicFileTypes;
    }

    public void setMusicFileTypes(String musicFileTypes) {
        this.musicFileTypes = musicFileTypes;
    }

    public String getVideoFileTypes() {
        return videoFileTypes;
    }

    public void setVideoFileTypes(String videoFileTypes) {
        this.videoFileTypes = videoFileTypes;
    }

    public String getCoverArtFileTypes() {
        return coverArtFileTypes;
    }

    public void setCoverArtFileTypes(String coverArtFileTypes) {
        this.coverArtFileTypes = coverArtFileTypes;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getIgnoredArticles() {
        return ignoredArticles;
    }

    public void setIgnoredArticles(String ignoredArticles) {
        this.ignoredArticles = ignoredArticles;
    }

    public String getShortcuts() {
        return shortcuts;
    }

    public void setShortcuts(String shortcuts) {
        this.shortcuts = shortcuts;
    }

    public String getWelcomeTitle() {
        return welcomeTitle;
    }

    public void setWelcomeTitle(String welcomeTitle) {
        this.welcomeTitle = welcomeTitle;
    }

    public String getWelcomeSubtitle() {
        return welcomeSubtitle;
    }

    public void setWelcomeSubtitle(String welcomeSubtitle) {
        this.welcomeSubtitle = welcomeSubtitle;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getLoginMessage() {
        return loginMessage;
    }

    public void setLoginMessage(String loginMessage) {
        this.loginMessage = loginMessage;
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

    public boolean isSortAlbumsByYear() {
        return sortAlbumsByYear;
    }

    public void setSortAlbumsByYear(boolean sortAlbumsByYear) {
        this.sortAlbumsByYear = sortAlbumsByYear;
    }

    public boolean isSortGenresByAlphabet() {
        return sortGenresByAlphabet;
    }

    public void setSortGenresByAlphabet(boolean sortGenresByAlphabet) {
        this.sortGenresByAlphabet = sortGenresByAlphabet;
    }

    public boolean isProhibitSortVarious() {
        return prohibitSortVarious;
    }

    public void setProhibitSortVarious(boolean prohibitSortVarious) {
        this.prohibitSortVarious = prohibitSortVarious;
    }

    public boolean isSortAlphanum() {
        return sortAlphanum;
    }

    public void setSortAlphanum(boolean sortAlphanum) {
        this.sortAlphanum = sortAlphanum;
    }

    public boolean isSortStrict() {
        return sortStrict;
    }

    public void setSortStrict(boolean sortStrict) {
        this.sortStrict = sortStrict;
    }

    public boolean isSearchComposer() {
        return searchComposer;
    }

    public void setSearchComposer(boolean searchComposer) {
        this.searchComposer = searchComposer;
    }

    public boolean isOutputSearchQuery() {
        return outputSearchQuery;
    }

    public void setOutputSearchQuery(boolean outputSearchQuery) {
        this.outputSearchQuery = outputSearchQuery;
    }

    public boolean isGettingStartedEnabled() {
        return gettingStartedEnabled;
    }

    public void setGettingStartedEnabled(boolean gettingStartedEnabled) {
        this.gettingStartedEnabled = gettingStartedEnabled;
    }

    public boolean isSearchMethodLegacy() {
        return searchMethodLegacy;
    }

    public void setSearchMethodLegacy(boolean searchMethodLegacy) {
        this.searchMethodLegacy = searchMethodLegacy;
    }

    public boolean isSearchMethodChanged() {
        return searchMethodChanged;
    }

    public void setSearchMethodChanged(boolean searchMethodChanged) {
        this.searchMethodChanged = searchMethodChanged;
    }

    public boolean isAnonymousTranscoding() {
        return anonymousTranscoding;
    }

    public void setAnonymousTranscoding(boolean anonymousTranscoding) {
        this.anonymousTranscoding = anonymousTranscoding;
    }

    public boolean isOpenDetailSetting() {
        return openDetailSetting;
    }

    public void setOpenDetailSetting(boolean openDetailSetting) {
        this.openDetailSetting = openDetailSetting;
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

    public void setUseSonos(boolean useSono) {
        this.useSonos = useSono;
    }

    public boolean isPublishPodcast() {
        return publishPodcast;
    }

    public void setPublishPodcast(boolean publishPodcast) {
        this.publishPodcast = publishPodcast;
    }

    public boolean isShowJavaJukebox() {
        return showJavaJukebox;
    }

    public void setShowJavaJukebox(boolean showJavaJukebox) {
        this.showJavaJukebox = showJavaJukebox;
    }

    public boolean isShowServerLog() {
        return showServerLog;
    }

    public void setShowServerLog(boolean showServerLog) {
        this.showServerLog = showServerLog;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
    }

    public boolean isOthersPlayingEnabled() {
        return othersPlayingEnabled;
    }

    public void setOthersPlayingEnabled(boolean othersplayingenabled) {
        this.othersPlayingEnabled = othersplayingenabled;
    }

    public boolean isShowRememberMe() {
        return showRememberMe;
    }

    public void setShowRememberMe(boolean showRememberMe) {
        this.showRememberMe = showRememberMe;
    }

    public boolean isShowToast() {
        return showToast;
    }

    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    public String getSimpleIndexString() {
        return simpleIndexString;
    }

    public void setSimpleIndexString(String simpleIndexString) {
        this.simpleIndexString = simpleIndexString;
    }

    public boolean isShowOutlineHelp() {
        return showOutlineHelp;
    }

    public void setShowOutlineHelp(boolean showOutlineHelp) {
        this.showOutlineHelp = showOutlineHelp;
    }

    public int getShareCount() {
        return shareCount;
    }

    public void setShareCount(int shareCount) {
        this.shareCount = shareCount;
    }

    public String getDefaultIndexString() {
        return defaultIndexString;
    }

    public void setDefaultIndexString(String defaultIndexString) {
        this.defaultIndexString = defaultIndexString;
    }

    public boolean isDefaultSortAlbumsByYear() {
        return defaultSortAlbumsByYear;
    }

    public void setDefaultSortAlbumsByYear(boolean defaultSortAlbumsByYear) {
        this.defaultSortAlbumsByYear = defaultSortAlbumsByYear;
    }

    public boolean isDefaultSortGenresByAlphabet() {
        return defaultSortGenresByAlphabet;
    }

    public void setDefaultSortGenresByAlphabet(boolean defaultSortGenresByAlphabet) {
        this.defaultSortGenresByAlphabet = defaultSortGenresByAlphabet;
    }

    public boolean isDefaultProhibitSortVarious() {
        return defaultProhibitSortVarious;
    }

    public void setDefaultProhibitSortVarious(boolean defaultProhibitSortVarious) {
        this.defaultProhibitSortVarious = defaultProhibitSortVarious;
    }

    public boolean isDefaultSortAlphanum() {
        return defaultSortAlphanum;
    }

    public void setDefaultSortAlphanum(boolean defaultSortAlphanum) {
        this.defaultSortAlphanum = defaultSortAlphanum;
    }

    public boolean isDefaultSortStrict() {
        return defaultSortStrict;
    }

    public void setDefaultSortStrict(boolean defaultSortStrict) {
        this.defaultSortStrict = defaultSortStrict;
    }
}
