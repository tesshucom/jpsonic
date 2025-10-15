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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.tesshu.jpsonic.domain.ArtistBio;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
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

    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final MediaFileService mediaFileService;
    private final LastFmService lastFmService;
    private final AirsonicLocaleResolver airsonicLocaleResolver;
    private final AjaxHelper ajaxHelper;

    public MultiService(MusicFolderService musicFolderService, SecurityService securityService,
            MediaFileService mediaFileService, LastFmService lastFmService,
            AirsonicLocaleResolver airsonicLocaleResolver, AjaxHelper ajaxHelper) {
        super();
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.lastFmService = lastFmService;
        this.airsonicLocaleResolver = airsonicLocaleResolver;
        this.ajaxHelper = ajaxHelper;
    }

    public ArtistInfo getArtistInfo(int mediaFileId, int maxSimilarArtists, int maxTopSongs) {
        HttpServletRequest request = ajaxHelper.getHttpServletRequest();

        User user = securityService.getCurrentUserStrict(request);
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        Locale locale = userSettings.isForceBio2Eng() ? Locale.ENGLISH
                : airsonicLocaleResolver.resolveLocale(request);

        MediaFile mediaFile = mediaFileService.getMediaFileStrict(mediaFileId);
        ArtistBio artistBio = lastFmService.getArtistBio(mediaFile, locale);
        List<TopSong> topSongs = getTopSongs(mediaFile, maxTopSongs);

        List<SimilarArtist> similarArtists = getSimilarArtists(mediaFileId, maxSimilarArtists);

        return new ArtistInfo(similarArtists, artistBio, topSongs);
    }

    private List<TopSong> getTopSongs(MediaFile mediaFile, int limit) {
        HttpServletRequest request = ajaxHelper.getHttpServletRequest();
        String username = securityService.getCurrentUsernameStrict(request);
        List<MusicFolder> musicFolders = musicFolderService.getMusicFoldersForUser(username);

        List<TopSong> result = new ArrayList<>();
        List<MediaFile> files = lastFmService.getTopSongs(mediaFile, limit, musicFolders);
        mediaFileService.populateStarredDate(files, username);
        for (MediaFile file : files) {
            result
                .add(new TopSong(file.getId(), file.getTitle(), file.getArtist(),
                        file.getAlbumName(), file.getDurationString(),
                        file.getStarredDate() != null));
        }
        return result;
    }

    private List<SimilarArtist> getSimilarArtists(int mediaFileId, int limit) {
        HttpServletRequest request = ajaxHelper.getHttpServletRequest();
        String username = securityService.getCurrentUsernameStrict(request);
        List<MusicFolder> musicFolders = musicFolderService.getMusicFoldersForUser(username);

        MediaFile artist = mediaFileService.getMediaFile(mediaFileId);
        List<MediaFile> similarArtists = lastFmService
            .getSimilarArtists(artist, limit, false, musicFolders);
        SimilarArtist[] result = new SimilarArtist[similarArtists.size()];
        for (int i = 0; i < result.length; i++) {
            MediaFile similarArtist = similarArtists.get(i);
            result[i] = new SimilarArtist(similarArtist.getId(), similarArtist.getName());
        }
        return Arrays.asList(result);
    }

    public void setCloseDrawer(boolean b) {
        HttpServletRequest request = ajaxHelper.getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = securityService.getUserSettings(username);
        userSettings.setCloseDrawer(b);
        userSettings.setChanged(now());
        securityService.updateUserSettings(userSettings);
    }
}
