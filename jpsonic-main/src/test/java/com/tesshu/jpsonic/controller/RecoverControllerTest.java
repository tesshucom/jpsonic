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

package com.tesshu.jpsonic.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/*
 * #1020 Implementation needs to be modified
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
class RecoverControllerTest {

    private static final String VIEW_NAME = "recover";

    @Autowired
    private RecoverController controller;

    @Autowired
    private SettingsService settingsService;

    private Method recover;

    @BeforeEach
    public void setup() throws ExecutionException {
        if (recover != null) {
            return;
        }
        try {
            recover = controller.getClass().getDeclaredMethod("recover", HttpServletRequest.class);
            recover.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new ExecutionException(e);
        }

    }

    private ModelAndView doRecover(HttpServletRequest req) throws ExecutionException {
        try {
            Object result = recover.invoke(controller, req);
            if (result != null) {
                return (ModelAndView) result;
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    @Documented
    private @interface RequestDecision {
        @interface Conditions {
            @interface CaptchaEnabled {
                @interface True {
                }

                @interface False {
                }
            }

            @interface UsernameOrEmail {
                @interface Null {
                }

                @interface NotNull {
                }
            }
        }
    }

    @RequestDecision.Conditions.CaptchaEnabled.False
    @RequestDecision.Conditions.UsernameOrEmail.Null
    @Test
    void testR1() throws ExecutionException {
        settingsService.setCaptchaEnabled(false);

        MockHttpServletRequest req = new MockHttpServletRequest();

        ModelAndView result = doRecover(req);
        assertNotNull(result);
        assertEquals(result.getViewName(), VIEW_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
        assertNotNull(model);
        assertEquals(0, model.size());
    }

    @RequestDecision.Conditions.CaptchaEnabled.True
    @RequestDecision.Conditions.UsernameOrEmail.Null
    @Test
    void testR2() throws ExecutionException {
        settingsService.setCaptchaEnabled(true);

        MockHttpServletRequest req = new MockHttpServletRequest();

        ModelAndView result = doRecover(req);
        assertNotNull(result);
        assertEquals(result.getViewName(), VIEW_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
        assertNotNull(model);
        assertEquals(1, model.size());
        assertEquals(settingsService.getRecaptchaSiteKey(), model.get("recaptchaSiteKey"));
    }

    @RequestDecision.Conditions.CaptchaEnabled.True
    @RequestDecision.Conditions.UsernameOrEmail.NotNull
    @Test
    void testR3() throws ExecutionException {
        settingsService.setCaptchaEnabled(true);
        String userName = "*userName*";

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), userName);

        ModelAndView result = doRecover(req);
        assertNotNull(result);
        assertEquals(result.getViewName(), VIEW_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
        assertNotNull(model);
        assertEquals(3, model.size());
        assertEquals(userName, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(settingsService.getRecaptchaSiteKey(), model.get("recaptchaSiteKey"));
        assertEquals("recover.error.invalidcaptcha", model.get(Attributes.Model.ERROR.value()));
    }

}
