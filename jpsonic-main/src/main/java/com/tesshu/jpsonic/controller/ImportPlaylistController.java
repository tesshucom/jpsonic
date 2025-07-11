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
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({ "/importPlaylist", "/importPlaylist.view" })
public class ImportPlaylistController {

    private static final String FIELD_NAME_FILE = "file";
    private static final long MAX_PLAYLIST_SIZE_MB = 5L;

    private final SecurityService securityService;
    private final PlaylistService playlistService;

    public ImportPlaylistController(SecurityService securityService,
            PlaylistService playlistService) {
        super();
        this.securityService = securityService;
        this.playlistService = playlistService;
    }

    private void playListSizeCheck(DiskFileItem item) throws ExecutionException {
        if (item.getSize() > MAX_PLAYLIST_SIZE_MB * 1024L * 1024L) {
            throw new ExecutionException(
                    new IOException("The playlist file is too large. Max file size is "
                            + MAX_PLAYLIST_SIZE_MB + " MB."));
        }
    }

    @SuppressFBWarnings(value = "FILE_UPLOAD_FILENAME", justification = "False positive. FilenameUtils eliminates traversal and injection")
    @PostMapping
    protected String handlePost(RedirectAttributes redirectAttributes, HttpServletRequest request) {
        Map<String, Object> map = LegacyMap.of();

        try {
            if (JakartaServletFileUpload.isMultipartContent(request)) {
                DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
                JakartaServletDiskFileUpload upload = new JakartaServletDiskFileUpload(factory);
                List<DiskFileItem> items = upload.parseRequest(request);
                for (DiskFileItem item : items) {
                    if (FIELD_NAME_FILE.equals(item.getFieldName())
                            && !StringUtils.isBlank(item.getName())) {
                        playListSizeCheck(item);
                        String playlistName = FilenameUtils.getBaseName(item.getName());
                        String fileName = FilenameUtils.getName(item.getName());
                        String username = securityService.getCurrentUsername(request);
                        Playlist playlist = playlistService
                            .importPlaylist(username, playlistName, fileName, item.getInputStream(),
                                    null);
                        map.put("playlist", playlist);
                    }
                }
            }
        } catch (IOException e) {
            map.put("Error in uploading playlist", e.getMessage());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            map.put("Error in uploading playlist", e.getMessage());
        }

        redirectAttributes.addFlashAttribute(Attributes.Redirect.MODEL.value(), map);
        return "redirect:importPlaylist";
    }

    @GetMapping
    public String get() {
        return "importPlaylist";
    }

}
