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

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.controller.CoverArtController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FontLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FontLoader.class);

    private static final Object FONT_LOCK = new Object();

    private final Ehcache fontCache;

    public FontLoader(Ehcache fontCache) {
        this.fontCache = fontCache;
    }

    public Font getFont(float fontSize) {
        Font font = null;
        String key = Float.toString(fontSize);
        synchronized (FONT_LOCK) {
            Element element = fontCache.get(key);
            if (element == null) {
                try (InputStream fontStream = CoverArtController.class
                        .getResourceAsStream("/fonts/kazesawa/Kazesawa-Regular.ttf")) {
                    font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(fontSize);
                } catch (IOException | FontFormatException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Failed to load font. ", e);
                    }
                    LOG.error("Failed to load font. ", e);
                } finally {
                    if (font == null) {
                        font = new Font(Font.SANS_SERIF, Font.BOLD, (int) fontSize);
                    }
                }
                fontCache.put(new Element(key, font));
            } else {
                return (Font) element.getObjectValue();
            }
        }
        return font;
    }
}
