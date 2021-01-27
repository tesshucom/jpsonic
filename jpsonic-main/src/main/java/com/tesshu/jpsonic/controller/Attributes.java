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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.controller;

/**
 * Hierarchical enumeration of Attributes used by the controller.
 */
/*
 * The immediate purpose is to port the legacy. It is useful for preventing spelling mistakes, understanding the
 * hierarchical structure, identifying where to use it, and considering better management methods. #820
 */
public class Attributes {

    public enum Model {

        ERROR("error");

        /*
         * Enum of keys mapped to the model
         */
        public enum Command {
            ID("id");

            public static final String VALUE = "command";

            private String v;

            private Command(String value) {
                this.v = value;
            }

            public String value() {
                return v;
            }
        }

        public static final String VALUE = "model";

        private String v;

        private Model(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }

    public enum Redirect {

        BINDING_RESULT("org.springframework.validation.BindingResult.command"), COMMAND("command"), ERROR("error"),
        MODEL("model"), NEW_TRANSCODING("newTranscoding"), RELOAD_FLAG("settings_reload"), TOAST_FLAG("settings_toast"),
        USER_INDEX("userIndex");

        private String v;

        private Redirect(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }

    public enum Request {
        ACTION(NameConstants.ACTION), ADD(NameConstants.ADD), ADD_TO_PLAYLIST(NameConstants.ADD_TO_PLAYLIST),
        ADMIN_ROLE(NameConstants.ADMIN_ROLE), ALBUM(NameConstants.ALBUM), ALBUM_COUNT(NameConstants.ALBUM_COUNT),
        ALBUM_OFFSET(NameConstants.ALBUM_OFFSET), ANY(NameConstants.ANY), ARTIST(NameConstants.ARTIST),
        ARTIST_COUNT(NameConstants.ARTIST_COUNT), ARTIST_OFFSET(NameConstants.ARTIST_OFFSET),
        BITRATE(NameConstants.BITRATE), C(NameConstants.C), CALLBACK(NameConstants.CALLBACK),
        CHANNEL_ID(NameConstants.CHANNEL_ID), CLONE(NameConstants.CLONE), COMMENT(NameConstants.COMMENT),
        COMMENT_ROLE(NameConstants.COMMENT_ROLE), COUNT(NameConstants.COUNT),
        COVER_ART_ROLE(NameConstants.COVER_ART_ROLE), CURRENT(NameConstants.CURRENT), DECADE(NameConstants.DECADE),
        DEFAULT_ACTIVE(NameConstants.DEFAULT_ACTIVE), DELETE(NameConstants.DELETE),
        DELETE_CHANNEL(NameConstants.DELETE_CHANNEL), DELETE_EPISODE(NameConstants.DELETE_EPISODE),
        DELETE_EXPIRED(NameConstants.DELETE_EXPIRED), DESCRIPTION(NameConstants.DESCRIPTION),
        DLNA_ALBUM_BYGENRE_VISIBLE(NameConstants.DLNA_ALBUM_BYGENRE_VISIBLE),
        DLNA_ALBUM_VISIBLE(NameConstants.DLNA_ALBUM_VISIBLE),
        DLNA_ARTIST_BY_FOLDER_VISIBLE(NameConstants.DLNA_ARTIST_BY_FOLDER_VISIBLE),
        DLNA_ARTIST_VISIBLE(NameConstants.DLNA_ARTIST_VISIBLE), DLNA_BASE_LAN_URL(NameConstants.DLNA_BASE_LAN_URL),
        DLNA_ENABLED(NameConstants.DLNA_ENABLED), DLNA_FOLDER_VISIBLE(NameConstants.DLNA_FOLDER_VISIBLE),
        DLNA_GENRE_COUNT_VISIBLE(NameConstants.DLNA_GENRE_COUNT_VISIBLE),
        DLNA_GUEST_PUBLISH(NameConstants.DLNA_GUEST_PUBLISH),
        DLNA_INDEX_ID3_VISIBLE(NameConstants.DLNA_INDEX_ID3_VISIBLE),
        DLNA_INDEX_VISIBLE(NameConstants.DLNA_INDEX_VISIBLE),
        DLNA_PLAYLIST_VISIBLE(NameConstants.DLNA_PLAYLIST_VISIBLE),
        DLNA_PODCAST_VISIBLE(NameConstants.DLNA_PODCAST_VISIBLE),
        DLNA_RANDOM_ALBUM_VISIBLE(NameConstants.DLNA_RANDOM_ALBUM_VISIBLE),
        DLNA_RANDOM_MAX(NameConstants.DLNA_RANDOM_MAX),
        DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE(NameConstants.DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE),
        DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE(NameConstants.DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE),
        DLNA_RANDOM_SONG_VISIBLE(NameConstants.DLNA_RANDOM_SONG_VISIBLE),
        DLNA_RECENT_ALBUM_ID3_VISIBLE(NameConstants.DLNA_RECENT_ALBUM_ID3_VISIBLE),
        DLNA_RECENT_ALBUM_VISIBLE(NameConstants.DLNA_RECENT_ALBUM_VISIBLE),
        DLNA_SERVER_NAME(NameConstants.DLNA_SERVER_NAME),
        DLNA_SONG_BY_GENRE_VISIBLE(NameConstants.DLNA_SONG_BY_GENRE_VISIBLE),
        DOWNLOAD_EPISODE(NameConstants.DOWNLOAD_EPISODE), DOWNLOAD_ROLE(NameConstants.DOWNLOAD_ROLE),
        DURATION(NameConstants.DURATION), EMAIL(NameConstants.EMAIL), ENABLED(NameConstants.ENABLED),
        ERROR(NameConstants.ERROR), EXPIRE_IN(NameConstants.EXPIREIN), EXPIRES(NameConstants.EXPIRES),
        F(NameConstants.F), FORCE_CUSTOM(NameConstants.FORCE_CUSTOM), FORMAT(NameConstants.FORMAT),
        FROM_YEAR(NameConstants.FROM_YEAR), G_RECAPTCHA_RESPONSE(NameConstants.G_RECAPTCHA_RESPONSE),
        GAIN(NameConstants.GAIN), GENRE(NameConstants.GENRE), HIDE(NameConstants.HIDE), HLS(NameConstants.HLS),
        HLS_COMMAND(NameConstants.HLS_COMMAND), HOMEPAGE_URL(NameConstants.HOMEPAGE_URL), I(NameConstants.I),
        ID(NameConstants.ID), IF_MODIFIED_SINCE(NameConstants.IF_MODIFIED_SINCE),
        INCLUDE_EPISODES(NameConstants.INCLUDE_EPISODES), INCLUDE_NOT_PRESENT(NameConstants.INCLUDE_NOT_PRESENT),
        INDEX(NameConstants.INDEX), J_PASSWORD(NameConstants.J_PASSWORD), J_USERNAME(NameConstants.J_USERNAME),
        JUKEBOX_ROLE(NameConstants.JUKEBOX_ROLE), LDAP_AUTHENTICATED(NameConstants.LDAP_AUTHENTICATED),
        LIST_OFFSET(NameConstants.LIST_OFFSET), LIST_TYPE(NameConstants.LIST_TYPE), LOGOUT(NameConstants.LOGOUT),
        MAX_BIT_RATE(NameConstants.MAX_BIT_RATE), MUSIC_FOLDER_ID(NameConstants.MUSIC_FOLDER_ID),
        NAME(NameConstants.NAME), OFFSET(NameConstants.OFFSET), OFFSET_SECONDS(NameConstants.OFFSET_SECONDS),
        P(NameConstants.P), PASSWORD(NameConstants.PASSWORD), PATH(NameConstants.PATH), PLAYER(NameConstants.PLAYER),
        PLAYLIST(NameConstants.PLAYLIST), PLAYLIST_ID(NameConstants.PLAYLIST_ID),
        PODCAST_ROLE(NameConstants.PODCAST_ROLE), POSITION(NameConstants.POSITION), PUBLIC(NameConstants.PUBLIC),
        QUERY(NameConstants.QUERY), RATING(NameConstants.RATING), REFRESH(NameConstants.REFRESH), S(NameConstants.S),
        SETTINGS_ROLE(NameConstants.SETTINGS_ROLE), SHARE_ROLE(NameConstants.SHARE_ROLE),
        SHOW_OUTLINE_HELP(NameConstants.SHOW_OUTLINE_HELP), SIZE(NameConstants.SIZE), SONG(NameConstants.SONG),
        SONG_COUNT(NameConstants.SONG_COUNT), SONG_ID(NameConstants.SONG_ID),
        SONG_ID_TO_ADD(NameConstants.SONG_ID_TO_ADD), SONG_INDEX_TO_REMOVE(NameConstants.SONG_INDEX_TO_REMOVE),
        SONG_OFFSET(NameConstants.SONG_OFFSET), SONOS_ENABLED(NameConstants.SONOS_ENABLED),
        SONOS_SERVICE_NAME(NameConstants.SONOS_SERVICE_NAME), SOURCE_FORMATS(NameConstants.SOURCE_FORMATS),
        STEP1(NameConstants.STEP1), STEP2(NameConstants.STEP2), STREAM_ROLE(NameConstants.STREAM_ROLE),
        STREAM_URL(NameConstants.STREAM_URL), SUBMISSION(NameConstants.SUBMISSION), SUFFIX(NameConstants.SUFFIX),
        T(NameConstants.T), TARGET_FORMAT(NameConstants.TARGET_FORMAT), TIME_OFFSET(NameConstants.TIME_OFFSET),
        TITLE(NameConstants.TITLE), TO_YEAR(NameConstants.TO_YEAR), TYPE(NameConstants.TYPE), U(NameConstants.U),
        UPLOAD_ROLE(NameConstants.UPLOAD_ROLE), URL(NameConstants.URL), USER(NameConstants.USER),
        USER_INDEX(NameConstants.USERINDEX), USER_NAME(NameConstants.USERNAME),
        USERNAME_OR_EMAIL(NameConstants.USERNAME_OR_EMAIL), V(NameConstants.V),
        VIEW_AS_LIST(NameConstants.VIEW_AS_LIST), X(NameConstants.X);

