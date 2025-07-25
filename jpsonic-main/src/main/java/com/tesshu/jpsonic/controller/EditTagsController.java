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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.metadata.MetaDataParser;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.metadata.ParserUtils;
import com.tesshu.jpsonic.util.LegacyMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the page used to edit MP3 tags.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/editTags", "/editTags.view" })
public class EditTagsController {

    private final SecurityService securityService;
    private final MetaDataParserFactory metaDataParserFactory;
    private final MediaFileService mediaFileService;
    private final ScannerStateService scannerStateService;

    public EditTagsController(SecurityService securityService,
            MetaDataParserFactory metaDataParserFactory, MediaFileService mediaFileService,
            ScannerStateService scannerStateService) {
        super();
        this.securityService = securityService;
        this.metaDataParserFactory = metaDataParserFactory;
        this.mediaFileService = mediaFileService;
        this.scannerStateService = scannerStateService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request)
            throws ServletRequestBindingException {

        int id = ServletRequestUtils
            .getRequiredIntParameter(request, Attributes.Request.ID.value());
        MediaFile dir = mediaFileService.getMediaFileStrict(id);
        List<MediaFile> files = mediaFileService.getChildrenOf(dir, true, false);

        Map<String, Object> map = LegacyMap.of();
        if (!files.isEmpty()) {
            map.put("defaultArtist", files.get(0).getArtist());
            map.put("defaultAlbum", files.get(0).getAlbumName());
            map.put("defaultYear", files.get(0).getYear());
            map.put("defaultGenre", files.get(0).getGenre());
        }
        map.put("allGenres", ParserUtils.getID3V1Genres());
        map.put("scanning", scannerStateService.isScanning());

        List<ParsedSong> parsedSongs = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            parsedSongs.add(createParsedSong(files.get(i), i));
        }
        map.put("id", id);
        map.put("songs", parsedSongs);
        map.put("ancestors", getAncestors(dir));

        String username = securityService.getCurrentUsernameStrict(request);
        UserSettings userSettings = securityService.getUserSettings(username);
        map.put("breadcrumbIndex", userSettings.isBreadcrumbIndex());
        map.put("dir", dir);
        map.put("selectedMusicFolder", securityService.getSelectedMusicFolder(username));

        return new ModelAndView("editTags", "model", map);
    }

    private List<MediaFile> getAncestors(MediaFile dir) {
        List<MediaFile> result = new ArrayList<>();
        if (securityService.isInPodcastFolder(dir.toPath())) {
            // For podcasts, don't use ancestors
            return result;
        }

        MediaFile parent = mediaFileService.getParentOf(dir);
        while (parent != null && !mediaFileService.isRoot(parent)) {
            result.add(parent);
            parent = mediaFileService.getParentOf(parent);
        }
        result.add(dir);
        return result;
    }

    private ParsedSong createParsedSong(MediaFile file, int index) {
        ParsedSong parsedSong = new ParsedSong();
        parsedSong.setId(file.getId());
        parsedSong.setFileName(FilenameUtils.getBaseName(file.getPathString()));
        parsedSong.setTrack(file.getTrackNumber());
        parsedSong.setSuggestedTrack(index + 1);
        parsedSong.setTitle(file.getTitle());
        Path path = file.toPath();
        MetaDataParser parser = metaDataParserFactory.getParser(path);
        if (parser != null) {
            parsedSong.setSuggestedTitle(parser.guessTitle(path));
        }
        parsedSong.setArtist(file.getArtist());
        parsedSong.setAlbum(file.getAlbumName());
        parsedSong.setYear(file.getYear());
        parsedSong.setGenre(file.getGenre());
        return parsedSong;
    }

    /**
     * Contains information about a single song.
     */
    public static class ParsedSong {
        private int id;
        private String fileName;
        private Integer suggestedTrack;
        private Integer track;
        private String suggestedTitle;
        private String title;
        private String artist;
        private String album;
        private Integer year;
        private String genre;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Integer getSuggestedTrack() {
            return suggestedTrack;
        }

        public void setSuggestedTrack(Integer suggestedTrack) {
            this.suggestedTrack = suggestedTrack;
        }

        public Integer getTrack() {
            return track;
        }

        public void setTrack(Integer track) {
            this.track = track;
        }

        public String getSuggestedTitle() {
            return suggestedTitle;
        }

        public void setSuggestedTitle(String suggestedTitle) {
            this.suggestedTitle = suggestedTitle;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }
    }
}
