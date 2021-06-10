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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.ibm.icu.text.Transliterator;
import com.tesshu.jpsonic.service.SettingsService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * Provide analysis of Japanese name.
 */
@Component
@DependsOn({ "settingsService" })
public class JapaneseReadingUtils {

    public static final Pattern ALPHA = Pattern.compile("^[a-zA-Zａ-ｚＡ-Ｚ]+$");
    private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF]+$");
    private static final String ASTER = "*";
    private static final String TILDE = "\uff5e"; // Special usage for Japanese
    private static final char WAVY_LINE = '\u007e'; // ~

    private final SettingsService settingsService;
    private final Tokenizer tokenizer;
    private final Map<String, String> readingMap;
    private final Map<String, String> truncatedReadingMap;

    private List<String> ignoredArticles;

    public static boolean isPunctuation(char ch) {
        switch (Character.getType(ch)) {
        case Character.SPACE_SEPARATOR:
        case Character.LINE_SEPARATOR:
        case Character.PARAGRAPH_SEPARATOR:
        case Character.CONTROL:
        case Character.FORMAT:
        case Character.DASH_PUNCTUATION:
        case Character.START_PUNCTUATION:
        case Character.END_PUNCTUATION:
        case Character.CONNECTOR_PUNCTUATION:
        case Character.OTHER_PUNCTUATION:
        case Character.MATH_SYMBOL:
        case Character.CURRENCY_SYMBOL:
        case Character.MODIFIER_SYMBOL:
        case Character.OTHER_SYMBOL:
        case Character.INITIAL_QUOTE_PUNCTUATION:
        case Character.FINAL_QUOTE_PUNCTUATION:
            return true;
        default:
            return false;
        }
    }

    public JapaneseReadingUtils(SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
        tokenizer = new Tokenizer();
        readingMap = new ConcurrentHashMap<>();
        truncatedReadingMap = new ConcurrentHashMap<>();
    }

    public void analyze(Genre g) {
        g.setReading(createReading(defaultIfBlank(g.getName(), g.getReading())));
    }

    public void analyze(MediaFile m) {
        m.setArtistSort(normalize(m.getArtistSort()));
        m.setArtistReading(createReading(m.getArtist(), m.getArtistSort()));
        m.setAlbumArtistSort(normalize(m.getAlbumArtistSort()));
        m.setAlbumArtistReading(createReading(m.getAlbumArtist(), m.getAlbumArtistSort()));
        m.setAlbumSort(normalize(m.getAlbumSort()));
        m.setAlbumReading(createReading(m.getAlbumName(), m.getAlbumSort()));
    }

    public void analyze(Playlist p) {
        p.setReading(createReading(defaultIfBlank(p.getName(), p.getReading())));
    }

    public void analyze(SortCandidate c) {
        c.setReading(createReading(c.getName(), c.getSort()));
        if (isEmpty(c.getSort())) {
            c.setSort(c.getReading());
        }
    }

    public void clear() {
        readingMap.clear();
        truncatedReadingMap.clear();
    }

    /**
     * 
     */
    private boolean isJapaneseReadable(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return Stream.of(str.split(EMPTY)).anyMatch(s -> {
            Character.UnicodeBlock b = Character.UnicodeBlock.of(s.toCharArray()[0]);
            return Character.UnicodeBlock.HIRAGANA.equals(b) || Character.UnicodeBlock.KATAKANA.equals(b)
                    || Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS.equals(b)
                    || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(b)
                    || Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(b)
                    || Character.UnicodeBlock.GREEK.equals(b);
        });
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

    private String createIgnoredArticles(String s) {
        if (isEmpty(s)) {
            return null;
        }
        /* @see MusicIndexService#createSortableName */
        String lower = s.toLowerCase(settingsService.getLocale());
        String result = s;
        if (ObjectUtils.isEmpty(ignoredArticles)) {
            ignoredArticles = Arrays.asList(settingsService.getIgnoredArticles().split("\\s+"));
        }
        for (String article : ignoredArticles) {
            if (lower.startsWith(article.toLowerCase(settingsService.getLocale()) + " ")) {
                // reading = lower.substring(article.length() + 1) + ", " + article;
                result = result.substring(article.length() + 1);
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
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

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
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

    /**
     * This method returns the normalized Artist name that can also be used to create the index prefix.
     * 
     * @param sort
     *            artist's sort string
     * 
     * @return indexable Name
     */
    private String createIndexableName(String sort) {
        String indexableName = sort;
        if (sort.charAt(0) > WAVY_LINE) {
            indexableName = Transliterator.getInstance("Fullwidth-Halfwidth").transliterate(indexableName);
            indexableName = Transliterator.getInstance("Hiragana-Katakana").transliterate(indexableName);
        }
        // http://www.unicode.org/reports/tr15/
        indexableName = Normalizer.normalize(indexableName, Normalizer.Form.NFD);
        return indexableName;
    }

    private String createReading(String s) {
        if (isEmpty(s)) {
            return null;
        }
        if (readingMap.containsKey(s)) {
            return readingMap.get(s);
        }
        List<Token> tokens = tokenizer.tokenize(normalize(s));

        final Collector<String, StringBuilder, String> join = Collector.of(StringBuilder::new, StringBuilder::append,
                StringBuilder::append, StringBuilder::toString);

        final Function<Token, String> readingAnalysis = token -> {
            if (KATAKANA.matcher(token.getSurface()).matches() || ALPHA.matcher(token.getSurface()).matches()
                    || ASTER.equals(token.getReading())) {
                return token.getSurface();
            }
            return token.getReading();
        };

        String reading = createIgnoredArticles(tokens.stream().map(readingAnalysis).collect(join));
        readingMap.put(s, reading);
        return reading;
    }

    private @NonNull String createReading(@NonNull String name, @Nullable String sort) {
        String n = createIgnoredArticles(name);
        String s = createIgnoredArticles(sort);
        String reading;
        if (isStartWithAlpha(n)) {
            if (isStartWithAlpha(s) && isJapaneseReadable(s)) {
                reading = createReading(s);
            } else {
                reading = createReading(n);
            }
        } else {
            reading = createReading(defaultIfBlank(s, n));
        }
        return reading;
    }

    private boolean isJapaneseReading(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return Stream.of(str.split(EMPTY)).anyMatch(s -> {
            Character.UnicodeBlock b = Character.UnicodeBlock.of(s.toCharArray()[0]);
            return Character.UnicodeBlock.HIRAGANA.equals(b) || Character.UnicodeBlock.KATAKANA.equals(b)
                    || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(b)
                    || Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS.equals(b)
                            && s.chars().anyMatch(c -> 65_382 <= c && c <= 65_437);
        });
    }

    /* AtoZ only true. */
    private boolean isStartWithAlpha(String s) {
        if (isEmpty(s)) {
            return false;
        }
        return ALPHA.matcher(s.substring(0, 1)).matches();
    }

    /**
     * There is no easy way to normalize Japanese words. Uses relatively natural NFKC, eliminates over-processing and
     * adds under-processing.
     */
    private String normalize(@Nullable String s) {
        if (isEmpty(s)) {
            return null;
        }
        // Normalize except for certain strings with NFKC
        StringBuilder excluded = new StringBuilder();
        int start = 0;
        int i = s.indexOf(TILDE);
        if (-1 != i) {
            while (-1 != i) {
                excluded.append(Normalizer.normalize(s.substring(start, i), Normalizer.Form.NFKC));
                excluded.append(TILDE);
                start = i + 1;
                i = s.indexOf(TILDE, i + 1);
            }
        }
        excluded.append(Normalizer.normalize(s.substring(start), Normalizer.Form.NFKC));

        // Convert certain strings additionally
        String expanded = excluded.toString();
        expanded = expanded.replaceAll("\u300c", "\uff62"); // Japanese braces
        expanded = expanded.replaceAll("\u300d", "\uff63"); // Japanese braces
        return expanded;
    }

    /**
     * Delete a specific Punctuation. This result value is not persisted in DB.
     * 
     * @param japaneseReading
     *            string after analysis
     */
    public String removePunctuationFromJapaneseReading(String japaneseReading) {
        if (isJapaneseReading(japaneseReading)) {
            if (truncatedReadingMap.containsKey(japaneseReading)) {
                return truncatedReadingMap.get(japaneseReading);
            }
            StringBuilder b = new StringBuilder();
            for (char c : japaneseReading.toCharArray()) {
                if (!isPunctuation(c)) {
                    b.append(c);
                }
            }
            String truncatedReading = b.toString();
            truncatedReadingMap.put(japaneseReading, truncatedReading);
            return truncatedReading;
        }
        return japaneseReading;
    }

}
