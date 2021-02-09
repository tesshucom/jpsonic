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

package com.tesshu.jpsonic.domain;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.checkerframework.checker.nullness.qual.Nullable;

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

    private static final Map<String, SupportableBCP47> SUPPORTABLE_LOCALES;

    static {
        SUPPORTABLE_LOCALES = new ConcurrentHashMap<>();

        // Add as needed
        SUPPORTABLE_LOCALES.put("en-US", EN_US);
        SUPPORTABLE_LOCALES.put("en-GB", EN_GB);
        SUPPORTABLE_LOCALES.put("fr", FR);
        SUPPORTABLE_LOCALES.put("es", ES);
        SUPPORTABLE_LOCALES.put("ca", CA);
        SUPPORTABLE_LOCALES.put("pt", PT);
        SUPPORTABLE_LOCALES.put("de", DE);
        SUPPORTABLE_LOCALES.put("it", IT);
        SUPPORTABLE_LOCALES.put("el", EL);
        SUPPORTABLE_LOCALES.put("ru", RU);
        SUPPORTABLE_LOCALES.put("sl", SL);
        SUPPORTABLE_LOCALES.put("mk", MK);
        SUPPORTABLE_LOCALES.put("pl", PL);
        SUPPORTABLE_LOCALES.put("bg", BG);
        SUPPORTABLE_LOCALES.put("cs", CS);
        SUPPORTABLE_LOCALES.put("zh-CN", ZH_CN);
        SUPPORTABLE_LOCALES.put("zh-TW", ZH_TW);
        SUPPORTABLE_LOCALES.put("ja-JP", JA_JP);
        SUPPORTABLE_LOCALES.put("ko", KO);
        SUPPORTABLE_LOCALES.put("nl", NL);
        SUPPORTABLE_LOCALES.put("no", NO);
        SUPPORTABLE_LOCALES.put("nn", NN);
        SUPPORTABLE_LOCALES.put("sv", SV);
        SUPPORTABLE_LOCALES.put("da", DA);
        SUPPORTABLE_LOCALES.put("fi", FI);
        SUPPORTABLE_LOCALES.put("is", IS);
        SUPPORTABLE_LOCALES.put("et", ET);
    }

    private final String value;

    SupportableBCP47(String n) {
        this.value = n;
    }

    public String getValue() {
        return this.value;
    }

    public static SupportableBCP47 valueOf(@Nullable Locale locale) {
        if (locale == null) {
            return EN;
        }
        SupportableBCP47 supportable = SUPPORTABLE_LOCALES.get(locale.toLanguageTag());
        if (supportable == null) {
            return EN;
        }
        return supportable;
    }
}
