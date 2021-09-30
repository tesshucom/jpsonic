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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes.Request.NameConstants;
import com.tesshu.jpsonic.dao.InternetRadioDao;
import com.tesshu.jpsonic.domain.InternetRadio;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class InternetRadioSettingsControllerTest {

    private InternetRadioDao internetRadioDao;
    private InternetRadioSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        InternetRadio radio = new InternetRadio(0, "*name*", "*streamUrl*", "*homepageUrl*", false, new Date());
        internetRadioDao = mock(InternetRadioDao.class);
        Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Arrays.asList(radio));
        InternetRadioService internetRadioService = new InternetRadioService(internetRadioDao);
        controller = new InternetRadioSettingsController(mock(SettingsService.class), internetRadioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testDoGet() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/internetRadioSettings.view")).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("internetRadioSettings"));
        mockMvc.perform(MockMvcRequestBuilders.get("/internetRadioSettings.view").param(NameConstants.TOAST, "true"))
                .andExpect(status().isOk()).andExpect(MockMvcResultMatchers.view().name("internetRadioSettings"));
    }

    @Documented
    private @interface RequestDecision {
        @interface Actions {
            @interface Delete {
            }

            @interface Update {
            }

            @interface New {
            }
        }

        @interface Conditions {
            @interface Delete {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Name {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface StreamUrl {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface HomepageUrl {
                @interface Null {
                }

                @interface NotNull {
                }
            }
        }
    }

    @Nested
    class TestErrorMessages {

        private MockHttpServletRequest createRequest(String streamUrl, String homepageUrl, String name,
                boolean enabled) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter(Attributes.Request.STREAM_URL.value(), streamUrl);
            req.setParameter(Attributes.Request.HOMEPAGE_URL.value(), homepageUrl);
            req.setParameter(Attributes.Request.NAME.value(), name);
            if (enabled) {
                req.setParameter(Attributes.Request.ENABLED.value(), Boolean.toString(enabled));
            }
            return req;
        }

        private MockHttpServletRequest createRequestForArrays(String streamUrl, String homepageUrl, String name,
                boolean enabled, boolean delete) {
            int id = internetRadioDao.getAllInternetRadios().get(0).getId();
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter(Attributes.Request.STREAM_URL.value() + "[" + id + "]", streamUrl);
            req.setParameter(Attributes.Request.HOMEPAGE_URL.value() + "[" + id + "]", homepageUrl);
            req.setParameter(Attributes.Request.NAME.value() + "[" + id + "]", name);
            if (enabled) {
                req.setParameter(Attributes.Request.ENABLED.value() + "[" + id + "]", Boolean.toString(enabled));
            }
            if (delete) {
                req.setParameter(Attributes.Request.DELETE.value() + "[" + id + "]", Boolean.toString(delete));
            }
            return req;
        }

        private String getRedirectError(HttpServletRequest req) throws ExecutionException {
            RedirectAttributes attributes = Mockito.mock(RedirectAttributes.class);
            ArgumentCaptor<Object> errorCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.doReturn(attributes).when(attributes).addFlashAttribute(Mockito.anyString(), errorCaptor.capture());
            controller.doPost(req, attributes);
            Object o = errorCaptor.getValue();
            if (o instanceof Boolean) {
                return null;
            }
            return errorCaptor.getValue().toString();
        }

        @RequestDecision.Actions.Delete
        @RequestDecision.Conditions.Delete.Null
        @Test
        void c1() throws ExecutionException {
            String name = null;
            String streamUrl = null;
            String homepageUrl = null;
            boolean enabled = false;
            boolean delete = true;
            MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

            assertNull(getRedirectError(req));
        }

        @RequestDecision.Actions.Update
        @RequestDecision.Conditions.Delete.NotNull
        @RequestDecision.Conditions.Name.Null
        @RequestDecision.Conditions.StreamUrl.NotNull
        @Test
        void c2() throws ExecutionException {
            String name = null;
            String streamUrl = "*streamUrl*";
            String homepageUrl = "*homepageUrl*";
            boolean enabled = false;
            boolean delete = false;
            MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

            assertEquals("internetradiosettings.noname", getRedirectError(req));
        }

        @RequestDecision.Actions.Update
        @RequestDecision.Conditions.Delete.NotNull
        @RequestDecision.Conditions.Name.NotNull
        @RequestDecision.Conditions.StreamUrl.Null
        @Test
        void c3() throws ExecutionException {
            String name = "*name*";
            String streamUrl = null;
            String homepageUrl = "*homepageUrl*";
            boolean enabled = false;
            boolean delete = false;
            MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

            assertEquals("internetradiosettings.nourl", getRedirectError(req));
        }

        @RequestDecision.Actions.Update
        @RequestDecision.Conditions.Delete.NotNull
        @RequestDecision.Conditions.Name.NotNull
        @RequestDecision.Conditions.StreamUrl.NotNull
        @Test
        void c4() throws ExecutionException {
            String name = "*name*";
            String streamUrl = "*streamUrl*";
            String homepageUrl = "*homepageUrl*";
            boolean enabled = true;
            boolean delete = false;
            MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

            assertNull(getRedirectError(req));
        }

        @RequestDecision.Actions.New
        @RequestDecision.Conditions.Name.Null
        @RequestDecision.Conditions.StreamUrl.Null
        @RequestDecision.Conditions.HomepageUrl.Null
        @Test
        void c5() throws ExecutionException {
            String name = null;
            String streamUrl = null;
            String homepageUrl = null;
            boolean enabled = false;
            MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

            Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Collections.emptyList());
            assertNull(getRedirectError(req));
        }

        @RequestDecision.Actions.New
        @RequestDecision.Conditions.Name.NotNull
        @RequestDecision.Conditions.StreamUrl.Null
        @RequestDecision.Conditions.HomepageUrl.Null
        @Test
        void c6() throws ExecutionException {
            String name = "*name*";
            String streamUrl = null;
            String homepageUrl = null;
            boolean enabled = false;
            MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

            Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Collections.emptyList());
            assertEquals("internetradiosettings.nourl", getRedirectError(req));
        }

        @RequestDecision.Actions.New
        @RequestDecision.Conditions.Name.Null
        @RequestDecision.Conditions.StreamUrl.NotNull
        @RequestDecision.Conditions.HomepageUrl.Null
        @Test
        void c7() throws ExecutionException {
            String name = null;
            String streamUrl = "*streamUrl*";
            String homepageUrl = null;
            boolean enabled = false;
            MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

            Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Collections.emptyList());
            assertEquals("internetradiosettings.noname", getRedirectError(req));
        }

        @RequestDecision.Actions.New
        @RequestDecision.Conditions.Name.Null
        @RequestDecision.Conditions.StreamUrl.Null
        @RequestDecision.Conditions.HomepageUrl.NotNull
        @Test
        void c8() throws ExecutionException {
            String name = null;
            String streamUrl = null;
            String homepageUrl = "*homepageUrl*";
            boolean enabled = false;
            MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

            Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Collections.emptyList());
            assertEquals("internetradiosettings.noname", getRedirectError(req));
        }

        @RequestDecision.Actions.New
        @RequestDecision.Conditions.Name.NotNull
        @RequestDecision.Conditions.StreamUrl.NotNull
        @RequestDecision.Conditions.HomepageUrl.NotNull
        @Test
        void c9() throws ExecutionException {
            String name = "*name*";
            String streamUrl = "*streamUrl*";
            String homepageUrl = "*homepageUrl*";
            boolean enabled = true;
            MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

            Mockito.when(internetRadioDao.getAllInternetRadios()).thenReturn(Collections.emptyList());
            assertNull(getRedirectError(req));
        }
    }
}
