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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.tesshu.jpsonic.domain.User;
import de.triology.recaptchav2java.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecoverService {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverService.class);

    private static final String SESSION_KEY_MAIL_PREF = "mail.";
    private static final String SESSION_VALUE_TRUE = "true";

    private final SettingsService settingsService;
    private final SecurityService securityService;

    public RecoverService(SettingsService settingsService, SecurityService securityService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
    }

    public boolean validateCaptcha(String captchaResponseToken) {
        if (settingsService.isCaptchaEnabled()) {
            ReCaptcha captcha = new ReCaptcha(settingsService.getRecaptchaSecretKey());
            return captchaResponseToken != null && captcha.isValid(captchaResponseToken);
        }
        return true;
    }

    public User getUserByUsernameOrEmail(String usernameOrEmail) {
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
    public boolean emailPassword(String password, String username, String email) {
        /* Default to protocol smtp when SmtpEncryption is set to "None" */
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
