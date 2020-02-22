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

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.domain;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.ibm.icu.text.Transliterator;

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Provide analysis of Japanese name.
 */
@Component
@DependsOn({ "settingsService" })
public class JapaneseReadingUtils {

    private final SettingsService settingsService;

    public static final Pattern ALPHA = Pattern.compile("^[a-zA-Zａ-ｚＡ-Ｚ]+$");
    private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF]+$");
    private static final String ASTER = "*";
    private final Tokenizer tokenizer = new Tokenizer();
    private Map<String, String> readingMap = new HashMap<>();

    public JapaneseReadingUtils(SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
    }

    /* AtoZ only true. */
    private boolean isStartWithAlpha(String s) {
        if (isEmpty(s)) {
            return false;
        }
        return ALPHA.matcher(s.substring(0, 1)).matches();
    }

    private final Collector<String, StringBuilder, String> join = Collector.of(StringBuilder::new,
            StringBuilder::append, StringBuilder::append, StringBuilder::toString);

    private final Function<Token, String> readingAnalysis = token -> {
        if (KATAKANA.matcher(token.getSurface()).matches()
                || ALPHA.matcher(token.getSurface()).matches()
                || ASTER.equals(token.getReading())) {
            return token.getSurface();
        }
        return token.getReading();
    };
    
    private List<String> ignoredArticles;

    private List<String> getIgnoredArticles() {
        if (ObjectUtils.isEmpty(ignoredArticles)) {
            ignoredArticles = Arrays.asList(settingsService.getIgnoredArticles().split("\\s+"));
        }
        return ignoredArticles;
    }

    private String createIgnoredArticles(String s) {
        if (isEmpty(s)) {
            return null;
        }
        /* @see MusicIndexService#createSortableName */
        String lower = s.toLowerCase();
        String result = s;
        for (String article : getIgnoredArticles()) {
            if (lower.startsWith(article.toLowerCase() + " ")) {
                // reading = lower.substring(article.length() + 1) + ", " + article;
                result = result.substring(article.length() + 1);
            }
        }
        return result;
    }

    String createReading(String s) {
        if (isEmpty(s)) {
            return null;
        }
        if (readingMap.containsKey(s)) {
            return readingMap.get(s);
        }
        List<Token> tokens = tokenizer.tokenize(Normalizer.normalize(s, Normalizer.Form.NFKC));
        String reading = tokens.stream().map(readingAnalysis).collect(join);
        reading = createIgnoredArticles(reading);
        readingMap.put(s, reading);
        return reading;
    }

    String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }

    public void analyze(MediaFile m) {

        m.setArtistSort(isEmpty(m.getArtistSort()) ? null : normalize(m.getArtistSort()));
        String artist = createIgnoredArticles(m.getArtist());
        if (isStartWithAlpha(artist)) {
            m.setArtistReading(createReading(artist));
        } else {
            m.setArtistReading(createReading(isEmpty(m.getArtistSort()) ? artist : m.getArtistSort()));
        }

        m.setAlbumArtistSort(isEmpty(m.getAlbumArtistSort()) ? null : normalize(m.getAlbumArtistSort()));
        String albumArtist = createIgnoredArticles(m.getAlbumArtist());
        if (isStartWithAlpha(albumArtist)) {
            m.setAlbumArtistReading(createReading(albumArtist));
        } else {
            m.setAlbumArtistReading(createReading(isEmpty(m.getAlbumArtistSort()) ? albumArtist : m.getAlbumArtistSort()));
        }

        m.setAlbumSort(isEmpty(m.getAlbumSort()) ? null : normalize(m.getAlbumSort()));
        String album = createIgnoredArticles(m.getAlbumName());
        if (isStartWithAlpha(album)) {
            m.setAlbumReading(createReading(album));
        } else {
            m.setAlbumReading(createReading(isEmpty(m.getAlbumSort()) ? album : m.getAlbumSort()));
        }

    }
    
    public void analyze(Playlist p) {
        p.setReading(isEmpty(p.getReading()) ? createReading(p.getName()) : p.getReading());
    }

    public void analyze(Genre g) {
        g.setReading(isEmpty(g.getReading()) ? createReading(g.getName()) : g.getReading());
    }

    public void clear() {
        readingMap.clear();
    }

    /**
     * This method returns the normalized Artist name that can also be used to create the index prefix.
     * @param mediaFile
     * @return indexable Name
     */
    private String createIndexableName(String s) {
        String indexableName = s;
        char c = s.charAt(0);
        if (!(c <= '\u007e') || (c == '\u00a5') || (c == '\u203e')) {
            indexableName = Transliterator.getInstance("Fullwidth-Halfwidth").transliterate(indexableName);
            indexableName = Transliterator.getInstance("Hiragana-Katakana").transliterate(indexableName);
        }
        // http://www.unicode.org/reports/tr15/
        indexableName = Normalizer.normalize(indexableName, Normalizer.Form.NFD);
        return indexableName;
    }

    public String createIndexableName(MediaFile mediaFile) {
        String indexableName = mediaFile.getName();
        if (settingsService.isIndexEnglishPrior() && isStartWithAlpha(mediaFile.getName())) {
            indexableName = mediaFile.getName();
        } else if (!isEmpty(mediaFile.getArtistReading())) {
            indexableName = mediaFile.getArtistReading();
        } else if (!isEmpty(mediaFile.getArtistSort())) {
            indexableName = createIndexableName(createReading(mediaFile.getArtistSort()));
        }
        return createIndexableName(indexableName);
    }

    public String createIndexableName(Artist artist) {
        String indexableName = artist.getName();
        if (settingsService.isIndexEnglishPrior() && isStartWithAlpha(artist.getName())) {
            indexableName = artist.getName();
        } else if (!isEmpty(artist.getReading())) {
            indexableName = artist.getReading();
        } else if (!isEmpty(artist.getSort())) {
            indexableName = createIndexableName(createReading(artist.getSort()));
        }
        return createIndexableName(indexableName);
    }

    public List<MediaFile> createArtistSortToBeUpdate(List<MediaFile> candidates) {
        List<MediaFile> toBeUpdate = new ArrayList<>();
        for (MediaFile candidate : candidates) {
            if (!candidate.getArtistReading().equals(candidate.getArtistSort())) {
                candidate.setId(candidate.getId());
                candidate.setArtistSort(candidate.getArtistSort());
                toBeUpdate.add(candidate);
            }
        }
        return toBeUpdate;
    }

    public List<MediaFile> createAlbumSortToBeUpdate(List<MediaFile> candidates) {
        List<MediaFile> toBeUpdate = new ArrayList<>();
        for (MediaFile candidate : candidates) {
            if (!candidate.getAlbumReading().equals(candidate.getAlbumSort())) {
                candidate.setAlbumSort(candidate.getAlbumSort());
                toBeUpdate.add(candidate);
            }
        }
        return toBeUpdate;
    }

}
