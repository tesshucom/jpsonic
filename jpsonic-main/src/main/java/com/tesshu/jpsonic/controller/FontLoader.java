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
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.exception.UncheckedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FontLoader {

    private final Ehcache fontCache;
    private final ReentrantLock fontLock = new ReentrantLock();

    public FontLoader(@Qualifier("fontCache") Ehcache fontCache) {
        this.fontCache = fontCache;
    }

    public boolean isEmbeddedFonts() {
        return Optional.ofNullable(System.getProperty("jpsonic.embeddedfont")).map(Boolean::parseBoolean).orElse(false);
    }

    public @NonNull Font getFont(float fontSize) {
        Font font = null;
        String key = Float.toString(fontSize);
        fontLock.lock();
        try {
            Element element = fontCache.get(key);
            if (element == null) {
                if (isEmbeddedFonts()) {
                    font = getFontFromResource(fontSize);
                }
                if (font == null) {
                    font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
                }
                fontCache.put(new Element(key, font));
            } else {
                return (Font) element.getObjectValue();
            }
        } finally {
            fontLock.unlock();
        }
        return font;
    }

    /*
     * Font stream may not be obtained except for general OpenJDK (based on Windows or Linux glibc). It is NonNull on
     * the Docker image provided by Jpsonic.
     */
    private @NonNull Font getFontFromResource(float fontSize) {
        Font font;
        try (InputStream fontStream = FontLoader.class.getResourceAsStream("/fonts/kazesawa/Kazesawa-Regular.ttf")) {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(fontSize);
        } catch (IOException | FontFormatException e) {
            throw new UncheckedException(e);
        }
        return font;
    }
}
