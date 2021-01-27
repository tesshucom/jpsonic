/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */

package org.airsonic.player.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.command.SearchCommand;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.IndexType;
import org.airsonic.player.service.search.SearchCriteria;
import org.airsonic.player.service.search.SearchCriteriaDirector;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private SearchCriteriaDirector director;

    @GetMapping
    protected String displayForm() {
        return "search";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        model.addAttribute(Attributes.Model.Command.VALUE, new SearchCommand());
    }

    @PostMapping
    protected String onSubmit(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute(Attributes.Model.Command.VALUE) SearchCommand command, Model model) throws Exception {

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
