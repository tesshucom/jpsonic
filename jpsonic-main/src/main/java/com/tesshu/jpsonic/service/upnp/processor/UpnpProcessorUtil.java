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

import java.net.URI;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.seamless.util.MimeType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UpnpProcessorUtil {

    private static final Object LOCK = new Object();
    private static final AtomicBoolean RESOURCE_LOADED = new AtomicBoolean();

    private static ResourceBundle resourceBundle;

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final JWTSecurityService jwtSecurityService;
    private final TranscodingService transcodingService;
    private final JpsonicComparators comparators;

    public UpnpProcessorUtil(SettingsService ss, MusicFolderService mfs, SecurityService securityService,
            JpsonicComparators c, JWTSecurityService jwt, TranscodingService ts) {
        settingsService = ss;
        musicFolderService = mfs;
        this.securityService = securityService;
        jwtSecurityService = jwt;
        comparators = c;
        transcodingService = ts;
    }

    public UriComponentsBuilder addJWTToken(UriComponentsBuilder builder) {
        return jwtSecurityService.addJWTToken(builder);
    }

    public String createURIStringWithToken(UriComponentsBuilder builder, MediaFile song) {
        String token = addJWTToken(builder).toUriString();
        if (settingsService.isUriWithFileExtensions() && !StringUtils.isEmpty(song.getFormat())) {
            token = token.concat(".").concat(song.getFormat());
        }
        return token;
    }

    public URI createURIWithToken(UriComponentsBuilder builder) {
        return addJWTToken(builder).build().encode().toUri();
    }

    public List<MusicFolder> getAllMusicFolders() {
        return musicFolderService.getMusicFoldersForUser(securityService.getGuestUser().getUsername());
    }

    public String getBaseUrl() {
        String dlnaBaseLANURL = settingsService.getDlnaBaseLANURL();
        if (StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new IllegalArgumentException("UPnP Base LAN URL is not set correctly");
        }
        return dlnaBaseLANURL;
    }

    public MimeType getMimeType(MediaFile song, Player player) {
        String suffix = song.isVideo() ? FilenameUtils.getExtension(song.getPath())
                : transcodingService.getSuffix(player, song, null);
        String mimeTypeString = StringUtil.getMimeType(suffix);
        return mimeTypeString == null ? null : MimeType.valueOf(mimeTypeString);
    }

    public String getResource(String key) {
        if (!RESOURCE_LOADED.get()) {
            synchronized (LOCK) {
                resourceBundle = ResourceBundle.getBundle("com.tesshu.jpsonic.i18n.ResourceBundle",
                        settingsService.getLocale());
                RESOURCE_LOADED.set(true);
            }
        }
        return resourceBundle.getString(key);
    }

    public boolean isDlnaGenreCountVisible() {
        return settingsService.isDlnaGenreCountVisible();
    }

    public boolean isProhibitSortVarious() {
        return settingsService.isProhibitSortVarious();
    }

    public boolean isSortAlbumsByYear() {
        return settingsService.isSortAlbumsByYear();
    }

    public boolean isSortAlbumsByYear(String artist) {
        return comparators.isSortAlbumsByYear(artist);
    }

}
