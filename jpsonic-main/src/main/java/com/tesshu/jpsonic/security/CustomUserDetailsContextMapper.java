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
/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tesshu.jpsonic.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

@Component
public class CustomUserDetailsContextMapper implements UserDetailsContextMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CustomUserDetailsContextMapper.class);

    private static final String ATTRIBUTE_NAME_PASSWORD = "userPassword";

    private final SecurityService securityService;
    private final SettingsService settingsService;

    public CustomUserDetailsContextMapper(SecurityService securityService, SettingsService settingsService) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {
        String dn = ctx.getNameInNamespace();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Mapping user details from context with DN: " + dn);
        }

        // User must be defined in Airsonic, unless auto-shadowing is enabled.
        User user = securityService.getUserByName(username, false);
        if (user == null && !settingsService.isLdapAutoShadowing()) {
            throw new BadCredentialsException("User does not exist.");
        }

        if (user == null) {
            User newUser = new User(username, "", null, true, 0L, 0L, 0L);
            newUser.setStreamRole(true);
            newUser.setSettingsRole(true);
            securityService.createUser(newUser);
            if (LOG.isInfoEnabled()) {
                LOG.info("Created local user '" + username + "' for DN " + dn);
            }
            user = securityService.getUserByName(username, false);
        }

        // LDAP authentication must be enabled for the given user.
        if (!user.isLdapAuthenticated()) {
            throw new BadCredentialsException("LDAP authentication disabled for user.");
        }

        LdapUserDetailsImpl.Essence essence = createEssence(ctx, dn, username);
        return essence.createUserDetails();
    }

    private LdapUserDetailsImpl.Essence createEssence(DirContextOperations ctx, String dn, String userName) {
        LdapUserDetailsImpl.Essence essence = new LdapUserDetailsImpl.Essence();
        essence.setDn(dn);

        Object passwordValue = ctx.getObjectAttribute(ATTRIBUTE_NAME_PASSWORD);

        if (passwordValue != null) {
            essence.setPassword(mapPassword(passwordValue));
        }

        essence.setUsername(userName);

        // Add the supplied authorities
        for (GrantedAuthority authority : securityService.getGrantedAuthorities(userName)) {
            essence.addAuthority(authority);
        }

        // Check for PPolicy data

        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ctx
                .getObjectAttribute(PasswordPolicyControl.OID);

        if (ppolicy != null) {
            essence.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
            essence.setGraceLoginsRemaining(ppolicy.getGraceLoginsRemaining());
        }
        return essence;
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("LdapUserDetailsMapper only supports reading from a context. Please "
                + "use a subclass if mapUserToContext() is required.");
    }

    /**
     * Extension point to allow customized creation of the user's password from the attribute stored in the directory.
     *
     * @param passwordValue
     *            the value of the password attribute
     * 
     * @return a String representation of the password.
     */
    protected String mapPassword(final Object passwordValue) {

        Object result = passwordValue;
        if (!(result instanceof String)) {
            // Assume it's binary
            result = new String((byte[]) result, StandardCharsets.UTF_8);
        }

        return (String) result;

    }
}
