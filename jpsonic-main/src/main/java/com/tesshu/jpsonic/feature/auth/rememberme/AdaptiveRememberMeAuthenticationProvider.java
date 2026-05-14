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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.feature.auth.rememberme;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class AdaptiveRememberMeAuthenticationProvider extends RememberMeAuthenticationProvider {

    private final RememberMeKeyManager rememberMeKeyManager;

    public AdaptiveRememberMeAuthenticationProvider(RememberMeKeyManager rememberMeKeyManager) {
        super("dummy");
        this.rememberMeKeyManager = rememberMeKeyManager;
    }

    @Override
    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures")
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }

        int expectedKeyHash = getKey().hashCode();
        int presentedKeyHash = ((RememberMeAuthenticationToken) authentication).getKeyHash();
        if (expectedKeyHash != presentedKeyHash) {
            throw new BadCredentialsException("""
                    The presented RememberMeAuthenticationToken \
                    does not contain the expected key.
                    """);
        }

        return authentication;
    }

    @Override
    public String getKey() {
        return rememberMeKeyManager.getKey();
    }
}
