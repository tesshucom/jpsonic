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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.command;

public class DLNASettingsCommand extends SettingsPageCommons {

    // UPnP basic settings
    private boolean dlnaEnabled;
    private String dlnaServerName;
    private String dlnaBaseLANURL;

    // Items to display
    private boolean dlnaIndexVisible;
    private boolean dlnaIndexId3Visible;
    private boolean dlnaFolderVisible;
    private boolean dlnaArtistVisible;
    private boolean dlnaArtistByFolderVisible;
    private boolean dlnaAlbumVisible;
    private boolean dlnaPlaylistVisible;
    private boolean dlnaAlbumByGenreVisible;
    private boolean dlnaSongByGenreVisible;
    private boolean dlnaRecentAlbumVisible;
    private boolean dlnaRecentAlbumId3Visible;
    private boolean dlnaRandomSongVisible;
    private boolean dlnaRandomAlbumVisible;
    private boolean dlnaRandomSongByArtistVisible;
    private boolean dlnaRandomSongByFolderArtistVisible;
    private boolean dlnaPodcastVisible;

    // Display options / Access control
    private boolean dlnaGenreCountVisible;
    private int dlnaRandomMax;
    private boolean dlnaGuestPublish;
    private boolean uriWithFileExtensions;

    public boolean isDlnaEnabled() {
        return dlnaEnabled;
    }

    public void setDlnaEnabled(boolean dlnaEnabled) {
        this.dlnaEnabled = dlnaEnabled;
    }

    public String getDlnaServerName() {
        return dlnaServerName;
    }

    public void setDlnaServerName(String dlnaServerName) {
        this.dlnaServerName = dlnaServerName;
    }

    public String getDlnaBaseLANURL() {
        return dlnaBaseLANURL;
    }

    public void setDlnaBaseLANURL(String dlnaBaseLANURL) {
        this.dlnaBaseLANURL = dlnaBaseLANURL;
    }

    public boolean isDlnaIndexVisible() {
        return dlnaIndexVisible;
    }

    public void setDlnaIndexVisible(boolean dlnaIndexVisible) {
        this.dlnaIndexVisible = dlnaIndexVisible;
    }

    public boolean isDlnaIndexId3Visible() {
        return dlnaIndexId3Visible;
    }

    public void setDlnaIndexId3Visible(boolean dlnaIndexId3Visible) {
        this.dlnaIndexId3Visible = dlnaIndexId3Visible;
    }

    public boolean isDlnaFolderVisible() {
        return dlnaFolderVisible;
    }

    public void setDlnaFolderVisible(boolean dlnaFolderVisible) {
        this.dlnaFolderVisible = dlnaFolderVisible;
    }

    public boolean isDlnaArtistVisible() {
        return dlnaArtistVisible;
    }

    public void setDlnaArtistVisible(boolean dlnaArtistVisible) {
        this.dlnaArtistVisible = dlnaArtistVisible;
    }

    public boolean isDlnaArtistByFolderVisible() {
        return dlnaArtistByFolderVisible;
    }

    public void setDlnaArtistByFolderVisible(boolean dlnaArtistByFolderVisible) {
        this.dlnaArtistByFolderVisible = dlnaArtistByFolderVisible;
    }

    public boolean isDlnaAlbumVisible() {
        return dlnaAlbumVisible;
    }

    public void setDlnaAlbumVisible(boolean dlnaAlbumVisible) {
        this.dlnaAlbumVisible = dlnaAlbumVisible;
    }

    public boolean isDlnaPlaylistVisible() {
        return dlnaPlaylistVisible;
    }

    public void setDlnaPlaylistVisible(boolean dlnaPlaylistVisible) {
        this.dlnaPlaylistVisible = dlnaPlaylistVisible;
    }

    public boolean isDlnaAlbumByGenreVisible() {
        return dlnaAlbumByGenreVisible;
    }

    public void setDlnaAlbumByGenreVisible(boolean dlnaAlbumByGenreVisible) {
        this.dlnaAlbumByGenreVisible = dlnaAlbumByGenreVisible;
    }

    public boolean isDlnaSongByGenreVisible() {
        return dlnaSongByGenreVisible;
    }

    public void setDlnaSongByGenreVisible(boolean dlnaSongByGenreVisible) {
        this.dlnaSongByGenreVisible = dlnaSongByGenreVisible;
    }

    public boolean isDlnaRecentAlbumVisible() {
        return dlnaRecentAlbumVisible;
    }

    public void setDlnaRecentAlbumVisible(boolean dlnaRecentAlbumVisible) {
        this.dlnaRecentAlbumVisible = dlnaRecentAlbumVisible;
    }

    public boolean isDlnaRecentAlbumId3Visible() {
        return dlnaRecentAlbumId3Visible;
    }

    public void setDlnaRecentAlbumId3Visible(boolean dlnaRecentAlbumId3Visible) {
        this.dlnaRecentAlbumId3Visible = dlnaRecentAlbumId3Visible;
    }

    public boolean isDlnaRandomSongVisible() {
        return dlnaRandomSongVisible;
    }

    public void setDlnaRandomSongVisible(boolean dlnaRandomSongVisible) {
        this.dlnaRandomSongVisible = dlnaRandomSongVisible;
    }

    public boolean isDlnaRandomAlbumVisible() {
        return dlnaRandomAlbumVisible;
    }

    public void setDlnaRandomAlbumVisible(boolean dlnaRandomAlbumVisible) {
        this.dlnaRandomAlbumVisible = dlnaRandomAlbumVisible;
    }

    public boolean isDlnaRandomSongByArtistVisible() {
        return dlnaRandomSongByArtistVisible;
    }

    public void setDlnaRandomSongByArtistVisible(boolean dlnaRandomSongByArtistVisible) {
        this.dlnaRandomSongByArtistVisible = dlnaRandomSongByArtistVisible;
    }

    public boolean isDlnaRandomSongByFolderArtistVisible() {
        return dlnaRandomSongByFolderArtistVisible;
    }

    public void setDlnaRandomSongByFolderArtistVisible(boolean dlnaRandomSongByFolderArtistVisible) {
        this.dlnaRandomSongByFolderArtistVisible = dlnaRandomSongByFolderArtistVisible;
    }

    public boolean isDlnaPodcastVisible() {
        return dlnaPodcastVisible;
    }

    public void setDlnaPodcastVisible(boolean dlnaPodcastVisible) {
        this.dlnaPodcastVisible = dlnaPodcastVisible;
    }

    public boolean isDlnaGenreCountVisible() {
        return dlnaGenreCountVisible;
    }

    public void setDlnaGenreCountVisible(boolean dlnaGenreCountVisible) {
        this.dlnaGenreCountVisible = dlnaGenreCountVisible;
    }

    public int getDlnaRandomMax() {
        return dlnaRandomMax;
    }

    public void setDlnaRandomMax(int dlnaRandomMax) {
        this.dlnaRandomMax = dlnaRandomMax;
    }

    public boolean isDlnaGuestPublish() {
        return dlnaGuestPublish;
    }

    public void setDlnaGuestPublish(boolean dlnaGuestPublish) {
        this.dlnaGuestPublish = dlnaGuestPublish;
    }

    public boolean isUriWithFileExtensions() {
        return uriWithFileExtensions;
    }

    public void setUriWithFileExtensions(boolean uriWithFileExtensions) {
        this.uriWithFileExtensions = uriWithFileExtensions;
    }
}
