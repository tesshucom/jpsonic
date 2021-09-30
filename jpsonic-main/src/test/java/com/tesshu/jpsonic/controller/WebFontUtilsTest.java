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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.PersonalSettingsCommand;
import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.catalina.connector.Request;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class WebFontUtilsTest {

    private static final String FONT_FACE_KEY = "viewhint.fontFace";
    private static final String FONT_FAMILY_KEY = "viewhint.fontFamily";
    private static final String FONT_SIZE_KEY = "viewhint.fontSize";

    private SecurityService securityService;

    @BeforeEach
    public void setup() {
        securityService = new SecurityService(mock(UserDao.class), mock(SettingsService.class),
                mock(MusicFolderService.class), null);
    }

    @Test
    @Order(1)
    void testSetToRequest() throws ExecutionException {

        UserSettings settings = securityService.getUserSettings("");

        // DEFAULT
        HttpServletRequest request = new Request(null);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        // JP_EMBED
        PersonalSettingsCommand command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(new UserSettings(""), command);
        command.setFontScheme(FontScheme.JP_EMBED);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY),
                "@font-face {" + "font-family: \"Kazesawa-Regular\";" + "src: "
                        + "url(\"/fonts/kazesawa/Kazesawa-Regular.woff\") format(\"woff\"), "
                        + "url(\"/fonts/kazesawa/Kazesawa-Regular.ttf\") format(\"truetype\");" + "}");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), WebFontUtils.DEFAULT_JP_FONT_SIZE);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY),
                WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY);

        // no settings(logon)
        request = new Request(null);
        WebFontUtils.setToRequest(null, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        // CUSTOM
        request = new Request(null);
        command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(securityService.getUserSettings(""), command);

        command.setFontScheme(FontScheme.CUSTOM);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), WebFontUtils.DEFAULT_FONT_SIZE);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY), WebFontUtils.DEFAULT_FONT_FAMILY);

        command.setFontScheme(FontScheme.JP_EMBED);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToCommand(settings, command);
        command.setFontScheme(FontScheme.CUSTOM);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), WebFontUtils.DEFAULT_JP_FONT_SIZE);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY),
                WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY);

        command.setFontFamily("Arial");
        command.setFontSize(20);
        WebFontUtils.setToSettings(command, settings);
        WebFontUtils.setToRequest(settings, request);
        assertEquals(request.getAttribute(FONT_FACE_KEY), "");
        assertEquals(request.getAttribute(FONT_SIZE_KEY), 20);
        assertEquals(request.getAttribute(FONT_FAMILY_KEY), "Arial");
    }

    @Test
    @Order(2)
    void testSetToCommand() throws ExecutionException {

        UserSettings from = securityService.getUserSettings("");

        PersonalSettingsCommand to = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(from, to);

        assertEquals(FontScheme.DEFAULT, to.getFontScheme());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        from.setFontSchemeName(FontScheme.JP_EMBED.name());
        from.setFontSize(20);
        from.setFontFamily("Arial");
        WebFontUtils.setToCommand(from, to);

        assertEquals(FontScheme.JP_EMBED, to.getFontScheme());
        assertEquals(20, to.getFontSize());
        assertEquals("Arial", to.getFontFamily());
    }

    @Test
    @Order(3)
    void testFormatFontFamily() {
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, WebFontUtils.formatFontFamily(WebFontUtils.DEFAULT_FONT_FAMILY));
        String jpFontfamily = WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY;
        assertEquals(jpFontfamily, WebFontUtils.formatFontFamily(jpFontfamily));
        assertEquals("", WebFontUtils.formatFontFamily(null));
        assertEquals("", WebFontUtils.formatFontFamily(""));
        assertEquals("", WebFontUtils.formatFontFamily(","));
        assertEquals("", WebFontUtils.formatFontFamily(", "));
        assertEquals("", WebFontUtils.formatFontFamily(" "));
        assertEquals("", WebFontUtils.formatFontFamily("\"\"\""));
        assertEquals("Arial", WebFontUtils.formatFontFamily("Arial"));
        assertEquals("Arial", WebFontUtils.formatFontFamily("\"Arial\""));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("\"Comic Sans\""));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("Comic Sans"));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("Comic\" Sans"));
        assertEquals("\"Comic Sans\"", WebFontUtils.formatFontFamily("C\\o\bm\fi\nc\" \rS\tans"));
        assertEquals("Arial, \"Comic Sans\"", WebFontUtils.formatFontFamily("Arial,C\\o\bm\fi\nc\" \rS\tans"));
        assertEquals("Arial, \"Comic Sans\"", WebFontUtils.formatFontFamily("Ari\"al,  Comi\"c Sans"));
    }

    @Test
    @Order(4)
    void testSetToSettings() throws ExecutionException {
        PersonalSettingsCommand command = new PersonalSettingsCommand();
        WebFontUtils.setToCommand(new UserSettings(""), command);

        @Unsigned
        UserSettings to = securityService.getUserSettings("");

        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.DEFAULT.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        // JP_EMBED
        command.setFontScheme(FontScheme.JP_EMBED);
        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.JP_EMBED.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE + 1, to.getFontSize());
        assertEquals(WebFontUtils.JP_FONT_NAME + ", " + WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());

        // Return to DEFAULT
        command.setFontScheme(FontScheme.DEFAULT);
        WebFontUtils.setToSettings(command, to);
        assertEquals(FontScheme.DEFAULT.name(), to.getFontSchemeName());
        assertEquals(WebFontUtils.DEFAULT_FONT_SIZE, to.getFontSize());
        assertEquals(WebFontUtils.DEFAULT_FONT_FAMILY, to.getFontFamily());
    }
}
