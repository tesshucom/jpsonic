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

import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.domain.MenuItem;
import com.tesshu.jpsonic.domain.MenuItemId;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.service.MenuItemService.MenuItemWithDefaultName;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;

public class DLNASettingsCommand extends SettingsPageCommons {

    // UPnP basic settings
    private boolean dlnaEnabled;
    private String dlnaServerName;
    private String dlnaBaseLANURL;
    private List<MusicFolder> allMusicFolders;
    private int[] allowedMusicFolderIds = new int[0];
    private List<Transcoding> allTranscodings;
    private TranscodeScheme transcodeScheme;
    private int[] activeTranscodingIds = new int[0];
    private boolean transcodingSupported;
    private String dlnaDefaultFilteredIp;
    private boolean dlnaEnabledFilteredIp;
    private String dlnaFilteredIp;
    private boolean uriWithFileExtensions;

    // Menu settings
    private List<MenuItemWithDefaultName> topMenuItems;

    // Menu detail settings
    private Map<MenuItemId, Boolean> topMenuEnableds;
    private List<MenuItemWithDefaultName> subMenuItems;
    private Map<MenuItemId, SubMenuItemRowInfo> subMenuItemRowInfos;

    // Display options / Access control
    private List<Sort> avairableAlbumGenreSort;
    private Sort albumGenreSort;
    private List<Sort> avairableSongGenreSort;
    private Sort songGenreSort;
    private Integer dlnaRandomMax;
    private boolean dlnaGuestPublish;

    // Search
    private UPnPSearchMethod searchMethod;

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

    public List<MusicFolder> getAllMusicFolders() {
        return allMusicFolders;
    }

    public void setAllMusicFolders(List<MusicFolder> allMusicFolders) {
        this.allMusicFolders = allMusicFolders;
    }

    public int[] getAllowedMusicFolderIds() {
        return allowedMusicFolderIds;
    }

    public void setAllowedMusicFolderIds(int... allowedMusicFolderIds) {
        if (allowedMusicFolderIds != null) {
            this.allowedMusicFolderIds = allowedMusicFolderIds.clone();
        }
    }

    public List<Transcoding> getAllTranscodings() {
        return allTranscodings;
    }

    public void setAllTranscodings(List<Transcoding> allTranscodings) {
        this.allTranscodings = allTranscodings;
    }

    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    public int[] getActiveTranscodingIds() {
        return activeTranscodingIds;
    }

    public void setActiveTranscodingIds(int... activeTranscodingIds) {
        if (activeTranscodingIds != null) {
            this.activeTranscodingIds = activeTranscodingIds.clone();
        }
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getDlnaDefaultFilteredIp() {
        return dlnaDefaultFilteredIp;
    }

    public void setDlnaDefaultFilteredIp(String dlnaDefaultFilteredIp) {
        this.dlnaDefaultFilteredIp = dlnaDefaultFilteredIp;
    }

    public boolean isDlnaEnabledFilteredIp() {
        return dlnaEnabledFilteredIp;
    }

    public void setDlnaEnabledFilteredIp(boolean dlnaEnabledFilteredIp) {
        this.dlnaEnabledFilteredIp = dlnaEnabledFilteredIp;
    }

    public String getDlnaFilteredIp() {
        return dlnaFilteredIp;
    }

    public void setDlnaFilteredIp(String dlnaFilteredIp) {
        this.dlnaFilteredIp = dlnaFilteredIp;
    }

    public boolean isUriWithFileExtensions() {
        return uriWithFileExtensions;
    }

    public void setUriWithFileExtensions(boolean uriWithFileExtensions) {
        this.uriWithFileExtensions = uriWithFileExtensions;
    }

    public List<MenuItemWithDefaultName> getTopMenuItems() {
        return topMenuItems;
    }

    public void setTopMenuItems(List<MenuItemWithDefaultName> topMenuItems) {
        this.topMenuItems = topMenuItems;
    }

    public Map<MenuItemId, Boolean> getTopMenuEnableds() {
        return topMenuEnableds;
    }

    public void setTopMenuEnableds(Map<MenuItemId, Boolean> topMenuEnableds) {
        this.topMenuEnableds = topMenuEnableds;
    }

    public List<MenuItemWithDefaultName> getSubMenuItems() {
        return subMenuItems;
    }

    public void setSubMenuItems(List<MenuItemWithDefaultName> menuItems) {
        this.subMenuItems = menuItems;
    }

    public Map<MenuItemId, SubMenuItemRowInfo> getSubMenuItemRowInfos() {
        return subMenuItemRowInfos;
    }

    public void setSubMenuItemRowInfos(Map<MenuItemId, SubMenuItemRowInfo> subMenuItemRowInfos) {
        this.subMenuItemRowInfos = subMenuItemRowInfos;
    }

    public List<Sort> getAvairableAlbumGenreSort() {
        return avairableAlbumGenreSort;
    }

    public void setAvairableAlbumGenreSort(List<Sort> avairableAlbumGenreSort) {
        this.avairableAlbumGenreSort = avairableAlbumGenreSort;
    }

    public Sort getAlbumGenreSort() {
        return albumGenreSort;
    }

    public List<Sort> getAvairableSongGenreSort() {
        return avairableSongGenreSort;
    }

    public void setAvairableSongGenreSort(List<Sort> avairableMusicGenreSort) {
        this.avairableSongGenreSort = avairableMusicGenreSort;
    }

    public void setAlbumGenreSort(Sort albumGenreSort) {
        this.albumGenreSort = albumGenreSort;
    }

    public Sort getSongGenreSort() {
        return songGenreSort;
    }

    public void setSongGenreSort(Sort musicGenreSort) {
        this.songGenreSort = musicGenreSort;
    }

    public Integer getDlnaRandomMax() {
        return dlnaRandomMax;
    }

    public void setDlnaRandomMax(Integer dlnaRandomMax) {
        this.dlnaRandomMax = dlnaRandomMax;
    }

    public boolean isDlnaGuestPublish() {
        return dlnaGuestPublish;
    }

    public void setDlnaGuestPublish(boolean dlnaGuestPublish) {
        this.dlnaGuestPublish = dlnaGuestPublish;
    }

    public UPnPSearchMethod getSearchMethod() {
        return searchMethod;
    }

    public void setSearchMethod(UPnPSearchMethod searchMethod) {
        this.searchMethod = searchMethod;
    }

    public record SubMenuItemRowInfo(MenuItem firstChild, int count) {
    }
}
