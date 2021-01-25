/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */

package com.tesshu.jpsonic.controller;

import static org.springframework.util.StringUtils.hasLength;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.FontScheme;
import org.airsonic.player.command.PersonalSettingsCommand;
import org.airsonic.player.domain.UserSettings;
import org.apache.commons.lang3.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class WebFontUtils {

    public static final int DEFAULT_FONT_SIZE = 14;

    public static final int DEFAULT_JP_FONT_SIZE = 15;

    public static final String DEFAULT_FONT_FAMILY = "-apple-system, blinkMacSystemFont, \"Helvetica Neue\", \"Segoe UI\", \"Noto Sans JP\", YuGothicM, YuGothic, Meiryo, sans-serif";

    public static final String JP_FONT_NAME = "Kazesawa-Regular";

    static final String FONT_FACE_KEY = "viewhint.fontFace";

    static final String FONT_FAMILY_KEY = "viewhint.fontFamily";

    static final String FONT_SIZE_KEY = "viewhint.fontSize";

    static String formatFontFamily(String raw) {
        if (raw == null) {
            return "";
        }
        String escaped = raw;
        escaped = escaped.replace("\\", "").replace("\b", "").replace("\f", "").replace("\n", "").replace("\r", "")
                .replace("\t", "");
        String fonts[] = escaped.split(",");
        escaped = "";
        for (String font : fonts) {
            String fontEscaped = font.trim().replaceAll("\"", "");
            if (fontEscaped.contains(" ")) {
                fontEscaped = "\"".concat(fontEscaped).concat("\"");
            }
            if (hasLength(escaped)) {
                escaped = escaped.concat(", ");
            }
            escaped = escaped.concat(fontEscaped);
        }
        return escaped;
    }

    public static void setToCommand(UserSettings from, PersonalSettingsCommand to) {
        to.setFontSchemeName(from.getFontSchemeName());
        to.setFontSize(from.getFontSize());
        to.setFontFamily(from.getFontFamily());
    }

    public static void setToRequest(@Nullable UserSettings from, HttpServletRequest to) {
        if (ObjectUtils.isEmpty(from)) {
            to.setAttribute(FONT_FACE_KEY, "");
            to.setAttribute(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
            to.setAttribute(FONT_FAMILY_KEY, DEFAULT_FONT_FAMILY);
            return;
        }
        String fontFace = FontScheme.JP_EMBED.name().equals(from.getFontSchemeName()) // lgtm
                // [java/dereferenced-value-may-be-null]
                ? new StringBuilder("@font-face ").append('{').append("font-family: \"").append(JP_FONT_NAME)
                        .append("\";").append("src: ").append("url(\"").append(to.getContextPath())
                        .append("/fonts/kazesawa/Kazesawa-Regular.woff\") format(\"woff\")").append(", ")
                        .append("url(\"").append(to.getContextPath())
                        .append("/fonts/kazesawa/Kazesawa-Regular.ttf\") format(\"truetype\")").append(';').append('}')
                        .toString()
                : "";
        to.setAttribute(FONT_FACE_KEY, fontFace);
        to.setAttribute(FONT_SIZE_KEY, from.getFontSize());
        to.setAttribute(FONT_FAMILY_KEY, from.getFontFamily());
    }

    public static void setToSettings(PersonalSettingsCommand from, UserSettings to) {
        if (FontScheme.DEFAULT.name().equals(from.getFontSchemeName())) {
            to.setFontSchemeName(FontScheme.DEFAULT.name());
            to.setFontSize(DEFAULT_FONT_SIZE);
            to.setFontFamily(DEFAULT_FONT_FAMILY);
        } else if (FontScheme.JP_EMBED.name().equals(from.getFontSchemeName())) {
            to.setFontSchemeName(FontScheme.JP_EMBED.name());
            to.setFontSize(DEFAULT_JP_FONT_SIZE);
            to.setFontFamily(JP_FONT_NAME.concat(", ").concat(DEFAULT_FONT_FAMILY));
        } else if (FontScheme.CUSTOM.name().equals(from.getFontSchemeName())) {
            to.setFontSchemeName(FontScheme.CUSTOM.name());
            to.setFontSize(from.getFontSize());
            to.setFontFamily(formatFontFamily(from.getFontFamily()));
        }
    }

    private WebFontUtils() {
    }
}
