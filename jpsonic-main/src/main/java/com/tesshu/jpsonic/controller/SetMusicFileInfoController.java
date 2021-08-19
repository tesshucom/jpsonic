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

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for updating music file metadata.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/setMusicFileInfo", "/setMusicFileInfo.view" })
public class SetMusicFileInfoController {

    private final MediaFileService mediaFileService;

    public SetMusicFileInfoController(MediaFileService mediaFileService) {
        super();
        this.mediaFileService = mediaFileService;
    }

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws ServletRequestBindingException {
        int id = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        String action = request.getParameter(Attributes.Request.ACTION.value());

        MediaFile mediaFile = mediaFileService.getMediaFile(id);

        if ("comment".equals(action)) {
            mediaFile
                    .setComment(StringEscapeUtils.escapeHtml(request.getParameter(Attributes.Request.COMMENT.value())));
            mediaFileService.updateMediaFile(mediaFile);
        } else if ("resetLastScanned".equals(action)) {
            mediaFileService.resetLastScanned(mediaFile);
        }

        String url = ViewName.MAIN.value() + "?" + Attributes.Request.ID.value() + "=" + id;
        return new ModelAndView(new RedirectView(url));
    }
}
