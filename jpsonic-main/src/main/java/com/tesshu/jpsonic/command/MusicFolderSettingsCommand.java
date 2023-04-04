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

import java.nio.file.Path;
import java.util.List;

import com.tesshu.jpsonic.controller.MusicFolderSettingsController;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;

/**
 * Command used in {@link MusicFolderSettingsController}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderSettingsCommand extends SettingsPageCommons {

    // Specify folder
    private List<MusicFolderInfo> musicFolders;
    private MusicFolderInfo newMusicFolder;

    // Run a scan
    private ScanEventType lastScanEventType;
    private boolean ignoreFileTimestamps;
    private String interval;
    private String hour;
    private boolean useCleanUp;

    // Exclusion settings
    private String excludePatternString;
    private boolean ignoreSymLinks;

    // for view page control
    private boolean useRefresh;
    private boolean cancel;

    public List<MusicFolderInfo> getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(List<MusicFolderInfo> musicFolders) {
        this.musicFolders = musicFolders;
    }

    public MusicFolderInfo getNewMusicFolder() {
        return newMusicFolder;
    }

    public void setNewMusicFolder(MusicFolderInfo newMusicFolder) {
        this.newMusicFolder = newMusicFolder;
    }

    public ScanEventType getLastScanEventType() {
        return lastScanEventType;
    }

    public void setLastScanEventType(ScanEventType lastScanEventType) {
        this.lastScanEventType = lastScanEventType;
    }

    public boolean isIgnoreFileTimestamps() {
        return ignoreFileTimestamps;
    }

    public void setIgnoreFileTimestamps(boolean ignoreFileTimes) {
        this.ignoreFileTimestamps = ignoreFileTimes;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public boolean isUseCleanUp() {
        return useCleanUp;
    }

    public void setUseCleanUp(boolean useCleanUp) {
        this.useCleanUp = useCleanUp;
    }

    public String getExcludePatternString() {
        return excludePatternString;
    }

    public void setExcludePatternString(String excludePatternString) {
        this.excludePatternString = excludePatternString;
    }

    public boolean isIgnoreSymLinks() {
        return ignoreSymLinks;
    }

    public void setIgnoreSymLinks(boolean ignoreSymLinks) {
        this.ignoreSymLinks = ignoreSymLinks;
    }

    public boolean isUseRefresh() {
        return useRefresh;
    }

    public void setUseRefresh(boolean useRefresh) {
        this.useRefresh = useRefresh;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public static class MusicFolderInfo {

        private Integer id;
        private String path;
        private String name;
        private boolean enabled;
        private Integer folderOrder;
        private boolean delete;
        private boolean existing;

        public MusicFolderInfo(MusicFolder musicFolder) {
            id = musicFolder.getId();
            name = musicFolder.getName();
            enabled = musicFolder.isEnabled();
            Path folderPath = musicFolder.toPath();
            path = folderPath.toString();
            existing = true;
        }

        public MusicFolderInfo() {
            enabled = true;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getFolderOrder() {
            return folderOrder;
        }

        public void setFolderOrder(Integer folderOrder) {
            this.folderOrder = folderOrder;
        }

        public boolean isDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }

        public boolean isExisting() {
            return existing;
        }

        public void setExisting(boolean existing) {
            this.existing = existing;
        }
    }
}
