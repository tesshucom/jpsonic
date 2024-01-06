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

import java.util.Locale;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.command.GettingStartedCommand;
import com.tesshu.jpsonic.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping({ "/gettingStarted", "/gettingStarted.view" })
public class GettingStartedController {

    private final SettingsService settingsService;

    public GettingStartedController(SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        GettingStartedCommand command = new GettingStartedCommand();
        settingsService.getAvailableLocales().stream().filter(locale -> locale.equals(settingsService.getLocale()))
                .findFirst().ifPresent(locale -> command
                        .setLocaleIndex(String.valueOf(settingsService.getAvailableLocales().indexOf(locale))));
        command.setLocales(settingsService.getAvailableLocales().stream().map(locale -> locale.getDisplayName())
                .collect(Collectors.toList()));
        model.addAttribute(Attributes.Model.Command.VALUE, command);
        model.addAttribute("runningAsRoot", "root".equals(System.getProperty("user.name")));
    }

    @GetMapping
    public ModelAndView get(HttpServletRequest request) {
        if (request.getParameter(Attributes.Request.HIDE.value()) != null) {
            settingsService.setGettingStartedEnabled(false);
            settingsService.save();
            return new ModelAndView(new RedirectView(ViewName.HOME.value()));
        }
        return new ModelAndView("gettingStarted");
    }

    @PostMapping
    protected ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) GettingStartedCommand command,
            RedirectAttributes redirectAttributes) {
        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = settingsService.getAvailableLocales().get(localeIndex);
        boolean isReload = !settingsService.getLocale().equals(locale);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), isReload);
        settingsService.setLocale(locale);
        settingsService.save();
        return new ModelAndView(new RedirectView(ViewName.GETTING_STARTED.value()));
    }
}
