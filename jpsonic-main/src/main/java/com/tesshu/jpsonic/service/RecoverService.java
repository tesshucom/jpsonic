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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.util.Date;
import java.util.Properties;

import com.tesshu.jpsonic.persistence.core.entity.User;
import com.tesshu.jpsonic.service.settings.SKeys;
import com.tesshu.jpsonic.service.settings.SettingsFacade;
import de.triology.recaptchav2java.ReCaptcha;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecoverService {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverService.class);

    private static final String SESSION_KEY_MAIL_PREF = "mail.";
    private static final String SESSION_VALUE_TRUE = "true";

    private final SettingsFacade settingsFacade;
    private final SecurityService securityService;

    public RecoverService(SettingsFacade settingsFacade, SecurityService securityService) {
        super();
        this.settingsFacade = settingsFacade;
        this.securityService = securityService;
    }

    public boolean validateCaptcha(String captchaResponseToken) {
        if (settingsFacade.get(SKeys.advanced.captcha.enabled)) {
            ReCaptcha captcha = new ReCaptcha(settingsFacade.get(SKeys.advanced.captcha.secretKey));
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
    public boolean sendEmail(String username, String email) {
        /* Default to protocol smtp when SmtpEncryption is set to "None" */
        String prot = "smtp";
        Properties props = new Properties();
        if ("SSL/TLS".equals(settingsFacade.get(SKeys.advanced.smtp.encryption))) {
            prot = "smtps";
            props.put(SESSION_KEY_MAIL_PREF + prot + ".ssl.enable", SESSION_VALUE_TRUE);
        } else if ("STARTTLS".equals(settingsFacade.get(SKeys.advanced.smtp.encryption))) {
            prot = "smtp";
            props.put(SESSION_KEY_MAIL_PREF + prot + ".starttls.enable", SESSION_VALUE_TRUE);
        }
        props
            .put(SESSION_KEY_MAIL_PREF + prot + ".host",
                    settingsFacade.get(SKeys.advanced.smtp.server));
        props
            .put(SESSION_KEY_MAIL_PREF + prot + ".port",
                    settingsFacade.get(SKeys.advanced.smtp.port));
        /* use authentication when SmtpUser is configured */
        if (settingsFacade.get(SKeys.advanced.smtp.user) != null
                && !settingsFacade.get(SKeys.advanced.smtp.user).isEmpty()) {
            props.put(SESSION_KEY_MAIL_PREF + prot + ".auth", SESSION_VALUE_TRUE);
        }

        Session session = Session.getInstance(props, null);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settingsFacade.get(SKeys.advanced.smtp.from)));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Jpsonic Password");
            message
                .setText(
                        """
                                Hi there!

                                You have requested to reset your Jpsonic password. Please find your new login details below.

                                Username: %s
                                Password: ******

                                """
                            .formatted(username));
            message.setSentDate(Date.from(now()));

            try (Transport trans = session.getTransport(prot)) {
                if (props.get(SESSION_KEY_MAIL_PREF + prot + ".auth") != null && SESSION_VALUE_TRUE
                    .equals(props.get(SESSION_KEY_MAIL_PREF + prot + ".auth"))) {
                    trans
                        .connect(settingsFacade.get(SKeys.advanced.smtp.server),
                                settingsFacade.get(SKeys.advanced.smtp.user),
                                settingsFacade.getDecodedString(SKeys.advanced.smtp.password));
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
