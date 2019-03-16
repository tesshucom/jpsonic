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
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.tesshu.jpsonic.service.search.IndexType.isHiraOrKata;

/**
 * Analyzer provider.
 * This class is a division of what was once part of SearchService
 * and added functionality.
 * 
 * Analyzer can be closed but is a reuse premise.
 * It is held in this class.
 * 
 * Some of the query parses performed in legacy services are defined in QueryAnalyzer.
 * Most of the actual query parsing is done with QueryFactory.
 */
public final class AnalyzerFactory {

  private static AnalyzerFactory instance;

  private static final Logger LOG = LoggerFactory.getLogger(AnalyzerFactory.class);

  /**
   * Returns an instance of AnalyzerFactory.
   * @return AnalyzerFactory instance
   */
  public static AnalyzerFactory getInstance() {
    if (null == instance) {
      synchronized (AnalyzerFactory.class) {
        if (instance == null) {
          instance = new AnalyzerFactory();
        }
      }
    }
    return instance;
  }

  private Analyzer analyzer;

  private Analyzer queryAnalyzer;

  private Analyzer keywordAnalyzer;

  private Analyzer keywordQueryAnalyzer;

  private Analyzer hiraKataQueryAnalyzer;

  private final String stopTags = "org/apache/lucene/analysis/ja/stoptags.txt";

  private final String stopWords = "com/tesshu/jpsonic/service/stopwords.txt";

  private AnalyzerFactory() {
  }

  private CustomAnalyzer.Builder addTokenFilters(CustomAnalyzer.Builder builder) {
    try {
      builder
          .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", stopTags)
          .addTokenFilter(LowerCaseFilterFactory.class)
          .addTokenFilter(CJKWidthFilterFactory.class) // before StopFilter
          .addTokenFilter(StopFilterFactory.class, "words", stopWords, "ignoreCase", "true")
          .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false");
    } catch (IOException e) {
      LOG.error("Error when initializing QueryAnalyzer", e);
    }
    return builder;
  }

  private Analyzer createAnalyzer() {
    try {
      CustomAnalyzer.Builder builder
          = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class);
      Analyzer analyzer = addTokenFilters(builder).build();
      Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
      analyzerMap.put(FieldNames.ARTIST_FULL, getKeywordAnalyzer());
      analyzerMap.put(FieldNames.ARTIST_READING_HIRAGANA, getKeywordAnalyzer());
      analyzerMap.put(FieldNames.ALBUM_FULL, getKeywordAnalyzer());
      analyzerMap.put(FieldNames.GENRE, getKeywordAnalyzer());
      analyzerMap.put(FieldNames.MEDIA_TYPE, getKeywordAnalyzer());
      analyzerMap.put(FieldNames.FOLDER, getKeywordAnalyzer());
      PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(analyzer, analyzerMap);
      return wrapper;
    } catch (IOException e) {
      LOG.error("Error when initializing Analyzer", e);
    }
    return null;
  }

  private Analyzer createQueryAnalyzer(boolean isHiraOrKata) {
    try {
      CustomAnalyzer.Builder builder = CustomAnalyzer.builder()
          .withTokenizer(
              isHiraOrKata
              ? WhitespaceTokenizerFactory.class
              : JapaneseTokenizerFactory.class);
      Analyzer analyzer =
          addTokenFilters(builder).addTokenFilter(PatternReplaceFilterFactory.class, "pattern",
          "(^.*(?<!\\*))", "replacement", "$1\\*", "replace", "all").build();
      Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
      analyzerMap.put(FieldNames.ARTIST_FULL, getKeywordQueryAnalyzer());
      analyzerMap.put(FieldNames.ARTIST_READING_HIRAGANA, getKeywordQueryAnalyzer());
      analyzerMap.put(FieldNames.ALBUM_FULL, getKeywordQueryAnalyzer());
      analyzerMap.put(FieldNames.GENRE, getKeywordQueryAnalyzer());
      analyzerMap.put(FieldNames.MEDIA_TYPE, getKeywordQueryAnalyzer());
      analyzerMap.put(FieldNames.FOLDER, getKeywordQueryAnalyzer());
      PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(analyzer, analyzerMap);
      return wrapper;
    } catch (IOException e) {
      LOG.error("Error when initializing QueryAnalyzer", e);
    }
    return null;
  }

  /**
   * Return analyzer.
   * @return analyzer for index
   */
  public Analyzer getAnalyzer() {
    if (null == analyzer) {
      analyzer = createAnalyzer();
    }
    return analyzer;
  }

  private Analyzer getHiraKataQueryAnalyzer() {
    if (null == hiraKataQueryAnalyzer) {
      hiraKataQueryAnalyzer = createQueryAnalyzer(true);
    }
    return hiraKataQueryAnalyzer;
  }

  private Analyzer getKeywordAnalyzer() {
    if (null == keywordAnalyzer) {
      try {
        CustomAnalyzer.Builder builder =
            CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class);
        addTokenFilters(builder);
        keywordAnalyzer = builder.build();
      } catch (IOException e) {
        LOG.error("Error when initializing QueryAnalyzer", e);
      }
    }
    return keywordAnalyzer;
  }

  private Analyzer getKeywordQueryAnalyzer() {
    if (null == keywordQueryAnalyzer) {
      try {
        CustomAnalyzer.Builder builder =
            CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class);
        keywordQueryAnalyzer =
            addTokenFilters(builder)
            .addTokenFilter(PatternReplaceFilterFactory.class,
                "pattern", "(^.*(?<!\\*))",
                "replacement", "$1\\*",
                "replace", "all")
            .build();
      } catch (IOException e) {
        LOG.error("Error when initializing QueryAnalyzer", e);
      }
    }
    return keywordQueryAnalyzer;
  }

  private Analyzer getQueryAnalyzer() {
    if (null == queryAnalyzer) {
      queryAnalyzer = createQueryAnalyzer(false);
    }
    return queryAnalyzer;
  }

  /**
   * Return query analyzer.
   * 
   * Based on the 100.0.0 implementation.
   * Paramater will disappear in future corrections
   * @param query Query string
   * @return query analyzer
   */
  public Analyzer getQueryAnalyzer(String query) {
    if (isHiraOrKata(query)) {
      return getHiraKataQueryAnalyzer();
    }
    return getQueryAnalyzer();
  }

}
