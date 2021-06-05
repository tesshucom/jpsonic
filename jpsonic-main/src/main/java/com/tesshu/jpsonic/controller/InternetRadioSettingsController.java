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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.InternetRadio;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the set of internet radio/tv stations.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/internetRadioSettings")
public class InternetRadioSettingsController {

    private final SettingsService settingsService;

    public InternetRadioSettingsController(SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
    }

    @GetMapping
    public String doGet(Model model, @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast) {
        Map<String, Object> map = LegacyMap.of();
        map.put("internetRadios", settingsService.getAllInternetRadios(true));
        map.put("useRadio", settingsService.isUseRadio());
        map.put("useSonos", settingsService.isUseSonos());
        toast.ifPresent(b -> map.put("showToast", b));
        model.addAttribute("model", map);
        return "internetRadioSettings";
    }

    @PostMapping
    public ModelAndView doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {

        String error = handleParameters(request);
        if (error == null) {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), true);
        }
        redirectAttributes.addFlashAttribute(Attributes.Redirect.ERROR.value(), error);
        return new ModelAndView(new RedirectView(ViewName.INTERNET_RADIO_SETTINGS.value()));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (InternetRadio) Not reusable
    private String handleParameters(HttpServletRequest request) {
        List<InternetRadio> radios = settingsService.getAllInternetRadios(true);
        Date current = new Date();
        for (InternetRadio radio : radios) {
            Integer id = radio.getId();
            String streamUrl = getParameter(request, Attributes.Request.STREAM_URL.value(), id);
            String homepageUrl = getParameter(request, Attributes.Request.HOMEPAGE_URL.value(), id);
            String name = getParameter(request, Attributes.Request.NAME.value(), id);
            boolean enabled = getParameter(request, Attributes.Request.ENABLED.value(), id) != null;
            boolean delete = getParameter(request, Attributes.Request.DELETE.value(), id) != null;

            if (delete) {
                settingsService.deleteInternetRadio(id);
            } else {
                if (name == null) {
                    return "internetradiosettings.noname";
                }
                if (streamUrl == null) {
                    return "internetradiosettings.nourl";
                }
                settingsService
                        .updateInternetRadio(new InternetRadio(id, name, streamUrl, homepageUrl, enabled, current));
            }
        }

        String name = StringUtils.trimToNull(request.getParameter(Attributes.Request.NAME.value()));
        String streamUrl = StringUtils.trimToNull(request.getParameter(Attributes.Request.STREAM_URL.value()));
        String homepageUrl = StringUtils.trimToNull(request.getParameter(Attributes.Request.HOMEPAGE_URL.value()));
        boolean enabled = StringUtils.trimToNull(request.getParameter(Attributes.Request.ENABLED.value())) != null;

        if (name != null || streamUrl != null || homepageUrl != null) {
            if (name == null) {
                return "internetradiosettings.noname";
            }
            if (streamUrl == null) {
                return "internetradiosettings.nourl";
            }
            settingsService.createInternetRadio(new InternetRadio(name, streamUrl, homepageUrl, enabled, new Date()));
        }

        return null;
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

}
