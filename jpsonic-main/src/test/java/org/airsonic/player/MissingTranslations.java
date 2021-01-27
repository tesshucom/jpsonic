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

package org.airsonic.player;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissingTranslations {

    private static final Logger LOG = LoggerFactory.getLogger(MissingTranslations.class);

    public static void main(String[] args) throws IOException {
        String[] locales = { "da", "de", "es", "pt", "fi", "fr", "is", "it", "ja_JP", "mk", "nl", "no", "pl", "ru",
                "sl", "sv", "zh_CN", "zh_TW" };

        for (String locale : locales) {
            diff(locale, "en");
            // diff("en", locale);
        }
    }

    private static void diff(String locale1, String locale2) throws IOException {
        Properties en = new Properties();
        en.load(MissingTranslations.class
                .getResourceAsStream("/org/airsonic/player/i18n/ResourceBundle_" + locale1 + ".properties"));
        SortedMap<Object, Object> enSorted = new TreeMap<>(en);

        Properties mk = new Properties();
        mk.load(MissingTranslations.class
                .getResourceAsStream("/org/airsonic/player/i18n/ResourceBundle_" + locale2 + ".properties"));

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nMessages present in locale " + locale1 + " and missing in locale " + locale2 + ":");
        }
        int count = 0;
        for (Map.Entry<Object, Object> entry : enSorted.entrySet()) {
            if (!mk.containsKey(entry.getKey())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(entry.getKey() + " = " + entry.getValue());
                }
                count++;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nTotal: " + count);
        }
    }
}
