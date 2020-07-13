package com.tesshu.jpsonic.controller;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.airsonic.player.controller.CoverArtController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

@Component
public class FontLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FontLoader.class);

    private final Ehcache fontCache;
    
    public FontLoader(Ehcache fontCache) {
        this.fontCache = fontCache;
    }

    private static final Object FONT_LOCK = new Object();

    public Font getFont(float fontSize) {
        Font font = null;
        String key = Float.toString(fontSize);
        synchronized (FONT_LOCK) {
            Element element = fontCache.get(key);
            if (element == null) {
                try (InputStream fontStream = CoverArtController.class
                        .getResourceAsStream("/fonts/Kazesawa-Regular.ttf")) {
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
