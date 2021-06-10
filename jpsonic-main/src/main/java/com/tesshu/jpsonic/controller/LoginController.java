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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import com.tesshu.jpsonic.util.StringUtil;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private final SecurityService securityService;
    private final SettingsService settingsService;

    public LoginController(SecurityService securityService, SettingsService settingsService) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response) {

        // Auto-login if "user" and "password" parameters are given.
        String username = request.getParameter(Attributes.Request.USER.value());
        String password = request.getParameter(Attributes.Request.PASSWORD.value());
        if (username != null && password != null) {
            username = StringUtil.urlEncode(username);
            password = StringUtil.urlEncode(password);
            return new ModelAndView(new RedirectView("/login?"
                    + UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY + "=" + username + "&"
                    + UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY + "=" + password));
        }

        Map<String, Object> map = LegacyMap.of("logout",
                request.getParameter(Attributes.Request.LOGOUT.value()) != null, "error",
                request.getParameter(Attributes.Request.ERROR.value()) != null, "brand", settingsService.getBrand(),
                "loginMessage", settingsService.getLoginMessage(), "showRememberMe",
                settingsService.isShowRememberMe());

        User admin = securityService.getUserByName("admin");
        if (admin != null && admin.getUsername().equals(admin.getPassword())) {
            map.put("insecure", true);
        }

        return new ModelAndView("login", "model", map);
    }
}
