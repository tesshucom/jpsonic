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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * @author Sindre Mehus
 */
public class LoginFailureListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(LoginFailureListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof AbstractAuthenticationFailureEvent)) {
            return;
        }

        Object source = event.getSource();
        if (!(source instanceof AbstractAuthenticationToken)) {
            return;
        }

        AbstractAuthenticationToken token = (AbstractAuthenticationToken) source;
        Object details = token.getDetails();
        if (!(details instanceof WebAuthenticationDetails)) {
            return;
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Login failed from [" + ((WebAuthenticationDetails) details).getRemoteAddress() + "]");
        }

    }
}
