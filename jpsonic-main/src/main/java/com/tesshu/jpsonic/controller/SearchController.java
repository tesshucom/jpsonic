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

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.command.SearchCommand;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.search.SearchCriteria;
import com.tesshu.jpsonic.service.search.SearchCriteriaDirector;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the search page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    private static final int MATCH_COUNT = 25;

    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final PlayerService playerService;
    private final SearchService searchService;
    private final SearchCriteriaDirector director;

    public SearchController(SecurityService securityService, SettingsService settingsService,
            PlayerService playerService, SearchService searchService, SearchCriteriaDirector director) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.playerService = playerService;
        this.searchService = searchService;
        this.director = director;
    }

    @GetMapping
    protected String displayForm() {
        return "search";
    }

    @ModelAttribute
    protected void formBackingObject(Model model) {
        model.addAttribute(Attributes.Model.Command.VALUE, new SearchCommand());
    }

    @PostMapping
    protected String onSubmit(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute(Attributes.Model.Command.VALUE) SearchCommand command)
            throws ServletRequestBindingException, IOException {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setUser(user);
        command.setPartyModeEnabled(userSettings.isPartyModeEnabled());
        command.setComposerVisible(userSettings.getMainVisibility().isComposerVisible());
        command.setGenreVisible(userSettings.getMainVisibility().isGenreVisible());
        command.setSimpleDisplay(userSettings.isSimpleDisplay());

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        String query = StringUtils.trimToNull(command.getQuery());

        if (query != null) {

            int offset = 0;
            int count = MATCH_COUNT;
            boolean includeComposer = settingsService.isSearchComposer()
                    || userSettings.getMainVisibility().isComposerVisible();

            SearchCriteria criteria = director.construct(query, offset, count, includeComposer, musicFolders,
                    IndexType.ARTIST);
            SearchResult artists = searchService.search(criteria);
            command.setArtists(artists.getMediaFiles());

            criteria = director.construct(query, offset, count, includeComposer, musicFolders, IndexType.ALBUM);
            SearchResult albums = searchService.search(criteria);
            command.setAlbums(albums.getMediaFiles());

            criteria = director.construct(query, offset, count, includeComposer, musicFolders, IndexType.SONG);
            SearchResult songs = searchService.search(criteria);
            command.setSongs(songs.getMediaFiles());

            command.setPlayer(playerService.getPlayer(request, response));
        }

        return "search";
    }
}
