package com.tesshu.jpsonic.service.search.analysis;

import com.tesshu.jpsonic.service.search.AnalyzerFactory;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class HiraganaStopFilter extends TokenFilter {

    private static Pattern ONLY_STOP_WORDS;

    private static final Pattern ONLY_HIRAGANA = Pattern.compile("^[\\u3040-\\u309F]+$");

    private static Reader getReafer(Class<?> clazz) {
        return IOUtils.getDecodingReader(clazz.getResourceAsStream("/".concat(AnalyzerFactory.STOP_WARDS)), UTF_8);
    }

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final boolean passableOnlyAllHiragana;

    public HiraganaStopFilter(TokenStream in) {
        super(in);
        passableOnlyAllHiragana = false;
    }

    public HiraganaStopFilter(TokenStream in, boolean passableOnlyAllHiragana) {
        super(in);
        this.passableOnlyAllHiragana = passableOnlyAllHiragana;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (null == ONLY_STOP_WORDS) {
            try (Reader reader = getReafer(getClass())) {
                CharArraySet stops = WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, true));
                StringBuffer buffer = new StringBuffer();
                stops.forEach(s -> {
                    buffer.append((char[]) s);
                    buffer.append("|");
                });
                ONLY_STOP_WORDS = Pattern.compile("^(" + buffer.toString().replaceAll("^\\||\\|$", "") + ")*$");
            } catch (IOException e) {
                LoggerFactory.getLogger(HiraganaStopFilter.class).error("Initialization error.", e);
            }
        }

        if (input.incrementToken()) {
            String term = termAtt.toString();
            if (ONLY_STOP_WORDS.matcher(term).matches() || (passableOnlyAllHiragana && ONLY_HIRAGANA.matcher(term).matches())) {
                return true;
            }
            input.end();
            return false;
        } else {
            input.end();
            return false;
        }
    }

}