        public static class NameConstants { // Used from annotation
            public static final String ACTION = "action";
            public static final String ADD = "add";
            public static final String ADD_TO_PLAYLIST = "addToPlaylist";
            public static final String ADMIN_ROLE = "adminRole";
            public static final String ALBUM = "album";
            public static final String ALBUM_COUNT = "albumCount";
            public static final String ALBUM_OFFSET = "albumOffset";
            public static final String ALBUM_RATING_COMP = "albumRatingComp";
            public static final String ALBUM_RATING_VALUE = "albumRatingValue";
            public static final String ANY = "any";
            public static final String ARTIST = "artist";
            public static final String ARTIST_COUNT = "artistCount";
            public static final String ARTIST_OFFSET = "artistOffset";
            public static final String AUTO_RANDOM = "autoRandom";
            public static final String BITRATE = "bitRate";
            public static final String C = "c";
            public static final String CALLBACK = "callback";
            public static final String CHANNEL_ID = "channelId";
            public static final String CLONE = "clone";
            public static final String COMMENT = "comment";
            public static final String COMMENT_ROLE = "commentRole";
            public static final String COUNT = "count";
            public static final String COVER_ART_ROLE = "coverArtRole";
            public static final String CURRENT = "current";
            public static final String DECADE = "decade";
            public static final String DEFAULT_ACTIVE = "defaultActive";
            public static final String DELETE = "delete";
            public static final String DELETE_CHANNEL = "deleteChannel";
            public static final String DELETE_EPISODE = "deleteEpisode";
            public static final String DELETE_EXPIRED = "deleteExpired";
            public static final String DESCRIPTION = "description";
            public static final String DLNA_ALBUM_BYGENRE_VISIBLE = "dlnaAlbumByGenreVisible";
            public static final String DLNA_ALBUM_VISIBLE = "dlnaAlbumVisible";
            public static final String DLNA_ARTIST_BY_FOLDER_VISIBLE = "dlnaArtistByFolderVisible";
            public static final String DLNA_ARTIST_VISIBLE = "dlnaArtistVisible";
            public static final String DLNA_BASE_LAN_URL = "dlnaBaseLANURL";
            public static final String DLNA_ENABLED = "dlnaEnabled";
            public static final String DLNA_FOLDER_VISIBLE = "dlnaFolderVisible";
            public static final String DLNA_GENRE_COUNT_VISIBLE = "dlnaGenreCountVisible";
            public static final String DLNA_GUEST_PUBLISH = "dlnaGuestPublish";
            public static final String DLNA_INDEX_ID3_VISIBLE = "dlnaIndexId3Visible";
            public static final String DLNA_INDEX_VISIBLE = "dlnaIndexVisible";
            public static final String DLNA_PLAYLIST_VISIBLE = "dlnaPlaylistVisible";
            public static final String DLNA_PODCAST_VISIBLE = "dlnaPodcastVisible";
            public static final String DLNA_RANDOM_ALBUM_VISIBLE = "dlnaRandomAlbumVisible";
            public static final String DLNA_RANDOM_MAX = "dlnaRandomMax";
            public static final String DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE = "dlnaRandomSongByArtistVisible";
            public static final String DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE = "dlnaRandomSongByFolderArtistVisible";
            public static final String DLNA_RANDOM_SONG_VISIBLE = "dlnaRandomSongVisible";
            public static final String DLNA_RECENT_ALBUM_ID3_VISIBLE = "dlnaRecentAlbumId3Visible";
            public static final String DLNA_RECENT_ALBUM_VISIBLE = "dlnaRecentAlbumVisible";
            public static final String DLNA_SERVER_NAME = "dlnaServerName";
            public static final String DLNA_SONG_BY_GENRE_VISIBLE = "dlnaSongByGenreVisible";
            public static final String DOWNLOAD_EPISODE = "downloadEpisode";
            public static final String DOWNLOAD_ROLE = "downloadRole";
            public static final String DURATION = "duration";
            public static final String EMAIL = "email";
            public static final String ENABLED = "enabled";
            public static final String ERROR = "error";
            public static final String EXPIREIN = "expireIn";
            public static final String EXPIRES = "expires";
            public static final String EXPUNGE = "expunge";
            public static final String F = "f";
            public static final String FORCE_CUSTOM = "forceCustom";
            public static final String FORMAT = "format";
            public static final String FROM_YEAR = "fromYear";
            public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
            public static final String GAIN = "gain";
            public static final String GENRE = "genre";
            public static final String HIDE = "hide";
            public static final String HLS = "hls";
            public static final String HLS_COMMAND = "hlsCommand";
            public static final String HOMEPAGE_URL = "homepageUrl";
            public static final String I = "i";
            public static final String ID = "id";
            public static final String IF_MODIFIED_SINCE = "ifModifiedSince";
            public static final String INCLUDE_EPISODES = "includeEpisodes";
            public static final String INCLUDE_NOT_PRESENT = "includeNotPresent";
            public static final String INDEX = "index";
            public static final String J_PASSWORD = "j_password";
            public static final String J_USERNAME = "j_username";
            public static final String JUKEBOX_ROLE = "jukeboxRole";
            public static final String LAST_PLAYED_COMP = "lastPlayedComp";
            public static final String LAST_PLAYED_VALUE = "lastPlayedValue";
            public static final String LDAP_AUTHENTICATED = "ldapAuthenticated";
            public static final String LIST_OFFSET = "listOffset";
            public static final String LIST_TYPE = "listType";
            public static final String LOGOUT = "logout";
            public static final String MAX_BIT_RATE = "maxBitRate";
            public static final String MUSIC_FOLDER_ID = "musicFolderId";
            public static final String NAME = "name";
            public static final String OFFSET = "offset";
            public static final String OFFSET_SECONDS = "offsetSeconds";
            public static final String P = "p";
            public static final String PASSWORD = "password";
            public static final String PATH = "path";
            public static final String PLAY_COUNT_COMP = "playCountComp";
            public static final String PLAY_COUNT_VALUE = "playCountValue";
            public static final String PLAYER = "player";
            public static final String PLAYLIST = "playlist";
            public static final String PLAYLIST_ID = "playlistId";
            public static final String PODCAST_ROLE = "podcastRole";
            public static final String POSITION = "position";
            public static final String PUBLIC = "public";
            public static final String QUERY = "query";
            public static final String RATING = "rating";
            public static final String REFRESH = "refresh";
            public static final String S = "s";
            public static final String SCAN_NOW = "scanNow";
            public static final String SETTINGS_ROLE = "settingsRole";
            public static final String SHARE_ROLE = "shareRole";
            public static final String SHOW_ALL = "showAll";
            public static final String SHOW_OUTLINE_HELP = "showOutlineHelp";
            public static final String SIZE = "size";
            public static final String SONG = "song";
            public static final String SONG_COUNT = "songCount";
            public static final String SONG_ID = "songId";
            public static final String SONG_ID_TO_ADD = "songIdToAdd";
            public static final String SONG_INDEX_TO_REMOVE = "songIndexToRemove";
            public static final String SONG_OFFSET = "songOffset";
            public static final String SONG_RATING = "songRating";
            public static final String SONOS_ENABLED = "sonosEnabled";
            public static final String SONOS_SERVICE_NAME = "sonosServiceName";
            public static final String SOURCE_FORMATS = "sourceFormats";
            public static final String STEP1 = "step1";
            public static final String STEP2 = "step2";
            public static final String STREAM_ROLE = "streamRole";
            public static final String STREAM_URL = "streamUrl";
            public static final String SUBMISSION = "submission";
            public static final String SUFFIX = "suffix";
            public static final String T = "t";
            public static final String TARGET_FORMAT = "targetFormat";
            public static final String TIME_OFFSET = "timeOffset";
            public static final String TITLE = "title";
            public static final String TO_YEAR = "toYear";
            public static final String TOAST = "toast";
            public static final String TYPE = "type";
            public static final String U = "u";
            public static final String UPLOAD_ROLE = "uploadRole";
            public static final String URL = "url";
            public static final String USER = "user";
            public static final String USERINDEX = "userIndex";
            public static final String USERNAME = "username";
            public static final String USERNAME_OR_EMAIL = "usernameOrEmail";
            public static final String V = "v";
            public static final String VIEW_AS_LIST = "viewAsList";
            public static final String X = "x";
            public static final String YEAR = "year";

            private NameConstants() {
            }
        }

        private String v;

        private Request(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }

    public enum Session {

        PLAYER("player"), UPLOAD_STATUS("uploadStatus");

        private String v;

        private Session(String value) {
            this.v = value;
        }

        public String value() {
            return v;
        }
    }

}
