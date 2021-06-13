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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.controller.Attributes.Request.NameConstants;
import com.tesshu.jpsonic.dao.InternetRadioDao;
import com.tesshu.jpsonic.domain.InternetRadio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class InternetRadioSettingsControllerTest {

    @Autowired
    private InternetRadioSettingsController controller;

    @Autowired
    private InternetRadioDao internetRadioDao;

    private MockMvc mockMvc;

    private Method handleParameters;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        if (internetRadioDao.getAllInternetRadios().size() == 0) {
            InternetRadio radio = new InternetRadio("*name*", "*streamUrl*", "*homepageUrl*", false, new Date());
            internetRadioDao.createInternetRadio(radio);
        }

        if (handleParameters != null) {
            return;
        }
        try {
            handleParameters = controller.getClass().getDeclaredMethod("handleParameters", HttpServletRequest.class);
            handleParameters.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new ExecutionException(e);
        }

    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testDoGet() throws Exception {
        mockMvc.perform(get("/internetRadioSettings.view")).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("internetRadioSettings"));
        mockMvc.perform(get("/internetRadioSettings.view").param(NameConstants.TOAST, "true"))
                .andExpect(status().isOk()).andExpect(MockMvcResultMatchers.view().name("internetRadioSettings"));
    }

    private MockHttpServletRequest createRequest(String streamUrl, String homepageUrl, String name, boolean enabled) {
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

    private String doHandleParameters(HttpServletRequest req) throws ExecutionException {
        try {
            Object errorMessage = handleParameters.invoke(controller, req);
            if (errorMessage != null) {
                return (String) errorMessage;
            }

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    private void clearRadios() {
        internetRadioDao.getAllInternetRadios().forEach(r -> internetRadioDao.deleteInternetRadio(r.getId()));
    }

    @RequestDecision.Actions.Delete
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp1() throws ExecutionException {
        String name = null;
        String streamUrl = null;
        String homepageUrl = null;
        boolean enabled = false;
        boolean delete = true;
        MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Delete.NotNull
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.StreamUrl.NotNull
    @Test
    void testHp2() throws ExecutionException {
        String name = null;
        String streamUrl = "*streamUrl*";
        String homepageUrl = "*homepageUrl*";
        boolean enabled = false;
        boolean delete = false;
        MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

        assertEquals("internetradiosettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Delete.NotNull
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.StreamUrl.Null
    @Test
    void testHp3() throws ExecutionException {
        String name = "*name*";
        String streamUrl = null;
        String homepageUrl = "*homepageUrl*";
        boolean enabled = false;
        boolean delete = false;
        MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

        assertEquals("internetradiosettings.nourl", doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Delete.NotNull
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.StreamUrl.NotNull
    @Test
    void testHp4() throws ExecutionException {
        String name = "*name*";
        String streamUrl = "*streamUrl*";
        String homepageUrl = "*homepageUrl*";
        boolean enabled = true;
        boolean delete = false;
        MockHttpServletRequest req = createRequestForArrays(streamUrl, homepageUrl, name, enabled, delete);

        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.StreamUrl.Null
    @RequestDecision.Conditions.HomepageUrl.Null
    @Test
    void testHp5() throws ExecutionException {
        String name = null;
        String streamUrl = null;
        String homepageUrl = null;
        boolean enabled = false;
        MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

        clearRadios();
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.StreamUrl.Null
    @RequestDecision.Conditions.HomepageUrl.Null
    @Test
    void testHp6() throws ExecutionException {
        String name = "*name*";
        String streamUrl = null;
        String homepageUrl = null;
        boolean enabled = false;
        MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

        clearRadios();
        assertEquals("internetradiosettings.nourl", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.StreamUrl.NotNull
    @RequestDecision.Conditions.HomepageUrl.Null
    @Test
    void testHp7() throws ExecutionException {
        String name = null;
        String streamUrl = "*streamUrl*";
        String homepageUrl = null;
        boolean enabled = false;
        MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

        clearRadios();
        assertEquals("internetradiosettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.StreamUrl.Null
    @RequestDecision.Conditions.HomepageUrl.NotNull
    @Test
    void testHp8() throws ExecutionException {
        String name = null;
        String streamUrl = null;
        String homepageUrl = "*homepageUrl*";
        boolean enabled = false;
        MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

        clearRadios();
        assertEquals("internetradiosettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.StreamUrl.NotNull
    @RequestDecision.Conditions.HomepageUrl.NotNull
    @Test
    void testHp9() throws ExecutionException {
        String name = "*name*";
        String streamUrl = "*streamUrl*";
        String homepageUrl = "*homepageUrl*";
        boolean enabled = true;
        MockHttpServletRequest req = createRequest(streamUrl, homepageUrl, name, enabled);

        clearRadios();
        assertNull(doHandleParameters(req));
    }
}
