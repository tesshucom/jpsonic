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

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller which receives uploaded avatar images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/avatarUpload", "/avatarUpload.view" })
public class AvatarUploadController {

    private static final Logger LOG = LoggerFactory.getLogger(AvatarUploadController.class);

    private final SecurityService securityService;
    private final AvatarService avatarService;

    public AvatarUploadController(SecurityService securityService, AvatarService avatarService) {
        super();
        this.securityService = securityService;
        this.avatarService = avatarService;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (Exception) Not reusable
    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws FileUploadException {

        // Check that we have a file upload request.
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IllegalArgumentException("Illegal request.");
        }

        String username = securityService.getCurrentUsername(request);

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = upload.parseRequest(request);

        Map<String, Object> map = LegacyMap.of();
        // Look for file items.
        for (FileItem item : items) {
            if (!item.isFormField()) {
                createAvatar(item, username, map);
                break;
            }
        }
        map.put("username", username);
        map.put("avatar", avatarService.getCustomAvatar(username));
        return new ModelAndView("avatarUploadResult", "model", map);
    }

    private void createAvatar(FileItem fileItem, String username, Map<String, Object> map) {
        if (StringUtils.isNotBlank(fileItem.getName()) && fileItem.getSize() > 0) {
            try {
                boolean resized = avatarService.createAvatar(fileItem.getFieldName(), fileItem.getInputStream(),
                        fileItem.getSize(), username);
                map.put("resized", resized);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to upload personal image: " + e, e);
                }
                map.put("error", e);
            }
        } else {
            map.put("error", new Exception("Missing file."));
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to upload personal image. No file specified.");
            }
        }
    }
}
