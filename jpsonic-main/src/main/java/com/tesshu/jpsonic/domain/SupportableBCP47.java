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

    private static Map<String, SupportableBCP47> supportableLocales = new ConcurrentHashMap<>();

    static {
        // Add as needed
        supportableLocales.put("en-US", EN_US);
        supportableLocales.put("en-GB", EN_GB);
        supportableLocales.put("fr", FR);
        supportableLocales.put("es", ES);
        supportableLocales.put("ca", CA);
        supportableLocales.put("pt", PT);
        supportableLocales.put("de", DE);
        supportableLocales.put("it", IT);
        supportableLocales.put("el", EL);
        supportableLocales.put("ru", RU);
        supportableLocales.put("sl", SL);
        supportableLocales.put("mk", MK);
        supportableLocales.put("pl", PL);
        supportableLocales.put("bg", BG);
        supportableLocales.put("cs", CS);
        supportableLocales.put("zh-CN", ZH_CN);
        supportableLocales.put("zh-TW", ZH_TW);
        supportableLocales.put("ja-JP", JA_JP);
        supportableLocales.put("ko", KO);
        supportableLocales.put("nl", NL);
        supportableLocales.put("no", NO);
        supportableLocales.put("nn", NN);
        supportableLocales.put("sv", SV);
        supportableLocales.put("da", DA);
        supportableLocales.put("fi", FI);
        supportableLocales.put("is", IS);
        supportableLocales.put("et", ET);
    }

    private String value;

    SupportableBCP47(String n) {
        this.value = n;
    }

    public String getValue() {
        return this.value;
    }

    public static SupportableBCP47 valueOf(Locale locale) {
        SupportableBCP47 supportable = supportableLocales.get(locale.toLanguageTag());
        if (supportable == null) {
            return EN;
        }
        return supportable;
    }
}
