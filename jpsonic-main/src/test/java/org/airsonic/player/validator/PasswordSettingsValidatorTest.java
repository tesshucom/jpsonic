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

package org.airsonic.player.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.airsonic.player.command.PasswordSettingsCommand;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@SpringBootTest
public class PasswordSettingsValidatorTest {

    private PasswordSettingsCommand psc;

    @Before
    public void setUp() {
        psc = new PasswordSettingsCommand();
        psc.setUsername("username");
        psc.setPassword("1234");
    }

    private Errors validatePassword() {
        PasswordSettingsValidator psv = new PasswordSettingsValidator();
        Errors errors = new BeanPropertyBindingResult(psc, "psv");
        psv.validate(psc, errors);
        return errors;
    }

    @Test
    public void testValidateEmptyPassword() {
        psc.setPassword("");
        Errors errors = this.validatePassword();
        assertTrue(errors.hasErrors());
    }

    @Test
    public void testValidateDifferentPasswords() {
        psc.setConfirmPassword("5678");

        Errors errors = this.validatePassword();
        assertTrue(errors.hasErrors());
    }

    @Test
    public void testValidateEverythingOK() {
        psc.setConfirmPassword("1234");

        Errors errors = this.validatePassword();
        assertFalse(errors.hasErrors());
    }
}
