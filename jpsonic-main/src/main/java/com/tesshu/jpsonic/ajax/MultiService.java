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

package com.tesshu.jpsonic.ajax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.ArtistBio;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.directwebremoting.WebContextFactory;
import org.springframework.stereotype.Service;

/**
 * Provides miscellaneous AJAX-enabled services.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxMultiService")
public class MultiService {

    private final MediaFileService mediaFileService;
    private final LastFmService lastFmService;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final AirsonicLocaleResolver airsonicLocaleResolver;

    public MultiService(MediaFileService mediaFileService, LastFmService lastFmService, SecurityService securityService,
            SettingsService settingsService, AirsonicLocaleResolver airsonicLocaleResolver) {
        super();
        this.mediaFileService = mediaFileService;
        this.lastFmService = lastFmService;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.airsonicLocaleResolver = airsonicLocaleResolver;
    }

    public ArtistInfo getArtistInfo(int mediaFileId, int maxSimilarArtists, int maxTopSongs) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();

        MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
        List<SimilarArtist> similarArtists = getSimilarArtists(mediaFileId, maxSimilarArtists);

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        Locale locale = userSettings.isForceBio2Eng() ? Locale.ENGLISH : airsonicLocaleResolver.resolveLocale(request);
        ArtistBio artistBio = lastFmService.getArtistBio(mediaFile, locale);
        List<TopSong> topSongs = getTopSongs(mediaFile, maxTopSongs);

        return new ArtistInfo(similarArtists, artistBio, topSongs);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (TopSong) Not reusable
    private List<TopSong> getTopSongs(MediaFile mediaFile, int limit) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        List<TopSong> result = new ArrayList<>();
        List<MediaFile> files = lastFmService.getTopSongs(mediaFile, limit, musicFolders);
        mediaFileService.populateStarredDate(files, username);
        for (MediaFile file : files) {
            result.add(new TopSong(file.getId(), file.getTitle(), file.getArtist(), file.getAlbumName(),
                    file.getDurationString(), file.getStarredDate() != null));
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (SimilarArtist) Not reusable
    private List<SimilarArtist> getSimilarArtists(int mediaFileId, int limit) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        MediaFile artist = mediaFileService.getMediaFile(mediaFileId);
        List<MediaFile> similarArtists = lastFmService.getSimilarArtists(artist, limit, false, musicFolders);
        SimilarArtist[] result = new SimilarArtist[similarArtists.size()];
        for (int i = 0; i < result.length; i++) {
            MediaFile similarArtist = similarArtists.get(i);
            result[i] = new SimilarArtist(similarArtist.getId(), similarArtist.getName());
        }
        return Arrays.asList(result);
    }

    public void setCloseDrawer(boolean b) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);
        userSettings.setCloseDrawer(b);
        userSettings.setChanged(new Date());
        settingsService.updateUserSettings(userSettings);
    }
}
