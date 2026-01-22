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

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import org.springframework.stereotype.Component;

@Component
public class UpnpProcessorUtil {

    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final JpsonicComparators comparators;

    public UpnpProcessorUtil(MusicFolderService musicFolderService, SecurityService securityService,
            SettingsService settingsService, JpsonicComparators comparators) {
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.comparators = comparators;
    }

    public List<MusicFolder> getGuestFolders() {
        return musicFolderService
            .getMusicFoldersForUser(securityService.getGuestUser().getUsername());
    }

    public boolean isSortAlbumsByYear(String artist) {
        return comparators.isSortAlbumsByYear(artist);
    }

    public UPnPSearchMethod getUPnPSearchMethod() {
        return UPnPSearchMethod.of(settingsService.getUPnPSearchMethod());
    }
}
