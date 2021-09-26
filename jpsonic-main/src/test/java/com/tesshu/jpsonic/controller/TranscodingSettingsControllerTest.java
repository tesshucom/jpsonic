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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TranscodingSettingsControllerTest {

    private TranscodingSettingsController controller;
    private TranscodingService transcodingService;
    private SettingsService settingsService;

    private MockMvc mockMvc;

    private Method handleParameters;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        SecurityService securityService = mock(SecurityService.class);
        transcodingService = new TranscodingService(settingsService, securityService, mock(TranscodingDao.class),
                mock(PlayerService.class), null);
        controller = new TranscodingSettingsController(settingsService, securityService, transcodingService,
                mock(ShareService.class), mock(OutlineHelpSelector.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        if (handleParameters != null) {
            return;
        }
        try {
            handleParameters = controller.getClass().getDeclaredMethod("handleParameters", HttpServletRequest.class,
                    RedirectAttributes.class);
            handleParameters.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new ExecutionException(e);
        }
    }

    @WithMockUser(username = "admin")
    @Test
    void testDoGet() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/transcodingSettings.view"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("transcodingSettings")).andReturn();
        Assertions.assertNotNull(mvcResult);
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) mvcResult.getModelAndView().getModel().get("model");
        assertEquals(8, model.size());
        assertEquals(settingsService.getHlsCommand(), model.get("hlsCommand"));
        assertFalse((Boolean) model.get("isOpenDetailSetting"));
        assertFalse((Boolean) model.get("showOutlineHelp"));
        assertEquals(0, model.get("shareCount"));
        assertFalse((Boolean) model.get("useRadio"));
        assertEquals(transcodingService.getTranscodeDirectory(), model.get("transcodeDirectory"));
        assertEquals(SettingsService.getBrand(), model.get("brand"));
        assertEquals(transcodingService.getAllTranscodings(), model.get("transcodings"));
    }

    private String doHandleParameters(HttpServletRequest req) throws ExecutionException {
        try {
            Object result = handleParameters.invoke(controller, req, Mockito.mock(RedirectAttributes.class));
            if (result != null) {
                return (String) result;
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        transcodingService.getAllTranscodings().stream().forEach(t -> {
            req.setParameter(Attributes.Request.NAME.value() + "[" + t.getId() + "]", t.getName());
            req.setParameter(Attributes.Request.SOURCE_FORMATS.value() + "[" + t.getId() + "]", t.getSourceFormats());
            req.setParameter(Attributes.Request.TARGET_FORMAT.value() + "[" + t.getId() + "]", t.getTargetFormat());
            req.setParameter(Attributes.Request.STEP1.value() + "[" + t.getId() + "]", t.getStep1());
            req.setParameter(Attributes.Request.STEP2.value() + "[" + t.getId() + "]", "");
        });
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

            @interface SourceFormats {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface TargetFormat {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Step1 {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Step2 {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface DefaultActive {
                @interface Null {
                }

                @interface NotNull {
                }
            }

        }
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp1() throws ExecutionException {
        HttpServletRequest req = createRequest();
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp2() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        transcodingService.getAllTranscodings().stream().findFirst().ifPresent(t -> {
            req.setParameter(Attributes.Request.NAME.value() + "[" + t.getId() + "]", "");
        });
        assertEquals("transcodingsettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.Null
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp3() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        transcodingService.getAllTranscodings().stream().findFirst().ifPresent(t -> {
            req.setParameter(Attributes.Request.SOURCE_FORMATS.value() + "[" + t.getId() + "]", "");
        });
        assertEquals("transcodingsettings.nosourceformat", doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.Null
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp4() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        transcodingService.getAllTranscodings().stream().findFirst().ifPresent(t -> {
            req.setParameter(Attributes.Request.TARGET_FORMAT.value() + "[" + t.getId() + "]", "");
        });
        assertEquals("transcodingsettings.notargetformat", doHandleParameters(req));
    }

    @RequestDecision.Actions.Update
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.Null
    @RequestDecision.Conditions.Delete.Null
    @Test
    void testHp5() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        transcodingService.getAllTranscodings().stream().findFirst().ifPresent(t -> {
            req.setParameter(Attributes.Request.STEP1.value() + "[" + t.getId() + "]", "");
        });
        assertEquals("transcodingsettings.nostep1", doHandleParameters(req));
    }

    @RequestDecision.Actions.Delete
    @RequestDecision.Conditions.Delete.NotNull
    @Test
    void testHp6() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        transcodingService.getAllTranscodings().stream().findFirst().ifPresent(t -> {
            req.setParameter(Attributes.Request.DELETE.value() + "[" + t.getId() + "]", "true");
        });
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.Null
    @Test
    void testHp7() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "*name*");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "");
        assertEquals("transcodingsettings.nosourceformat", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.Null
    @Test
    void testHp8() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "*name*");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "*source*");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "");
        assertEquals("transcodingsettings.notargetformat", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.Null
    @Test
    void testHp9() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "*name*");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "*source*");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "*target*");
        req.setParameter(Attributes.Request.STEP1.value(), "");
        assertEquals("transcodingsettings.nostep1", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Step2.Null
    @RequestDecision.Conditions.DefaultActive.Null
    @Test
    void testHp10() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "*name*");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "*source*");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "*target*");
        req.setParameter(Attributes.Request.STEP1.value(), "*step1*");
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.NotNull
    @RequestDecision.Conditions.SourceFormats.NotNull
    @RequestDecision.Conditions.TargetFormat.NotNull
    @RequestDecision.Conditions.Step1.NotNull
    @RequestDecision.Conditions.Step2.NotNull
    @RequestDecision.Conditions.DefaultActive.NotNull
    @Test
    void testHp11() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "*name*");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "*source*");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "*target*");
        req.setParameter(Attributes.Request.STEP1.value(), "*step1*");
        req.setParameter(Attributes.Request.STEP2.value(), "*step2*");
        req.setParameter(Attributes.Request.DEFAULT_ACTIVE.value(), "*active*");
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.SourceFormats.Null
    @RequestDecision.Conditions.TargetFormat.Null
    @RequestDecision.Conditions.Step1.Null
    @RequestDecision.Conditions.Step2.Null
    @Test
    void testHp12() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "");
        req.setParameter(Attributes.Request.STEP1.value(), "");
        req.setParameter(Attributes.Request.STEP2.value(), "");
        assertNull(doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.SourceFormats.NotNull
    @Test
    void testHp13() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "");
        req.setParameter(Attributes.Request.SOURCE_FORMATS.value(), "*source*");
        assertEquals("transcodingsettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.TargetFormat.NotNull
    @Test
    void testHp14() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "");
        req.setParameter(Attributes.Request.TARGET_FORMAT.value(), "*target*");
        assertEquals("transcodingsettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.Step1.NotNull
    @Test
    void testHp15() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "");
        req.setParameter(Attributes.Request.STEP1.value(), "*step1*");
        assertEquals("transcodingsettings.noname", doHandleParameters(req));
    }

    @RequestDecision.Actions.New
    @RequestDecision.Conditions.Name.Null
    @RequestDecision.Conditions.Step2.NotNull
    @Test
    void testHp16() throws ExecutionException {
        MockHttpServletRequest req = createRequest();
        req.setParameter(Attributes.Request.NAME.value(), "");
        req.setParameter(Attributes.Request.STEP2.value(), "*step2*");
        assertEquals("transcodingsettings.noname", doHandleParameters(req));
    }
}
