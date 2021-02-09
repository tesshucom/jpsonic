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

package com.tesshu.jpsonic.service.search.analysis;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.LoggerFactory;

public final class ComplementaryFilter extends TokenFilter {

    private static final AtomicBoolean STOPWORD_LOADED = new AtomicBoolean();
    private static final Object LOCK = new Object();

    private static Pattern onlyStopWords;

    private final Mode mode;
    private final String stopwards;
    private final CharTermAttribute termAtt;

    private Reader getReafer(Class<?> clazz) {
        if (stopwards != null) {
            return IOUtils.getDecodingReader(clazz.getResourceAsStream("/".concat(stopwards)), UTF_8);
        }
        return null;
    }

    public enum Mode {
        STOP_WORDS_ONLY("swo"), STOP_WORDS_ONLY_AND_HIRA_KATA_ONLY("swoahka"), HIRA_KATA_ONLY("hko");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Optional<Mode> fromValue(final String value) {
            return Stream.of(Mode.values()).filter(m -> m.value.equals(value)).findFirst();
        }

    }

    public ComplementaryFilter(@NonNull TokenStream in, @NonNull Mode mode, @Nullable String stopwards) {
        super(in);
        this.mode = mode;
        this.stopwards = stopwards;
        termAtt = addAttribute(CharTermAttribute.class);
        if (mode != Mode.HIRA_KATA_ONLY && null == this.stopwards) {
            throw new IllegalArgumentException("Stopwards not specified.");
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!STOPWORD_LOADED.get() && Mode.HIRA_KATA_ONLY != mode) {
            synchronized (LOCK) {
                try (Reader reader = getReafer(getClass())) {
                    CharArraySet stops = WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, true));
                    StringBuffer buffer = new StringBuffer();
                    stops.forEach(s -> {
                        buffer.append((char[]) s);
                        buffer.append('|');
                    });
                    onlyStopWords = Pattern.compile("^(" + buffer.toString().replaceAll("^\\||\\|$", "") + ")*$");
                } catch (IOException e) {
                    LoggerFactory.getLogger(ComplementaryFilter.class).error("Initialization error.", e);
                }
                STOPWORD_LOADED.set(true);
            }
        }

        if (input.incrementToken()) {
            String term = termAtt.toString();

            if (Mode.HIRA_KATA_ONLY != mode && onlyStopWords.matcher(term).matches()) {
                return true;
            }
            if (Mode.STOP_WORDS_ONLY != mode && isHiraKataOnly(term)) {
                return true;
            }
        }
        input.end();
        return false;
    }

    private boolean isHiraKataOnly(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return Stream.of(str.split(EMPTY)).allMatch(s -> {
            Character.UnicodeBlock b = Character.UnicodeBlock.of(s.toCharArray()[0]);
            return Character.UnicodeBlock.HIRAGANA.equals(b) || Character.UnicodeBlock.KATAKANA.equals(b);
        });
    }

}
