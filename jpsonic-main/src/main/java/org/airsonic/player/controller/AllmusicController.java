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

package org.airsonic.player.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the page which forwards to allmusic.com.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/allmusic")
public class AllmusicController {

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView result = new ModelAndView();
        result.addObject(Attributes.Request.ALBUM.value(), request.getParameter(Attributes.Request.ALBUM.value()));
        return result;
    }
}
