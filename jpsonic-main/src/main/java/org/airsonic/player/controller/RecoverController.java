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

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.controller.Attributes;
import de.triology.recaptchav2java.ReCaptcha;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring MVC Controller that serves the login page.
 */
@Controller
@RequestMapping("/recover")
public class RecoverController {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverController.class);
    private static final String SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final int PASSWORD_LENGTH = 32;
    private static final String SESSION_KEY_MAIL_PREF = "mail.";
    private static final String SESSION_VALUE_TRUE = "true";

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final SecureRandom random;

    public RecoverController(SettingsService settingsService, SecurityService securityService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.random = new SecureRandom();
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView recover(HttpServletRequest request) {

        Map<String, Object> map = LegacyMap.of();
        String usernameOrEmail = StringUtils
                .trimToNull(request.getParameter(Attributes.Request.USERNAME_OR_EMAIL.value()));

        if (usernameOrEmail != null) {

            map.put(Attributes.Request.USERNAME_OR_EMAIL.value(), usernameOrEmail);
            User user = getUserByUsernameOrEmail(usernameOrEmail);

            boolean captchaOk;
            if (settingsService.isCaptchaEnabled()) {
                String recaptchaResponse = request.getParameter(Attributes.Request.G_RECAPTCHA_RESPONSE.value());
                ReCaptcha captcha = new ReCaptcha(settingsService.getRecaptchaSecretKey());
                captchaOk = recaptchaResponse != null && captcha.isValid(recaptchaResponse);
            } else {
                captchaOk = true;
            }

            if (!captchaOk) {
                map.put(Attributes.Model.ERROR.value(), "recover.error.invalidcaptcha");
            } else if (user == null) {
                map.put(Attributes.Model.ERROR.value(), "recover.error.usernotfound");
            } else if (user.getEmail() == null) {
                map.put(Attributes.Model.ERROR.value(), "recover.error.noemail");
            } else {
                StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
                for (int i = 0; i < PASSWORD_LENGTH; i++) {
                    int index = random.nextInt(SYMBOLS.length());
                    sb.append(SYMBOLS.charAt(index));
                }
                String password = sb.toString();

                if (emailPassword(password, user.getUsername(), user.getEmail())) {
                    map.put("sentTo", user.getEmail());
                    user.setLdapAuthenticated(false);
                    user.setPassword(password);
                    securityService.updateUser(user);
                } else {
                    map.put(Attributes.Model.ERROR.value(), "recover.error.sendfailed");
                }
            }
        }

        if (settingsService.isCaptchaEnabled()) {
            map.put("recaptchaSiteKey", settingsService.getRecaptchaSiteKey());
        }

        return new ModelAndView("recover", "model", map);
    }

    private User getUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail != null) {
            User user = securityService.getUserByName(usernameOrEmail);
            if (user != null) {
                return user;
            }
            return securityService.getUserByEmail(usernameOrEmail);
        }
        return null;
    }

    /*
     * e-mail user new password via configured Smtp server
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private boolean emailPassword(String password, String username, String email) {
        /* Default to protocol smtp when SmtpEncryption is set to "None" */

        if (settingsService.getSmtpServer() == null || settingsService.getSmtpServer().isEmpty()) {
            LOG.warn("Can not send email; no Smtp server configured.");
            return false;
        }

        String prot = "smtp";
        Properties props = new Properties();
        if ("SSL/TLS".equals(settingsService.getSmtpEncryption())) {
            prot = "smtps";
            props.put(SESSION_KEY_MAIL_PREF + prot + ".ssl.enable", SESSION_VALUE_TRUE);
        } else if ("STARTTLS".equals(settingsService.getSmtpEncryption())) {
            prot = "smtp";
            props.put(SESSION_KEY_MAIL_PREF + prot + ".starttls.enable", SESSION_VALUE_TRUE);
        }
        props.put(SESSION_KEY_MAIL_PREF + prot + ".host", settingsService.getSmtpServer());
        props.put(SESSION_KEY_MAIL_PREF + prot + ".port", settingsService.getSmtpPort());
        /* use authentication when SmtpUser is configured */
        if (settingsService.getSmtpUser() != null && !settingsService.getSmtpUser().isEmpty()) {
            props.put(SESSION_KEY_MAIL_PREF + prot + ".auth", SESSION_VALUE_TRUE);
        }

        Session session = Session.getInstance(props, null);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settingsService.getSmtpFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Jpsonic Password");
            message.setText("Hi there!\n\n"
                    + "You have requested to reset your Jpsonic password.  Please find your new login details below.\n\n"
                    + "Username: " + username + "\n" + "Password: " + password + "\n\n" + "--\n"
                    + "Your Jpsonic server\n" + "tesshu.com/");
            message.setSentDate(new Date());

            try (Transport trans = session.getTransport(prot)) {
                if (props.get(SESSION_KEY_MAIL_PREF + prot + ".auth") != null
                        && props.get(SESSION_KEY_MAIL_PREF + prot + ".auth").equals(SESSION_VALUE_TRUE)) {
                    trans.connect(settingsService.getSmtpServer(), settingsService.getSmtpUser(),
                            settingsService.getSmtpPassword());
                } else {
                    trans.connect();
                }
                trans.sendMessage(message, message.getAllRecipients());
            }
            return true;

        } catch (MessagingException e) {
            LOG.warn("Failed to send email.", e);
            return false;
        }
    }

}
