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
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.ibm.icu.text.Transliterator;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
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
    private static final String HYPHEN = "-";
    private static final String TILDE = "\uff5e"; // Special usage for Japanese
    private static final char WAVY_LINE = '\u007e'; // ~

    private final SettingsService settingsService;
    private final Tokenizer tokenizer;
    private final Map<String, String> readingMap;
    private final Map<String, String> truncatedReadingMap;

    private List<String> ignoredArticles;

    /**
     * It's a part of speech tag.
     */
    public enum Tag {
        INTERJECTION("感動詞"), POSTPOSITIONAL_PARTICLE("助詞"), CONJUNCTIVE_PARTICLE("接続助詞"),
        SENTENCE_ENDING_PARTICLE("終助詞"), ADVERBIAL_PARTICLE("副助詞"), MULTI_PARTICLE("副助詞／並立助詞／終助詞"), SYMBOL("記号"),
        ALPHABET("アルファベット"), COMMA("読点"), PERIOD("句点"), NOUN("名詞"), SUFFIX("接尾"), ANTHROPONYM("接尾"), VERB("動詞"),
        INDEPENDENCE("自立"), ADVERB("副詞"), UNUSED("未使用");

        private final String value;

        Tag(final String text) {
            this.value = text;
        }

        public static @NonNull Tag of(String name) {
            return Stream.of(Tag.values()).filter(t -> t.value.equals(name)).findAny().orElse(UNUSED);
        }
    }

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

    public void analyze(@NonNull Genre g) {
        g.setReading(createReading(defaultIfBlank(g.getName(), g.getReading())));
    }

    public void analyze(@NonNull MediaFile m) {
        m.setArtistSort(normalize(m.getArtistSort()));
        m.setArtistReading(createReading(m.getArtist(), m.getArtistSort()));
        m.setAlbumArtistSort(normalize(m.getAlbumArtistSort()));
        m.setAlbumArtistReading(createReading(m.getAlbumArtist(), m.getAlbumArtistSort()));
        m.setAlbumSort(normalize(m.getAlbumSort()));
        m.setAlbumReading(createReading(m.getAlbumName(), m.getAlbumSort()));
    }

    public void analyze(@NonNull Playlist p) {
        p.setReading(createReading(defaultIfBlank(p.getName(), p.getReading())));
    }

    public void analyze(@NonNull SortCandidate c) {
        c.setReading(createReading(c.getName(), c.getSort()));
        if (isEmpty(c.getSort())) {
            c.setSort(c.getReading());
        }
    }

    public void clear() {
        readingMap.clear();
        truncatedReadingMap.clear();
    }

    private boolean isJapaneseReadable(@Nullable String str) {
        if (isEmpty(str)) {
            return false;
        }
        return Stream.of(str.split(EMPTY)).anyMatch(s -> {
            Character.UnicodeBlock b = Character.UnicodeBlock.of(s.toCharArray()[0]);
            return Character.UnicodeBlock.HIRAGANA.equals(b) || Character.UnicodeBlock.KATAKANA.equals(b)
                    || Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS.equals(b)
                    || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(b)
                    || Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(b)
                    || settingsService.isReadGreekInJapanese() && Character.UnicodeBlock.GREEK.equals(b);
        });
    }

    private String createIgnoredArticles(@Nullable String s) {
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
            if (lower.startsWith(article.toLowerCase(settingsService.getLocale()) + SPACE)) {
                // reading = lower.substring(article.length() + 1) + ", " + article;
                result = result.substring(article.length() + 1);
            }
        }
        return result;
    }

    public String createIndexableName(@NonNull Artist artist) {
        if (settingsService.isIndexEnglishPrior() && isStartWithAlpha(artist.getName())) {
            return createIndexableName(artist.getName());
        } else if (isEmpty(artist.getReading())) {
            if (isEmpty(artist.getSort())) {
                return createIndexableName(artist.getName());
            }
            return createIndexableName(createReading(artist.getSort()));
        }
        return createIndexableName(artist.getReading());
    }

    public String createIndexableName(@NonNull MediaFile artist) {
        if (settingsService.isIndexEnglishPrior() && isStartWithAlpha(artist.getName())) {
            return createIndexableName(artist.getName());
        } else if (isEmpty(artist.getArtistReading())) {
            if (isEmpty(artist.getArtistSort())) {
                return createIndexableName(artist.getName());
            }
            return createIndexableName(createReading(artist.getArtistSort()));
        }
        return createIndexableName(artist.getArtistReading());
    }

    /**
     * This method returns the normalized Artist name that can also be used to create the index prefix.
     * 
     * @param sort
     *            artist's sort string
     * 
     * @return indexable Name
     */
    private String createIndexableName(@NonNull String sort) {
        String indexableName = sort;
        if (sort.charAt(0) > WAVY_LINE) {
            indexableName = transliterate(ID.TO_HALFWIDTH, indexableName);
            indexableName = transliterate(ID.TO_KATAKANA, indexableName);
        }
        // http://www.unicode.org/reports/tr15/
        indexableName = Normalizer.normalize(indexableName, Normalizer.Form.NFD);
        return indexableName;
    }

    /*
     * TransliteratorID
     */
    private enum ID {
        TO_HALFWIDTH("Fullwidth-Halfwidth"), TO_KATAKANA("Hiragana-Katakana"), TO_LATIN("Katakana-Latin");

        private final String value;

        ID(String name) {
            this.value = name;
        }

        public String getValue() {
            return this.value;
        }
    }

    final String transliterate(ID id, String text) {
        return Transliterator.getInstance(id.getValue()).transliterate(text);
    }

    static class ReadingResult {
        public final Token token;
        public final String reading;

        private ReadingResult(Token token, String reading) {
            super();
            this.token = token;
            this.reading = reading;
        }
    }

    private ReadingResult toReading(Token token) {
        return new ReadingResult(token, analyzeReading(token));
    }

    /*
     * Removes certain phoneme rules from the phoneme string and converts them to the Latin alphabet. This phoneme rule
     * is a morphological analyzer-specific specification.
     */
    private String romanize(String pronunciation) {
        /*
         * Remove unnecessary single quotes. "'" is granted to "ん(N)". e.g. はんなり(ha'nnari[phoneme] -> hannari[Latin]).
         */
        String result = pronunciation.replaceAll("n\'", "n");

        /*
         * Remove tildes. "Small kana used for diphthongs" or "Small tsu" is expressed in tildes.
         */

        int start = 0;
        while (true) {
            start = result.indexOf('~', start);
            if (start == -1 || start == result.length() - 1) {
                break;
            }
            String next = result.substring(start + 1, start + 2);
            if (Stream.of("a", "i", "u", "e", "o").anyMatch(s -> next.equals(s))) {
                // Small kana -> remove tildes.
                result = result.substring(0, start) + result.substring(start + 1);
            } else {
                start++;
            }
        }

        start = 0;
        String smallTsu = "~tsu";
        while (true) {
            start = result.indexOf(smallTsu, start);
            if (start == -1) {
                break;
            }
            /*
             * Small tsu -> The first consonant letter that comes next is superimposed, but when 'ch' continues next,
             * 't' is used without overlapping 'c'.
             */
            int end = start + smallTsu.length();
            String repeatable = "ch".equals(result.substring(end, Math.min(end + 2, result.length()))) ? "t"
                    : result.substring(end, Math.min(end + 1, result.length()));
            result = result.substring(0, start) + repeatable + result.substring(end);
        }

        /*
         * Remove macron.
         */
        result = result.replaceAll("Ā", "Aa");
        result = result.replaceAll("Ī", "Ii");
        result = result.replaceAll("Ū", "Uu");
        result = result.replaceAll("Ē", "Ee");
        result = result.replaceAll("Ō", "Oo");
        result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return result;
    }

    private String analyzeReading(Token token) {

        if (!settingsService.isReadGreekInJapanese() && Tag.of(token.getPartOfSpeechLevel1()) == Tag.SYMBOL
                && Tag.of(token.getPartOfSpeechLevel2()) == Tag.ALPHABET) {
            return token.getSurface();
        } else if (KATAKANA.matcher(token.getSurface()).matches() || ALPHA.matcher(token.getSurface()).matches()
                || ASTER.equals(token.getReading())) {
            return IndexScheme.of(settingsService.getIndexSchemeName()) == IndexScheme.ROMANIZED_JAPANESE
                    ? transliterate(ID.TO_LATIN, token.getSurface()) : token.getSurface();
        } else if (IndexScheme.of(settingsService.getIndexSchemeName()) != IndexScheme.ROMANIZED_JAPANESE) {
            return token.getReading();
        }

        final String pron = transliterate(ID.TO_LATIN, token.getPronunciation());
        final Tag level1 = Tag.of(token.getPartOfSpeechLevel1());
        final Tag level2 = Tag.of(token.getPartOfSpeechLevel2());

        switch (level1) {
        case INTERJECTION:
            return pron.concat(SPACE);
        case ADVERB:
            return pron.concat(SPACE);
        case POSTPOSITIONAL_PARTICLE:
            switch (level2) {
            case CONJUNCTIVE_PARTICLE:
                return pron.concat(SPACE);
            case ADVERBIAL_PARTICLE:
                return pron.concat(SPACE);
            case SENTENCE_ENDING_PARTICLE:
                return pron;
            case MULTI_PARTICLE:
                return pron;
            default:
                return SPACE.concat(pron).concat(SPACE);
            }
        case SYMBOL:
            switch (level2) {
            case COMMA:
                return pron.concat(SPACE);
            case PERIOD:
                return pron.concat(SPACE);
            default:
                return pron;
            }
        case NOUN:
            if (level2 == Tag.SUFFIX) {
                return HYPHEN.concat(pron);
            }
            break;
        default:
            break;
        }
        return pron;
    }

    private String createReading(@Nullable String line) {
        if (isEmpty(line)) {
            return null;
        } else if (readingMap.containsKey(line)) {
            return readingMap.get(line);
        }
        List<Token> tokens = tokenizer.tokenize(createIgnoredArticles(normalize(line)));
        List<ReadingResult> results = tokens.stream().map(this::toReading).collect(Collectors.toList());
        String reading;
        if (IndexScheme.of(settingsService.getIndexSchemeName()) == IndexScheme.ROMANIZED_JAPANESE) {
            StringBuffer buf = new StringBuffer(StringUtils.capitalize(results.get(0).reading));
            for (int pos = 1; pos < results.size(); pos++) {
                if (Tag.of(results.get(pos - 1).token.getPartOfSpeechLevel1()) == Tag.POSTPOSITIONAL_PARTICLE
                        && Tag.of(results.get(pos - 1).token.getPartOfSpeechLevel2()) != Tag.MULTI_PARTICLE
                        || results.get(pos - 1).reading.endsWith(SPACE)
                        || Tag.of(results.get(pos - 1).token.getPartOfSpeechLevel1()) == Tag.SYMBOL) {
                    buf.append(StringUtils.capitalize(results.get(pos).reading));
                } else {
                    buf.append(results.get(pos).reading);
                }
            }
            reading = romanize(buf.toString());
            if (!reading.isBlank()) {
                reading = reading.replaceAll("\\s+", SPACE).replaceAll(SPACE + "$", "");
            }
        } else {
            reading = results.stream().map(r -> r.reading).collect(Collectors.joining());
        }
        readingMap.put(line, reading);
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

    private boolean isJapaneseReading(@Nullable String str) {
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
    private boolean isStartWithAlpha(@Nullable String s) {
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
        while (-1 != i) {
            excluded.append(Normalizer.normalize(s.substring(start, i), Normalizer.Form.NFKC));
            excluded.append(TILDE);
            start = i + 1;
            i = s.indexOf(TILDE, i + 1);
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
    public String removePunctuationFromJapaneseReading(@Nullable String japaneseReading) {
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
