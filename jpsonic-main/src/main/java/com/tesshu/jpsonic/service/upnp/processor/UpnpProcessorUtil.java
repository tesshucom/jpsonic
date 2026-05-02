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

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.UserService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import com.tesshu.jpsonic.service.upnp.UPnPSKeys;
import org.springframework.stereotype.Component;

@Component
public class UpnpProcessorUtil {

    private final MusicFolderService musicFolderService;
    private final UserService userService;
    private final SettingsFacade settingsFacade;
    private final JpsonicComparators comparators;

    public UpnpProcessorUtil(MusicFolderService musicFolderService, UserService userService,
            SettingsFacade settingsFacade, JpsonicComparators comparators) {
        this.musicFolderService = musicFolderService;
        this.userService = userService;
        this.settingsFacade = settingsFacade;
        this.comparators = comparators;
    }

    public List<MusicFolder> getGuestFolders() {
        return musicFolderService.getMusicFoldersForUser(userService.getGuestUser().getUsername());
    }

    public boolean isSortAlbumsByYear(String artist) {
        return comparators.isSortAlbumsByYear(artist);
    }

    public UPnPSearchMethod getUPnPSearchMethod() {
        return UPnPSearchMethod.of(settingsFacade.get(UPnPSKeys.search.upnpSearchMethod));
    }
}
