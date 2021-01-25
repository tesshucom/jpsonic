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
package org.airsonic.player.domain;

/**
 * Represent a user.
 *
 * @author Sindre Mehus
 */
public class User {

    public static final String USERNAME_GUEST = "guest";

    private final String username;
    private String password;
    private String email;
    private boolean ldapAuthenticated;
    private long bytesStreamed;
    private long bytesDownloaded;
    private long bytesUploaded;

    private boolean adminRole;
    private boolean settingsRole;
    private boolean downloadRole;
    private boolean uploadRole;
    private boolean playlistRole;
    private boolean coverArtRole;
    private boolean commentRole;
    private boolean podcastRole;
    private boolean streamRole;
    private boolean jukeboxRole;
    private boolean shareRole;

    public User(String username, String password, String email, boolean ldapAuthenticated, long bytesStreamed,
            long bytesDownloaded, long bytesUploaded) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.ldapAuthenticated = ldapAuthenticated;
        this.bytesStreamed = bytesStreamed;
        this.bytesDownloaded = bytesDownloaded;
        this.bytesUploaded = bytesUploaded;
    }

    public User(String username, String password, String email) {
        this(username, password, email, false, 0, 0, 0);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isLdapAuthenticated() {
        return ldapAuthenticated;
    }

    public void setLdapAuthenticated(boolean ldapAuthenticated) {
        this.ldapAuthenticated = ldapAuthenticated;
    }

    public long getBytesStreamed() {
        return bytesStreamed;
    }

    public void setBytesStreamed(long bytesStreamed) {
        this.bytesStreamed = bytesStreamed;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public long getBytesUploaded() {
        return bytesUploaded;
    }

    public void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public boolean isAdminRole() {
        return adminRole;
    }

    public void setAdminRole(boolean isAdminRole) {
        this.adminRole = isAdminRole;
    }

    public boolean isSettingsRole() {
        return adminRole || settingsRole;
    }

    public void setSettingsRole(boolean isSettingsRole) {
        this.settingsRole = isSettingsRole;
    }

    public boolean isCommentRole() {
        return commentRole;
    }

    public void setCommentRole(boolean isCommentRole) {
        this.commentRole = isCommentRole;
    }

    public boolean isDownloadRole() {
        return downloadRole;
    }

    public void setDownloadRole(boolean isDownloadRole) {
        this.downloadRole = isDownloadRole;
    }

    public boolean isUploadRole() {
        return uploadRole;
    }

    public void setUploadRole(boolean isUploadRole) {
        this.uploadRole = isUploadRole;
    }

    public boolean isPlaylistRole() {
        return playlistRole;
    }

    public void setPlaylistRole(boolean isPlaylistRole) {
        this.playlistRole = isPlaylistRole;
    }

    public boolean isCoverArtRole() {
        return coverArtRole;
    }

    public void setCoverArtRole(boolean isCoverArtRole) {
        this.coverArtRole = isCoverArtRole;
    }

    public boolean isPodcastRole() {
        return podcastRole;
    }

    public void setPodcastRole(boolean isPodcastRole) {
        this.podcastRole = isPodcastRole;
    }

    public boolean isStreamRole() {
        return streamRole;
    }

    public void setStreamRole(boolean streamRole) {
        this.streamRole = streamRole;
    }

    public boolean isJukeboxRole() {
        return jukeboxRole;
    }

    public void setJukeboxRole(boolean jukeboxRole) {
        this.jukeboxRole = jukeboxRole;
    }

    public boolean isShareRole() {
        return shareRole;
    }

    public void setShareRole(boolean shareRole) {
        this.shareRole = shareRole;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(username);

        if (adminRole) {
            result.append(" [admin]");
        }
        if (settingsRole) {
            result.append(" [settings]");
        }
        if (downloadRole) {
            result.append(" [download]");
        }
        if (uploadRole) {
            result.append(" [upload]");
        }
        if (playlistRole) {
            result.append(" [playlist]");
        }
        if (coverArtRole) {
            result.append(" [coverart]");
        }
        if (commentRole) {
            result.append(" [comment]");
        }
        if (podcastRole) {
            result.append(" [podcast]");
        }
        if (streamRole) {
            result.append(" [stream]");
        }
        if (jukeboxRole) {
            result.append(" [jukebox]");
        }
        if (shareRole) {
            result.append(" [share]");
        }

        return result.toString();
    }
}
