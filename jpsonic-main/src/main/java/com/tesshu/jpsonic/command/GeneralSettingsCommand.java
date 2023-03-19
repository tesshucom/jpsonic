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

import com.tesshu.jpsonic.controller.GeneralSettingsController;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.Theme;

/**
 * Command used in {@link GeneralSettingsController}.
 *
 * @author Sindre Mehus
 */
public class GeneralSettingsCommand extends SettingsPageCommons {

    // Language and theme
    private List<Theme> themes;
    private String themeIndex = "0";
    private List<String> locales;
    private String localeIndex = "0";
    private IndexScheme indexScheme;

    // Index settings
    private String defaultIndexString;
    private String simpleIndexString;
    private String index;
    private String ignoredArticles;
    private boolean ignoreFullWidth;
    private boolean deleteDiacritic;

    // Sort settings
    private boolean sortAlbumsByYear;
    private boolean sortGenresByAlphabet;
    private boolean prohibitSortVarious;
    private boolean defaultSortAlbumsByYear;
    private boolean defaultSortGenresByAlphabet;
    private boolean defaultProhibitSortVarious;

    // Search settings
    private boolean searchComposer;
    private boolean outputSearchQuery;

    // Suppressed legacy features
    private boolean showServerLog;
    private boolean showStatus;
    private boolean othersPlayingEnabled;
    private boolean showRememberMe;
    private boolean publishPodcast;
    private boolean useExternalPlayer;
    private boolean useCopyOfAsciiUnprintable;
    private boolean useJsonp;
    private boolean useRemovingTrackFromId3Title;
    private boolean useCleanUp;
    private boolean redundantFolderCheck;
    private boolean showIndexDetails;
    private boolean showDBDetails;

    // Extensions and shortcuts
    private String musicFileTypes;
    private String videoFileTypes;
    private String coverArtFileTypes;
    private String excludedCoverArts;
    private String playlistFolder;
    private String shortcuts;
    private String defaultMusicFileTypes;
    private String defaultVideoFileTypes;
    private String defaultCoverArtFileTypes;
    private String defaultExcludedCoverArts;
    private String defaultPlaylistFolder;
    private String defaultShortcuts;

    // Welcom message
    private boolean gettingStartedEnabled;
    private String welcomeTitle;
    private String welcomeSubtitle;
    private String welcomeMessage;
    private String loginMessage;

    public List<Theme> getThemes() {
        return themes;
    }

    public void setThemes(List<Theme> themes) {
        this.themes = themes;
    }

    public String getThemeIndex() {
        return themeIndex;
    }

    public void setThemeIndex(String themeIndex) {
        this.themeIndex = themeIndex;
    }

    public List<String> getLocales() {
        return locales;
    }

    public void setLocales(List<String> locales) {
        this.locales = locales;
    }

    public String getLocaleIndex() {
        return localeIndex;
    }

    public void setLocaleIndex(String localeIndex) {
        this.localeIndex = localeIndex;
    }

    public IndexScheme getIndexScheme() {
        return indexScheme;
    }

    public void setIndexScheme(IndexScheme indexScheme) {
        this.indexScheme = indexScheme;
    }

    public String getDefaultIndexString() {
        return defaultIndexString;
    }

    public void setDefaultIndexString(String defaultIndexString) {
        this.defaultIndexString = defaultIndexString;
    }

    public String getSimpleIndexString() {
        return simpleIndexString;
    }

