
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
