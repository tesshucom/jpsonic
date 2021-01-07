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
package org.airsonic.player.command;

import org.airsonic.player.controller.UserSettingsController;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;

import java.util.List;

/**
 * Command used in {@link UserSettingsController}.
 *
 * @author Sindre Mehus
 */
public class UserSettingsCommand {
    private String username;
    private boolean adminRole;
    private boolean downloadRole;
    private boolean uploadRole;
    private boolean coverArtRole;
    private boolean commentRole;
    private boolean podcastRole;
    private boolean streamRole;
    private boolean jukeboxRole;
    private boolean settingsRole;
    private boolean shareRole;

    private List<User> users;
    private boolean admin;
    private boolean currentUser;
    private boolean passwordChange;
    private boolean newUser;
    private boolean deleteUser;
    private String password;
    private String confirmPassword;
    private String email;
    private boolean ldapAuthenticated;
    private boolean ldapEnabled;
    private List<MusicFolder> allMusicFolders;
    private int[] allowedMusicFolderIds;

    private String transcodeSchemeName;
    private EnumHolder[] transcodeSchemeHolders;
    private boolean transcodingSupported;
    private String transcodeDirectory;

    private boolean useRadio;
    private boolean useSonos;
    private boolean showToast;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdminRole() {
        return adminRole;
    }

    public void setAdminRole(boolean adminRole) {
        this.adminRole = adminRole;
    }

    public boolean isDownloadRole() {
        return downloadRole;
    }

    public void setDownloadRole(boolean downloadRole) {
        this.downloadRole = downloadRole;
    }

    public boolean isUploadRole() {
        return uploadRole;
    }

    public void setUploadRole(boolean uploadRole) {
        this.uploadRole = uploadRole;
    }

    public boolean isCoverArtRole() {
        return coverArtRole;
    }

    public void setCoverArtRole(boolean coverArtRole) {
        this.coverArtRole = coverArtRole;
    }

    public boolean isCommentRole() {
        return commentRole;
    }

    public void setCommentRole(boolean commentRole) {
        this.commentRole = commentRole;
    }

    public boolean isPodcastRole() {
        return podcastRole;
    }

    public void setPodcastRole(boolean podcastRole) {
        this.podcastRole = podcastRole;
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

    public boolean isSettingsRole() {
        return settingsRole;
    }

    public void setSettingsRole(boolean settingsRole) {
        this.settingsRole = settingsRole;
    }

    public boolean isShareRole() {
        return shareRole;
    }

    public void setShareRole(boolean shareRole) {
        this.shareRole = shareRole;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isPasswordChange() {
        return passwordChange;
    }

    public void setPasswordChange(boolean passwordChange) {
        this.passwordChange = passwordChange;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public void setNewUser(boolean isNewUser) {
        this.newUser = isNewUser;
    }

    public boolean isDeleteUser() {
        return deleteUser;
    }

    public void setDeleteUser(boolean deleteUser) {
        this.deleteUser = deleteUser;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
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

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        this.ldapEnabled = ldapEnabled;
    }

    public List<MusicFolder> getAllMusicFolders() {
        return allMusicFolders;
    }

    public void setAllMusicFolders(List<MusicFolder> allMusicFolders) {
        this.allMusicFolders = allMusicFolders;
    }

    public int[] getAllowedMusicFolderIds() {
        return allowedMusicFolderIds;
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly") // reference
    public void setAllowedMusicFolderIds(int... allowedMusicFolderIds) {
        this.allowedMusicFolderIds = allowedMusicFolderIds;
    }

    public String getTranscodeSchemeName() {
        return transcodeSchemeName;
    }

    public void setTranscodeSchemeName(String transcodeSchemeName) {
        this.transcodeSchemeName = transcodeSchemeName;
    }

    public EnumHolder[] getTranscodeSchemeHolders() {
        return transcodeSchemeHolders;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void setTranscodeSchemes(TranscodeScheme... transcodeSchemes) {
        transcodeSchemeHolders = new EnumHolder[transcodeSchemes.length];
        for (int i = 0; i < transcodeSchemes.length; i++) {
            TranscodeScheme scheme = transcodeSchemes[i];
            transcodeSchemeHolders[i] = new EnumHolder(scheme.name(), scheme.toString());
        }
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getTranscodeDirectory() {
        return transcodeDirectory;
    }

    public void setTranscodeDirectory(String transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
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

    public boolean isShowToast() {
        return showToast;
    }

    public void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    public void setUser(User user) {
        username = user == null ? null : user.getUsername();
        adminRole = user != null && user.isAdminRole();
        downloadRole = user != null && user.isDownloadRole();
        uploadRole = user != null && user.isUploadRole();
        coverArtRole = user != null && user.isCoverArtRole();
        commentRole = user != null && user.isCommentRole();
        podcastRole = user != null && user.isPodcastRole();
        streamRole = user != null && user.isStreamRole();
        jukeboxRole = user != null && user.isJukeboxRole();
        settingsRole = user != null && user.isSettingsRole();
        shareRole = user != null && user.isShareRole();
        ldapAuthenticated = user != null && user.isLdapAuthenticated();
        newUser = false;
    }

}