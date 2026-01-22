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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.domain.system.PreferredFormatSheme;
import com.tesshu.jpsonic.spring.DataSourceConfigType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit test of {@link SettingsService}.
 *
 * @author Sindre Mehus
 */
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(NeedsHome.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidUsingHardCodedIP" })
class SettingsServiceTest {

    @Autowired
    private ApacheCommonsConfigurationService configurationService;
    @Autowired
    private SettingsService settingsService;

    @Test
    void testSetLocale() {
        Locale defaultValue = settingsService.getLocale();
        Locale dummyParam = Locale.CANADA_FRENCH;
        assertNotEquals(defaultValue, dummyParam);
        settingsService.setLocale(dummyParam);
        settingsService.save();
        assertEquals(dummyParam, settingsService.getLocale(), "Wrong locale.");
        settingsService.setLocale(defaultValue);
        settingsService.save();
    }

    @Test
    void testSetDatabaseConfigType() {
        DataSourceConfigType defaultValue = settingsService.getDatabaseConfigType();
        DataSourceConfigType dummyParam = DataSourceConfigType.JNDI;
        assertNotEquals(defaultValue, dummyParam);
        settingsService.setDatabaseConfigType(dummyParam);
        settingsService.save();
        assertEquals(dummyParam, settingsService.getDatabaseConfigType(), "Wrong locale.");
        settingsService.setDatabaseConfigType(defaultValue);
        settingsService.save();
    }

    @Test
    void testGeneralStringSetter() throws ExecutionException {
        Method[] methods = settingsService.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1
                    || method.getParameters()[0].getType() != String.class) {
                continue;
            }
            try {
                String setterName = method.getName();
                String getterName = setterName.replaceAll("^set", "get");
                Method getter = settingsService.getClass().getMethod(getterName, new Class<?>[0]);
                Object o = getter.invoke(settingsService);
                String defaultValue = o == null ? null : o.toString();
                String dummyParam = "dummy";
                assertNotEquals(defaultValue, dummyParam);
                method.invoke(settingsService, dummyParam);
                settingsService.save();
                o = getter.invoke(settingsService);
                String result = o == null ? null : o.toString();
                assertEquals(dummyParam, result);
                method.invoke(settingsService, defaultValue);
                settingsService.save();
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Test
    void testGeneralBooleanSetter() throws ExecutionException {
        Method[] methods = settingsService.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1
                    || method.getParameters()[0].getType() != boolean.class) {
                continue;
            }
            try {
                String setterName = method.getName();
                String getterName = setterName.replaceAll("^set", "is");
                Method getter = settingsService.getClass().getMethod(getterName, new Class<?>[0]);
                boolean defaultValue = Boolean
                    .parseBoolean(getter.invoke(settingsService).toString());
                boolean dummyParam = !defaultValue;
                assertNotEquals(defaultValue, dummyParam);
                method.invoke(settingsService, dummyParam);
                settingsService.save();
                boolean result = Boolean.parseBoolean(getter.invoke(settingsService).toString());
                assertEquals(dummyParam, result);
                method.invoke(settingsService, defaultValue);
                settingsService.save();
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Test
    void testGeneralIntSetter() throws ExecutionException {
        Method[] methods = settingsService.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1
                    || method.getParameters()[0].getType() != int.class) {
                continue;
            }
            try {
                String setterName = method.getName();
                String getterName = setterName.replaceAll("^set", "get");
                Method getter = settingsService.getClass().getMethod(getterName, new Class<?>[0]);
                int defaultValue = Integer.parseInt(getter.invoke(settingsService).toString());
                int dummyParam = defaultValue * 2;
                assertNotEquals(defaultValue, dummyParam);
                method.invoke(settingsService, dummyParam);
                settingsService.save();
                int result = Integer.parseInt(getter.invoke(settingsService).toString());
                assertEquals(dummyParam, result);
                method.invoke(settingsService, defaultValue);
                settingsService.save();
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Test
    void testGeneralLongSetter() throws ExecutionException {
        Method[] methods = settingsService.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1
                    || method.getParameters()[0].getType() != long.class) {
                continue;
            }
            try {
                String setterName = method.getName();
                String getterName = setterName.replaceAll("^set", "get");
                Method getter = settingsService.getClass().getMethod(getterName, new Class<?>[0]);
                long defaultValue = Long.parseLong(getter.invoke(settingsService).toString());
                long dummyParam = defaultValue * 2;
                method.invoke(settingsService, dummyParam);
                settingsService.save();
                long result = Long.parseLong(getter.invoke(settingsService).toString());
                assertEquals(dummyParam, result);
                method.invoke(settingsService, defaultValue);
                settingsService.save();
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Test
    void testJpsonicHome() {
        String homePath = System.getProperty("jpsonic.home");
        assertEquals(homePath, SettingsService.getJpsonicHome().toString(), "Wrong Jpsonic home.");
    }

    @Test
    void testIsScanOnBoot() {
        assertFalse(SettingsService.isScanOnBoot());
    }

    @Test
    void testGetDefaultJDBCPath() {
        assertNotNull(SettingsService.getDefaultJDBCPath());
    }

    @Test
    void testGetDefaultJDBCUrl() {
        assertNotNull(SettingsService.getDefaultJDBCUrl());
    }

    @Test
    void testGetDBScript() {
        assertNotNull(SettingsService.getDBScript());
    }

    @Test
    void testGetBackupDBScript() {
        Path backupDir = Path.of("./");
        assertEquals("./jpsonic.script", SettingsService.getBackupDBScript(backupDir));
    }

    @Test
    void testGetDefaultJDBCUsername() {
        assertEquals("sa", SettingsService.getDefaultJDBCUsername());
    }

    @Test
    void testGetDefaultJDBCPassword() {
        assertEquals("", SettingsService.getDefaultJDBCPassword());
    }

    @Test
    void testGetDefaultUPnPPort() {
        assertEquals(-1, SettingsService.getDefaultUPnPPort());
    }

    @Test
    void testGetLogFile() {
        assertNotNull(SettingsService.getLogFile());
    }

    @Test
    void testGetPropertyFile() {
        assertNotNull(SettingsService.getPropertyFile());
    }

    @Test
    void testGetDefaultIndexString() {
        assertEquals("""
                A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) \
                あ(ア) い(イ) う(ウ) え(エ) お(オ) か(カ) き(キ) く(ク) け(ケ) こ(コ) \
                さ(サ) し(シ) す(ス) せ(セ) そ(ソ) た(タ) ち(チ) つ(ツ) て(テ) と(ト) \
                な(ナ) に(ニ) ぬ(ヌ) ね(ネ) の(ノ) は(ハ) ひ(ヒ) ふ(フ) へ(ヘ) ほ(ホ) \
                ま(マ) み(ミ) む(ム) め(メ) も(モ) や(ヤ) ゆ(ユ) よ(ヨ) ら(ラ) り(リ) る(ル) れ(レ) ろ(ロ) わ(ワ) を(ヲ) ん(ン)\
                """, SettingsService.getDefaultIndexString());
    }

    @Test
    void testGetIndexString() {
        assertEquals("""
                A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) \
                あ(ア) い(イ) う(ウ) え(エ) お(オ) か(カ) き(キ) く(ク) け(ケ) こ(コ) \
                さ(サ) し(シ) す(ス) せ(セ) そ(ソ) た(タ) ち(チ) つ(ツ) て(テ) と(ト) \
                な(ナ) に(ニ) ぬ(ヌ) ね(ネ) の(ノ) は(ハ) ひ(ヒ) ふ(フ) へ(ヘ) ほ(ホ) \
                ま(マ) み(ミ) む(ム) め(メ) も(モ) や(ヤ) ゆ(ユ) よ(ヨ) ら(ラ) り(リ) る(ル) れ(レ) ろ(ロ) わ(ワ) を(ヲ) ん(ン)\
                """, settingsService.getIndexString());
    }

    @Test
    void testGetIgnoredArticles() {
        assertEquals("The El La Las Le Les", settingsService.getIgnoredArticles());
    }

    @Test
    void testGetIgnoredArticlesAsArray() {
        assertEquals(6, settingsService.getIgnoredArticlesAsArray().size());
    }

    @Test
    void testGetShortcuts() {
        assertEquals("\"New Incoming\" Podcast", settingsService.getShortcuts());
    }

    @Test
    void testGetShortcutsAsArray() {
        assertEquals(2, settingsService.getShortcutsAsArray().size());
    }

    @Test
    void testGetPlaylistFolder() {
        assertNotNull(settingsService.getPlaylistFolder());
    }

    @Test
    void testGetMusicFileTypes() {
        assertEquals(
                "mp3 ogg oga aac m4a m4b flac wav wma aif aiff aifc ape mpc shn mka opus dsf dsd",
                settingsService.getMusicFileTypes());
    }

    @Test
    void testGetMusicFileTypesAsArray() {
        assertEquals(19, settingsService.getMusicFileTypesAsArray().size());
        assertEquals(19, settingsService.getMusicFileTypesAsArray().size()); // Cashed path
    }

    @Test
    void testGetVideoFileTypes() {
        assertEquals("flv avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts webm",
                settingsService.getVideoFileTypes());
    }

    @Test
    void testGetVideoFileTypesAsArray() {
        assertEquals(13, settingsService.getVideoFileTypesAsArray().size());
        assertEquals(13, settingsService.getVideoFileTypesAsArray().size()); // Cashed path
    }

    @Test
    void testGetCoverArtFileTypes() {
        assertEquals("cover.jpg cover.png cover.gif folder.jpg jpg jpeg gif png",
                settingsService.getCoverArtFileTypes());
    }

    @Test
    void testGetCoverArtFileTypesAsArray() {
        assertEquals(8, settingsService.getCoverArtFileTypesAsArray().size());
        assertEquals(8, settingsService.getCoverArtFileTypesAsArray().size()); // Cashed path
    }

    @Test
    void testGetExcludedCoverArts() {
        assertEquals("AlbumArtSmall.jpg small.jpg large.jpg",
                settingsService.getExcludedCoverArts());
    }

    @Test
    void testGetExcludedCoverArtsAsArray() {
        assertEquals(3, settingsService.getExcludedCoverArtsAsArray().size());
        assertEquals(3, settingsService.getExcludedCoverArtsAsArray().size()); // Cashed path
    }

    @Test
    void testGetWelcomeTitle() {
        assertNotNull(settingsService.getWelcomeTitle());
    }

    @Test
    void testGetWelcomeSubtitle() {
        assertNull(settingsService.getWelcomeSubtitle());
    }

    @Test
    void testGetWelcomeMessage() {
        assertNull(settingsService.getWelcomeMessage());
    }

    @Test
    void testGetLoginMessage() {
        assertNull(settingsService.getLoginMessage());
    }

    @Test
    void testIsIgnoreFileTimestamps() {
        assertFalse(settingsService.isIgnoreFileTimestamps());
    }

    @Test
    void testGetIndexCreationInterval() {
        assertEquals(1, settingsService.getIndexCreationInterval(),
                "Wrong default index creation interval.");
    }

    @Test
    void testGetIndexCreationHour() {
        assertEquals(3, settingsService.getIndexCreationHour(),
                "Wrong default index creation hour.");
    }

    @Test
    void testGetPodcastUpdateInterval() {
        assertEquals(24, settingsService.getPodcastUpdateInterval(),
                "Wrong default Podcast update interval.");
    }

    @Test
    void testGetPodcastEpisodeRetentionCount() {
        assertEquals(10, settingsService.getPodcastEpisodeRetentionCount(),
                "Wrong default Podcast episode retention count.");
    }

    @Test
    void testGetPodcastEpisodeDownloadCount() {
        assertEquals(1, settingsService.getPodcastEpisodeDownloadCount(),
                "Wrong default Podcast episode download count.");
    }

    @Test
    void testGetPodcastFolder() {
        assertTrue(settingsService.getPodcastFolder().endsWith("Podcast"),
                "Wrong default Podcast folder.");
    }

    @Test
    void testGetDownloadBitrateLimit() {
        assertEquals(0, settingsService.getDownloadBitrateLimit());
    }

    @Test
    void testGetUploadBitrateLimit() {
        assertEquals(0, settingsService.getUploadBitrateLimit());
    }

    @Test
    void testGetBufferSize() {
        assertEquals(32_768, settingsService.getBufferSize());
    }

    @Test
    void testPreferredFormatShemeName() {
        assertEquals(PreferredFormatSheme.ANNOYMOUS.name(),
                settingsService.getPreferredFormatShemeName());
    }

    @Test
    void testPreferredFormatSheme() {
        assertEquals("mp3", settingsService.getPreferredFormat());
    }

    @Test
    void testGetHlsCommand() {
        assertEquals(
                "ffmpeg -ss %o -t %d -i %s -async 1 -b:v %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mpegts -c:v libx264 -preset superfast -c:a libmp3lame -threads 0 -",
                settingsService.getHlsCommand());
    }

    @Test
    void testIsLdapEnabled() {
        assertFalse(settingsService.isLdapEnabled(), "Wrong default LDAP enabled.");
    }

    @Test
    void testGetLdapUrl() {
        assertEquals("ldap://host.domain.com:389/cn=Users,dc=domain,dc=com",
                settingsService.getLdapUrl(), "Wrong default LDAP URL.");
    }

    @Test
    void testGetLdapSearchFilter() {
        assertEquals("(sAMAccountName={0})", settingsService.getLdapSearchFilter(),
                "Wrong default LDAP search filter.");
    }

    @Test
    void testGetLdapManagerDn() {
        assertNull(settingsService.getLdapManagerDn(), "Wrong default LDAP manager DN.");
    }

    @Test
    void testGetLdapManagerPassword() {
        assertNull(settingsService.getLdapManagerPassword(),
                "Wrong default LDAP manager password.");
    }

    @Test
    void testIsLdapAutoShadowing() {
        assertFalse(settingsService.isLdapAutoShadowing(), "Wrong default LDAP auto-shadowing.");
    }

    @Test
    void testIsGettingStartedEnabled() {
        assertTrue(settingsService.isGettingStartedEnabled());
    }

    @Test
    void testGetSettingsChanged() {
        assertNotEquals(0, settingsService.getSettingsChanged());
    }

    @Test
    void testIsSortAlbumsByYear() {
        assertTrue(settingsService.isSortAlbumsByYear());
    }

    @Test
    void testIsSortGenresByAlphabet() {
        assertTrue(settingsService.isSortGenresByAlphabet());
    }

    @Test
    void testIsProhibitSortVarious() {
        assertTrue(settingsService.isProhibitSortVarious());
    }

    @Test
    void testIsSearchComposer() {
        assertFalse(settingsService.isSearchComposer());
    }

    @Test
    void testIsOutputSearchQuery() {
        assertFalse(settingsService.isOutputSearchQuery());
    }

    @Test
    void testIsIgnoreSymLinks() {
        assertFalse(settingsService.isIgnoreSymLinks());
    }

    @Test
    void testGetExcludePatternString() {
        assertNull(settingsService.getExcludePatternString());
    }

    @Test
    void testGetExcludePattern() {
        assertNull(settingsService.getExcludePattern());
    }

    @Test
    void testIsDevelopmentMode() {
        assertFalse(SettingsService.isDevelopmentMode());
    }

    @Test
    void testGetRememberMeKey() {
        assertNull(settingsService.getRememberMeKey());
    }

    @Test
    void testGetLocale() {
        assertEquals("ja", settingsService.getLocale().getLanguage(), "Wrong default language.");
    }

    @Test
    void testGetThemeId() {
        assertEquals("jpsonic", settingsService.getThemeId(), "Wrong default theme.");
    }

    @Test
    void testGetAvailableThemes() {
        assertEquals(21, SettingsService.getAvailableThemes().size());
        assertEquals(21, SettingsService.getAvailableThemes().size()); // Cashed path
    }

    @Test
    void testGetAvailableLocales() {
        assertEquals(28, settingsService.getAvailableLocales().size());
        assertEquals(28, settingsService.getAvailableLocales().size()); // Cashed path
    }

    @Test
    void testGetBrand() {
        assertEquals("Jpsonic", SettingsService.getBrand());
    }

    @Test
    void testIsDlnaEnabled() {
        assertFalse(settingsService.isDlnaEnabled());
    }

    @Test
    void testGetDlnaServerName() {
        assertEquals("Jpsonic", settingsService.getDlnaServerName());
    }

    @Test
    void testGetDlnaBaseLANURL() {
        assertNull(settingsService.getDlnaBaseLANURL());
    }

    @Test
    void testIsUriWithFileExtensions() {
        assertTrue(settingsService.isUriWithFileExtensions());
    }

    @Test
    void testIsDlnaEnabledFilteredIp() {
        assertTrue(settingsService.isDlnaEnabledFilteredIp());
    }

    @Test
    void testGetDlnaFilteredIp() {
        assertEquals("172.17.16.1", settingsService.getDlnaFilteredIp());
    }

    @Test
    void testGetUPnPAlbumGenreSort() {
        assertEquals("FREQUENCY", settingsService.getUPnPAlbumGenreSort());
    }

    @Test
    void testGetUPnPSongGenreSort() {
        assertEquals("FREQUENCY", settingsService.getUPnPSongGenreSort());
    }

    @Test
    void testIsDlnaGuestPublish() {
        assertTrue(settingsService.isDlnaGuestPublish());
    }

    @Test
    void testGetDlnaRandomMax() {
        assertEquals(50, settingsService.getDlnaRandomMax());
    }

    @Test
    void testIsShowRememberMe() {
        assertFalse(settingsService.isShowRememberMe());
    }

    @Test
    void testIsSonosEnabled() {
        assertFalse(settingsService.isSonosEnabled());
    }

    @Test
    void testGetSonosServiceName() {
        assertEquals("Jpsonic", settingsService.getSonosServiceName());
    }

    @Test
    void testGetSmtpServer() {
        assertNull(settingsService.getSmtpServer());
    }

    @Test
    void testGetSmtpPort() {
        assertEquals("25", settingsService.getSmtpPort());
    }

    @Test
    void testIsUseRadio() {
        assertFalse(settingsService.isUseRadio());
    }

    @Test
    void testIsUseJsonp() {
        assertFalse(settingsService.isUseJsonp());
    }

    @Test
    void testIsShowIndexDetails() {
        assertFalse(settingsService.isShowIndexDetails());
    }

    @Test
    void testIsShowDBDetails() {
        assertFalse(settingsService.isShowDBDetails());
    }

    @Test
    void testIsUseCast() {
        assertFalse(settingsService.isUseCast());
    }

    @Test
    void testIsUsePartyMode() {
        assertFalse(settingsService.isUsePartyMode());
    }

    @Test
    void testGetSmtpEncryption() {
        assertEquals("None", settingsService.getSmtpEncryption());
    }

    @Test
    void testGetSmtpUser() {
        assertNull(settingsService.getSmtpUser());
    }

    @Test
    void testGetSmtpPassword() {
        assertNull(settingsService.getSmtpPassword());
    }

    @Test
    void testGetSmtpFrom() {
        assertEquals("jpsonic@tesshu.com", settingsService.getSmtpFrom());

    }

    @Test
    void testIsCaptchaEnabled() {
        assertFalse(settingsService.isCaptchaEnabled());
    }

    @Test
    void testGetRecaptchaSiteKey() {
        assertNotNull(settingsService.getRecaptchaSiteKey());
    }

    @Test
    void testGetRecaptchaSecretKey() {
        assertNotNull(settingsService.getRecaptchaSecretKey());
    }

    @Test
    void testIsUseScanLog() {
        assertFalse(settingsService.isUseScanLog());
    }

    @Test
    void testGetScanLogRetention() {
        assertEquals(-1, settingsService.getScanLogRetention());
    }

    @Test
    void testIsUseScanEvents() {
        assertFalse(settingsService.isUseScanEvents());
    }

    @Test
    void testIsMeasureMemory() {
        assertFalse(settingsService.isMeasureMemory());
    }

    @Test
    void testGetIndexSchemeName() {
        assertEquals(IndexScheme.NATIVE_JAPANESE.name(), settingsService.getIndexSchemeName());
    }

    @Test
    void testisForceInternalValueInsteadOfTags() {
        assertFalse(settingsService.isForceInternalValueInsteadOfTags());
    }

    @Test
    void testIsIgnoreFullWidth() {
        assertTrue(settingsService.isIgnoreFullWidth());
    }

    @Test
    void testIsDeleteDiacritic() {
        assertTrue(settingsService.isDeleteDiacritic());
    }

    @Test
    void testIsSortAlphanum() {
        assertTrue(settingsService.isSortAlphanum());
    }

    @Test
    void testIsSortStrict() {
        assertTrue(settingsService.isSortStrict());
    }

    @Test
    void testGetDatabaseConfigType() {
        assertEquals(DataSourceConfigType.HOST, settingsService.getDatabaseConfigType());
    }

    @Test
    void testGetDatabaseConfigEmbedDriver() {
        assertNull(settingsService.getDatabaseConfigEmbedDriver());
    }

    @Test
    void testGetDatabaseConfigEmbedUrl() {
        assertNull(settingsService.getDatabaseConfigEmbedUrl());
    }

    @Test
    void testGetDatabaseConfigEmbedUsername() {
        assertNull(settingsService.getDatabaseConfigEmbedUsername());
    }

    @Test
    void testGetDatabaseConfigEmbedPassword() {
        assertNull(settingsService.getDatabaseConfigEmbedPassword());
    }

    @Test
    void testGetDatabaseConfigJNDIName() {
        assertNull(settingsService.getDatabaseConfigJNDIName());
    }

    @Test
    void testGetDatabaseMysqlVarcharMaxlength() {
        assertEquals(512, settingsService.getDatabaseMysqlVarcharMaxlength());
    }

    @Test
    void testGetDatabaseUsertableQuote() {
        assertNull(settingsService.getDatabaseUsertableQuote());
    }

    @Test
    void testGetJWTKey() {
        assertNotNull(settingsService.getJWTKey());
    }

    @Test
    void testIsDefaultSortAlbumsByYear() {
        assertTrue(SettingsService.isDefaultSortAlbumsByYear());
    }

    @Test
    void testIsDefaultSortGenresByAlphabet() {
        assertTrue(SettingsService.isDefaultSortGenresByAlphabet());
    }

    @Test
    void testIsDefaultProhibitSortVarious() {
        assertTrue(SettingsService.isDefaultProhibitSortVarious());
    }

    @Test
    void testIsDefaultSortAlphanum() {
        assertTrue(SettingsService.isDefaultSortAlphanum());
    }

    @Test
    void testIsDefaultSortStrict() {
        assertTrue(SettingsService.isDefaultSortStrict());
    }

    @Nested
    class TestUPnPSubnet {

        private UPnPSubnet uPnPSubnet;
        private SettingsService service;

        @BeforeEach
        public void setup() {
            uPnPSubnet = mock(UPnPSubnet.class);
            service = new SettingsService(configurationService, uPnPSubnet);
        }

        @Test
        void testSetDlnaBaseLANURL() {
            service.setDlnaBaseLANURL("http://localhost:8080/jpsonic");
            Mockito.verify(uPnPSubnet, Mockito.times(1)).setDlnaBaseLANURL(Mockito.anyString());
        }

        @Test
        void testIsInUPnPRange() {
            service.isInUPnPRange("192.168.1.2");
            Mockito.verify(uPnPSubnet, Mockito.times(1)).isInUPnPRange(Mockito.anyString());
        }
    }
}
