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

package org.airsonic.player.service.sonos;

/**
 * @author Sindre Mehus
 */
@SuppressWarnings("serial")
public class SonosSoapFault extends RuntimeException {

    private final String faultCode;
    private final int sonosError;

    protected SonosSoapFault(String faultCode, int sonosError) {
        super();
        this.faultCode = faultCode;
        this.sonosError = sonosError;
    }

    public String getFaultCode() {
        return faultCode;
    }

    /*
     * Must match values in strings.xml
     */
    public int getSonosError() {
        return sonosError;
    }

    public static class LoginInvalid extends SonosSoapFault {

        public LoginInvalid() {
            super("Client.LoginInvalid", 0);
        }
    }

    public static class LoginUnauthorized extends SonosSoapFault {

        public LoginUnauthorized() {
            super("Client.LoginUnauthorized", 1);
        }
    }
}
