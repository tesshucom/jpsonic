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

package com.tesshu.jpsonic.controller;

import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the page used to generate the Podcast XML file.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/podcast", "/podcast.view" })
public class PodcastController {

    private static final Object DATE_LOCK = new Object();

    private final PlaylistService playlistService;
    private final SettingsService settingsService;
    private final SecurityService securityService;

    // Locale is changed by Setting, but restart is required.
    private DateFormat rssDateFormat;
    private String lang;

    public PodcastController(PlaylistService playlistService, SettingsService settingsService,
            SecurityService securityService) {
        super();
        this.playlistService = playlistService;
        this.settingsService = settingsService;
        this.securityService = securityService;
    }

    public DateFormat getRssDateFormat() {
        synchronized (DATE_LOCK) {
            if (rssDateFormat == null) {
                Locale locale = settingsService.getLocale();
                rssDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", locale);
                lang = locale.getLanguage();
            }
        }
        return rssDateFormat;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Podcast) Not reusable
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws ExecutionException {

        if (!settingsService.isPublishPodcast()) {
            throw new ExecutionException(new GeneralSecurityException("Podcast not allowed to publish."));
        }

        String url = request.getRequestURL().toString();
        String username = securityService.getCurrentUsername(request);
        List<Playlist> playlists = playlistService.getReadablePlaylistsForUser(username);
        List<Podcast> podcasts = new ArrayList<>();

        for (Playlist playlist : playlists) {

            List<MediaFile> songs = playlistService.getFilesInPlaylist(playlist.getId());
            if (songs.isEmpty()) {
                continue;
            }
            long length = 0L;
            for (MediaFile song : songs) {
                length += song.getFileSize();
            }
            String publishDate;
            synchronized (getRssDateFormat()) {
                publishDate = getRssDateFormat().format(playlist.getCreated());
            }

            // Resolve content type.
            String suffix = songs.get(0).getFormat();
            String type = StringUtil.getMimeType(suffix);

            String enclosureUrl = url + "/stream?playlist=" + playlist.getId();

            podcasts.add(new Podcast(playlist.getName(), publishDate, enclosureUrl, length, type));
        }

        return new ModelAndView("podcast", "model",
                LegacyMap.of("url", url, "lang", lang, "logo",
                        url.replaceAll("podcast/" + ViewName.PODCAST.value() + "$", "") + "/icons/logo.svg", "podcasts",
                        podcasts));
    }

    /**
     * Contains information about a single Podcast.
     */
    public static class Podcast {
        private final String name;
        private final String publishDate;
        private final String enclosureUrl;
        private final long length;
        private final String type;

        public Podcast(String name, String publishDate, String enclosureUrl, long length, String type) {
            this.name = name;
            this.publishDate = publishDate;
            this.enclosureUrl = enclosureUrl;
            this.length = length;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getPublishDate() {
            return publishDate;
        }

        public String getEnclosureUrl() {
            return enclosureUrl;
        }

        public long getLength() {
            return length;
        }

        public String getType() {
            return type;
        }
    }
}
