/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2021 (C) tesshu.com
 */
package com.tesshu.jpsonic.controller;

/**
 * ViewName enumeration.
 */
/*
 * @see #826. Lists the names that currently use "view" in the suffix. (That is, the url where the view is processed by
 * pattern matching.) If we want to eliminate pattern matching, we need to change to a new mapping implementation that
 * meets the current matching specifications.
 */
public enum ViewName {

    ACCESS_DENIED(ViewNameConstants.ACCESS_DENIED), ADVANCED_SETTINGS(ViewNameConstants.ADVANCED_SETTINGS),
    AVATAR(ViewNameConstants.AVATAR), COVER_ART(ViewNameConstants.COVER_ART),
    DATABASE_SETTINGS(ViewNameConstants.DATABASE_SETTINGS), DLNA_SETTINGS(ViewNameConstants.DLNA_SETTINGS),
    GENERAL_SETTINGS(ViewNameConstants.GENERAL_SETTINGS), GETTING_STARTED(ViewNameConstants.GETTING_STARTED),
    HELP(ViewNameConstants.HELP), HOME(ViewNameConstants.HOME),
    INTERNET_RADIO_SETTINGS(ViewNameConstants.INTERNET_RADIO_SETTINGS),
    JUKEBOX_CONTROL(ViewNameConstants.JUKEBOX_CONTROL), LYRICS(ViewNameConstants.LYRICS), MAIN(ViewNameConstants.MAIN),
    MORE(ViewNameConstants.MORE), MUSIC_FOLDER_SETTINGS(ViewNameConstants.MUSIC_FOLDER_SETTINGS),
    NOTFOUND(ViewNameConstants.NOTFOUND), PASSWORD_SETTINGS(ViewNameConstants.PASSWORD_SETTINGS),
    PERSONAL_SETTINGS(ViewNameConstants.PERSONAL_SETTINGS), PLAY_QUEUE(ViewNameConstants.PLAY_QUEUE),
    PLAYER_SETTINGS(ViewNameConstants.PLAYER_SETTINGS), PODCAST(ViewNameConstants.PODCAST),
    PODCAST_CHANNEL(ViewNameConstants.PODCAST_CHANNEL), PODCAST_CHANNELS(ViewNameConstants.PODCAST_CHANNELS),
    PODCAST_SETTINGS(ViewNameConstants.PODCAST_SETTINGS), RANDOM_PLAYQUEUE(ViewNameConstants.RANDOM_PLAYQUEUE),
    SHARE_SETTINGS(ViewNameConstants.SHARE_SETTINGS), SONOS_SETTINGS(ViewNameConstants.SONOS_SETTINGS),
    STATUS_CHART(ViewNameConstants.STATUS_CHART), TOP(ViewNameConstants.TOP),
    TRANSCODING_SETTINGS(ViewNameConstants.TRANSCODING_SETTINGS), USER_CHART(ViewNameConstants.USER_CHART),
    USER_SETTINGS(ViewNameConstants.USER_SETTINGS);

    public static final class ViewNameConstants {

        public static final String ACCESS_DENIED = "accessDenied.view";
        public static final String ADVANCED_SETTINGS = "advancedSettings.view";
        public static final String AVATAR = "avatar.view";
        public static final String COVER_ART = "coverArt.view";
        public static final String DATABASE_SETTINGS = "databaseSettings.view";
        public static final String DLNA_SETTINGS = "dlnaSettings.view";
        public static final String GENERAL_SETTINGS = "generalSettings.view";
        public static final String GETTING_STARTED = "gettingStarted.view";
        public static final String HELP = "help.view";
        public static final String HOME = "home.view";
        public static final String INTERNET_RADIO_SETTINGS = "internetRadioSettings.view";
        public static final String JUKEBOX_CONTROL = "jukeboxControl.view";
        public static final String LYRICS = "lyrics.view";
        public static final String MAIN = "main.view";
        public static final String MORE = "more.view";
        public static final String MUSIC_FOLDER_SETTINGS = "musicFolderSettings.view";
        public static final String NOTFOUND = "notFound.view";
        public static final String PASSWORD_SETTINGS = "passwordSettings.view";
        public static final String PERSONAL_SETTINGS = "personalSettings.view";
        public static final String PLAY_QUEUE = "playQueue.view";
        public static final String PLAYER_SETTINGS = "playerSettings.view";
        public static final String PODCAST = "podcast.view";
        public static final String PODCAST_CHANNEL = "podcastChannel.view";
        public static final String PODCAST_CHANNELS = "podcastChannels.view";
        public static final String PODCAST_SETTINGS = "podcastSettings.view";
        public static final String RANDOM_PLAYQUEUE = "randomPlayQueue.view";// Used from annotation
        public static final String SHARE_SETTINGS = "shareSettings.view";
        public static final String SONOS_SETTINGS = "sonosSettings.view";
        public static final String STATUS_CHART = "statusChart.view";
        public static final String TOP = "top.view";
        public static final String TRANSCODING_SETTINGS = "transcodingSettings.view";
        public static final String USER_CHART = "userChart.view";
        public static final String USER_SETTINGS = "userSettings.view";

        private ViewNameConstants() {
        }
    }

    private String v;

    private ViewName(String value) {
        this.v = value;
    }

    public String value() {
        return v;
    }
}
