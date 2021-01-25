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

package com.tesshu.jpsonic.domain;

import java.util.Locale;

/**
 * BCP47 enumeration of speech recognition supported by Jpsonic. Currently, there are languages such as Norwegian that
 * cannot be simply replaced with Language Tags (for google). Therefore, it is used to replace the user locale of
 * personal settings with this enumeration and replace it with a supported BCP47.
 */
public enum SupportableBCP47 {

    EN("en-US"), EN_GB("en-GB"), EN_US("en-US"), FR("fr-FR"), ES("es-ES"), CA("ca-ES"), PT("pt-BR"), DE("de-DE"),
    IT("it-IT"), EL("el-GR"), RU("ru-RU"), SL("sl-SI"), MK("mk-MK"), PL("pl-PL"), BG("bg-BG"), CS("cs-CZ"), ZH_CN("zh"),
    ZH_TW("zh-TW"), JA_JP("ja-JP"), KO("ko-KR"), NL("nl-NL"), NO("no-NO"), NN("no-NO"), SV("sv-SE"), DA("da-DK"),
    FI("fi-FI"), IS("is-IS"), ET("et-EE");

    private String value;

    private SupportableBCP47(String n) {
        this.value = n;
    }

    public String getValue() {
        return this.value;
    }

    public static SupportableBCP47 valueOf(Locale locale) {
        if (locale == null || EN_US.name().equals(locale.toLanguageTag())) {
            return EN;
        } else if ("en-GB".equals(locale.toLanguageTag())) {
            return EN_GB;
        } else if ("fr".equals(locale.toLanguageTag())) {
            return FR;
        } else if ("es".equals(locale.toLanguageTag())) {
            return ES;
        } else if ("ca".equals(locale.toLanguageTag())) {
            return CA;
        } else if ("pt".equals(locale.toLanguageTag())) {
            return PT;
        } else if ("de".equals(locale.toLanguageTag())) {
            return DE;
        } else if ("it".equals(locale.toLanguageTag())) {
            return IT;
        } else if ("el".equals(locale.toLanguageTag())) {
            return EL;
        } else if ("ru".equals(locale.toLanguageTag())) {
            return RU;
        } else if ("sl".equals(locale.toLanguageTag())) {
            return SL;
        } else if ("mk".equals(locale.toLanguageTag())) {
            return MK;
        } else if ("pl".equals(locale.toLanguageTag())) {
            return PL;
        } else if ("bg".equals(locale.toLanguageTag())) {
            return BG;
        } else if ("cs".equals(locale.toLanguageTag())) {
            return CS;
        } else if ("zh-CN".equals(locale.toLanguageTag())) {
            return ZH_CN;
        } else if ("zh-TW".equals(locale.toLanguageTag())) {
            return ZH_TW;
        } else if ("zh-TW".equals(locale.toLanguageTag())) {
            return ZH_TW;
        } else if ("ja-JP".equals(locale.toLanguageTag())) {
            return JA_JP;
        } else if ("ko".equals(locale.toLanguageTag())) {
            return KO;
        } else if ("nl".equals(locale.toLanguageTag())) {
            return NL;
        } else if ("no".equals(locale.toLanguageTag())) {
            return NO;
        } else if ("nn".equals(locale.toLanguageTag())) {
            return NN;
        } else if ("sv".equals(locale.toLanguageTag())) {
            return SV;
        } else if ("da".equals(locale.toLanguageTag())) {
            return DA;
        } else if ("fi".equals(locale.toLanguageTag())) {
            return FI;
        } else if ("is".equals(locale.toLanguageTag())) {
            return IS;
        } else if ("et".equals(locale.toLanguageTag())) {
            return ET;
        }
        return EN;
    }
}
