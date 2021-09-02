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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.RecoverService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
class RecoverControllerTest {

    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_MAIL = "admin@tesshu.com";
    private static final String ATTR_MODEL = "model";
    private static final String ATTR_SITE_KEY = "recaptchaSiteKey";
    private String testRecaptchaSiteKey = "testRecaptchaSiteKey";

    @Mock
    private SettingsService settingsService;
    @Mock
    private SecurityService securityService;
    @Mock
    private RecoverService recoverService;

    private RecoverController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        controller = new RecoverController(settingsService, securityService, recoverService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Documented
    private @interface RecoverDecision {
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

            @interface ValidateCaptcha {
                @interface True {
                }

                @interface False {

                }
            }

            @interface User {
                @interface Null {
                }

                @interface NotNull {

                }

                @interface Email {
                    @interface Null {
                    }

                    @interface NotNull {

                    }
                }

            }

            @interface SmtpServer {
                @interface Null {
                }

                @interface Empty {
                }

                @interface NotEmpty {
                }
            }

            @interface SendMail {
                @interface Fail {
                }

                @interface Success {
                }
            }

        }

        @interface Result {
            @interface Success {
            }

            @interface Error {
            }

            @interface UpdateUser {
            }
        }
    }

    @RecoverDecision.Conditions.UsernameOrEmail.Null
    @RecoverDecision.Conditions.CaptchaEnabled.False
    @RecoverDecision.Result.Success
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR01() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(false);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recover.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("recover", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);
        assertNotNull(model);
        assertFalse(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertFalse(model.containsKey(ATTR_SITE_KEY));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.Null
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Result.Success
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR02() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recover.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("recover", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);
        assertNotNull(model);

        assertFalse(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.False
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR03() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.invalidcaptcha", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.Null
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR04() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.usernotfound", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.NotNull
    @RecoverDecision.Conditions.User.Email.Null
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR05() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);
        Mockito.when(recoverService.getUserByUsernameOrEmail(Mockito.any()))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, null));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.noemail", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.NotNull
    @RecoverDecision.Conditions.User.Email.NotNull
    @RecoverDecision.Conditions.SmtpServer.Null
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR06() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);
        Mockito.when(recoverService.getUserByUsernameOrEmail(Mockito.any()))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ADMIN_MAIL));
        Mockito.when(settingsService.getSmtpServer()).thenReturn(null);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.sendfailed", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.NotNull
    @RecoverDecision.Conditions.User.Email.NotNull
    @RecoverDecision.Conditions.SmtpServer.Empty
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR07() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);
        Mockito.when(recoverService.getUserByUsernameOrEmail(Mockito.any()))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ADMIN_MAIL));
        Mockito.when(settingsService.getSmtpServer()).thenReturn("");

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.sendfailed", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.NotNull
    @RecoverDecision.Conditions.User.Email.NotNull
    @RecoverDecision.Conditions.SmtpServer.NotEmpty
    @RecoverDecision.Conditions.SendMail.Fail
    @RecoverDecision.Result.Error
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR08() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);
        Mockito.when(recoverService.getUserByUsernameOrEmail(Mockito.any()))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ADMIN_MAIL));
        Mockito.when(settingsService.getSmtpServer()).thenReturn("dummySMTP");
        Mockito.when(recoverService.emailPassword(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertEquals("recover.error.sendfailed", model.get(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.never()).updateUser(Mockito.any());
    }

    @RecoverDecision.Conditions.UsernameOrEmail.NotNull
    @RecoverDecision.Conditions.CaptchaEnabled.True
    @RecoverDecision.Conditions.ValidateCaptcha.True
    @RecoverDecision.Conditions.User.NotNull
    @RecoverDecision.Conditions.User.Email.NotNull
    @RecoverDecision.Conditions.SmtpServer.NotEmpty
    @RecoverDecision.Conditions.SendMail.Success
    @RecoverDecision.Result.UpdateUser
    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testR09() throws Exception {
        Mockito.when(settingsService.isCaptchaEnabled()).thenReturn(true);
        Mockito.when(settingsService.getRecaptchaSiteKey()).thenReturn(testRecaptchaSiteKey);
        Mockito.when(recoverService.validateCaptcha(Mockito.any())).thenReturn(true);
        Mockito.when(recoverService.getUserByUsernameOrEmail(Mockito.any()))
                .thenReturn(new User(ADMIN_NAME, ADMIN_NAME, ADMIN_MAIL));
        Mockito.when(settingsService.getSmtpServer()).thenReturn("dummySMTP");
        Mockito.when(recoverService.emailPassword(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USERNAME_OR_EMAIL.value(), ADMIN_NAME);
        ModelAndView modelAndView = controller.recover(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(ATTR_MODEL);

        assertTrue(model.containsKey(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(ADMIN_NAME, model.get(Attributes.Request.USERNAME_OR_EMAIL.value()));
        assertEquals(testRecaptchaSiteKey, model.get(ATTR_SITE_KEY));
        assertFalse(model.containsKey(Attributes.Request.ERROR.value()));
        Mockito.verify(securityService, Mockito.times(1)).updateUser(Mockito.any());
    }
}
