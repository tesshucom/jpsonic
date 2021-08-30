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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.command.PersonalSettingsCommand;
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
@SuppressWarnings("PMD.TooManyStaticImports")
class PersonalSettingsControllerTest {

    private static final String ADMIN_NAME = "admin";
    private static final String VIEW_NAME = "personalSettings";

    @Autowired
    private PersonalSettingsController controller;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private SecurityService securityService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @WithMockUser(username = ADMIN_NAME)
    @Test
    void testDisplayForm() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertEquals("random", command.getAlbumListId());
        assertEquals(10, command.getAlbumLists().length);
        assertTrue(command.isAlternativeDrawer());
        assertTrue(command.isAssignAccesskeyToNumber());
        assertTrue(command.isAutoHidePlayQueue());
        assertEquals(-1, command.getAvatarId());
        assertEquals(30, command.getAvatars().size());
        assertFalse(command.isBetaVersionNotificationEnabled());
        assertTrue(command.isBreadcrumbIndex());
        assertFalse(command.isCloseDrawer());
        assertTrue(command.isClosePlayQueue());
        assertNull(command.getCustomAvatar());
        assertNotNull(command.getDefaultSettings());
        assertTrue(command.isFinalVersionNotificationEnabled());
        assertFalse(command.isForceBio2Eng());
        assertNotNull(command.getFontFamily());
        assertNotNull(command.getFontFamilyDefault());
        assertNotNull(command.getFontFamilyJpEmbedDefault());
        assertEquals(FontScheme.DEFAULT, command.getFontScheme());
        assertEquals(14, command.getFontSize());
        assertEquals(14, command.getFontSizeDefault());
        assertNotNull(command.getFontSizeJpEmbedDefault());
        assertNotNull(command.getFontFamilyJpEmbedDefault());
        assertNotNull(command.getIetf());
        assertNotNull(command.getIetfDefault());
        assertNotNull(command.getIetfDisplayDefault());
        assertTrue(command.isKeyboardShortcutsEnabled());
        assertFalse(command.isLastFmEnabled());
        assertNull(command.getLastFmPassword());
        assertNull(command.getLastFmUsername());
        assertFalse(command.isListenBrainzEnabled());
        assertNull(command.getListenBrainzToken());
        assertEquals("-1", command.getLocaleIndex());
        assertEquals(28, command.getLocales().length);
        assertNotNull(command.getMainVisibility());
        assertFalse(command.isNowPlayingAllowed());
        assertFalse(command.isOpenDetailIndex());
        assertFalse(command.isOpenDetailSetting());
        assertFalse(command.isOpenDetailStar());
        assertFalse(command.isOthersPlayingEnabled());
        assertEquals(40, command.getPaginationSize());
        assertFalse(command.isPartyModeEnabled());
        assertNotNull(command.getPlaylistVisibility());
        assertFalse(command.isPutMenuInDrawer());
        assertTrue(command.isQueueFollowingSongs());
        assertEquals(0, command.getShareCount());
        assertFalse(command.isShowAlbumActions());
        assertFalse(command.isShowAlbumSearch());
        assertFalse(command.isShowArtistInfoEnabled());
        assertFalse(command.isShowChangeCoverArt());
        assertFalse(command.isShowComment());
        assertTrue(command.isShowCurrentSongInfo());
        assertFalse(command.isShowDownload());
        assertTrue(command.isShowIndex());
        assertFalse(command.isShowLastPlay());
        assertFalse(command.isShowNowPlayingEnabled());
        assertFalse(command.isShowOutlineHelp());
        assertFalse(command.isShowRate());
        assertFalse(command.isShowShare());
        assertFalse(command.isShowSibling());
        assertFalse(command.isShowSimilar());
        assertFalse(command.isShowTag());
        assertFalse(command.isShowToast());
        assertFalse(command.isShowTopSongs());
        assertTrue(command.isSimpleDisplay());
        assertNotNull(command.getSmartphoneSettings());
        assertFalse(command.isSongNotificationEnabled());
        assertEquals(SpeechToTextLangScheme.DEFAULT, command.getSpeechToTextLangScheme());
        assertNotNull(command.getTabletSettings());
        assertEquals("-1", command.getThemeIndex());
        assertEquals(18, command.getThemes().length);
        assertEquals(ADMIN_NAME, command.getUser().getUsername());
        assertFalse(command.isUseRadio());
        assertFalse(command.isUseSonos());
        assertFalse(command.isVoiceInputEnabled());
    }

    @Documented
    private @interface DoSubmitDecision {
        @interface Conditions {
            @interface UserSettings {
                @interface Locale {
                    @interface Null {
                    }

                    @interface NotNull {
                    }
                }

                @interface SpeechLangSchemeName {
                    @interface Default {

                    }

                    @interface NotDefault {
                    }
                }

                @interface ThemeId {
                    @interface Null {
                    }

                    @interface NotNull {
                    }
                }
            }

            @interface SettingsService {
                @interface OthersPlayingEnabled {
                    @interface False {
                    }

                    @interface True {
                    }
                }
            }

            @interface Command {

                @interface SpeechLangSchemeName {
                    @interface Default {

                    }

                    @interface NotDefault {
                    }
                }

                @interface Ietf {
                    @interface Blank {
                    }

                    @interface NotBlank {
                        @interface FineFormat {
                        }

                        @interface NotFine {
                        }
                    }
                }

                @interface LastFmPassword {
                    @interface Blank {
                    }

                    @interface NotBlank {
                    }
                }

                @interface AvatarScheme {
                    @interface NONE {
                    }

                    @interface CUSTOM {
                    }

                    @interface SYSTEM {
                    }
                }
            }

        }
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class DoSubmitAction {

        @DoSubmitDecision.Conditions.UserSettings.Locale.Null
        @DoSubmitDecision.Conditions.UserSettings.SpeechLangSchemeName.Default
        @DoSubmitDecision.Conditions.UserSettings.ThemeId.Null
        @DoSubmitDecision.Conditions.SettingsService.OthersPlayingEnabled.False
        @DoSubmitDecision.Conditions.Command.Ietf.NotBlank.FineFormat
        @DoSubmitDecision.Conditions.Command.LastFmPassword.Blank
        @DoSubmitDecision.Conditions.Command.SpeechLangSchemeName.Default
        @DoSubmitDecision.Conditions.Command.AvatarScheme.NONE
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(1)
        void c1() throws Exception {

            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            assertNotNull(command.getIetf()); // Depends on the test environment

            UserSettings userSettings = securityService.getUserSettings(ADMIN_NAME);
            assertNull(userSettings.getLocale());
            assertEquals(SpeechToTextLangScheme.DEFAULT.name(), userSettings.getSpeechLangSchemeName());
            assertNull(userSettings.getThemeId());
            assertFalse(settingsService.isOthersPlayingEnabled());

            result = mockMvc
                    .perform(MockMvcRequestBuilders.post("/" + ViewName.PERSONAL_SETTINGS.value())
                            .requestAttr(Attributes.Model.Command.VALUE, command))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
            assertNotNull(result);
        }

        @DoSubmitDecision.Conditions.UserSettings.Locale.NotNull
        @DoSubmitDecision.Conditions.UserSettings.SpeechLangSchemeName.NotDefault
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(2)
        void c2() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            UserSettings userSettings = securityService.getUserSettings(ADMIN_NAME);
            userSettings.setLocale(Locale.JAPANESE);
            userSettings.setSpeechLangSchemeName(SpeechToTextLangScheme.BCP47.name());
            securityService.updateUserSettings(userSettings);

            result = mockMvc
                    .perform(MockMvcRequestBuilders.post("/" + ViewName.PERSONAL_SETTINGS.value())
                            .requestAttr(Attributes.Model.Command.VALUE, command))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
            assertNotNull(result);
        }

        @DoSubmitDecision.Conditions.UserSettings.Locale.NotNull
        @DoSubmitDecision.Conditions.UserSettings.SpeechLangSchemeName.Default
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(3)
        void c3() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            UserSettings userSettings = securityService.getUserSettings(ADMIN_NAME);
            userSettings.setLocale(StringUtil.parseLocale("ja_JP"));
            userSettings.setSpeechLangSchemeName(SpeechToTextLangScheme.DEFAULT.name());
            securityService.updateUserSettings(userSettings);

            result = mockMvc
                    .perform(MockMvcRequestBuilders.post("/" + ViewName.PERSONAL_SETTINGS.value())
                            .requestAttr(Attributes.Model.Command.VALUE, command))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
            assertNotNull(result);
        }

        @DoSubmitDecision.Conditions.UserSettings.ThemeId.NotNull
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(4)
        void c4() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            UserSettings userSettings = securityService.getUserSettings(ADMIN_NAME);
            userSettings.setThemeId("jpsonic");
            securityService.updateUserSettings(userSettings);

            result = mockMvc
                    .perform(MockMvcRequestBuilders.post("/" + ViewName.PERSONAL_SETTINGS.value())
                            .requestAttr(Attributes.Model.Command.VALUE, command))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
            assertNotNull(result);
        }

        @DoSubmitDecision.Conditions.SettingsService.OthersPlayingEnabled.True
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(5)
        void c5() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            settingsService.setOthersPlayingEnabled(true);

            result = mockMvc
                    .perform(MockMvcRequestBuilders.post("/" + ViewName.PERSONAL_SETTINGS.value())
                            .requestAttr(Attributes.Model.Command.VALUE, command))
                    .andExpect(MockMvcResultMatchers.status().isFound())
                    .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
            assertNotNull(result);
        }

        @DoSubmitDecision.Conditions.Command.SpeechLangSchemeName.NotDefault
        @DoSubmitDecision.Conditions.Command.Ietf.Blank
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(6)
        void c6() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            command.setSpeechToTextLangScheme(SpeechToTextLangScheme.BCP47);
            command.setIetf(null);

            settingsService.setOthersPlayingEnabled(true);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertNotNull(command);
            assertNotNull(command.getIetf());
        }

        @DoSubmitDecision.Conditions.Command.SpeechLangSchemeName.NotDefault
        @DoSubmitDecision.Conditions.Command.Ietf.NotBlank.NotFine
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(7)
        void c7() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            command.setSpeechToTextLangScheme(SpeechToTextLangScheme.BCP47);
            command.setIetf("Unknown.Unknown");

            settingsService.setOthersPlayingEnabled(true);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertNotNull(command);
            assertNotNull(command.getIetf());
        }

        @DoSubmitDecision.Conditions.Command.LastFmPassword.NotBlank
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(8)
        void c8() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            command.setLastFmPassword("pass");

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertNotNull(command);
            assertNotNull(command.getLastFmPassword());
        }

        @DoSubmitDecision.Conditions.Command.AvatarScheme.CUSTOM
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(9)
        void c9() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            command.setAvatarId(AvatarScheme.CUSTOM.getCode());

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertNotNull(command);
            assertEquals(AvatarScheme.CUSTOM.getCode(), command.getAvatarId());
        }

        @DoSubmitDecision.Conditions.Command.AvatarScheme.SYSTEM
        @WithMockUser(username = ADMIN_NAME)
        // @Test NeedScan
        @Order(10)
        void c10() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);
            command.setAvatarId(1);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertNotNull(command);
            assertEquals(AvatarScheme.CUSTOM.getCode(), command.getAvatarId());
        }
    }

    @Documented
    private @interface NowPlayingDecision {
        @interface Conditions {
            @interface SettingsService {
                @interface OthersPlayingEnabled {
                    @interface False {
                    }

                    @interface True {
                    }
                }
            }

            @interface UserSettings {
                @interface ShowNowPlayingEnabled {
                    @interface False {
                    }

                    @interface True {
                    }
                }

                @interface NowPlayingAllowed {
                    @interface False {
                    }

                    @interface True {
                    }
                }
            }

            @interface Command {
                @interface ShowNowPlayingEnabled {
                    @interface True {
                    }
                }

                @interface NowPlayingAllowed {
                    @interface True {
                    }
                }
            }
        }

        @interface Result {
            @interface UserSettings {
                @interface ShowNowPlayingEnabled {
                    @interface False {
                    }

                    @interface True {
                    }
                }

                @interface NowPlayingAllowed {

                    @interface True {
                    }
                }
            }
        }
    }

    /*
     * #638, #1048
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class NowPlaying {

        @NowPlayingDecision.Conditions.UserSettings.NowPlayingAllowed.False
        @NowPlayingDecision.Conditions.UserSettings.ShowNowPlayingEnabled.False
        @NowPlayingDecision.Conditions.SettingsService.OthersPlayingEnabled.False
        @NowPlayingDecision.Conditions.Command.NowPlayingAllowed.True
        @NowPlayingDecision.Conditions.Command.ShowNowPlayingEnabled.True
        @NowPlayingDecision.Result.UserSettings.NowPlayingAllowed.True
        @NowPlayingDecision.Result.UserSettings.ShowNowPlayingEnabled.False
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(1)
        void c1() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            assertFalse(command.isNowPlayingAllowed());
            assertFalse(command.isShowNowPlayingEnabled());
            command.setNowPlayingAllowed(true);
            command.setShowNowPlayingEnabled(true);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertTrue(command.isNowPlayingAllowed());
            assertFalse(command.isShowNowPlayingEnabled());
        }

        @NowPlayingDecision.Conditions.UserSettings.NowPlayingAllowed.True
        @NowPlayingDecision.Conditions.UserSettings.ShowNowPlayingEnabled.False
        @NowPlayingDecision.Conditions.SettingsService.OthersPlayingEnabled.True
        @NowPlayingDecision.Conditions.Command.NowPlayingAllowed.True
        @NowPlayingDecision.Conditions.Command.ShowNowPlayingEnabled.True
        @NowPlayingDecision.Result.UserSettings.NowPlayingAllowed.True
        @NowPlayingDecision.Result.UserSettings.ShowNowPlayingEnabled.True
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(2)
        void c2() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            assertTrue(command.isNowPlayingAllowed());
            assertFalse(command.isShowNowPlayingEnabled());
            command.setNowPlayingAllowed(true);
            command.setShowNowPlayingEnabled(true);
            settingsService.setOthersPlayingEnabled(true);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertTrue(command.isNowPlayingAllowed());
            assertTrue(command.isShowNowPlayingEnabled());
        }

        @NowPlayingDecision.Conditions.UserSettings.NowPlayingAllowed.True
        @NowPlayingDecision.Conditions.UserSettings.ShowNowPlayingEnabled.True
        @NowPlayingDecision.Conditions.SettingsService.OthersPlayingEnabled.False
        @NowPlayingDecision.Conditions.Command.NowPlayingAllowed.True
        @NowPlayingDecision.Conditions.Command.ShowNowPlayingEnabled.True
        @NowPlayingDecision.Result.UserSettings.NowPlayingAllowed.True
        @NowPlayingDecision.Result.UserSettings.ShowNowPlayingEnabled.False
        @WithMockUser(username = ADMIN_NAME)
        @Test
        @Order(3)
        void c3() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            assertNotNull(result);
            ModelAndView modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            PersonalSettingsCommand command = (PersonalSettingsCommand) modelAndView.getModelMap()
                    .get(Attributes.Model.Command.VALUE);
            assertNotNull(command);

            assertTrue(command.isNowPlayingAllowed());
            assertTrue(command.isShowNowPlayingEnabled());
            command.setNowPlayingAllowed(true);
            command.setShowNowPlayingEnabled(true);
            settingsService.setOthersPlayingEnabled(false);

            modelAndView = controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
            assertNull(modelAndView.getViewName());

            result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PERSONAL_SETTINGS.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
            modelAndView = result.getModelAndView();
            assertEquals(VIEW_NAME, modelAndView.getViewName());
            command = (PersonalSettingsCommand) modelAndView.getModelMap().get(Attributes.Model.Command.VALUE);

            assertTrue(command.isNowPlayingAllowed());
            assertFalse(command.isShowNowPlayingEnabled());
        }
    }
}