    public void setSimpleIndexString(String simpleIndexString) {
        this.simpleIndexString = simpleIndexString;
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

    public boolean isIgnoreFullWidth() {
        return ignoreFullWidth;
    }

    public void setIgnoreFullWidth(boolean ignoreFullWidth) {
        this.ignoreFullWidth = ignoreFullWidth;
    }

    public boolean isDeleteDiacritic() {
        return deleteDiacritic;
    }

    public void setDeleteDiacritic(boolean deleteDiacritic) {
        this.deleteDiacritic = deleteDiacritic;
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

    public void setOthersPlayingEnabled(boolean othersPlayingEnabled) {
        this.othersPlayingEnabled = othersPlayingEnabled;
    }

    public boolean isShowRememberMe() {
        return showRememberMe;
    }

    public void setShowRememberMe(boolean showRememberMe) {
        this.showRememberMe = showRememberMe;
    }

    public boolean isPublishPodcast() {
        return publishPodcast;
    }

    public void setPublishPodcast(boolean publishPodcast) {
        this.publishPodcast = publishPodcast;
    }

    public boolean isUseExternalPlayer() {
        return useExternalPlayer;
    }

    public void setUseExternalPlayer(boolean useExternalPlayer) {
        this.useExternalPlayer = useExternalPlayer;
    }

    public boolean isUseCopyOfAsciiUnprintable() {
        return useCopyOfAsciiUnprintable;
    }

    public void setUseCopyOfAsciiUnprintable(boolean useCopyOfAsciiUnprintable) {
        this.useCopyOfAsciiUnprintable = useCopyOfAsciiUnprintable;
    }

    public boolean isUseJsonp() {
        return useJsonp;
    }

    public void setUseJsonp(boolean useJsonp) {
        this.useJsonp = useJsonp;
    }

    public boolean isUseRemovingTrackFromId3Title() {
        return useRemovingTrackFromId3Title;
    }

    public void setUseRemovingTrackFromId3Title(boolean useRemovingTrackFromId3Title) {
        this.useRemovingTrackFromId3Title = useRemovingTrackFromId3Title;
    }

    public boolean isUseCleanUp() {
        return useCleanUp;
    }

    public void setUseCleanUp(boolean useCleanUp) {
        this.useCleanUp = useCleanUp;
    }

    public boolean isRedundantFolderCheck() {
        return redundantFolderCheck;
    }

    public void setRedundantFolderCheck(boolean redundantFolderCheck) {
        this.redundantFolderCheck = redundantFolderCheck;
    }

    public boolean isShowIndexDetails() {
        return showIndexDetails;
    }

    public void setShowIndexDetails(boolean showIndexDetails) {
        this.showIndexDetails = showIndexDetails;
    }

    public boolean isShowDBDetails() {
        return showDBDetails;
    }

    public void setShowDBDetails(boolean showDBDetails) {
        this.showDBDetails = showDBDetails;
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

    public String getExcludedCoverArts() {
        return excludedCoverArts;
    }

    public void setExcludedCoverArts(String excludedCoverArts) {
        this.excludedCoverArts = excludedCoverArts;
    }

    public String getPlaylistFolder() {
        return playlistFolder;
    }

    public void setPlaylistFolder(String playlistFolder) {
        this.playlistFolder = playlistFolder;
    }

    public String getShortcuts() {
        return shortcuts;
    }

    public void setShortcuts(String shortcuts) {
        this.shortcuts = shortcuts;
    }

    public String getDefaultMusicFileTypes() {
        return defaultMusicFileTypes;
    }

    public void setDefaultMusicFileTypes(String defaultMusicFileTypes) {
        this.defaultMusicFileTypes = defaultMusicFileTypes;
    }

    public String getDefaultVideoFileTypes() {
        return defaultVideoFileTypes;
    }

    public void setDefaultVideoFileTypes(String defaultVideoFileTypes) {
        this.defaultVideoFileTypes = defaultVideoFileTypes;
    }

    public String getDefaultCoverArtFileTypes() {
        return defaultCoverArtFileTypes;
    }

    public void setDefaultCoverArtFileTypes(String defaultCoverArtFileTypes) {
        this.defaultCoverArtFileTypes = defaultCoverArtFileTypes;
    }

    public String getDefaultExcludedCoverArts() {
        return defaultExcludedCoverArts;
    }

    public void setDefaultExcludedCoverArts(String defaultExcludedCoverArts) {
        this.defaultExcludedCoverArts = defaultExcludedCoverArts;
    }

    public String getDefaultPlaylistFolder() {
        return defaultPlaylistFolder;
    }

    public void setDefaultPlaylistFolder(String defaultPlaylistFolder) {
        this.defaultPlaylistFolder = defaultPlaylistFolder;
    }

    public String getDefaultShortcuts() {
        return defaultShortcuts;
    }

    public void setDefaultShortcuts(String defaultShortcuts) {
        this.defaultShortcuts = defaultShortcuts;
    }

    public boolean isGettingStartedEnabled() {
        return gettingStartedEnabled;
    }

    public void setGettingStartedEnabled(boolean gettingStartedEnabled) {
        this.gettingStartedEnabled = gettingStartedEnabled;
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
}
