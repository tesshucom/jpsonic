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

import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.tesshu.jpsonic.service.search.IndexType.ALBUM_ID3;
import static com.tesshu.jpsonic.service.search.IndexType.ARTIST_ID3;
import static com.tesshu.jpsonic.service.search.IndexType.normalizeGenre;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Factory class of Lucene Query.
 * This class is a division of what was once part of SearchService
 * and added functionality.
 **/
/*
 * Generate queries that have roughly the same meaning as old queries.
 * However, the implementation version differs significantly from legacy services.
 * Improvements have been made to 
 * "parts with minor differences in API" and
 * "query that is originally unsuitable for specification".
 */
public class QueryFactory {

	private QueryFactory() {
	}

	public static Query createQuery(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {

		String[] targetFields = Arrays.stream(indexType.getFields())
				.filter(f -> !f.equals(FieldNames.FOLDER))
				.toArray(s -> new String[s]);

		Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(criteria.getQuery());

		MultiFieldQueryParser parser = new MultiFieldQueryParser(targetFields, analyzer);

		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		try {
			builder.add(parser.parse(criteria.getQuery()), Occur.MUST);
		} catch (ParseException e) {
			LoggerFactory.getLogger(QueryFactory.class).error("Error during query analysis.", e);
		}

		SpanQuery[] musicFolderQueries = musicFolders.stream().map(musicFolder -> {
			if (indexType == ALBUM_ID3 || indexType == ARTIST_ID3) {
				byte[] bytes = new byte[Integer.BYTES];
				NumericUtils.intToSortableBytes(musicFolder.getId(), bytes, 0);
				BytesRef ref = new BytesRef(bytes);
				return new SpanTermQuery(new Term(FieldNames.FOLDER_ID, ref));
			} else {
				return new SpanTermQuery(
						new Term(FieldNames.FOLDER, musicFolder.getPath().getPath()));
			}
		}).toArray(i -> new SpanQuery[i]);

		builder.add(new SpanOrQuery(musicFolderQueries), Occur.MUST);

		return builder.build();

	}

	public static Query createQuery(RandomSearchCriteria criteria) {

		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		builder.add(new TermQuery(new Term(FieldNames.MEDIA_TYPE, MediaType.MUSIC.name().toLowerCase())), Occur.MUST);

		if (!isEmpty(criteria.getGenre())) {
			builder.add(new TermQuery(
					new Term(FieldNames.GENRE, normalizeGenre.apply(criteria.getGenre()))), Occur.MUST);
		}

		/*
		 * @see IntPoint#newRangeQuery
		 */
		if(!(isEmpty(criteria.getFromYear()) && isEmpty(criteria.getToYear()))) {
			builder.add(IntPoint.newRangeQuery(
					FieldNames.YEAR,
					isEmpty(criteria.getFromYear()) ? Integer.MIN_VALUE : criteria.getFromYear(),
					isEmpty(criteria.getToYear()) ? Integer.MAX_VALUE : criteria.getToYear()),
					Occur.MUST);
		}

		SpanQuery[] musicFolderQueries = criteria.getMusicFolders().stream()
				.map(musicFolder -> new SpanTermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())))
				.toArray(i -> new SpanQuery[i]);
		builder.add(new SpanOrQuery(musicFolderQueries), Occur.MUST);

		return builder.build();
	}

	public static Query searchByName(String name, String field) {
		Analyzer queryAnalyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(name);
		QueryParser queryParser = new QueryParser(field, queryAnalyzer);
		Query parsedQuery = null;
		try {
			parsedQuery = queryParser.parse(name);
		} catch (ParseException e) {
			LoggerFactory.getLogger(QueryFactory.class).error("Error during query analysis.", e);
		}
		return parsedQuery;
	}

	public static Query searchByNameMusicFolderPath(List<MusicFolder> musicFolders) {
		SpanQuery[] musicFolderQueries = musicFolders.stream()
				.map(musicFolder -> new SpanTermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())))
				.toArray(i -> new SpanQuery[i]);
		return new SpanOrQuery(musicFolderQueries);
	}
	
	public static Query searchByNameMusicFolderIds(List<MusicFolder> musicFolders) {
		SpanQuery[] musicFolderQueries = musicFolders.stream()
				.map(musicFolder -> {
					byte[] bytes = new byte[Integer.BYTES];
					NumericUtils.intToSortableBytes(musicFolder.getId(), bytes, 0);
					BytesRef ref = new BytesRef(bytes);
					return new SpanTermQuery(new Term(FieldNames.FOLDER_ID, ref));
				}).toArray(i -> new SpanQuery[i]);
		return new SpanOrQuery(musicFolderQueries);
	}

}
